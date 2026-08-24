import { execSync } from "child_process";
import type { ITrackerAdapter, TrackerIssue } from "./tracker-interface.js";

export class JiraAdapter implements ITrackerAdapter {
  public readonly name = "Jira";

  public async createIssue(title: string, body: string, options?: any): Promise<TrackerIssue> {
    const project = options?.project || "PROJ";
    try {
      const output = execSync(
        `jira issue create -p "${project}" -t "Story" -s "${title.replace(/"/g, '\\"')}" -b "${body.replace(/"/g, '\\"')}" --no-input`,
        { encoding: "utf-8" }
      ).trim();

      const keyMatch = output.match(/[A-Z0-9]+-\d+/);
      const issueKey = keyMatch ? keyMatch[0] : "JIRA-1";
      return {
        issueId: issueKey,
        title,
        status: "Open",
        url: output
      };
    } catch {
      return {
        issueId: title,
        title,
        status: "Open"
      };
    }
  }

  public async updateStatus(issueId: string, status: string, notes?: string): Promise<void> {
    try {
      if (status === "done" || status === "closed") {
        await this.closeIssue(issueId, notes);
      } else if (notes && issueId.includes("-")) {
        execSync(`jira issue comment add ${issueId} "${notes.replace(/"/g, '\\"')}"`, { stdio: "ignore" });
      }
    } catch {
      // Non-fatal
    }
  }

  public async closeIssue(issueId: string, comment?: string): Promise<void> {
    try {
      if (issueId.includes("-")) {
        if (comment) {
          execSync(`jira issue comment add ${issueId} "${comment.replace(/"/g, '\\"')}"`, { stdio: "ignore" });
        }
        execSync(`jira issue move ${issueId} "Done"`, { stdio: "ignore" });
      }
    } catch {
      // Non-fatal
    }
  }

  public async getIssue(issueId: string): Promise<TrackerIssue | null> {
    try {
      if (!issueId.includes("-")) return null;
      const jsonStr = execSync(`jira issue view ${issueId} --json`, { encoding: "utf-8" });
      const data = JSON.parse(jsonStr);
      return {
        issueId: data.key || issueId,
        title: data.fields?.summary || issueId,
        status: data.fields?.status?.name || "Open",
        body: data.fields?.description || ""
      };
    } catch {
      return null;
    }
  }

  public async listUncompletedIssues(): Promise<TrackerIssue[]> {
    try {
      const jsonStr = execSync(`jira issue list -s~Done --json`, { encoding: "utf-8" });
      const list = JSON.parse(jsonStr);
      return list.map((item: any) => ({
        issueId: item.key,
        title: item.fields?.summary || item.key,
        status: item.fields?.status?.name || "Open"
      }));
    } catch {
      return [];
    }
  }
}
