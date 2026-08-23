# Implementation Plan - Issue #8: Hello World Greeting Endpoint

**Branch:** `feature/issue-8`  
**Story:** `docs/stories/hello-world-api/01-hello-world-endpoint.md`

## Overview
Implement a lightweight REST API endpoint `GET /api/v1/hello` in the Spring Boot backend service to provide system health verification and basic connectivity checks.

## Acceptance Criteria
1. `GET /api/v1/hello` returns HTTP status `200 OK`.
2. Response body contains JSON payload:
   - `"message": "Hello World!"`
   - `"status": "UP"`
   - `"timestamp"`: valid ISO-8601 UTC timestamp.
3. Response header `Content-Type` includes `application/json`.

## Tasks & Strategy
1. **DTO Record (`backend/src/main/java/com/company/app/hello/api/dto/HelloResponse.java`):**
   - Java record with `message`, `status`, and `timestamp` fields.
2. **Controller (`backend/src/main/java/com/company/app/hello/api/HelloController.java`):**
   - `@RestController` at `/api/v1/hello` returning `HelloResponse` with `"Hello World!"`, `"UP"`, and `Instant.now()`.
3. **Tests (`backend/src/test/java/com/company/app/hello/api/HelloControllerTest.java`):**
   - MockMvc controller unit tests verifying status 200, Content-Type JSON, and payload structure.
4. **Verification:**
   - `./gradlew test jacocoTestReport` in `backend/`
   - Check JaCoCo report XML/HTML for coverage (line >= 80%, branch >= 70%)
   - `./gradlew clean build`
