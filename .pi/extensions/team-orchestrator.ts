import type { ExtensionAPI } from "@earendil-works/pi-coding-agent";
import { registerBacklogTool } from "./backlog-tool.js";
import { registerStatusBanner } from "./status-banner.js";
import { TmuxManager } from "./tmux-manager.js";
import * as fs from "fs";
import * as path from "path";

export interface TeamConfig {
  mode: "step-hitl" | "loop-hitl" | "auto";
  layout: "panes" | "windows";
  panes: {
    orchestrator: string;
    ba: string;
    developer: string;
    qa: string;
    reviewer: string;
  };
}

function getOrCreateConfig(): TeamConfig {
  const configPath = path.join(process.cwd(), ".pi", "team-config.json");
  let config: TeamConfig = {
    mode: "loop-hitl",
    layout: "panes",
    panes: { orchestrator: "", ba: "", developer: "", qa: "", reviewer: "" }
  };

  if (fs.existsSync(configPath)) {
    try {
      const loaded = JSON.parse(fs.readFileSync(configPath, "utf-8"));
      config = {
        mode: loaded.mode || "loop-hitl",
        layout: loaded.layout || "panes",
        panes: loaded.panes || config.panes
      };
    } catch {
      // Use defaults on parse error
    }
  }

  return config;
}

function saveConfig(config: TeamConfig): void {
  const configPath = path.join(process.cwd(), ".pi", "team-config.json");
  fs.mkdirSync(path.dirname(configPath), { recursive: true });
  fs.writeFileSync(configPath, JSON.stringify(config, null, 2), "utf-8");
}

