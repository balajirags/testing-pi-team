import type { ExtensionAPI } from "@earendil-works/pi-coding-agent";
import { registerBacklogTool } from "./backlog-tool.js";
import { registerStatusBanner } from "./status-banner.js";
import { TmuxManager } from "./tmux-manager.js";
import * as fs from "fs";
import * as path from "path";

export interface TeamConfig {
  mode: "step-hitl" | "loop-hitl" | "auto";
  panes: {
    orchestrator: string;
    ba: string;
    developer: string;
    qa: string;
    reviewer: string;
  };
}

export default function (pi: ExtensionAPI) {
  registerBacklogTool(pi);
  registerStatusBanner(pi);

  const tmux = new TmuxManager();

  // Command 1: /team-dev
  pi.registerCommand("team-dev", {
    description: "Launch multi-agent team workflow in tmux (Master Orchestrator, BA, Dev, QA, Reviewer). Options: --team-mode=loop|auto|step",
    handler: async (args, ctx) => {
      if (!tmux.isTmuxAvailable()) {
        ctx.ui.notify("Error: tmux is required to run multi-agent team mode.", "error");
        return;
      }

      // Parse control mode: --team-mode=loop | --team-mode=auto | --team-mode=step
      let mode: TeamConfig["mode"] = "loop-hitl"; // default loop-hitl
      const argStr = args?.toLowerCase() || "";

      if (argStr.includes("team-mode=auto") || argStr.includes("--auto") || argStr.includes("auto")) {
        mode = "auto";
      } else if (argStr.includes("team-mode=step") || argStr.includes("--step") || argStr.includes("step")) {
        mode = "step-hitl";
      } else if (argStr.includes("team-mode=loop") || argStr.includes("--loop") || argStr.includes("loop")) {
        mode = "loop-hitl";
      }

      ctx.ui.notify(`Initializing Pi Agent Team [--team-mode=${mode}]...`, "info");

      const panes = tmux.setup5PaneLayout();

      // Write team-config.json so backlog tool can auto-steer transitions across panes
      const configPath = path.join(process.cwd(), ".pi", "team-config.json");
      fs.mkdirSync(path.dirname(configPath), { recursive: true });
      const teamConfig: TeamConfig = { mode, panes };
      fs.writeFileSync(configPath, JSON.stringify(teamConfig, null, 2), "utf-8");

      // Reset active-task.json to idle state so no auto-steering triggers prematurely
      const activeTaskPath = path.join(process.cwd(), ".pi", "active-task.json");
      fs.writeFileSync(activeTaskPath, JSON.stringify({
        activeIssueId: "",
        status: "awaiting-human-input",
        updatedBy: "system",
        timestamp: new Date().toISOString()
      }, null, 2), "utf-8");

      // Check if project-context.md defines local app start command and launch Window 1 (app-server)
      try {
        const appWindow = tmux.startAppServerWindow("echo '=== APP SERVER WINDOW ==='; exec bash");
        ctx.ui.notify(`App server window ready at '${appWindow}'`, "info");
      } catch {
        // Non-fatal
      }

      // Initial greeting prompt in Orchestrator Pane 0: Display interactive welcome menu and STOP to wait for human instruction
      tmux.sendPromptToPane(
        panes.orchestrator,
        `Master Orchestrator online [--team-mode=${mode}]! Print the welcome banner and options (1. Implement BRD/PRD, 2. Pick a specific Story/Issue, 3. Random Implementation, 4. Custom Task/Bug Fix). DO NOT call send_agent_message or start any work automatically. STOP IMMEDIATELY and wait for the human user in Pane 0 to enter their instruction.`
      );

      ctx.ui.notify(`Team setup complete in 5-pane tmux session 'pi-team' [--team-mode=${mode}]. Attach with: tmux attach -t pi-team`, "info");
    }
  });

  // Command 2: /team-recover (Recover any accidentally closed pane)
  pi.registerCommand("team-recover", {
    description: "Recover any closed agent pane in the active tmux workspace",
    handler: async (_args, ctx) => {
      const configPath = path.join(process.cwd(), ".pi", "team-config.json");
      if (!fs.existsSync(configPath)) {
        ctx.ui.notify("Error: team-config.json not found. Run /team-dev first.", "error");
        return;
      }

      try {
        const config: TeamConfig = JSON.parse(fs.readFileSync(configPath, "utf-8"));
        const recoveredPanes = tmux.recoverMissingPanes(config.panes);
        config.panes = recoveredPanes;
        fs.writeFileSync(configPath, JSON.stringify(config, null, 2), "utf-8");
        ctx.ui.notify("Pane recovery check complete!", "info");
      } catch (err: any) {
        ctx.ui.notify(`Recovery failed: ${err.message}`, "error");
      }
    }
  });
}
