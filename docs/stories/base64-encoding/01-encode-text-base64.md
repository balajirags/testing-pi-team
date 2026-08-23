# Story: Encode Text to Base64 Endpoint

> Owner: Business Analyst · Path: `docs/stories/base64-encoding/01-encode-text-base64.md`

| Field | Value |
|---|---|
| Epic | Base64 Encoding API |
| BRD / Requirement | Base64 Text Encoding Service |
| Label | Backend |
| Order | 1 |
| Depends on | None |
| Status | Done |

## Description

As an **API Client / Developer**,
I want **a REST API endpoint at `POST /api/v1/base64/encode`**,
so that **I can submit raw text input and receive its UTF-8 Base64 encoded string representation**.

## Spec coverage

- `POST /api/v1/base64/encode`

## Acceptance Criteria

| Scenario | Given | When | Then |
|---|---|---|---|
| Successfully encode standard ASCII text | The backend API service is running | `POST /api/v1/base64/encode` is called with body `{"text": "Hello World"}` | HTTP 200 OK is returned with JSON payload `{"originalText": "Hello World", "encodedText": "SGVsbG8gV29ybGQ="}` |
| Successfully encode UTF-8 text with special characters and emoji | The backend API service is running | `POST /api/v1/base64/encode` is called with body `{"text": "Hello World! 🚀"}` | HTTP 200 OK is returned with JSON payload containing the exact `originalText` and its correct UTF-8 Base64 `encodedText` (`SGVsbG8gV29ybGQhIPCfm8A=`) |
| Successfully encode empty text string | The backend API service is running | `POST /api/v1/base64/encode` is called with body `{"text": ""}` | HTTP 200 OK is returned with JSON payload `{"originalText": "", "encodedText": ""}` |
| Missing or null text field validation | The backend API service is running | `POST /api/v1/base64/encode` is called with body `{}` or `{"text": null}` | HTTP 400 Bad Request is returned with RFC 7807 ProblemDetail containing validation error details for `text` |
| Invalid content type header | The backend API service is running | `POST /api/v1/base64/encode` is called with `Content-Type: text/plain` | HTTP 415 Unsupported Media Type is returned |
| Correct response Content-Type header | The backend API service is running | `POST /api/v1/base64/encode` is called with valid JSON request | Response includes `Content-Type: application/json` header |

## Request Schema

`POST /api/v1/base64/encode`

```json
{
  "text": "Hello World"
}
```

- `text` (String, required): The input string to be encoded into Base64 format.

## Response Schema

HTTP 200 OK:

```json
{
  "originalText": "Hello World",
  "encodedText": "SGVsbG8gV29ybGQ="
}
```

- `originalText` (String): The original input text provided.
- `encodedText` (String): The resulting UTF-8 Base64 encoded string.

HTTP 400 Bad Request (RFC 7807 ProblemDetail):

```json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "Validation failed for request",
  "instance": "/api/v1/base64/encode",
  "invalidParams": [
    {
      "name": "text",
      "reason": "must not be null"
    }
  ]
}
```

## Assumptions

- Encoding uses standard RFC 4648 Base64 encoding scheme with UTF-8 character encoding.
- The endpoint is stateless and thread-safe.

## Open Questions

- None
