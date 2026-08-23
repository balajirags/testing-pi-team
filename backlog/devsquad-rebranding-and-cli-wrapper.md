# BRD: DevSquad AI — Custom CLI Wrapper, Rebranding & Distribution Framework

## Overview
Rebrand `pi-team` into **DevSquad AI** — an AI-native 5-agent software engineering squad operating in a 5-pane tmux workspace. Provide a standalone CLI binary (`devsquad`), a distribution initialization command (`devsquad init`), rebranded tmux workspace borders/banners, and updated operational documentation so DevSquad can be easily distributed to other software development teams.

---

## Key Requirements

### 1. `devsquad` CLI Binary Wrapper (`bin/devsquad.js`)
- Register `"bin": { "devsquad": "./bin/devsquad.js" }` in `package.json`.
- Commands:
  - `devsquad start [--mode=loop|auto|step]`: Launches the 5-pane `devsquad-workspace` session.
  - `devsquad init`: Bootstraps `.pi/` extensions, `Agent.md`, `project-context.md`, and template folders into any target repository.
  - `devsquad attach`: Attaches to the active `devsquad-workspace` tmux session.
  - `devsquad recover`: Recovers any closed or crashed agent pane.
  - `devsquad status`: Shows the active task state from `.pi/active-task.json`.
  - `devsquad help`: Displays CLI usage guide and available options.

### 2. Tmux Workspace & Extension Rebranding
- **Session Name**: `devsquad-workspace` (formerly `pi-team`).
- **Pane Titles**:
  - Pane 0: `[ DEVSQUAD :: MASTER ORCHESTRATOR ]`
  - Pane 1: `[ DEVSQUAD :: BUSINESS ANALYST (BA) ]`
  - Pane 2: `[ DEVSQUAD :: DEVELOPER (DEV) ]`
  - Pane 3: `[ DEVSQUAD :: QUALITY ANALYST (QA) ]`
  - Pane 4: `[ DEVSQUAD :: CODE REVIEWER (REVIEWER) ]`
- **Commands**:
  - Register `/devsquad-start` (aliasing `/team-dev`).
  - Register `/devsquad-recover` (aliasing `/team-recover`).

### 3. Welcome Banner & Agent Prompts
- Master Orchestrator in Pane 0 displays the DevSquad welcome banner upon startup:
```text
==============================================================
🚀 DEVSQUAD AI ONLINE [--team-mode=loop-hitl]
==============================================================
Please select an option or state your request:

1. 📄 Implement a BRD / PRD (e.g., 'Groom & implement docs/brd/campaigns.md')
2. 📌 Pick a specific Story / Issue (e.g., 'Implement Story #12')
3. 🎲 Random Implementation (e.g., 'Pick a random story / random feature')
4. 🛠️ Custom Task / Bug Fix (e.g., 'Fix NPE in auth controller')
==============================================================
```

### 4. Distribution Architecture
- Design `devsquad init` so that any software development team can install DevSquad as a global npm package (`npm install -g devsquad` or `npx devsquad init`) and run the 5-agent squad inside their own repositories with zero friction.
