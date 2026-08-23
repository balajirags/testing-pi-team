import type { ExtensionAPI } from "@earendil-works/pi-coding-agent";
import { Type } from "@sinclair/typebox";
import { execSync } from "child_process";
import * as fs from "fs";
import * as path from "path";

export interface ActiveTaskState {
  activeIssueId: string;
  status: "ba-in-progress" | "ready-for-dev" | "dev-in-progress" | "qa-verifying" | "code-review" | "done" | "dev-rework" | "loop-halted";
  updatedBy: string;
  notes?: string;
  timestamp: string;
}

export function registerBacklogTool(pi: ExtensionAPI) {
  // Tool 1: team_update_status
  pi.registerTool({
    name: "team_update_status",
    label: "Team Update Status",
    description: "Update the status of the current active development task/issue across the agent team",
    parameters: Type.Object({
      issueId: Type.String({ description: "Target GitHub Issue / Jira Ticket ID / Story path" }),
      newStatus: Type.Union([
        Type.Literal("ready-for-dev"),
        Type.Literal("qa-verifying"),
        Type.Literal("code-review"),
        Type.Literal("done"),
        Type.Literal("dev-rework")
      ], { description: "New lifecycle status" }),
      notes: Type.Optional(Type.String({ description: "Summary of actions taken or feedback" }))
    }),
    async execute(toolCallId, params, signal, onUpdate, ctx) {
      const activeTaskPath = path.join(process.cwd(), ".pi", "active-task.json");
      fs.mkdirSync(path.dirname(activeTaskPath), { recursive: true });

      const state: ActiveTaskState = {
        activeIssueId: params.issueId,
        status: params.newStatus as ActiveTaskState["status"],
        updatedBy: ctx.cwd || "agent",
        notes: params.notes,
        timestamp: new Date().toISOString()
      };

      fs.writeFileSync(activeTaskPath, JSON.stringify(state, null, 2), "utf-8");

      // Read team-config.json if available
      const configPath = path.join(process.cwd(), ".pi", "team-config.json");
      if (fs.existsSync(configPath)) {
        try {
          const config = JSON.parse(fs.readFileSync(configPath, "utf-8"));
          const mode = config.mode || "step-hitl";
          const panes = config.panes;

          if (panes) {
            // Send event notification to Master Orchestrator (Pane 0)
            const notifyMsg = `EVENT: Task #${params.issueId} updated to '${params.newStatus}' by ${params.notes ? params.notes : "subagent"}.`;
            try {
              execSync(`tmux send-keys -t ${panes.orchestrator} "${notifyMsg.replace(/"/g, '\\"')}" Enter`, { stdio: "ignore" });
            } catch {
              // Ignore if tmux session not attached
            }

            // Helper to send keys with optional context clear
            const sendPrompt = (paneTarget: string, promptText: string, clear = true) => {
              try {
                if (clear) {
                  execSync(`tmux send-keys -t ${paneTarget} "/clear" Enter`, { stdio: "ignore" });
                }
                const escaped = promptText.replace(/"/g, '\\"');
                execSync(`tmux send-keys -t ${paneTarget} "${escaped}" Enter`, { stdio: "ignore" });
              } catch {
                // Ignore if tmux session not attached
              }
            };

            // Auto-steer transitions according to mode
            if (params.newStatus === "ready-for-dev" && (mode === "loop-hitl" || mode === "auto")) {
              const prompt = `Issue #${params.issueId} is ready for development. Read project-context.md, checkout branch feature/issue-${params.issueId}, implement code inside backend/ or frontend/, run tests & verify coverage, commit, push, and update status to 'qa-verifying'.`;
              sendPrompt(panes.developer, prompt, true);
            } else if (params.newStatus === "qa-verifying" && (mode === "loop-hitl" || mode === "auto")) {
              const prompt = `Issue #${params.issueId} is ready for QA verification. Checkout feature branch, verify app server is running on localhost port, execute live HTTP/UI AC checks, and update status.`;
              sendPrompt(panes.qa, prompt, true);
            } else if (params.newStatus === "code-review" && (mode === "loop-hitl" || mode === "auto")) {
              const prompt = `Issue #${params.issueId} is ready for Code Review. Inspect git diff vs main, verify P1/P2 standards, merge PR to main, and update status to 'done'.`;
              sendPrompt(panes.reviewer, prompt, true);
            } else if (params.newStatus === "dev-rework" && (mode === "loop-hitl" || mode === "auto")) {
              const prompt = `Issue #${params.issueId} requires rework. Read feedback notes: '${params.notes || "Check review/QA comments"}', fix issues in backend/ or frontend/, re-run build-verify, commit, push, and update status to 'qa-verifying'.`;
              sendPrompt(panes.developer, prompt, false);
            } else if (params.newStatus === "done") {
              if (mode === "loop-hitl") {
                // STRICT LOOP-HITL HALT: Stop and wait for human input at story completion
                state.status = "loop-halted";
                fs.writeFileSync(activeTaskPath, JSON.stringify(state, null, 2), "utf-8");

                const prompt = `STORY #${params.issueId} COMPLETE & MERGED! [LOOP-HITL HALT] The 1-story loop has completed end-to-end. Workflow is HALTED. Type 'next' or press Enter to pick up and develop the next story in the backlog.`;
                sendPrompt(panes.orchestrator, prompt, false);
              } else if (mode === "auto") {
                const prompt = `Story #${params.issueId} is COMPLETE & MERGED! [AUTO MODE] Picking up next story from backlog for BA grooming/dev.`;
                sendPrompt(panes.ba, prompt, true);
              }
            }
          }
        } catch {
          // Ignore parse errors
        }
      }

      return {
        content: [{ type: "text", text: `Updated task #${params.issueId} status to '${params.newStatus}'` }],
        details: state
      };
    }
  });

  // Tool 2: send_agent_message (Direct Inter-Agent Messaging)
  pi.registerTool({
    name: "send_agent_message",
    label: "Send Agent Message",
    description: "Send a direct message or feedback to another agent persona pane in tmux (ba, developer, qa, reviewer, orchestrator)",
    parameters: Type.Object({
      targetRole: Type.Union([
        Type.Literal("ba"),
        Type.Literal("developer"),
        Type.Literal("qa"),
        Type.Literal("reviewer"),
        Type.Literal("orchestrator")
      ], { description: "Target agent persona role" }),
      message: Type.String({ description: "Direct feedback, question, or instruction message to send" }),
      clearContext: Type.Optional(Type.Boolean({ description: "Whether to clear target agent conversation context before sending message (default: false)" }))
    }),
    async execute(toolCallId, params, signal, onUpdate, ctx) {
      const configPath = path.join(process.cwd(), ".pi", "team-config.json");
      if (!fs.existsSync(configPath)) {
        return {
          content: [{ type: "text", text: "Error: team-config.json not found. Run /team-dev first." }],
          details: { success: false }
        };
      }

      try {
        const config = JSON.parse(fs.readFileSync(configPath, "utf-8"));
        const targetPane = config.panes?.[params.targetRole];
        if (!targetPane) {
          return {
            content: [{ type: "text", text: `Error: pane for role '${params.targetRole}' not found.` }],
            details: { success: false }
          };
        }

        const shouldClear = params.clearContext === true;
        if (shouldClear) {
          try {
            execSync(`tmux send-keys -t ${targetPane} "/clear" Enter`, { stdio: "ignore" });
          } catch {
            // Ignore if tmux send-keys fails
          }
        }

        const formatted = `[DIRECT MESSAGE to ${params.targetRole.toUpperCase()}]: ${params.message}`;
        execSync(`tmux send-keys -t ${targetPane} "${formatted.replace(/"/g, '\\"')}" Enter`, { stdio: "ignore" });

        return {
          content: [{ type: "text", text: `Sent direct message to ${params.targetRole.toUpperCase()} pane (${targetPane})${shouldClear ? " [context cleared]" : ""}.` }],
          details: { success: true, targetPane, message: params.message, clearedContext: shouldClear }
        };
      } catch (err: any) {
        return {
          content: [{ type: "text", text: `Error sending message: ${err.message}` }],
          details: { success: false, error: err.message }
        };
      }
    }
  });
}
