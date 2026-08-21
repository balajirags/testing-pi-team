# Implementation Plan - Issue #4: Deactivate or Delete Campaign API

**Branch:** `feature/issue-4`  
**Story:** `docs/stories/campaigns-management/04-delete-campaign-backend.md`

## Overview
Implement backend API endpoint for soft-deleting/archiving a campaign (`DELETE /api/v1/campaigns/{id}`) by setting its status to `ARCHIVED`.

## Acceptance Criteria
1. `DELETE /api/v1/campaigns/{id}` on an existing campaign sets status to `ARCHIVED` and returns `204 No Content`.
2. `DELETE /api/v1/campaigns/{id}` on a non-existent campaign returns `404 Not Found` with ProblemDetail.
3. `GET /api/v1/campaigns/{id}` on an archived campaign returns `200 OK` with status `ARCHIVED`.
4. `GET /api/v1/campaigns` excludes `ARCHIVED` campaigns by default unless a specific status filter (e.g. `status=ARCHIVED`) is requested.

## Tasks & Strategy
1. **Service Layer (`CampaignService`):**
   - Implement `void deleteCampaign(UUID id)`:
     - Find campaign by ID or throw `ResourceNotFoundException("Campaign not found with ID: " + id)`.
     - Set status to `CampaignStatus.ARCHIVED`.
     - Save entity.
   - Update `listCampaigns(UUID brandId, String channel, CampaignStatus status, Pageable pageable)`:
     - Add optional `CampaignStatus status` parameter.
     - If `status` is specified, filter `root.get("status") == status`.
     - If `status` is NOT specified, filter `root.get("status") != CampaignStatus.ARCHIVED`.
2. **Controller Layer (`CampaignController`):**
   - Add `@DeleteMapping("/{id}")` endpoint returning `ResponseEntity<Void>` with status `204 No Content`.
   - Update `GET /api/v1/campaigns` controller method to accept optional `@RequestParam(required = false) CampaignStatus status`.
3. **Tests:**
   - `CampaignServiceTest`:
     - Test `deleteCampaign` happy path.
     - Test `deleteCampaign` not found throws `ResourceNotFoundException`.
     - Test `listCampaigns` excludes `ARCHIVED` by default and includes when `status=ARCHIVED`.
   - `CampaignControllerTest`:
     - Test `DELETE /api/v1/campaigns/{id}` returns 204 No Content.
     - Test `DELETE /api/v1/campaigns/{id}` non-existent returns 404 Not Found.
   - `CampaignIntegrationTest`:
     - Test end-to-end delete, soft delete verification via GET by ID, and exclusion from default list query.
4. **Build & Quality Gate Verification:**
   - Run `./gradlew clean build jacocoTestReport`.
   - Verify JaCoCo report XML/HTML for line coverage >= 80% and branch coverage >= 70%.
