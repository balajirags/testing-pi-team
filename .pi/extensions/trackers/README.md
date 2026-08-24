# How to Add a New Delivery Tracker Adapter to DevSquad AI

DevSquad AI uses the **Strategy Pattern** to support any Delivery Tracker (GitHub Issues, Jira, Local Markdown Stories, Linear, Azure DevOps, Trello, etc.).

All tracker logic is decoupled behind the `ITrackerAdapter` interface and instantiated dynamically by `TrackerFactory`.

---

## 🏛️ Architecture Overview

```
                      ┌───────────────────────┐
                      │   ITrackerAdapter     │
                      │  (Interface Contract) │
                      └──────────▲────────────┘
                                 │
     ┌───────────────────────────┼───────────────────────────┐
     │                           │                           │
┌────┴─────────────────┐ ┌───────┴──────────────┐ ┌──────────┴─────────────┐
│ GitHubIssuesAdapter  │ │     JiraAdapter      │ │ MarkdownStoryAdapter  │
│ (.pi/extensions/     │ │ (.pi/extensions/     │ │ (.pi/extensions/      │
│  trackers/github-*)  │ │  trackers/jira-*)    │ │  trackers/markdown-*) │
└──────────────────────┘ └──────────────────────┘ └───────────────────────┘
```

---

## 🛠️ Step-by-Step Guide: Adding a New Tracker

To add a new tracker (for example, **Linear** or **Azure DevOps**):

### Step 1: Create the Adapter File
Create a new file in `.pi/extensions/trackers/`:
```text
.pi/extensions/trackers/linear-adapter.ts
```

### Step 2: Implement `ITrackerAdapter`
Import `ITrackerAdapter` and `TrackerIssue` from `./tracker-interface.js` and implement all required methods:

```typescript
import { execSync } from "child_process";
import type { ITrackerAdapter, TrackerIssue } from "./tracker-interface.js";

export class LinearAdapter implements ITrackerAdapter {
  public readonly name = "Linear";

  public async createIssue(title: string, body: string, options?: any): Promise<TrackerIssue> {
    // 1. Execute CLI or REST API call to create ticket
    // 2. Return TrackerIssue object { issueId, title, status, body, url }
  }

  public async updateStatus(issueId: string, status: string, notes?: string): Promise<void> {
    // Update issue state in Linear
    if (status === "done" || status === "closed") {
      await this.closeIssue(issueId, notes);
    }
  }

  public async closeIssue(issueId: string, comment?: string): Promise<void> {
    // Execute CLI or API call to transition issue to Done/Closed
  }

  public async getIssue(issueId: string): Promise<TrackerIssue | null> {
    // Query issue details from Linear API
  }

  public async listUncompletedIssues(): Promise<TrackerIssue[]> {
    // Query open/uncompleted issues from Linear API
  }
}
```

### Step 3: Register in `TrackerFactory`
Open `.pi/extensions/trackers/tracker-factory.ts`:

1. Import your new adapter:
   ```typescript
   import { LinearAdapter } from "./linear-adapter.js";
   ```
2. Instantiate it inside `TrackerFactory`:
   ```typescript
   private static linearAdapter = new LinearAdapter();
   ```
3. Update `getAdapter()` auto-detection logic:
   ```typescript
   if (typeLower.includes("linear")) return TrackerFactory.linearAdapter;
   ```

### Step 4: Configure in `project-context.md`
In any repository using DevSquad AI, set the `Delivery Tracker` row in `project-context.md`:

```markdown
## Repository

| Field | Value |
|---|---|
| Delivery Tracker | Linear |
```

DevSquad AI's `TrackerFactory` will automatically detect `"Linear"`, instantiate `LinearAdapter`, and route all story creation, status updates, and issue closures through your adapter!

---

## 📋 `ITrackerAdapter` Method Specifications

| Method | Parameters | Return Type | Purpose |
| :--- | :--- | :--- | :--- |
| `createIssue` | `(title, body, options)` | `Promise<TrackerIssue>` | Creates a new issue/ticket on the tracker. |
| `updateStatus` | `(issueId, status, notes)` | `Promise<void>` | Updates story status (e.g. `ready-for-dev`, `qa-verifying`, `done`). |
| `closeIssue` | `(issueId, comment)` | `Promise<void>` | Closes or transitions story to `Done`/`Closed`. |
| `getIssue` | `(issueId)` | `Promise<TrackerIssue \| null>` | Queries issue details and current status. |
| `listUncompletedIssues` | `()` | `Promise<TrackerIssue[]>` | Fetches all open/uncompleted stories from the backlog. |
