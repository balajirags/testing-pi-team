# Agent Team Context & Operational Mandate (`pi-team`)

This repository is powered by an AI-native 5-agent team operating concurrently in a 5-pane tmux workspace. Every agent session initialized in this directory must adhere to the rules, context, and operational parameters defined below.

---

## 🏛️ Team Architecture & Pane Layout

```
+------------------------------------------------------------------------+
| Pane 0: [MASTER ORCHESTRATOR] Intent-Based Router & Interactive Control|
| Command: pi -a orchestrator                                            |
+------------------------------------+-----------------------------------+
| Pane 1: [BUSINESS ANALYST (BA)]    | Pane 2: [DEVELOPER (DEV)]         |
| Command: pi -a ba                  | Command: pi -a developer          |
+------------------------------------+-----------------------------------+
| Pane 3: [QUALITY ANALYST (QA)]     | Pane 4: [CODE REVIEWER (REVIEWER)]|
| Command: pi -a qa                  | Command: pi -a reviewer           |
+------------------------------------+-----------------------------------+
```

- **Pane 0: Master Orchestrator** (`orchestrator`): Central Human-in-the-Loop command room. Greets the user, parses intent, allocates tasks to subagents via `send_agent_message`, and controls state transitions.
- **Pane 1: Business Analyst** (`ba`): Auto re-slices PRDs/BRDs into atomic user stories with Gherkin Acceptance Criteria (ACs). Creates issues/tickets in GitHub Issues, Jira, or local markdown files.
- **Pane 2: Fullstack Developer** (`developer`): Implements code and unit tests inside strict directory boundaries (`backend/` or `frontend/`). Reads JaCoCo XML/HTML coverage reports to enforce quality thresholds before commit & push.
- **Pane 3: Quality Analyst** (`qa`): Validates story ACs against a **RUNNING LIVE SYSTEM** (`http://localhost:8080` or `5173`) using HTTP probes and records evidence.
- **Pane 4: Code Reviewer** (`reviewer`): Performs cold-read P1/P2 security & architectural audits on branch diffs, merges approved PRs to `main`, and signals task completion.

---

## 🤖 Orchestrator Control & Task Allocation

Master Orchestrator (Pane 0) is the single entry point for human instructions.
🚫 **STRICT DELEGATION RULE**: Master Orchestrator NEVER creates story files, BRDs/PRDs, GitHub Issues, or code files itself. Orchestrator delegates ALL requirement grooming and story creation to the **BA agent (`ba`)** in Pane 1 via `send_agent_message`.

Upon session launch via `/team-dev`, Orchestrator displays a menu with 4 core choices:

1. **Implement a BRD / PRD**:
   - Human request: *"Proceed with docs/brd/campaigns.md"* or *"Groom the PRD"*
   - Delegation: Orchestrator sends instructions to BA (Pane 1) to read BRD, run Auto Re-slice, create stories with Gherkin ACs, and update status to `ready-for-dev`.
2. **Pick a Specific Story / Issue**:
   - Human request: *"Implement Story #42"*
   - Delegation: Orchestrator checks story status. If `ready-for-dev`, dispatches Developer (Pane 2) to implement directly. If ungroomed, dispatches BA first.
3. **Random Implementation**:
   - Human request: *"Pick a random story"* or *"Random feature"*
   - Delegation: Orchestrator scans `docs/stories/`, `docs/brd/`, and GitHub/Jira issues to pick an uncompleted story or BRD at random. If backlog is empty, Orchestrator synthesizes a feature recommendation based on `project-context.md` and delegates to BA.
4. **Custom Task / Bug Fix**:
   - Human request: *"Fix NPE in auth controller"* or *"Add Redis cache"*
   - Delegation: Orchestrator sends direct task requirements to Developer (Pane 2).

---

## 🎛️ Workflow Modes (`--team-mode`) & Automated Context Clearing

- **`loop-hitl` (Default - Single Story Loop)**:
  Runs a single story end-to-end (BA → Dev → QA → Reviewer → Done). Upon story completion, workflow halts at Orchestrator Pane 0 for human approval before picking up the next story.
- **`auto` (Full Autonomous)**:
  Processes all backlog stories sequentially end-to-end without stopping until no uncompleted stories remain.
- **`step-hitl` (Step Human-in-the-Loop)**:
  Pauses for human approval at every stage transition (BA → HITL → Dev → HITL → QA → HITL → Reviewer → HITL).

### 🧹 Universal Automatic Context Clearing (`/clear`)
In **ALL team modes** (`loop-hitl`, `auto`, and `step-hitl`), the system automatically issues a `/clear` command to target child agent panes whenever a task or message is assigned (`team_update_status` or `send_agent_message`):
- **Developer Pane**: Context automatically cleared before receiving `ready-for-dev` / `dev-rework` assignment.
- **QA Pane**: Context automatically cleared before receiving `qa-verifying` assignment (guaranteeing cold-read isolation).
- **Reviewer Pane**: Context automatically cleared before receiving `code-review` assignment (guaranteeing cold-read isolation).
- **BA Pane**: Context automatically cleared before receiving new story/BRD grooming assignment.

---

## 📐 Project Boundaries & Quality Thresholds

1. **Source Layout Boundaries**:
   - Backend code, Gradle configuration, and tests MUST live in `backend/`.
   - Frontend React/Vite code and config MUST live in `frontend/`.
   - Root level is reserved for team configuration, documentation, and extension files.
2. **Coverage Inspection Gate**:
   - Developer MUST generate and read actual coverage report files (e.g. `backend/build/reports/jacoco/test/jacocoTestReport.xml`) before claiming coverage metrics are satisfied.
3. **Live App Verification Gate**:
   - QA MUST test against a reachable live application server before issuing a pass verdict. Unit test suites alone do NOT satisfy QA verification.
4. **Code Review & Branch Merge Gate**:
   - Reviewer performs P1/P2 audit. Approved code MUST be merged to `main` via `gh pr merge` or `git merge feature/issue-<id>` before status is updated to `done`.
