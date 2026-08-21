# Implementation Plan - Issue #6: Campaigns Dashboard UI

**Branch:** `feature/issue-6`  
**Story:** `docs/stories/campaigns-management/06-campaigns-dashboard-ui.md`

## Overview
Implement the frontend Campaigns Dashboard UI at route `/campaigns` using React, Vite, TypeScript, `@tanstack/react-query`, and `zod`.

## Acceptance Criteria
1. Navigating to `/campaigns` renders a table displaying campaigns with columns: Name, Brand, Ad Account, Channel, Budget, Status, External ID, and Action buttons (Edit/Delete).
2. Selecting a channel from the Channel dropdown filter filters the campaign table (e.g. `META`).
3. Clicking "+ New Campaign" opens a modal with inputs for Name, Brand ID, Ad Account ID, Channel, Budget, Start Date, End Date, and External ID.
4. Submitting the Create Campaign form sends a `POST /api/v1/campaigns` request, closes the modal, shows a success toast notification, and invalidates/refreshes the campaigns query cache.
5. Clicking "Edit" action opens a modal populated with campaign details, allowing updating fields via `PUT /api/v1/campaigns/{id}` and refreshing the table.

## Tasks & Strategy
1. **Frontend Infrastructure Setup:**
   - Initialize Vite + React + TypeScript configuration inside `frontend/`.
   - Setup `package.json`, `tsconfig.json`, `vite.config.ts`, `index.html`, and `src/main.tsx`.
   - Configure React Query (`QueryClientProvider`) and Zod schemas for API responses and request validation.
2. **API Layer (`src/api/campaigns.ts`):**
   - Implement typed API client with Zod parsing:
     - `fetchCampaigns(params: { brandId?: string; channel?: string; status?: string; page?: number; size?: number })`
     - `createCampaign(payload: CreateCampaignInput)`
     - `updateCampaign(id: string, payload: UpdateCampaignInput)`
     - `deleteCampaign(id: string)`
3. **UI Components:**
   - `src/components/Header.tsx`: Navigation bar with app title and nav links.
   - `src/components/CampaignsTable.tsx`: Responsive data table with channel filter, pagination, and action triggers.
   - `src/components/CampaignModal.tsx`: Modal form for both creating new campaigns and editing existing campaigns.
   - `src/components/Toast.tsx`: Lightweight notification toast for feedback.
   - `src/pages/CampaignsDashboard.tsx`: Dashboard layout combining filters, table, "+ New Campaign" modal trigger, and notifications.
4. **Unit & Component Testing:**
   - Setup Vitest + `@testing-library/react` inside `frontend/`.
   - Add unit tests for API parsing and `CampaignsDashboard` component rendering, filtering, and modal interaction.
5. **Quality Gate Verification:**
   - Run `npx tsc --noEmit` in `frontend/`.
   - Run `npm test -- --coverage` in `frontend/`.
   - Run `npm run build` in `frontend/`.
