# Story: Batch Campaign CSV Import API

> Owner: Business Analyst · Path: `docs/stories/campaigns-management/05-batch-csv-import-backend.md`

| Field | Value |
|---|---|
| Epic | Campaigns Management |
| BRD | `docs/brd/campaigns-management.md` |
| Label | Backend |
| Spec | `docs/specs/campaigns-management.md` |
| Order | 5 |
| Depends on | 01-create-campaign-backend |
| Status | Done |

## Description

As a **Growth Ops user**, I want **an API endpoint to upload a CSV file containing multiple campaign definitions**, so that **I can onboard large numbers of campaigns efficiently in bulk**.

## Spec coverage

- `POST /api/v1/campaigns/import`

## Acceptance Criteria

| Scenario | Given | When | Then |
|---|---|---|---|
| Valid CSV batch import | A multi-part CSV file with 50 valid campaign records (headers: `name`, `brand_id`, `ad_account_id`, `budget`, `currency`, `channel`, `external_campaign_id`) | `POST /api/v1/campaigns/import` is called | HTTP 200 OK is returned with summary report (e.g. `{ "total": 50, "created": 50, "failed": 0, "errors": [] }`) |
| Partial CSV import with row errors | A CSV file with 10 rows where row 3 has an invalid `brand_id` and row 7 is missing `external_campaign_id` | `POST /api/v1/campaigns/import` is called | HTTP 277 or HTTP 200 OK with summary listing 8 created, 2 failed, and specific line-by-line error details |
| Empty or invalid file format | A non-CSV file or empty file | `POST /api/v1/campaigns/import` is called | HTTP 400 Bad Request is returned explaining file validation error |

## Assumptions

- Maximum CSV import batch size is 1,000 rows per request.

## Open Questions

- None
