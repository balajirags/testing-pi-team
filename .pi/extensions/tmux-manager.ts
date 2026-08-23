import { execSync } from "child_process";

export interface TmuxPaneMap {
  orchestrator: string; // Pane 0
  ba: string;           // Pane 1
  developer: string;    // Pane 2
  qa: string;           // Pane 3
  reviewer: string;     // Pane 4
}

export class TmuxManager {
  private sessionName = "pi-team";

  public isTmuxAvailable(): boolean {
    try {
      execSync("tmux -V", { stdio: "ignore" });
      return true;
    } catch {
      return false;
    }
  }

  public setup5PaneLayout(): TmuxPaneMap {
    // Kill existing session if present
    try {
      execSync(`tmux kill-session -t ${this.sessionName} 2>/dev/null`, { stdio: "ignore" });
    } catch {
      // Ignore if session didn't exist
    }

    // 1. Create Window 0 (team) with Pane 0: Master Orchestrator Pi Agent
    const orchestratorPane = execSync(
      `tmux new-session -d -s ${this.sessionName} -n team -P -F "#{pane_id}" "pi -a orchestrator; exec bash"`
    ).toString().trim();

    // 2. Split bottom area for BA
    const baPane = execSync(
      `tmux split-window -v -t ${orchestratorPane} -P -F "#{pane_id}" "pi -a ba; exec bash"`
    ).toString().trim();

    // 3. Split bottom area horizontally for DEV
    const devPane = execSync(
      `tmux split-window -h -t ${baPane} -P -F "#{pane_id}" "pi -a developer; exec bash"`
    ).toString().trim();

    // 4. Split Left column vertically for QA
    const qaPane = execSync(
      `tmux split-window -v -t ${baPane} -P -F "#{pane_id}" "pi -a qa; exec bash"`
    ).toString().trim();

    // 5. Split Right column vertically for REVIEWER
    const reviewerPane = execSync(
      `tmux split-window -v -t ${devPane} -P -F "#{pane_id}" "pi -a reviewer; exec bash"`
    ).toString().trim();

    // Configure Pane Persona Titles & Border Styling (using custom @persona property immune to terminal OSC title sequences)
    try {
      execSync(`tmux set-option -t ${this.sessionName} allow-rename off`);
      execSync(`tmux set-window-option -t ${this.sessionName} automatic-rename off`);
      execSync(`tmux set-option -t ${this.sessionName} pane-border-status top`);
      execSync(`tmux set-option -t ${this.sessionName} pane-border-style "fg=blue"`);
      execSync(`tmux set-option -t ${this.sessionName} pane-active-border-style "fg=green,bold"`);
      execSync(`tmux set-option -t ${this.sessionName} pane-border-format "#[fg=black,bg=cyan,bold] #{@persona} #[default]"`);

      execSync(`tmux set-option -p -t ${orchestratorPane} @persona "🤖 MASTER ORCHESTRATOR (Pane 0)"`);
      execSync(`tmux set-option -p -t ${baPane} @persona "📋 BUSINESS ANALYST - BA (Pane 1)"`);
      execSync(`tmux set-option -p -t ${devPane} @persona "💻 FULLSTACK DEVELOPER - DEV (Pane 2)"`);
      execSync(`tmux set-option -p -t ${qaPane} @persona "🧪 QUALITY ANALYST - QA (Pane 3)"`);
      execSync(`tmux set-option -p -t ${reviewerPane} @persona "🔍 CODE REVIEWER - REVIEWER (Pane 4)"`);

      // Resize Orchestrator top pane to 25% height
      execSync(`tmux resize-pane -t ${orchestratorPane} -y 25%`);
    } catch {
      // Non-fatal if styling fails
    }

    return {
      orchestrator: orchestratorPane,
      ba: baPane,
      developer: devPane,
      qa: qaPane,
      reviewer: reviewerPane
    };
  }

  // Create Window 1 for running the Application Server isolated
  public startAppServerWindow(startCommand?: string): string {
    const windowName = "app-server";
    try {
      // Check if window already exists
      const windows = execSync(`tmux list-windows -t ${this.sessionName} -F "#{window_name}"`).toString();
      if (windows.includes(windowName)) {
        return `${this.sessionName}:app-server`;
      }

      const cmd = startCommand || "echo '=== APP SERVER WINDOW ==='; exec bash";
      execSync(`tmux new-window -t ${this.sessionName} -n ${windowName} "${cmd.replace(/"/g, '\\"')}"`);
      return `${this.sessionName}:${windowName}`;
    } catch {
      return `${this.sessionName}:0`;
    }
  }

  // Verify and recover any closed pane
  public recoverMissingPanes(currentPanes: TmuxPaneMap): TmuxPaneMap {
    try {
      const livePanes = execSync(`tmux list-panes -t ${this.sessionName}:0 -F "#{pane_id}"`).toString();
      const updated = { ...currentPanes };

      if (!livePanes.includes(currentPanes.ba)) {
        updated.ba = execSync(`tmux split-window -v -t ${currentPanes.orchestrator} -P -F "#{pane_id}" "pi -a ba; exec bash"`).toString().trim();
        execSync(`tmux set-option -p -t ${updated.ba} @persona "📋 BUSINESS ANALYST - BA (Pane 1)"`);
      }
      if (!livePanes.includes(currentPanes.developer)) {
        updated.developer = execSync(`tmux split-window -h -t ${updated.ba} -P -F "#{pane_id}" "pi -a developer; exec bash"`).toString().trim();
        execSync(`tmux set-option -p -t ${updated.developer} @persona "💻 FULLSTACK DEVELOPER - DEV (Pane 2)"`);
      }
      if (!livePanes.includes(currentPanes.qa)) {
        updated.qa = execSync(`tmux split-window -v -t ${updated.ba} -P -F "#{pane_id}" "pi -a qa; exec bash"`).toString().trim();
        execSync(`tmux set-option -p -t ${updated.qa} @persona "🧪 QUALITY ANALYST - QA (Pane 3)"`);
      }
      if (!livePanes.includes(currentPanes.reviewer)) {
        updated.reviewer = execSync(`tmux split-window -v -t ${updated.developer} -P -F "#{pane_id}" "pi -a reviewer; exec bash"`).toString().trim();
        execSync(`tmux set-option -p -t ${updated.reviewer} @persona "🔍 CODE REVIEWER - REVIEWER (Pane 4)"`);
      }

      return updated;
    } catch {
      return currentPanes;
    }
  }

  public sendPromptToPane(paneTarget: string, promptText: string): void {
    const escaped = promptText.replace(/"/g, '\\"');
    execSync(`tmux send-keys -t ${paneTarget} "${escaped}" Enter`);
  }
}
