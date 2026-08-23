# Story: Create Campaign API

> Owner: Business Analyst · Path: `docs/stories/campaigns-management/01-create-campaign-backend.md`

| Field | Value |
|---|---|
| Epic | Campaigns Management |
| BRD | `docs/brd/campaigns-management.md` |
| Label | Backend |
| Spec | `docs/specs/campaigns-management.md` |
| Order | 1 |
| Depends on | None |
| Status | Done |

## Description

As a **Performance Marketer**, I want **an API endpoint to create new marketing campaigns with brand and ad account links**, so that **I can establish internal campaign records and external channel mapping rules**.

## Spec coverage

- `POST /api/v1/campaigns`

## Acceptance Criteria

| Scenario | Given | When | Then |
|---|---|---|---|
| Happy path campaign creation | A valid brand ID, ad account ID, name "Q1 Retargeting", budget 5000.00 USD, channel "META", and external campaign ID "meta_12345" | `POST /api/v1/campaigns` is called with request body | HTTP 201 Created is returned with internal `id`, status `DRAFT`, and full created entity payload |
| Invalid brand or ad account | A request with non-existent `brandId` or `adAccountId` | `POST /api/v1/campaigns` is called | HTTP 400 Bad Request / 404 Not Found is returned with ProblemDetail describing invalid association |
| Missing mandatory fields | A request missing `name`, `channel`, or `externalCampaignId` | `POST /api/v1/campaigns` is called | HTTP 400 Bad Request is returned with validation field errors |
| Duplicate external campaign mapping | An active campaign already exists with `channel="META"` and `externalCampaignId="meta_12345"` | `POST /api/v1/campaigns` is called with same channel and external ID | HTTP 409 Conflict is returned indicating duplicate mapping |

## Assumptions

- Brand and Ad Account entities exist in the database or mock context.
- Currency defaults to USD if omitted.

## Open Questions

- None
