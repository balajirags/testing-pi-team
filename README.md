# DevSquad AI — Autonomous Multi-Agent Engineering Squad (`devsquad`)

**DevSquad AI** turns Pi Agent into an AI-native 5-agent software development squad operating concurrently in a tmux workspace (either 5 split panes in 1 window or 5 full-screen windows in the same session):

1. **🤖 Master Orchestrator (Pane 0 / Window 0)**: Central Human-in-the-Loop (HITL) command room. Displays interactive options (*"Implement BRD"*, *"Pick Story #42"*, *"Random Implementation"*, *"Custom Task"*), delegates tasks to child agents via `send_agent_message`, and manages state transitions.
2. **📋 BA - Business Analyst (Pane 1 / Window 1)**: Auto re-slices PRDs/BRDs into atomic user stories with Gherkin Acceptance Criteria (ACs), creating tickets in GitHub Issues, Jira, or local story files.
3. **💻 Developer - Fullstack (Pane 2 / Window 2)**: Implements code and unit tests following a 12-phase flow with strict directory boundary enforcement (`backend/`, `frontend/`), mandatory JaCoCo XML/HTML coverage report verification, and required git commit & push before handoff.
4. **🧪 QA - Quality Analyst (Pane 3 / Window 3)**: Verifies ACs against a **running live application server** (`http://localhost:8080` or `5173`) using live HTTP requests in a cold-read isolated context.
5. **🔍 Code Reviewer (Pane 4 / Window 4)**: Performs cold-read P1/P2 security & architectural audits on branch diffs, merges approved PRs to `main`, and signals task completion.

