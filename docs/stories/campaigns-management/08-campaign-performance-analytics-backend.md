# Story: Campaign Performance Analytics API

> Owner: Business Analyst · Path: `docs/stories/campaigns-management/08-campaign-performance-analytics-backend.md`

| Field | Value |
|---|---|
| Epic | Campaigns Management |
| BRD | `docs/brd/campaigns-management.md` |
| Label | Backend |
| Order | 8 |
| Depends on | Story 02 (List & Get Campaign API) |
| Status | Ready for Dev |

## Description

As an **Advertiser or Campaign Manager**,
I want **a REST API endpoint at `GET /api/v1/campaigns/{id}/analytics`**,
so that **I can retrieve campaign performance metrics including daily impressions, clicks, spend, conversions, CTR (Click-Through Rate), and CPC (Cost-Per-Click)**.

## Spec coverage

- `GET /api/v1/campaigns/{id}/analytics`

## Acceptance Criteria

| Scenario | Given | When | Then |
|---|---|---|---|
| Successful analytics retrieval | Campaign with ID exists in database | `GET /api/v1/campaigns/{id}/analytics` is called | HTTP 200 OK is returned with JSON payload containing `summary` (total impressions, clicks, spend, conversions, CTR, CPC) and `dailyBreakdown` list |
| Date range filtering | Campaign with ID exists in database | `GET /api/v1/campaigns/{id}/analytics?startDate=2026-08-01&endDate=2026-08-15` is called | HTTP 200 OK is returned with metrics filtered strictly within the specified date range |
| Safe division handling for zero impressions and clicks | Campaign exists with 0 impressions and 0 clicks | `GET /api/v1/campaigns/{id}/analytics` is called | HTTP 200 OK is returned with `ctr: 0.0` and `cpc: 0.0` without divide-by-zero errors |
| Campaign not found | No campaign exists with specified ID | `GET /api/v1/campaigns/999999/analytics` is called | HTTP 404 Not Found is returned with RFC 7807 ProblemDetail response |
| Invalid date parameter format | Campaign exists | `GET /api/v1/campaigns/{id}/analytics?startDate=invalid-date` is called | HTTP 400 Bad Request is returned with validation details |

## Request Schema

`GET /api/v1/campaigns/{id}/analytics`

Query Parameters (Optional):
- `startDate` (String, ISO-8601 date format `YYYY-MM-DD`)
- `endDate` (String, ISO-8601 date format `YYYY-MM-DD`)

## Response Schema

HTTP 200 OK:

```json
{
  "campaignId": "123e4567-e89b-12d3-a456-426614174000",
  "summary": {
    "totalImpressions": 10000,
    "totalClicks": 250,
    "totalSpend": 312.50,
    "totalConversions": 15,
    "ctr": 2.50,
    "cpc": 1.25
  },
  "dailyBreakdown": [
    {
      "date": "2026-08-01",
      "impressions": 2000,
      "clicks": 50,
      "spend": 62.50,
      "conversions": 3,
      "ctr": 2.50,
      "cpc": 1.25
    }
  ]
}
```

HTTP 404 Not Found:

```json
{
  "type": "about:blank",
  "title": "Campaign Not Found",
  "status": 404,
  "detail": "Campaign with ID 999999 was not found",
  "instance": "/api/v1/campaigns/999999/analytics"
}
```

## Assumptions

- `ctr` (Click-Through Rate) formula: `(totalClicks / totalImpressions) * 100` rounded to 2 decimal places. Returns `0.0` if `totalImpressions == 0`.
- `cpc` (Cost Per Click) formula: `totalSpend / totalClicks` rounded to 2 decimal places. Returns `0.0` if `totalClicks == 0`.
- If no date range parameters are provided, defaults to the last 30 days.

## Open Questions

- None
