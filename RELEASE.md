# RELEASE NOTES — DevSquad AI v1.0.0

**DevSquad AI** turns Pi Agent into an AI-native 5-agent software development squad operating concurrently in a tmux workspace (either 5 split panes in 1 window or 5 full-screen windows in the same session).

---

## 🚀 Distribution & Team Adoption Guide

### Method 1: Global / Registry Installation (Recommended)

#### Step 1: Install `devsquad` globally
```bash
npm install -g devsquad
# Or from local package link:
npm link
```

#### Step 2: Navigate to ANY target project repository
```bash
cd ~/projects/my-company-app
```

#### Step 3: Run `devsquad init`
```bash
devsquad init
```
`devsquad init` automatically bootstraps the target repo with:
* `.pi/` — Agent personas (`orchestrator`, `ba`, `developer`, `qa`, `reviewer`), extensions, and skills.
* `Agent.md` — Operational mandate and workspace context.
* `project-context.md` — Template for build commands, test suites, and source directory boundaries.
* `docs/brd/` & `docs/stories/` — Template folders for requirements and user stories.

#### Step 4: Configure `project-context.md` & `.pi/team-config.json`
Edit `project-context.md` to specify your build/test commands and source boundaries (`backend/`, `frontend/`). Edit `.pi/team-config.json` to select your preferred mode and layout:

```json
{
  "mode": "loop-hitl",
  "layout": "panes"
}
```

#### Step 5: Launch DevSquad
```bash
devsquad start
```

Attach to the tmux workspace:
```bash
devsquad attach
```

---

### Method 2: Zero-Install via `npx`

If published to an internal or public npm registry, teams can run without installing globally:

```bash
cd ~/projects/my-company-app

# 1. Bootstrap context and templates
npx devsquad init

# 2. Launch DevSquad AI squad
npx devsquad start

# 3. Attach to tmux workspace
npx devsquad attach
```

---

### Method 3: Direct Git Copy (Self-Contained Repo Share)

Teams can also share DevSquad directly inside a git repo by copying 3 items:
1. `.pi/` directory
2. `Agent.md` file
3. `bin/` directory

Running `./bin/devsquad.js start` inside the repo immediately launches the 5-agent squad.

---

## 🌟 Key Features in v1.0.0

### 1. `devsquad` Standalone CLI Executable (`bin/devsquad.js`)
* **`devsquad start`**: Starts a fresh DevSquad session, resets `.pi/active-task.json`, launches workspace.
* **`devsquad resume`**: Resumes active DevSquad session from `.pi/active-task.json` log.
* **`devsquad init`**: Bootstraps `.pi/`, `Agent.md`, `project-context.md`, and template folders into any repository.
* **`devsquad attach`**: Attaches directly to the `devsquad-workspace` tmux session.
* **`devsquad status`**: Displays active task state directly from terminal.
* **`devsquad config`**: Views or updates `.pi/team-config.json` (`mode` and `layout`).
* **`devsquad help`**: Displays CLI usage guide.

### 2. Flexible Configuration (`.pi/team-config.json`)
* **Workflow Modes (`mode`)**:
  * `"loop-hitl"` (Default): Runs 1 story end-to-end (BA → Dev → QA → Reviewer → Done) and halts at Master Orchestrator for human approval before picking up the next story.
  * `"auto"`: Processes all backlog stories sequentially end-to-end without pausing.
  * `"step-hitl"`: Pauses for human approval at every stage transition.
* **Workspace Layouts (`layout`)**:
  * `"panes"` (Default): 5 split panes within 1 window (`team`).
  * `"windows"`: 5 full-screen windows within the same session (`orchestrator`, `ba`, `developer`, `qa`, `reviewer`).

### 3. Human First Command Gate & 100% Idle Startup
* All 4 child agents (BA, Dev, QA, Reviewer) start up in a **100% idle and silent state** without checking previous state or running background work.
* Master Orchestrator in Pane 0 displays the welcome menu options and **stops to wait for human input** before dispatching any work.

### 4. Strict Orchestrator Delegation Mandate
* Master Orchestrator is defined as an **INTENT ROUTER AND DELEGATOR ONLY**.
* Orchestrator NEVER creates story files, BRDs/PRDs, GitHub Issues, Jira tickets, or code files itself. All requirement grooming and story creation MUST be delegated to the **BA agent (`ba`)** in Pane 1.

### 5. Targeted Context Clearing (`/clear`)
* `/clear` is automatically executed on a child agent pane ONLY when starting a brand-new story assignment (`ready-for-dev`, `qa-verifying`, `code-review`).
* QA and Code Reviewer always evaluate code in a cold, fresh context.
* Dev rework tasks (`dev-rework`) do NOT clear Developer memory so feedback notes are preserved.

### 6. Rework Loop Circuit Breaker
* Tracks `reworkCount` in `.pi/active-task.json`.
* If `reworkCount > 3`, automatically halts auto-steering, triggers a `REWORK_CIRCUIT_BREAKER_TRIPPED` alert event to Master Orchestrator in Pane 0, and waits for human operator intervention.

### 7. Structured JSON Checkpoint Audit Trail
* Writes structured JSON audit logs for every story at `docs/dev-checkpoints/<story-id>.json`.

### 8. Tmux Terminal UX & Ergonomics
* **Immune Persona Headers**: Custom `@persona` tmux properties lock titles on top of every pane, immune to shell OSC title overrides.
* **Bottom Status Ticker**: Live status bar displaying `🚀 DEVSQUAD AI` badge and timestamp.
* **Instant `Alt` Shortcuts**:
  * `Alt+↑`, `Alt+↓`, `Alt+←`, `Alt+→` — Instant pane navigation (no `Ctrl+b` required).
  * `Alt+0` .. `Alt+4` — Direct jump to Panes 0..4.
  * `Alt+z` — Toggle full-screen zoom on active agent pane.

### 9. QA Self-Managed App Server Verification
* QA agent checks if the local application server (`http://localhost:8080/actuator/health` or `http://localhost:5173`) is running. If offline, QA starts it in the background before executing live API probes.

### 10. End-of-Story Summary Card
* Master Orchestrator receives and displays a formatted completion summary card in Pane 0 upon story completion (`done`).