Based on the philosophy from [AI-Native Development Specifications](https://aishippingblog.com/p/ai-native-development-specifications).

---

## ⚙️ Configuration (`.pi/team-config.json`)

Configure team workflow modes and workspace layouts directly in `.pi/team-config.json`:

```json
{
  "mode": "loop-hitl",
  "layout": "panes"
}
```

### Workflow Control Modes (`"mode"`)
- **`"loop-hitl"` (Loop Human-In-The-Loop - Default)**:
  Runs a single story automatically through BA → Dev → QA → Reviewer → Done, then halts at Master Orchestrator Pane 0 awaiting human instruction before picking up the next story.
- **`"auto"` (Full Autonomous)**:
  Processes all backlog stories sequentially end-to-end without pausing until no uncompleted stories remain.
- **`"step-hitl"` (Step Human-In-The-Loop)**:
  Pauses for human approval at EVERY stage transition (BA → HITL → Dev → HITL → QA → HITL → Reviewer → HITL).

### Workspace Layouts (`"layout"`)
- **`"panes"` (Default Split View)**:
  Launches all 5 agents concurrently as split panes in 1 window so you can watch team collaboration in real time.
- **`"windows"` (Full-Screen Windows)**:
  Launches all 5 agents as separate full-screen windows inside the same tmux session (`pi-team`). Switch full-screen agent views using `Ctrl+b` + `0..4`.

---

## 🤖 Interactive Startup & Task Allocation

When you launch `/team-start`, Master Orchestrator in Pane 0 displays the welcome menu options and **STOPs to wait for your instruction**:

```text
==============================================================
🚀 DEVSQUAD AI ONLINE [--mode=loop-hitl, --layout=panes]
==============================================================
Please select an option or state your request:

1. 📄 Implement a BRD / PRD (e.g., 'Groom & implement docs/brd/campaigns.md')
2. 📌 Pick a specific Story / Issue (e.g., 'Implement Story #12')
3. 🎲 Random Implementation (e.g., 'Pick a random story / random feature')
4. 🛠️ Custom Task / Bug Fix (e.g., 'Fix NPE in auth controller')
==============================================================
```

### Mandatory Startup Rules
1. **Idle Child Agents**: On startup, all child agents (BA, Dev, QA, Reviewer) start in a **100% idle and silent state** without checking previous files or auto-running background work.
2. **Human First Command Gate**: Orchestrator waits for the human operator in Pane 0 to enter the first instruction before dispatching work.
3. **Strict Delegation Mandate**: Orchestrator NEVER creates story files or writes code itself—it routes requirements to the BA agent (`ba`) in Pane 1 or Developer (`developer`) in Pane 2 via `send_agent_message`.

---

## 🚀 Team Commands

### `/team-start`
Starts a fresh team session. Resets `.pi/active-task.json` log to clean idle state (`status: "awaiting-human-input"`), sets up the tmux workspace layout (`panes` or `windows`), launches idle child agents, and waits for human input in Pane 0.

### `/team-resume`
Resumes an active team session. Reads `.pi/active-task.json` log to recover task state (`activeIssueId`, `status`, `notes`), recovers/opens the workspace layout, and presents the active recovery banner in Pane 0 to guide next steps.

### `/team-recover`
Recovers any closed or crashed agent pane/window in the active tmux workspace.

### `/team-dev`
Backward-compatible alias to `/team-start`.

---

## 🧹 Targeted Context Clearing (`/clear`)

To maintain clean context windows without destroying mid-task feedback:
- **New Task Assignment**: `/clear` is automatically executed on a child agent pane ONLY when starting a brand-new story assignment (`ready-for-dev`, `qa-verifying`, `code-review`).
- **QA & Reviewer Isolation**: QA and Code Reviewer always evaluate code in a cold, fresh context.
- **Rework Context Preservation**: Dev rework tasks (`dev-rework`) do NOT clear Developer memory so feedback notes are preserved.
- **Master Orchestrator**: Pane 0 context is never wiped.

---

## 📋 Prerequisites

Before running DevSquad AI, ensure you have installed:

- **Node.js**: v20 or later
- **Pi Coding Agent**: Installed globally
  ```bash
  npm install -g @earendil-works/pi-coding-agent
  ```
- **tmux**: Required for multi-pane/window visualization (v3.2+)
  ```bash
  # macOS
  brew install tmux

  # Ubuntu/Debian
  sudo apt install tmux
  ```
- **GitHub CLI (`gh`)** *(Required if using GitHub Issues as Delivery Tracker)*:
  ```bash
  brew install gh
  gh auth login
  ```

---

## 🚀 Quickstart Guide

### Step 1: Install DevSquad AI
```bash
npm install -g devsquad
```

### Step 2: Initialize in Any Repository
Navigate to your project repository and run `devsquad init`:

```bash
cd ~/projects/my-company-app
devsquad init
```

### Step 3: Configure `project-context.md` and `.pi/team-config.json`
Set your repository boundaries, build/test commands, and delivery tracker in `project-context.md` and `.pi/team-config.json`.

### Step 4: Launch DevSquad AI
```bash
devsquad start
```

### Step 5: Attach to the Tmux Workspace
```bash
devsquad attach
```

#### Tmux 5-Pane Split Layout (`"layout": "panes"`):
```
+------------------------------------------------------------------------+
| Pane 0: [🤖 MASTER ORCHESTRATOR] Intent Router & Interactive Control  |
+------------------------------------+-----------------------------------+
| Pane 1: [📋 BUSINESS ANALYST (BA)] | Pane 2: [💻 FULLSTACK DEV (DEV)]  |
| pi -a ba                           | pi -a developer                   |
+------------------------------------+-----------------------------------+
| Pane 3: [🧪 QUALITY ANALYST (QA)]  | Pane 4: [🔍 CODE REVIEWER]        |
| pi -a qa                           | pi -a reviewer                    |
+------------------------------------+-----------------------------------+
```

#### Tmux 5-Window Layout (`"layout": "windows"`):
- `Window 0`: `orchestrator` (`pi -a orchestrator`)
- `Window 1`: `ba` (`pi -a ba`)
- `Window 2`: `developer` (`pi -a developer`)
- `Window 3`: `qa` (`pi -a qa`)
- `Window 4`: `reviewer` (`pi -a reviewer`)
- `Window 5`: `app-server` (Application Server)
