import type { ExtensionAPI } from "@earendil-works/pi-coding-agent";
import { Type } from "@sinclair/typebox";
import { execSync } from "child_process";
import * as fs from "fs";
import * as path from "path";

export interface ActiveTaskState {
  activeIssueId: string;
  status: "ba-in-progress" | "ready-for-dev" | "dev-in-progress" | "qa-verifying" | "code-review" | "done" | "dev-rework" | "loop-halted" | "circuit-breaker-tripped";
  updatedBy: string;
  notes?: string;
  reworkCount?: number;
  timestamp: string;
}

function appendEventLog(eventRecord: any): void {
  try {
    const eventsPath = path.join(process.cwd(), ".pi", "events.jsonl");
    fs.mkdirSync(path.dirname(eventsPath), { recursive: true });

    let lines: string[] = [];
    if (fs.existsSync(eventsPath)) {
      lines = fs.readFileSync(eventsPath, "utf-8").trim().split("\n").filter(Boolean);
    }

    lines.push(JSON.stringify(eventRecord));

    // Cap at 1000 events max (auto-truncation log rotation) to prevent file bloat
    if (lines.length > 1000) {
      lines = lines.slice(-1000);
    }

    fs.writeFileSync(eventsPath, lines.join("\n") + "\n", "utf-8");
  } catch {
    // Ignore log write error
  }
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

      // Read previous active-task state to track reworkCount
      let previousReworkCount = 0;
      if (fs.existsSync(activeTaskPath)) {
        try {
          const prevState: ActiveTaskState = JSON.parse(fs.readFileSync(activeTaskPath, "utf-8"));
          if (prevState.activeIssueId === params.issueId) {
            previousReworkCount = prevState.reworkCount || 0;
          }
        } catch {
          // Ignore
        }
      }

      let currentReworkCount = previousReworkCount;
      if (params.newStatus === "dev-rework") {
        currentReworkCount += 1;
      }

      const state: ActiveTaskState = {
        activeIssueId: params.issueId,
        status: params.newStatus as ActiveTaskState["status"],
        updatedBy: ctx.cwd || "agent",
        notes: params.notes,
        reworkCount: currentReworkCount,
        timestamp: new Date().toISOString()
      };

      // Check circuit breaker
      let circuitBreakerTripped = false;
      if (currentReworkCount > 3) {
        state.status = "circuit-breaker-tripped";
        circuitBreakerTripped = true;
      }

      fs.writeFileSync(activeTaskPath, JSON.stringify(state, null, 2), "utf-8");

      // Write structured JSON audit checkpoint
      if (params.issueId) {
        try {
          const checkpointDir = path.join(process.cwd(), "docs", "dev-checkpoints");
          fs.mkdirSync(checkpointDir, { recursive: true });
          const checkpointPath = path.join(checkpointDir, `${params.issueId}.json`);
          fs.writeFileSync(checkpointPath, JSON.stringify({
            storyId: params.issueId,
            status: state.status,
            updatedBy: state.updatedBy,
            notes: params.notes || "",
            reworkCount: currentReworkCount,
            timestamp: state.timestamp
          }, null, 2), "utf-8");
        } catch {
          // Ignore checkpoint write error
        }
      }

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

            // Helper to send keys with optional context clear (with delay after /clear to prevent dropped prompts)
            const sendPromptAsync = async (paneTarget: string, promptText: string, clear = true) => {
              try {
                if (clear) {
                  execSync(`tmux send-keys -t ${paneTarget} "/clear" Enter`, { stdio: "ignore" });
                  await new Promise(resolve => setTimeout(resolve, 1500));
                }
                const escaped = promptText.replace(/"/g, '\\"');
                execSync(`tmux send-keys -t ${paneTarget} "${escaped}" Enter`, { stdio: "ignore" });
              } catch {
                // Ignore if tmux session not attached
              }
            };

            // Handle circuit breaker alert
            if (circuitBreakerTripped) {
              const alertMsg = `⚠️ ALERT: Issue #${params.issueId} has exceeded 3 rework attempts! REWORK_CIRCUIT_BREAKER_TRIPPED. Workflow is PAUSED. Please intervene in Pane 0 to guide Developer or review changes.`;
              await sendPromptAsync(panes.orchestrator, alertMsg, false);
              return {
                content: [{ type: "text", text: `Updated task #${params.issueId} status to 'circuit-breaker-tripped' (rework count: ${currentReworkCount})` }],
                details: state
              };
            }

            // Auto-steer transitions according to mode
            if (params.newStatus === "ready-for-dev" && (mode === "loop-hitl" || mode === "auto")) {
              const prompt = `Issue #${params.issueId} is ready for development. Read project-context.md, checkout main, pull latest main, checkout branch feature/issue-${params.issueId}, implement code inside backend/ or frontend/, run tests & verify coverage, commit, push, and update status to 'qa-verifying'.`;
              await sendPromptAsync(panes.developer, prompt, true);
            } else if (params.newStatus === "qa-verifying" && (mode === "loop-hitl" || mode === "auto")) {
              const prompt = `Issue #${params.issueId} is ready for QA verification. Checkout feature branch feature/issue-${params.issueId}, verify app server is running on localhost port, execute live HTTP/UI AC checks, and update status.`;
              await sendPromptAsync(panes.qa, prompt, true);
            } else if (params.newStatus === "code-review" && (mode === "loop-hitl" || mode === "auto")) {
              const prompt = `Issue #${params.issueId} is ready for Code Review. Inspect git diff feature/issue-${params.issueId} vs main, verify P1/P2 standards, merge branch to main, push main, close tracker issue, and update status to 'done'.`;
              await sendPromptAsync(panes.reviewer, prompt, true);
            } else if (params.newStatus === "dev-rework" && (mode === "loop-hitl" || mode === "auto")) {
              const prompt = `Issue #${params.issueId} requires rework (attempt ${currentReworkCount}/3). Read feedback notes: '${params.notes || "Check review/QA comments"}', fix issues in backend/ or frontend/, re-run build-verify, commit, push, and update status to 'qa-verifying'.`;
              await sendPromptAsync(panes.developer, prompt, false);
            } else if (params.newStatus === "done") {
              // Automated fallback: close GitHub Issue on tracker if issue ID is numeric
              if (params.issueId && /^\d+$/.test(params.issueId.trim())) {
                try {
                  execSync(`gh issue close ${params.issueId} --comment "Completed and merged to main by DevSquad AI"`, { stdio: "ignore" });
                } catch {
                  // Ignore if gh CLI not logged in or non-github tracker
                }
              }

              // Send end-of-story summary card to Pane 0
              const summaryCard = `\n==============================================================\n` +
                `🎉 STORY #${params.issueId} COMPLETE & MERGED TO MAIN\n` +
                `==============================================================\n` +
                `• Active Issue: #${params.issueId}\n` +
                `• Status: done\n` +
                `• Notes: ${params.notes || "PR merged successfully"}\n` +
                `• Audit Log: docs/dev-checkpoints/${params.issueId}.json\n` +
                `==============================================================\n`;

              await sendPromptAsync(panes.orchestrator, summaryCard, false);

              if (mode === "loop-hitl") {
                // STRICT LOOP-HITL HALT: Stop and wait for human input at story completion
                state.status = "loop-halted";
                fs.writeFileSync(activeTaskPath, JSON.stringify(state, null, 2), "utf-8");

                const prompt = `[LOOP-HITL HALT] The 1-story loop for #${params.issueId} has completed end-to-end. Workflow is HALTED. Type 'next' or press Enter to pick up and develop the next story in the backlog.`;
                await sendPromptAsync(panes.orchestrator, prompt, false);
              } else if (mode === "auto") {
                const prompt = `Story #${params.issueId} is COMPLETE & MERGED! [AUTO MODE] Picking up next story from backlog for BA grooming/dev.`;
                await sendPromptAsync(panes.ba, prompt, true);
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
            await new Promise(resolve => setTimeout(resolve, 1500));
          } catch {
            // Ignore if tmux send-keys fails
          }
        }

        const formatted = `[DIRECT MESSAGE to ${params.targetRole.toUpperCase()}]: ${params.message}`;
        execSync(`tmux send-keys -t ${targetPane} "${formatted.replace(/"/g, '\\"')}" Enter`, { stdio: "ignore" });

        // Log direct message to .pi/events.jsonl with auto-truncation cap
        appendEventLog({
          eventId: `evt_${Date.now()}`,
          type: "direct_message",
          targetRole: params.targetRole,
          message: params.message,
          timestamp: new Date().toISOString()
        });

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

  // Tool 3: get_team_events (Persistent Event Bus Inspection)
  pi.registerTool({
    name: "get_team_events",
    label: "Get Team Events",
    description: "Read recent status update events and inter-agent messages from the persistent event log (.pi/events.jsonl)",
    parameters: Type.Object({
      limit: Type.Optional(Type.Number({ description: "Number of recent events to return (default: 10)" }))
    }),
    async execute(toolCallId, params, signal, onUpdate, ctx) {
      const eventsPath = path.join(process.cwd(), ".pi", "events.jsonl");
      if (!fs.existsSync(eventsPath)) {
        return {
          content: [{ type: "text", text: "No events logged in .pi/events.jsonl yet." }],
          details: { events: [] }
        };
      }

      try {
        const lines = fs.readFileSync(eventsPath, "utf-8").trim().split("\n").filter(Boolean);
        const limit = params.limit || 10;
        const recentLines = lines.slice(-limit);
        const events = recentLines.map(line => {
          try {
            return JSON.parse(line);
          } catch {
            return { raw: line };
          }
        });

        return {
          content: [{ type: "text", text: `Retrieved ${events.length} recent team events:\n` + JSON.stringify(events, null, 2) }],
          details: { events }
        };
      } catch (err: any) {
        return {
          content: [{ type: "text", text: `Error reading events: ${err.message}` }],
          details: { events: [], error: err.message }
        };
      }
    }
  });
}
