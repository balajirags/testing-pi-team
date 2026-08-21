# Implementation Plan - Issue #2: List and Get Campaign API

**Branch:** `feature/issue-2`  
**Story:** `docs/stories/campaigns-management/02-list-get-campaigns-backend.md`

## Overview
Implement backend API endpoints for searching/listing campaigns with optional pagination & filters (`GET /api/v1/campaigns`) and fetching a single campaign by ID (`GET /api/v1/campaigns/{id}`).

## Acceptance Criteria
1. `GET /api/v1/campaigns` returns 200 OK with paginated list of campaigns (default size = 20).
2. `GET /api/v1/campaigns?brandId={brandId}&channel={channel}` returns 200 OK with filtered list matching criteria.
3. `GET /api/v1/campaigns/{id}` returns 200 OK with single `CampaignResponse` when found.
4. `GET /api/v1/campaigns/{id}` returns 404 Not Found with `ProblemDetail` when campaign with given ID is not found.

## Tasks & Strategy
1. **Repository Layer:**
   - Extend `CampaignRepository` with `JpaSpecificationExecutor<CampaignEntity>`.
   - Create specification helper or lambda in service/repository for filtering by `brandId` (optional) and `channel` (optional).
2. **Service Layer (`CampaignService`):**
   - Add `Page<CampaignResponse> listCampaigns(UUID brandId, String channel, Pageable pageable)`.
   - Add `CampaignResponse getCampaignById(UUID id)` throwing `ResourceNotFoundException("Campaign not found with ID: " + id)` when absent.
3. **Controller Layer (`CampaignController`):**
   - Add `@GetMapping` endpoint for `listCampaigns(@RequestParam(required = false) UUID brandId, @RequestParam(required = false) String channel, @PageableDefault(size = 20) Pageable pageable)`.
   - Add `@GetMapping("/{id}")` endpoint for `getCampaignById(@PathVariable UUID id)`.
4. **Unit & Integration Tests:**
   - `CampaignServiceTest`:
     - Test `listCampaigns` with no filters.
     - Test `listCampaigns` with `brandId` and `channel` filters.
     - Test `getCampaignById` success.
     - Test `getCampaignById` not found throws `ResourceNotFoundException`.
   - `CampaignControllerTest`:
     - Test `GET /api/v1/campaigns` returns 200 OK with paginated response.
     - Test `GET /api/v1/campaigns?brandId=...&channel=...` returns 200 OK.
     - Test `GET /api/v1/campaigns/{id}` returns 200 OK.
     - Test `GET /api/v1/campaigns/{id}` returns 404 Not Found when absent.
   - `CampaignIntegrationTest`:
     - Test end-to-end list, filter, and get endpoints against database.
5. **Build & Quality Gate Verification:**
   - Run `./gradlew clean build jacocoTestReport`.
   - Check JaCoCo report `backend/build/reports/jacoco/test/jacocoTestReport.xml` to ensure line coverage >= 80% and branch coverage >= 70%.
