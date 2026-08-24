import * as fs from "fs";
import * as path from "path";
import type { ITrackerAdapter, TrackerIssue } from "./tracker-interface.js";

export class MarkdownStoryAdapter implements ITrackerAdapter {
  public readonly name = "Markdown Local Stories";

  private getStoriesDir(): string {
    const storiesDir = path.join(process.cwd(), "docs", "stories");
    fs.mkdirSync(storiesDir, { recursive: true });
    return storiesDir;
  }

  private findStoryFiles(dir: string, targetId: string): string[] {
    let results: string[] = [];
    if (!fs.existsSync(dir)) return results;

    const numMatch = targetId.match(/\b\d+\b/);
    const num = numMatch ? numMatch[0] : null;

    const list = fs.readdirSync(dir);
    list.forEach(file => {
      const filePath = path.join(dir, file);
      const stat = fs.statSync(filePath);
      if (stat && stat.isDirectory()) {
        results = results.concat(this.findStoryFiles(filePath, targetId));
      } else if (file.endsWith(".md")) {
        if (file.includes(targetId) || (num && file.includes(num))) {
          results.push(filePath);
        }
      }
    });
    return results;
  }

  public async createIssue(title: string, body: string, options?: any): Promise<TrackerIssue> {
    const epicSlug = options?.epic || "general";
    const dir = path.join(this.getStoriesDir(), epicSlug);
    fs.mkdirSync(dir, { recursive: true });

    const num = Date.now().toString().slice(-4);
    const filename = `${num}-${title.toLowerCase().replace(/[^a-z0-9]+/g, "-")}.md`;
    const filePath = path.join(dir, filename);

    const content = `# Story #${num}: ${title}\n\nStatus: Open\n\n${body}\n`;
    fs.writeFileSync(filePath, content, "utf-8");

    return {
      issueId: num,
      title,
      status: "Open",
      url: filePath
    };
  }

  public async updateStatus(issueId: string, status: string, notes?: string): Promise<void> {
    const files = this.findStoryFiles(this.getStoriesDir(), issueId);
    files.forEach(file => {
      try {
        let content = fs.readFileSync(file, "utf-8");
        const formattedStatus = status === "done" || status === "closed" ? "Closed" : status;
        content = content.replace(/Status:\s*(Open|In\s*Progress|Ready|Pending|qa-verifying|code-review)/i, `Status: ${formattedStatus}`);
        if (notes) {
          content += `\n\n> **Status Note (${new Date().toISOString()})**: ${notes}\n`;
        }
        fs.writeFileSync(file, content, "utf-8");
      } catch {
        // Ignore
      }
    });
  }

  public async closeIssue(issueId: string, comment?: string): Promise<void> {
    await this.updateStatus(issueId, "Closed", comment || "Story closed");
  }

  public async getIssue(issueId: string): Promise<TrackerIssue | null> {
    const files = this.findStoryFiles(this.getStoriesDir(), issueId);
    if (files.length === 0) return null;

    try {
      const content = fs.readFileSync(files[0], "utf-8");
      const titleMatch = content.match(/^#\s+(.+)$/m);
      const title = titleMatch ? titleMatch[1] : issueId;
      const statusMatch = content.match(/Status:\s*(.+)$/m);
      const status = statusMatch ? statusMatch[1].trim() : "Open";

      return {
        issueId,
        title,
        status,
        body: content,
        url: files[0]
      };
    } catch {
      return null;
    }
  }

  public async listUncompletedIssues(): Promise<TrackerIssue[]> {
    const results: TrackerIssue[] = [];
    const scanDir = (dir: string) => {
      if (!fs.existsSync(dir)) return;
      const list = fs.readdirSync(dir);
      list.forEach(file => {
        const filePath = path.join(dir, file);
        const stat = fs.statSync(filePath);
        if (stat && stat.isDirectory()) {
          scanDir(filePath);
        } else if (file.endsWith(".md")) {
          try {
            const content = fs.readFileSync(filePath, "utf-8");
            const statusMatch = content.match(/Status:\s*(.+)$/m);
            const status = statusMatch ? statusMatch[1].trim().toLowerCase() : "open";
            if (status !== "closed" && status !== "done") {
              const titleMatch = content.match(/^#\s+(.+)$/m);
              results.push({
                issueId: path.basename(file, ".md"),
                title: titleMatch ? titleMatch[1] : file,
                status,
                url: filePath
              });
            }
          } catch {
            // Ignore
          }
        }
      });
    };

    scanDir(this.getStoriesDir());
    return results;
  }
}