export default function (pi: ExtensionAPI) {
  registerBacklogTool(pi);
  registerStatusBanner(pi);

  const tmux = new TmuxManager();

  // Handler for fresh start
  const handleTeamStart = async (ctx: any) => {
    if (!tmux.isTmuxAvailable()) {
      ctx.ui.notify("Error: tmux is required to run multi-agent team mode.", "error");
      return;
    }

    const config = getOrCreateConfig();
    ctx.ui.notify(`Initializing DevSquad Team [--mode=${config.mode}, --layout=${config.layout}]...`, "info");

    const panes = tmux.setupLayout(config.layout);
    config.panes = panes;
    saveConfig(config);

    // Reset active-task.json to clean idle state
    const activeTaskPath = path.join(process.cwd(), ".pi", "active-task.json");
    fs.mkdirSync(path.dirname(activeTaskPath), { recursive: true });
    fs.writeFileSync(activeTaskPath, JSON.stringify({
      activeIssueId: "",
      status: "awaiting-human-input",
      updatedBy: "system",
      notes: "Fresh team session started. Awaiting human input in Pane 0.",
      timestamp: new Date().toISOString()
    }, null, 2), "utf-8");

    // Initial greeting prompt in Orchestrator: Display welcome menu and STOP
    tmux.sendPromptToPane(
      panes.orchestrator,
      `Master Orchestrator online [--mode=${config.mode}, --layout=${config.layout}]! Print the welcome banner and options (1. Implement BRD/PRD, 2. Pick a specific Story/Issue, 3. Random Implementation, 4. Custom Task/Bug Fix). DO NOT call send_agent_message or start any work automatically. STOP IMMEDIATELY and wait for the human user in Pane 0 to enter their instruction.`
    );

    ctx.ui.notify(`DevSquad setup complete in tmux session 'devsquad-workspace' [--mode=${config.mode}, --layout=${config.layout}]. Attach with: tmux attach -t devsquad-workspace`, "info");
  };

  // Handler for session resume
  const handleTeamResume = async (ctx: any) => {
    if (!tmux.isTmuxAvailable()) {
      ctx.ui.notify("Error: tmux is required to run multi-agent team mode.", "error");
      return;
    }

    const config = getOrCreateConfig();

    // Read active-task.json if available
    const activeTaskPath = path.join(process.cwd(), ".pi", "active-task.json");
    let activeIssueId = "";
    let activeStatus = "awaiting-human-input";
    let activeNotes = "";

    if (fs.existsSync(activeTaskPath)) {
      try {
        const taskState = JSON.parse(fs.readFileSync(activeTaskPath, "utf-8"));
        activeIssueId = taskState.activeIssueId || "";
        activeStatus = taskState.status || "awaiting-human-input";
        activeNotes = taskState.notes || "";
      } catch {
        // Ignore parse error
      }
    }

    ctx.ui.notify(`Resuming DevSquad Team [--mode=${config.mode}, --layout=${config.layout}, activeIssue=#${activeIssueId}]...`, "info");

    const panes = tmux.setupLayout(config.layout);
    config.panes = panes;
    saveConfig(config);

    // Resume prompt in Orchestrator Pane 0
    const resumePrompt = `Master Orchestrator RESUMED [--mode=${config.mode}, --layout=${config.layout}]! ` +
      `Active task recovered from log: Issue #${activeIssueId || 'None'} (Status: '${activeStatus}'). Notes: '${activeNotes || 'N/A'}'. ` +
      `Print session recovery banner to user, state active task status, and ask the human user in Pane 0 how to proceed or continue.`;

    tmux.sendPromptToPane(panes.orchestrator, resumePrompt);

    ctx.ui.notify(`DevSquad session resumed in tmux 'devsquad-workspace' [Issue #${activeIssueId || 'None'}, Status: ${activeStatus}]. Attach with: tmux attach -t devsquad-workspace`, "info");
  };

  // Command 1: /devsquad-start
  pi.registerCommand("devsquad-start", {
    description: "Start a fresh DevSquad AI session in tmux based on .pi/team-config.json",
    handler: async (_args, ctx) => {
      await handleTeamStart(ctx);
    }
  });

  // Command 2: /devsquad-resume
  pi.registerCommand("devsquad-resume", {
    description: "Resume active DevSquad AI session from .pi/active-task.json log",
    handler: async (_args, ctx) => {
      await handleTeamResume(ctx);
    }
  });

  // Command 3: /devsquad-recover
  pi.registerCommand("devsquad-recover", {
    description: "Recover any closed agent pane or window in the active DevSquad workspace",
    handler: async (_args, ctx) => {
      const configPath = path.join(process.cwd(), ".pi", "team-config.json");
      if (!fs.existsSync(configPath)) {
        ctx.ui.notify("Error: team-config.json not found. Run /devsquad-start first.", "error");
        return;
      }

      try {
        const config: TeamConfig = JSON.parse(fs.readFileSync(configPath, "utf-8"));
        const recoveredPanes = tmux.recoverMissingPanes(config.panes);
        config.panes = recoveredPanes;
        saveConfig(config);
        ctx.ui.notify("Pane recovery check complete!", "info");
      } catch (err: any) {
        ctx.ui.notify(`Recovery failed: ${err.message}`, "error");
      }
    }
  });

  // Aliases for backward compatibility
  pi.registerCommand("team-start", {
    description: "Start fresh team session (alias to /devsquad-start)",
    handler: async (_args, ctx) => { await handleTeamStart(ctx); }
  });

  pi.registerCommand("team-resume", {
    description: "Resume team session (alias to /devsquad-resume)",
    handler: async (_args, ctx) => { await handleTeamResume(ctx); }
  });

  pi.registerCommand("team-dev", {
    description: "Launch team workflow (alias to /devsquad-start)",
    handler: async (_args, ctx) => { await handleTeamStart(ctx); }
  });

  pi.registerCommand("team-recover", {
    description: "Recover team workspace (alias to /devsquad-recover)",
    handler: async (_args, ctx) => {
      const configPath = path.join(process.cwd(), ".pi", "team-config.json");
      if (!fs.existsSync(configPath)) {
        ctx.ui.notify("Error: team-config.json not found. Run /devsquad-start first.", "error");
        return;
      }

      try {
        const config: TeamConfig = JSON.parse(fs.readFileSync(configPath, "utf-8"));
        const recoveredPanes = tmux.recoverMissingPanes(config.panes);
        config.panes = recoveredPanes;
        saveConfig(config);
        ctx.ui.notify("Pane recovery check complete!", "info");
      } catch (err: any) {
        ctx.ui.notify(`Recovery failed: ${err.message}`, "error");
      }
    }
  });
}
