import { execSync } from "child_process";
import type { ITrackerAdapter, TrackerIssue } from "./tracker-interface.js";

export class GitHubIssuesAdapter implements ITrackerAdapter {
  public readonly name = "GitHub Issues";

  private extractNumericId(issueId: string): string {
    const match = issueId.match(/\b\d+\b/);
    return match ? match[0] : issueId;
  }

  public async createIssue(title: string, body: string): Promise<TrackerIssue> {
    try {
      const output = execSync(
        `gh issue create --title "${title.replace(/"/g, '\\"')}" --body "${body.replace(/"/g, '\\"')}"`,
        { encoding: "utf-8" }
      ).trim();

      const issueNum = output.split("/").pop() || "";
      return {
        issueId: issueNum,
        title,
        status: "open",
        url: output
      };
    } catch (err: any) {
      throw new Error(`GitHubIssuesAdapter createIssue failed: ${err.message}`);
    }
  }

  public async updateStatus(issueId: string, status: string, notes?: string): Promise<void> {
    const num = this.extractNumericId(issueId);
    if (!num) return;

    try {
      if (status === "done" || status === "closed") {
        await this.closeIssue(num, notes);
      } else if (notes) {
        execSync(`gh issue comment ${num} --body "Status updated to '${status}': ${notes.replace(/"/g, '\\"')}"`, { stdio: "ignore" });
      }
    } catch {
      // Non-fatal
    }
  }

  public async closeIssue(issueId: string, comment?: string): Promise<void> {
    const num = this.extractNumericId(issueId);
    if (!num) return;

    try {
      const commentFlag = comment ? `--comment "${comment.replace(/"/g, '\\"')}"` : '--comment "Closed by DevSquad AI"';
      execSync(`gh issue close ${num} ${commentFlag}`, { stdio: "ignore" });
    } catch {
      // Non-fatal if gh CLI fails
    }
  }

  public async getIssue(issueId: string): Promise<TrackerIssue | null> {
    const num = this.extractNumericId(issueId);
    if (!num) return null;

    try {
      const jsonStr = execSync(`gh issue view ${num} --json number,title,state,url,body`, { encoding: "utf-8" });
      const data = JSON.parse(jsonStr);
      return {
        issueId: String(data.number),
        title: data.title,
        status: data.state.toLowerCase(),
        body: data.body,
        url: data.url
      };
    } catch {
      return null;
    }
  }

  public async listUncompletedIssues(): Promise<TrackerIssue[]> {
    try {
      const jsonStr = execSync(`gh issue list --state open --json number,title,state,url`, { encoding: "utf-8" });
      const list = JSON.parse(jsonStr);
      return list.map((item: any) => ({
        issueId: String(item.number),
        title: item.title,
        status: item.state.toLowerCase(),
        url: item.url
      }));
    } catch {
      return [];
    }
  }
}
