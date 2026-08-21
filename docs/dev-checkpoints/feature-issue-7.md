# Implementation Plan - Issue #7: Campaign Batch CSV Import UI

**Branch:** `feature/issue-7`  
**Story:** `docs/stories/campaigns-management/07-csv-import-ui.md`

## Overview
Implement the Campaign Batch CSV Import UI (`CampaignCsvImportModal.tsx`) on the `/campaigns` dashboard, enabling Growth Ops users to upload CSV files, view validation feedback, download sample CSV templates, and review row-level error reports.

## Acceptance Criteria
1. Dashboard includes an "Import CSV" button next to "New Campaign".
2. Clicking "Import CSV" opens a modal containing a file picker/dropzone and a "Download Template" link.
3. Selecting a valid CSV file and clicking "Upload and Process" sends a multipart request to `POST /api/v1/campaigns/import` with a loading indicator.
4. On upload completion, the summary view displays the number of total, created, and failed campaigns.
5. If errors occurred, an error table displays row numbers and specific failure reasons, with a button to download the error log.
6. Successful imports refresh the campaigns table on the dashboard.

## Tasks & Strategy
1. **Types & API Client (`frontend/src/types/campaign.ts`, `frontend/src/api/campaigns.ts`):**
   - Add Zod schemas and TypeScript types for `CsvRowError` and `CsvImportSummaryResponse`.
   - Implement `importCampaignsCsv(file: File): Promise<CsvImportSummaryResponse>` sending multipart form data to `/api/v1/campaigns/import`.
2. **Components (`frontend/src/components/CampaignCsvImportModal.tsx`):**
   - Implement modal component supporting file drop/selection, template generation (`name,brand_id,ad_account_id,budget,currency,channel,external_campaign_id`), progress indicator, summary display, error table, and error log download (`.txt`/`.csv`).
3. **Dashboard Integration (`frontend/src/components/CampaignsTable.tsx`, `frontend/src/pages/CampaignsDashboard.tsx`):**
   - Add "Import CSV" button to `CampaignsTable` toolbar.
   - Connect modal open/close state and React Query cache invalidation upon completion.
4. **Testing & Quality Verification:**
   - Add unit tests in `src/test/api.test.ts` for CSV upload function.
   - Add component tests in `src/test/CampaignsDashboard.test.tsx` and `src/test/CampaignCsvImportModal.test.tsx`.
   - Run `npx tsc --noEmit`.
   - Run `npm test -- --coverage`.
   - Run `npm run build`.
