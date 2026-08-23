# Story: Hello World Greeting Endpoint

> Owner: Business Analyst · Path: `docs/stories/hello-world-api/01-hello-world-endpoint.md`

| Field | Value |
|---|---|
| Epic | Hello World API |
| BRD | `docs/brd/hello-world-api.md` |
| Label | Backend |
| Order | 1 |
| Depends on | None |
| Status | Done |

## Description

As a **System Administrator or Developer**,
I want **a REST API endpoint at `GET /api/v1/hello`**,
so that **I can verify the backend service is running properly and test system connectivity**.

## Spec coverage

- `GET /api/v1/hello`

## Acceptance Criteria

| Scenario | Given | When | Then |
|---|---|---|---|
| Successful hello greeting | The backend application server is running | `GET /api/v1/hello` is called | HTTP 200 OK is returned with JSON payload containing `"message": "Hello World!"`, `"status": "UP"`, and a valid ISO-8601 UTC `"timestamp"` |
| Correct Content-Type header | The backend application server is running | `GET /api/v1/hello` is called | The response header `Content-Type` includes `application/json` |

## Assumptions

- Endpoint requires no authentication / public access for health verification unless global security rules dictate otherwise.
- Timestamp is generated dynamically in ISO-8601 UTC format.

## Open Questions

- None
