# Story: Campaign Batch CSV Import UI

> Owner: Business Analyst · Path: `docs/stories/campaigns-management/07-csv-import-ui.md`

| Field | Value |
|---|---|
| Epic | Campaigns Management |
| BRD | `docs/brd/campaigns-management.md` |
| Label | UI |
| Spec | `docs/specs/campaigns-management.md` |
| Order | 7 |
| Depends on | 05-batch-csv-import-backend, 06-campaigns-dashboard-ui |
| Status | Done |

## Description

As a **Growth Ops user**, I want **a CSV upload dialog in the UI with instant validation feedback and import summary**, so that **I can easily perform bulk campaign onboardings**.

## Spec coverage

- React component `CampaignCsvImportModal` on `/campaigns`

## Acceptance Criteria

| Scenario | Given | When | Then |
|---|---|---|---|
| Open CSV Import Modal | User is on the `/campaigns` dashboard | User clicks "Import CSV" button | Upload modal opens with drag-and-drop file dropzone and template download link |
| Upload valid CSV file | User selects a valid `.csv` file | User clicks "Upload and Process" | Progress indicator displays during upload; on completion, summary screen shows count of imported campaigns |
| Display row error feedback | CSV file contains validation errors on rows 2 and 5 | Upload completes | Modal displays error table listing row numbers and error reasons with option to download error log |

## Assumptions

- Users can download a sample CSV template directly from the modal.

## Open Questions

- None
