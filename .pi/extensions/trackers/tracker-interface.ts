export interface TrackerIssue {
  issueId: string;
  title: string;
  status: string;
  body?: string;
  url?: string;
}

export interface ITrackerAdapter {
  readonly name: string;
  createIssue(title: string, body: string, options?: any): Promise<TrackerIssue>;
  updateStatus(issueId: string, status: string, notes?: string): Promise<void>;
  closeIssue(issueId: string, comment?: string): Promise<void>;
  getIssue(issueId: string): Promise<TrackerIssue | null>;
  listUncompletedIssues(): Promise<TrackerIssue[]>;
}
