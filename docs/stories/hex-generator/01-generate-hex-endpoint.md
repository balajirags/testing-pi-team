# Story: Generate Random Hex String Endpoint

> Owner: Business Analyst · Path: `docs/stories/hex-generator/01-generate-hex-endpoint.md`

| Field | Value |
|---|---|
| Epic | Hex Generator API |
| BRD / Requirement | Hex Generation Service |
| Label | Backend |
| Order | 1 |
| Depends on | None |
| Status | Ready for Dev |

## Description

As an **API Client or Developer**,
I want **a REST API endpoint at `POST /api/v1/hex/generate`**,
so that **I can submit an input text string and receive a random hexadecimal string representation of a specified character length**.

## Spec coverage

- `POST /api/v1/hex/generate`

## Acceptance Criteria

| Scenario | Given | When | Then |
|---|---|---|---|
| Default hex length generation | The backend API service is running | `POST /api/v1/hex/generate` is called with body `{"text": "hello"}` | HTTP 200 OK is returned with JSON payload containing `text: "hello"`, a 32-character lowercase hex string in `hex`, `length: 32`, and an ISO-8601 UTC `timestamp` |
| Custom valid hex length generation | The backend API service is running | `POST /api/v1/hex/generate` is called with body `{"text": "test", "length": 64}` | HTTP 200 OK is returned with JSON payload containing `text: "test"`, a 64-character hex string in `hex`, `length: 64`, and `timestamp` |
| Minimum allowed length boundary | The backend API service is running | `POST /api/v1/hex/generate` is called with body `{"text": "sample", "length": 8}` | HTTP 200 OK is returned with `hex` of length 8 and `length: 8` |
| Maximum allowed length boundary | The backend API service is running | `POST /api/v1/hex/generate` is called with body `{"text": "sample", "length": 128}` | HTTP 200 OK is returned with `hex` of length 128 and `length: 128` |
| Missing or null text validation | The backend API service is running | `POST /api/v1/hex/generate` is called with body `{"length": 32}` or `{"text": null}` | HTTP 400 Bad Request is returned with RFC 7807 ProblemDetail stating `text` must not be null |
| Out of bounds length validation | The backend API service is running | `POST /api/v1/hex/generate` is called with body `{"text": "data", "length": 6}` | HTTP 400 Bad Request is returned with RFC 7807 ProblemDetail stating length must be between 8 and 128 |
| Non-even length validation | The backend API service is running | `POST /api/v1/hex/generate` is called with body `{"text": "data", "length": 15}` | HTTP 400 Bad Request is returned with RFC 7807 ProblemDetail stating length must be an even integer |
| Invalid content type header | The backend API service is running | `POST /api/v1/hex/generate` is called with header `Content-Type: text/plain` | HTTP 415 Unsupported Media Type is returned |
| Response Content-Type header | The backend API service is running | `POST /api/v1/hex/generate` is called with valid request | Response header includes `Content-Type: application/json` |

## Request Schema

`POST /api/v1/hex/generate`

```json
{
  "text": "sample input text",
  "length": 32
}
```

- `text` (String, required): Input string / seed text. Must not be null.
- `length` (Integer, optional, default: 32, min: 8, max: 128): Length of the resulting hex string in characters. Must be an even integer.

## Response Schema

HTTP 200 OK:

```json
{
  "text": "sample input text",
  "hex": "4a8b2c9d0e1f3a5b7c9d1e3f5a7b9c1d",
  "length": 32,
  "timestamp": "2026-08-24T06:55:00Z"
}
```

- `text` (String): The original input text provided.
- `hex` (String): Randomly generated lowercase hexadecimal string (`0-9`, `a-f`).
- `length` (Integer): The character length of the generated hex string.
- `timestamp` (String): ISO-8601 UTC timestamp of generation.

HTTP 400 Bad Request (RFC 7807 ProblemDetail):

```json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "length must be an even integer between 8 and 128",
  "instance": "/api/v1/hex/generate"
}
```

## Assumptions

- Hex string consists of valid lowercase hexadecimal characters (`0-9`, `a-f`).
- Generated hex string is produced using a cryptographically secure random source combined with/derived for the input text context.
- Timestamp is in ISO-8601 UTC format.

## Open Questions

- None
