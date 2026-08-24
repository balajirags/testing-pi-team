import * as fs from "fs";
import * as path from "path";
import type { ITrackerAdapter } from "./tracker-interface.js";
import { GitHubIssuesAdapter } from "./github-adapter.js";
import { JiraAdapter } from "./jira-adapter.js";
import { MarkdownStoryAdapter } from "./markdown-adapter.js";

export class TrackerFactory {
  private static githubAdapter = new GitHubIssuesAdapter();
  private static jiraAdapter = new JiraAdapter();
  private static markdownAdapter = new MarkdownStoryAdapter();

  public static getAdapter(trackerType?: string): ITrackerAdapter {
    if (trackerType) {
      const typeLower = trackerType.toLowerCase();
      if (typeLower.includes("jira")) return TrackerFactory.jiraAdapter;
      if (typeLower.includes("markdown") || typeLower.includes("local") || typeLower.includes("file")) {
        return TrackerFactory.markdownAdapter;
      }
      if (typeLower.includes("github") || typeLower.includes("gh")) {
        return TrackerFactory.githubAdapter;
      }
    }

    // Auto-detect from project-context.md
    const projectContextPath = path.join(process.cwd(), "project-context.md");
    if (fs.existsSync(projectContextPath)) {
      try {
        const content = fs.readFileSync(projectContextPath, "utf-8");
        const match = content.match(/Delivery Tracker\s*\|\s*(.+)$/m);
        if (match && match[1]) {
          const trackerVal = match[1].toLowerCase();
          if (trackerVal.includes("jira")) return TrackerFactory.jiraAdapter;
          if (trackerVal.includes("github") || trackerVal.includes("issues")) {
            return TrackerFactory.githubAdapter;
          }
          if (trackerVal.includes("markdown") || trackerVal.includes("local") || trackerVal.includes("file")) {
            return TrackerFactory.markdownAdapter;
          }
        }
      } catch {
        // Fallback
      }
    }

    // Default fallback to GitHub Issues
    return TrackerFactory.githubAdapter;
  }
}
