# Implementation Plan - Issue #5: Batch Campaign CSV Import API

**Branch:** `feature/issue-5`  
**Story:** `docs/stories/campaigns-management/05-batch-csv-import-backend.md`

## Overview
Implement backend API endpoint for batch importing campaign definitions from a multi-part CSV file (`POST /api/v1/campaigns/import`).

## Acceptance Criteria
1. `POST /api/v1/campaigns/import` with a valid CSV file containing campaign records returns `200 OK` with summary report (`total`, `created`, `failed`, `errors`).
2. Partial CSV import with row-level errors processes valid rows, skips invalid rows, and includes row number and specific error details in `errors`.
3. Empty file or non-CSV file returns `400 Bad Request` with ProblemDetail error.
4. Exceeding maximum batch size (1,000 rows) returns `400 Bad Request`.

## Tasks & Strategy
1. **DTOs:**
   - Create `CsvRowError` record (`int rowNumber`, `String message`).
   - Create `CsvImportSummaryResponse` record (`int total`, `int created`, `int failed`, `List<CsvRowError> errors`).
2. **Service Layer (`CampaignService`):**
   - Implement `CsvImportSummaryResponse importCampaignsFromCsv(MultipartFile file)`:
     - Validate file presence and extension/content.
     - Parse CSV lines into header mapping and data rows.
     - Enforce 1,000 row max limit.
     - For each data row (starting row 2):
       - Extract values for `name`, `brand_id`, `ad_account_id`, `budget`, `currency`, `channel`, `external_campaign_id`.
       - Validate UUIDs, mandatory string fields, existing brand/ad-account IDs, negative budget, and duplicate channel + external ID mapping.
       - On success: persist `CampaignEntity` (status `DRAFT`).
       - On failure: record `CsvRowError(rowNumber, errorMessage)`.
     - Return `CsvImportSummaryResponse(total, created, failed, errors)`.
3. **Controller Layer (`CampaignController`):**
   - Add `@PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)` endpoint with `@RequestParam("file") MultipartFile file`.
4. **Tests:**
   - Unit tests in `CampaignServiceTest` for CSV import (valid file, partial row errors, empty file, exceeding row limit).
   - Controller tests in `CampaignControllerTest` using `MockMvc` `multipart("/api/v1/campaigns/import")`.
   - Integration test in `CampaignIntegrationTest` for end-to-end multipart CSV import.
5. **Build & Quality Gate Verification:**
   - Run `./gradlew clean build jacocoTestReport`.
   - Verify JaCoCo report XML/HTML for line coverage >= 80% and branch coverage >= 70%.
