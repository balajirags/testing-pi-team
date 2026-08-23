# Story: Deactivate or Delete Campaign API

> Owner: Business Analyst · Path: `docs/stories/campaigns-management/04-delete-campaign-backend.md`

| Field | Value |
|---|---|
| Epic | Campaigns Management |
| BRD | `docs/brd/campaigns-management.md` |
| Label | Backend |
| Spec | `docs/specs/campaigns-management.md` |
| Order | 4 |
| Depends on | 01-create-campaign-backend |
| Status | Done |

## Description

As a **Performance Marketer**, I want **an API endpoint to soft-delete or archive a campaign**, so that **unwanted campaigns are removed from active operations without losing historical reporting data**.

## Spec coverage

- `DELETE /api/v1/campaigns/{id}`

## Acceptance Criteria

| Scenario | Given | When | Then |
|---|---|---|---|
| Soft delete campaign happy path | An existing campaign with ID `cmp_100` | `DELETE /api/v1/campaigns/cmp_100` is called | HTTP 204 No Content is returned and campaign status is set to `ARCHIVED` |
| Delete non-existent campaign | No campaign exists with ID `cmp_999` | `DELETE /api/v1/campaigns/cmp_999` is called | HTTP 404 Not Found is returned |
| Read archived campaign | A campaign with ID `cmp_100` that has been soft deleted | `GET /api/v1/campaigns/cmp_100` is called | HTTP 200 OK is returned with status `ARCHIVED` (or excluded from standard active list queries) |

## Assumptions

- Campaigns are soft-deleted (status set to `ARCHIVED`) to preserve historical event attribution integrity.

## Open Questions

- None
