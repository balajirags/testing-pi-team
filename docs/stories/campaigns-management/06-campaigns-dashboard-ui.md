# Story: Campaigns Dashboard UI

> Owner: Business Analyst · Path: `docs/stories/campaigns-management/06-campaigns-dashboard-ui.md`

| Field | Value |
|---|---|
| Epic | Campaigns Management |
| BRD | `docs/brd/campaigns-management.md` |
| Label | UI |
| Spec | `docs/specs/campaigns-management.md` |
| Order | 6 |
| Depends on | 01-create-campaign-backend, 02-list-get-campaigns-backend |
| Status | Done |

## Description

As a **Performance Marketer**, I want **a web interface to view, filter, create, and manage campaigns**, so that **I can visually monitor campaigns and update settings easily**.

## Spec coverage

- React frontend view `/campaigns`

## Acceptance Criteria

| Scenario | Given | When | Then |
|---|---|---|---|
| Display campaign table | Navigating to `/campaigns` with existing campaigns | Page renders | Table displays campaigns with columns: Name, Brand, Ad Account, Channel, Budget, Status, External ID, and Action buttons |
| Filter by channel | Campaigns with channels META and GOOGLE exist | User selects "META" from Channel dropdown filter | Table updates to display only META campaigns |
| Open Create Campaign modal | On `/campaigns` page | User clicks "+ New Campaign" button | Modal opens with form inputs for Name, Brand, Ad Account, Channel, Budget, Dates, and External ID |
| Submit Create Campaign form | Modal form is filled with valid data | User clicks "Save Campaign" | Form submits via API, modal closes, toast notification shows success, and table refreshes |
| Edit campaign inline or via modal | An existing campaign in the table | User clicks "Edit" action | Modal opens populated with campaign data; saving updates table row |

## Assumptions

- React + Vite app with Tailwind / UI component library.

## Open Questions

- None
