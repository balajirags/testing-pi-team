# Story: Generate Password Endpoint

> Owner: Business Analyst · Path: `docs/stories/password-generator/01-generate-password-endpoint.md`

| Field | Value |
|---|---|
| Epic | Password Generator API |
| BRD / Requirement | Password Generation Service |
| Label | Backend |
| Order | 1 |
| Depends on | None |
| Status | Ready for Dev |

## Description

As a **User or API Client**,
I want **a REST API endpoint at `GET /api/v1/passwords/generate`**,
so that **I can request a cryptographically secure random alphanumeric password of a specified length**.

## Spec coverage

- `GET /api/v1/passwords/generate`

## Acceptance Criteria

| Scenario | Given | When | Then |
|---|---|---|---|
| Default password length generation | The backend API service is running | `GET /api/v1/passwords/generate` is called without parameters | HTTP 200 OK is returned with JSON payload containing `password` of length 12, `length: 12`, and a valid ISO-8601 UTC `timestamp` |
| Custom valid password length | The backend API service is running | `GET /api/v1/passwords/generate?length=16` is called | HTTP 200 OK is returned with JSON payload containing `password` of length 16, `length: 16`, and `timestamp` |
| Minimum allowed length boundary | The backend API service is running | `GET /api/v1/passwords/generate?length=8` is called | HTTP 200 OK is returned with `password` of length 8, `length: 8` |
| Maximum allowed length boundary | The backend API service is running | `GET /api/v1/passwords/generate?length=64` is called | HTTP 200 OK is returned with `password` of length 64, `length: 64` |
| Length below minimum boundary | The backend API service is running | `GET /api/v1/passwords/generate?length=7` is called | HTTP 400 Bad Request is returned with RFC 7807 ProblemDetail stating length must be between 8 and 64 |
| Length above maximum boundary | The backend API service is running | `GET /api/v1/passwords/generate?length=65` is called | HTTP 400 Bad Request is returned with RFC 7807 ProblemDetail stating length must be between 8 and 64 |
| Non-numeric length parameter | The backend API service is running | `GET /api/v1/passwords/generate?length=invalid` is called | HTTP 400 Bad Request is returned with RFC 7807 ProblemDetail response |
| Response Content-Type header | The backend API service is running | `GET /api/v1/passwords/generate` is called | Response header includes `Content-Type: application/json` |

## Request Schema

`GET /api/v1/passwords/generate`

Query Parameters (Optional):
- `length` (Integer, default: 12, min: 8, max: 64)

## Response Schema

HTTP 200 OK:

```json
{
  "password": "aB3k9XmP2vQ1",
  "length": 12,
  "timestamp": "2026-08-23T13:45:00Z"
}
```

- `password` (String): Cryptographically secure random alphanumeric string (A-Z, a-z, 0-9).
- `length` (Integer): The length of the generated password.
- `timestamp` (String): ISO-8601 UTC timestamp of generation.

HTTP 400 Bad Request (RFC 7807 ProblemDetail):

```json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "length must be between 8 and 64",
  "instance": "/api/v1/passwords/generate"
}
```

## Assumptions

- Random generator uses `java.security.SecureRandom` for cryptographic security.
- Character set consists of uppercase ASCII letters (`A-Z`), lowercase ASCII letters (`a-z`), and digits (`0-9`).
- Timestamp is ISO-8601 format in UTC timezone.

## Open Questions

- None
