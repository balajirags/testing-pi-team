# BRD: DevSquad AI — Standalone CLI, Rebranding, Resilience & Distribution Framework

## Executive Summary
This document consolidates all product requirements for **DevSquad AI** into a single master specification. It defines the standalone CLI wrapper (`devsquad`), distribution framework (`devsquad init`), rebranded tmux workspace UI, workflow circuit breakers, QA-managed quality gates, and automated story telemetry.

---

## 1. Standalone CLI Executable (`devsquad`) & Distribution Framework

### 1.1 Package Executable (`bin/devsquad.js`)
- **Requirement**: Register `"bin": { "devsquad": "./bin/devsquad.js" }` in `package.json` for global (`npm install -g devsquad`) or `npx` execution.
- **Commands**:
  - `devsquad start`: Starts fresh session, resets active-task log (`activeIssueId=""`, `status="awaiting-human-input"`), launches layout (`panes` or `windows`).
  - `devsquad resume`: Recovers active session from `.pi/active-task.json` log.
  - `devsquad init`: Bootstraps `.pi/` extensions, `Agent.md`, `project-context.md`, and BRD/story template directories into any target repository.
  - `devsquad attach`: Attaches directly to the `devsquad-workspace` tmux session.
  - `devsquad status`: Displays current active task state from terminal.
  - `devsquad config`: Displays or updates `.pi/team-config.json` (`mode` and `layout`).
  - `devsquad help`: Displays CLI usage guide.

---

## 2. Tmux Workspace & Extension Rebranding

### 2.1 Workspace Rebranding
- **Session Name**: `devsquad-workspace` (formerly `pi-team`).
- **Pane Titles**:
  - Pane 0: `[ DEVSQUAD :: MASTER ORCHESTRATOR ]`
  - Pane 1: `[ DEVSQUAD :: BUSINESS ANALYST (BA) ]`
  - Pane 2: `[ DEVSQUAD :: DEVELOPER (DEV) ]`
  - Pane 3: `[ DEVSQUAD :: QUALITY ANALYST (QA) ]`
  - Pane 4: `[ DEVSQUAD :: CODE REVIEWER (REVIEWER) ]`
- **Commands**: Register `/devsquad-start` and `/devsquad-recover` aliases.

### 2.2 Welcome Banner
- Master Orchestrator in Pane 0 displays the DevSquad welcome banner upon startup:
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

---

## 3. Workflow Resilience & Circuit Breakers

### 3.1 Rework Loop Circuit Breaker
- **Requirement**: Track `reworkCount` in `.pi/active-task.json` whenever status transitions to `dev-rework`.
- **Threshold**: If `reworkCount > 3`, automatically halt auto-steering, trigger a `REWORK_CIRCUIT_BREAKER_TRIPPED` alert event to Master Orchestrator in Pane 0, and wait for human operator intervention.

### 3.2 Structured Checkpoint Audit Trail
- **Requirement**: Write a structured JSON audit log for every story at `docs/dev-checkpoints/<story-id>.json`.
- **Fields**: Include `storyId`, `branch`, `prUrl`, `mergeCommit`, `jacocoCoverage` (line % and branch %), `qaHttpEvidence` (status code, endpoint, response payload), and `reviewerVerdict`.

---

## 4. Terminal UX & Ergonomics

### 4.1 Tmux Bottom Status Bar Ticker
- **Requirement**: Configure tmux `status-right` / `status-left` to display a live real-time status ticker:
  ```text
  [ DEVSQUAD AI ] | Mode: LOOP-HITL | Layout: PANES | Active: Issue #12 [DEV in progress]
  ```

### 4.2 Instant Navigation Shortcuts (No `Ctrl+b` Required)
- **Requirement**: Add custom tmux keybindings in `tmux-manager.ts`:
  - `Alt + Arrow keys` (`Alt+↑`, `Alt+↓`, `Alt+←`, `Alt+→`): Move focus to adjacent pane.
  - `Alt + 0..4`: Jump directly to Pane 0 (Orchestrator), Pane 1 (BA), Pane 2 (Dev), Pane 3 (QA), or Pane 4 (Reviewer).
  - `Alt + z`: Toggle full-screen zoom on active agent pane.

---

## 5. Quality Gates & Single-Session Control

### 5.1 QA Agent Self-Managed App Server Verification
- **Requirement**: QA agent is sole owner of verifying app server reachability (`http://localhost:8080/actuator/health` or `http://localhost:5173`). If offline, QA starts the background process (`cd backend && ./gradlew bootRun &`) and waits for `200 OK` before proceeding.

### 5.2 In-Session Tmux Workspace Management
- **Requirement**: Orchestrator and `tmux-manager` manage windows (`new-window`, `select-window`) and panes (`split-window`, `select-pane`) **within the active tmux session**. Never execute `tmux new-session` inside an existing session.

### 5.3 Multi-Tracker Auto-Detection
- **Requirement**: BA and Developer agents auto-detect tracker specified in `project-context.md` (GitHub Issues, Jira, or Local Markdown files).

---

## 6. Story Completion Telemetry & Summary Report

### 6.1 End-of-Story Summary Card
- **Requirement**: When a story reaches `done` status, Master Orchestrator prints an end-to-end verification summary card in Pane 0 detailing branch name, PR merge commit ID, JaCoCo coverage %, QA HTTP probe result, and execution duration:
```text
==============================================================
🎉 STORY #12 COMPLETE & MERGED TO MAIN
==============================================================
• Feature Branch: feature/issue-12
• PR Merge Commit: 7f8a12b
• JaCoCo Line Coverage: 85.4% (Threshold: 80%)
• QA Live Probe Evidence: GET /api/v1/hello -> 200 OK [PASS]
• Code Review Verdict: APPROVE (0 P1/P2 findings)
• End-to-End Duration: 4m 12s
==============================================================
```
