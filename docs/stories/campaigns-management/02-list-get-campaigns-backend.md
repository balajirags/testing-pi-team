# Story: List and Get Campaign API

> Owner: Business Analyst · Path: `docs/stories/campaigns-management/02-list-get-campaigns-backend.md`

| Field | Value |
|---|---|
| Epic | Campaigns Management |
| BRD | `docs/brd/campaigns-management.md` |
| Label | Backend |
| Spec | `docs/specs/campaigns-management.md` |
| Order | 2 |
| Depends on | 01-create-campaign-backend |
| Status | Done |

## Description

As a **Performance Marketer or Growth Ops user**, I want **API endpoints to search, list, and fetch details for individual campaigns**, so that **I can inspect active campaigns and verify channel mappings**.

## Spec coverage

- `GET /api/v1/campaigns`
- `GET /api/v1/campaigns/{id}`

## Acceptance Criteria

| Scenario | Given | When | Then |
|---|---|---|---|
| List campaigns happy path | Multiple campaigns exist across brands and channels | `GET /api/v1/campaigns` is called | HTTP 200 OK is returned with a paginated list of campaigns |
| Filter campaigns by brand or channel | Campaigns exist for brand `brand_1` and channel `GOOGLE` | `GET /api/v1/campaigns?brandId=brand_1&channel=GOOGLE` is called | HTTP 200 OK is returned containing only matching campaigns |
| Get campaign by ID happy path | A campaign with ID `cmp_100` exists | `GET /api/v1/campaigns/cmp_100` is called | HTTP 200 OK is returned with complete campaign detail payload |
| Get campaign by ID not found | No campaign exists with ID `cmp_999` | `GET /api/v1/campaigns/cmp_999` is called | HTTP 404 Not Found is returned with ProblemDetail error |

## Assumptions

- Default pagination size is 20 items per page.

## Open Questions

- None
