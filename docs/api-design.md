# Proposed API design

Endpoints for the proposed submission service (docs/architecture.md). This is
OUR proposal, not a description of Cynbit's existing API.

## POST /api/submissions

Creates or updates the candidate's submission.

- Headers: `Idempotency-Key: <opaque>` (required). The candidate is resolved
  server-side from the bearer link; the link itself is never stored as identity
  or logged.
- Body: `{ "link": "https://...", "notes": "..." }`
  - link: required, https, allowlisted hosts - enforced in code, not by
    annotation alone.
  - notes: optional, max 4000 chars.

Responses:

- 201 Created: receipt body `{ submissionId, version, receivedAt }`. Replaying
  the same key with the same payload returns the ORIGINAL receipt (still 201
  semantics for that stored operation), with no new version.
- 400 Bad Request: validation failure, with per-field errors.
- 409 Conflict: the idempotency key was reused with a different payload.

Design sketch (illustrative, adapted into a real project before it compiles):

```java
@PostMapping("/api/submissions")
public ResponseEntity<Receipt> submit(
        @AuthenticationPrincipal CandidatePrincipal principal,
        @RequestHeader("Idempotency-Key") String idemKey,
        @Valid @RequestBody SubmissionRequest req) {
    Receipt receipt = submissions.record(principal.id(), idemKey, req);
    return ResponseEntity.status(HttpStatus.CREATED).body(receipt);
}

record SubmissionRequest(
    @NotBlank String link,
    @Size(max = 4000) String notes
) {}
```

## GET /api/submissions/current

Returns the candidate's latest application state. Deliberately separate from
receipts: a receipt is immutable per operation, while this read always shows
current state.

## Status semantics note

For an unknown or malformed portal token, prefer a uniform 404 with no data
over distinguishable errors, so token validity cannot be probed by error
shape. (Their current invalid-link page already fails gracefully with no data;
this is the same principle at the API layer.)
