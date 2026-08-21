> Owner: `brd` agent · Path: `docs/brd/campaigns-management.md`

## Meta

| Field | Value |
|---|---|
| Title | Campaigns Management |
| Jira Epic | `<TBD>` |
| PRD | `docs/prd/performance-marketing-mvp.md` |
| Status | Draft |
| Assignee (dev) | |

## 1. Business objective

Allow users to create and manage campaigns and map incoming spend/conversion events to the correct campaign for reporting and optimization.

## 2. In scope

- CRUD for Campaigns.
- Campaign metadata: name, budget, start/end, status, channel (Meta/Google), external_campaign_id, brand link, ad account link.
- Mapping logic: external provider campaign IDs → internal `campaign_id`.
- Simple import UI for batch campaign creation (CSV) for onboarding.

## 3. Out of scope

- Advanced campaign lifecycle automation and bid management.

## 4. Actors

- Performance Marketer (creates/manages campaigns)
- Growth Ops (bulk imports, mappings)

## 5. Business rules

1. Campaign budgets are nominal values expressed in currency; system stores currency and value.
2. Campaigns must belong to a brand and be linked to an ad account.
3. Incoming events must reference an existing campaign mapping; events without a matching campaign are rejected.

## 6. Data touched

| Entity / concept | Read / Write | Notes |
|---|---|---|
| campaigns | Read/Write | Core campaign metadata and mapping fields |
| brands | Read | Ensure brand association |
| ad_accounts | Read | Ensure account association |

## 7. Dependencies

| Depends on | Why |
|---|---|
| Brands management | To associate campaigns with brands |
| Ad accounts management | To link campaigns to provider accounts |
| Ingestion pipeline | To map events to campaigns |

## 8. Acceptance themes

1. Users can create campaigns with brand and ad account associations.
2. Incoming events map to campaigns when mapping exists; unmapped events appear for review.

## 9. Traceability to PRD

| PRD section | Covered how |
|---|---|
| 7 (Scope) | Entities: Campaigns |
+