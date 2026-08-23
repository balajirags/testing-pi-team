# Story: Update Campaign API

> Owner: Business Analyst · Path: `docs/stories/campaigns-management/03-update-campaign-backend.md`

| Field | Value |
|---|---|
| Epic | Campaigns Management |
| BRD | `docs/brd/campaigns-management.md` |
| Label | Backend |
| Spec | `docs/specs/campaigns-management.md` |
| Order | 3 |
| Depends on | 01-create-campaign-backend |
| Status | Done |

## Description

As a **Performance Marketer**, I want **an API endpoint to update campaign parameters (name, budget, dates, status)**, so that **I can adjust running campaign parameters and lifecycle states**.

## Spec coverage

- `PUT /api/v1/campaigns/{id}`

## Acceptance Criteria

| Scenario | Given | When | Then |
|---|---|---|---|
| Happy path campaign update | An existing campaign with ID `cmp_100` | `PUT /api/v1/campaigns/cmp_100` is called with updated budget `10000.00` and status `ACTIVE` | HTTP 200 OK is returned with updated campaign fields |
| Update non-existent campaign | No campaign exists with ID `cmp_999` | `PUT /api/v1/campaigns/cmp_999` is called with valid body | HTTP 404 Not Found is returned |
| Invalid status transition | A campaign in status `COMPLETED` | `PUT /api/v1/campaigns/cmp_100` attempts to transition status back to `DRAFT` | HTTP 400 Bad Request is returned with error message explaining invalid state transition |
| Validation failure on negative budget | An existing campaign with ID `cmp_100` | `PUT /api/v1/campaigns/cmp_100` is called with budget `-500.00` | HTTP 400 Bad Request is returned with validation error |

## Assumptions

- End dates must be equal to or after start dates.

## Open Questions

- None
