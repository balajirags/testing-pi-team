import type { ExtensionAPI } from "@earendil-works/pi-coding-agent";
import { execSync } from "child_process";
import * as fs from "fs";
import * as path from "path";

const DEFAULT_PROTECTED_PATTERNS = [
  /\.env($|\..*)/i,
  /\.bashrc/i,
  /\.zshrc/i,
  /\.bash_profile/i,
  /\.zprofile/i,
  /id_rsa/i,
  /id_ed25519/i,
  /\.aws\/credentials/i,
  /\.ssh\//i,
  /\.gnupg\//i,
  /\.kube\/config/i
];

const DESTRUCTIVE_COMMAND_PATTERNS = [
  /\brm\s+.*$/i,
  /\bgit\s+push\s+.*--force\b/i,
  /\bgit\s+push\s+.*-f\b/i,
  /\bgit\s+reset\s+--hard\b/i,
  /\bgit\s+clean\s+.*-f/i
];

interface SecurityConfig {
  destructiveCommandPolicy?: "ask-human" | "block" | "allow";
  customProtectedFiles?: string[];
}

function loadSecurityConfig(): SecurityConfig {
  const configPath = path.join(process.cwd(), ".pi", "team-config.json");
  if (fs.existsSync(configPath)) {
    try {
      const config = JSON.parse(fs.readFileSync(configPath, "utf-8"));
      return {
        destructiveCommandPolicy: config.destructiveCommandPolicy || "ask-human",
        customProtectedFiles: config.customProtectedFiles || []
      };
    } catch {
      // Default
    }
  }
  return { destructiveCommandPolicy: "ask-human", customProtectedFiles: [] };
}

function logSecurityEvent(eventRecord: any): void {
  try {
    const eventsPath = path.join(process.cwd(), ".pi", "events.jsonl");
    fs.mkdirSync(path.dirname(eventsPath), { recursive: true });
    let lines: string[] = [];
    if (fs.existsSync(eventsPath)) {
      lines = fs.readFileSync(eventsPath, "utf-8").trim().split("\n").filter(Boolean);
    }
    lines.push(JSON.stringify(eventRecord));
    if (lines.length > 1000) lines = lines.slice(-1000);
    fs.writeFileSync(eventsPath, lines.join("\n") + "\n", "utf-8");
  } catch {
    // Ignore write errors
  }
}

function notifyOrchestrator(message: string): void {
  try {
    const configPath = path.join(process.cwd(), ".pi", "team-config.json");
    if (fs.existsSync(configPath)) {
      const config = JSON.parse(fs.readFileSync(configPath, "utf-8"));
      const orchPane = config.panes?.orchestrator;
      if (orchPane) {
        const escaped = message.replace(/"/g, '\\"');
        execSync(`tmux send-keys -t ${orchPane} "${escaped}" Enter`, { stdio: "ignore" });
      }
    }
  } catch {
    // Ignore notify errors
  }
}

export function registerSecurityGuard(pi: ExtensionAPI) {
  if (typeof (pi as any).on === "function") {
    (pi as any).on("tool_call", async (event: any) => {
      const toolName = event?.toolName || event?.name;
      const params = event?.params || event?.args;
      const config = loadSecurityConfig();

      // Combine default patterns with custom protected files
      const customPatterns = (config.customProtectedFiles || []).map(file => new RegExp(file.replace(/[.*+?^${}()|[\]\\]/g, '\\$&'), "i"));
      const allProtectedPatterns = [...DEFAULT_PROTECTED_PATTERNS, ...customPatterns];

      // Check 1: Protected File Reading Check
      let targetText = "";
      if (toolName === "read" && params?.path) {
        targetText = String(params.path);
      } else if (toolName === "bash" && params?.command) {
        targetText = String(params.command);
      }

      if (targetText) {
        for (const pattern of allProtectedPatterns) {
          if (pattern.test(targetText)) {
            const blockMsg = `🛑 ACCESS DENIED: Reading or modifying sensitive environment files or system credentials (${targetText}) is strictly prohibited by DevSquad Security Guard.`;
            logSecurityEvent({
              eventId: `evt_${Date.now()}`,
              type: "security_blocked",
              reason: "protected_file_access",
              target: targetText,
              timestamp: new Date().toISOString()
            });
            notifyOrchestrator(`⚠️ SECURITY BLOCKED: Prohibited access attempt to protected file/credential '${targetText}'.`);
            throw new Error(blockMsg);
          }
        }
      }

      // Check 2: Destructive Command Policy Check for bash tool
      if (toolName === "bash" && params?.command) {
        const cmd = String(params.command);
        const isDestructive = DESTRUCTIVE_COMMAND_PATTERNS.some(pat => pat.test(cmd));

        if (isDestructive) {
          const policy = config.destructiveCommandPolicy || "ask-human";

          if (policy === "block") {
            const blockMsg = `🛑 SECURITY POLICY VIOLATION: Destructive command '${cmd}' is blocked by DevSquad Security Guard policy (destructiveCommandPolicy: "block").`;
            logSecurityEvent({
              eventId: `evt_${Date.now()}`,
              type: "security_blocked",
              reason: "destructive_command_blocked",
              command: cmd,
              timestamp: new Date().toISOString()
            });
            notifyOrchestrator(`⚠️ SECURITY BLOCKED: Destructive command '${cmd}' was blocked by security policy.`);
            throw new Error(blockMsg);
          } else if (policy === "ask-human") {
            const humanAlert = `\n==============================================================\n` +
              `⚠️ HUMAN APPROVAL REQUIRED FOR DESTRUCTIVE COMMAND\n` +
              `==============================================================\n` +
              `Command: '${cmd}'\n` +
              `Policy: destructiveCommandPolicy = "ask-human"\n` +
              `Do you approve this command? Type 'yes' or 'no' in Pane 0.\n` +
              `==============================================================\n`;

            logSecurityEvent({
              eventId: `evt_${Date.now()}`,
              type: "destructive_command_approval_requested",
              command: cmd,
              timestamp: new Date().toISOString()
            });

            notifyOrchestrator(humanAlert);

            throw new Error(`🛑 DESTRUCTIVE COMMAND PAUSED: Command '${cmd}' requires human approval in Pane 0 before execution.`);
          }
        }
      }
    });
  }
}
