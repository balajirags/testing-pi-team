# Implementation Plan - Issue #9: GET /api/v1/random-hash Endpoint

**Branch:** `feature/issue-9`  
**Issue:** #9

## Overview
Implement a REST API endpoint `GET /api/v1/random-hash` in the Spring Boot backend service to generate and return a random hash (SHA-256 string), status, and ISO-8601 UTC timestamp.

## Requirements & Acceptance Criteria
1. `GET /api/v1/random-hash` returns HTTP status `200 OK`.
2. Response header `Content-Type` includes `application/json`.
3. Response body JSON payload contains:
   - `"hash"`: valid 64-character SHA-256 hex hash string.
   - `"status"`: `"SUCCESS"`.
   - `"timestamp"`: valid ISO-8601 UTC timestamp.
4. Consecutive calls generate unique random hashes.
5. Unit tests with JUnit 5, Mockito, and AssertJ achieve >=80% line coverage and >=70% branch coverage.
6. Bruno E2E API test added under `api-tests/random-hash/01-get-random-hash.bru`.

## Strategy & File Layout
1. **DTO:** `backend/src/main/java/com/company/app/hash/api/dto/RandomHashResponse.java`
2. **Service:** `backend/src/main/java/com/company/app/hash/service/RandomHashService.java`
3. **Controller:** `backend/src/main/java/com/company/app/hash/api/RandomHashController.java`
4. **Service Test:** `backend/src/test/java/com/company/app/hash/service/RandomHashServiceTest.java`
5. **Controller Test:** `backend/src/test/java/com/company/app/hash/api/RandomHashControllerTest.java`
6. **Bruno API Test:** `api-tests/random-hash/01-get-random-hash.bru`
