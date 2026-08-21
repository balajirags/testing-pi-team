# Implementation Plan - Issue #3: Update Campaign API

**Branch:** `feature/issue-3`  
**Story:** `docs/stories/campaigns-management/03-update-campaign-backend.md`

## Overview
Implement backend API endpoint for updating campaign parameters (`PUT /api/v1/campaigns/{id}`) including name, budget, currency, status, start date, and end date.

## Acceptance Criteria
1. `PUT /api/v1/campaigns/{id}` with valid payload updates campaign fields and returns `200 OK` with updated `CampaignResponse`.
2. `PUT /api/v1/campaigns/{id}` with non-existent ID returns `404 Not Found` with ProblemDetail.
3. Invalid status transition (e.g. from `COMPLETED` or `ARCHIVED` back to `DRAFT`) returns `400 Bad Request` with ProblemDetail explanation.
4. Validation failure on negative budget (`budget < 0`) returns `400 Bad Request` with ProblemDetail.
5. End date must be equal to or after start date (`endDate >= startDate`), otherwise returns `400 Bad Request`.

## Tasks & Strategy
1. **Flyway Migration & Schema Update:**
   - Create `V2__add_campaign_dates.sql` adding `start_date` and `end_date` (TIMESTAMP WITH TIME ZONE) to `campaigns` table.
   - Update `CampaignEntity` with `startDate` and `endDate` fields.
2. **Domain & DTO Updates:**
   - Update `CampaignStatus` enum to include `COMPLETED`.
   - Update `CampaignResponse` record to include `startDate` and `endDate`.
   - Update `CreateCampaignRequest` record to optionally accept `startDate` and `endDate`.
   - Create `UpdateCampaignRequest` record with validation constraints (`@DecimalMin("0.00")` for budget).
3. **Exception Handling:**
   - Create `InvalidStateTransitionException` extending `RuntimeException` or handle status transition check in `GlobalExceptionHandler` returning `400 Bad Request`.
4. **Service Layer (`CampaignService`):**
   - Implement `CampaignResponse updateCampaign(UUID id, UpdateCampaignRequest request)`.
   - Enforce status transition rules (cannot transition from `COMPLETED`/`ARCHIVED` to `DRAFT`/`ACTIVE`/`PAUSED`).
   - Enforce date validation rule (`endDate >= startDate`).
5. **Controller Layer (`CampaignController`):**
   - Add `@PutMapping("/{id}")` endpoint for `updateCampaign(@PathVariable UUID id, @Valid @RequestBody UpdateCampaignRequest request)`.
6. **Tests:**
   - Unit tests in `CampaignServiceTest` for update happy path, not found, invalid status transition, invalid dates.
   - Controller tests in `CampaignControllerTest` for HTTP responses (200, 404, 400).
   - Integration test in `CampaignIntegrationTest` for end-to-end update scenario.
7. **Build & Quality Gate Verification:**
   - Run `./gradlew clean build jacocoTestReport`.
   - Verify JaCoCo report XML/HTML for line coverage >= 80% and branch coverage >= 70%.
