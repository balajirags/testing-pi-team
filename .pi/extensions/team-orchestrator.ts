import type { ExtensionAPI } from "@earendil-works/pi-coding-agent";
import { registerBacklogTool } from "./backlog-tool.js";
import { TmuxManager } from "./tmux-manager.js";

export default function (pi: ExtensionAPI) {
  registerBacklogTool(pi);

  const tmux = new TmuxManager();

  pi.registerCommand("team-dev", {
    description: "Launch multi-agent team workflow in tmux (Master Orchestrator, BA, Dev, QA, Reviewer)",
    handler: async (args, ctx) => {
      if (!tmux.isTmuxAvailable()) {
        ctx.ui.notify("Error: tmux is required to run multi-agent team mode.", "error");
        return;
      }

      const isHitl = !args?.includes("--mode=auto");
      ctx.ui.notify(`Initializing Pi Agent Team (${isHitl ? "HITL Mode" : "Autonomous Mode"})...`, "info");

      const panes = tmux.setup5PaneLayout();

      // Trigger initial BA prompt in Pane 1
      tmux.sendPromptToPane(
        panes.ba,
        "Read prd.md (or docs/brd/) and project-context.md. Run Auto Re-slice and break down requirements into atomic stories/issues with Gherkin ACs."
      );

      ctx.ui.notify("Team setup complete in 5-pane tmux session 'pi-team'. Attach with: tmux attach -t pi-team", "info");
    }
  });
}
