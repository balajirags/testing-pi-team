# BRD: DevSquad AI — Resilience, Ergonomics, Quality Gates & Telemetry Enhancements

## Overview
This requirement document outlines key architectural enhancements to elevate **DevSquad AI** into an enterprise-grade, resilient, and ergonomic multi-agent development engine. Key capabilities include rework circuit breakers, a standalone CLI binary (`devsquad`), tmux status dashboard tickers, automated app server health gates, and structured story completion telemetry.

---

## 1. Workflow Resilience & Circuit Breakers

### 1.1 Rework Loop Circuit Breaker
- **Requirement**: Track `reworkCount` in `.pi/active-task.json` whenever status transitions to `dev-rework`.
- **Threshold**: If `reworkCount > 3`, automatically halt the auto-steering workflow, trigger a `REWORK_CIRCUIT_BREAKER_TRIPPED` alert event to Master Orchestrator in Pane 0, and wait for human operator intervention.

### 1.2 Structured Checkpoint Audit Trail
- **Requirement**: Write a structured JSON audit log for every story at `docs/dev-checkpoints/<story-id>.json`.
- **Fields**: Include `storyId`, `branch`, `prUrl`, `mergeCommit`, `jacocoCoverage` (line % and branch %), `qaHttpEvidence` (status code, endpoint, response payload), and `reviewerVerdict`.

---

## 2. Standalone CLI Executable (`devsquad`) & Distribution

### 2.1 Package Binary (`bin/devsquad.js`)
- **Requirement**: Register `"bin": { "devsquad": "./bin/devsquad.js" }` in `package.json` for global or `npx` execution.
- **Commands**:
  - `devsquad start`: Initializes fresh session, resets active-task log, launches workspace layout (`panes` or `windows`).
  - `devsquad resume`: Recovers active session from `.pi/active-task.json` log.
  - `devsquad init`: Bootstraps `.pi/` extensions, `Agent.md`, `project-context.md`, and BRD/story template directories into any target repository.
  - `devsquad attach`: Attaches directly to the `pi-team` tmux session.
  - `devsquad status`: Displays current active task state from terminal.
  - `devsquad config`: Displays or updates `.pi/team-config.json` (`mode` and `layout`).

---

## 3. Terminal UX & Ergonomics

### 3.1 Tmux Bottom Status Bar Ticker
- **Requirement**: Configure tmux `status-right` / `status-left` to display a live real-time status ticker:
  ```text
  [ DEVSQUAD AI ] | Mode: LOOP-HITL | Layout: PANES | Active: Issue #12 [DEV in progress]
  ```

### 3.2 Instant Navigation Shortcuts (No `Ctrl+b` Required)
- **Requirement**: Add custom tmux keybindings in `tmux-manager.ts`:
  - `Alt + Arrow keys` (`Alt+↑`, `Alt+↓`, `Alt+←`, `Alt+→`): Move focus to adjacent pane.
  - `Alt + 0..4`: Jump directly to Pane 0 (Orchestrator), Pane 1 (BA), Pane 2 (Dev), Pane 3 (QA), or Pane 4 (Reviewer).
  - `Alt + z`: Toggle full-screen zoom on active agent pane.

---

## 4. Automated Quality & App Server Health Gates

### 4.1 App Server Auto-Health Readiness
- **Requirement**: Before QA agent executes live HTTP probes, Window 1 (`app-server`) automatically verifies application server health (`http://localhost:8080/actuator/health` or `http://localhost:5173`). If offline, starts the app server background process defined in `project-context.md` and waits for `200 OK` health response before signaling QA.

### 4.2 Multi-Tracker Auto-Detection
- **Requirement**: BA and Developer agents auto-detect the tracker specified in `project-context.md`:
  - GitHub Issues (`gh issue create / gh issue list`)
  - Jira (`jira issue create / jira issue view`)
  - Local Markdown files (`docs/stories/<epic-slug>/<story-id>.md`)

---

## 5. Story Completion Telemetry & Summary Report

### 5.1 End-of-Story Summary Report
- **Requirement**: When a story reaches `done` status, Master Orchestrator prints an end-to-end verification summary card in Pane 0:
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
