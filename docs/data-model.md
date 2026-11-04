# Proposed data model

Tables for the proposed submission service (docs/architecture.md). OUR
proposal; Cynbit's actual schema is not observable.

## submissions

One record per candidate.

- id (PK)
- candidate_id (UNIQUE) - enforces one record per candidate; first creation
  serializes on this constraint (see the known limit in architecture.md)
- status (e.g. OPEN, SUBMITTED)
- version (bigint) - optimistic locking counter, or use pessimistic locks
- created_at, updated_at

## submission_versions

Immutable history; rows are never updated or deleted.

- id (PK)
- submission_id (FK -> submissions.id)
- version_no (int) - UNIQUE (submission_id, version_no)
- link (text) - validated https + allowlisted host at intake
- notes (text, up to 4000)
- created_at

## idempotency_operations

One row per accepted operation.

- id (PK)
- candidate_id
- idempotency_key
- request_hash - fingerprint of the canonical request payload
- receipt (jsonb or columns) - immutable receipt returned for this operation
- created_at
- UNIQUE (candidate_id, idempotency_key)

Replay rule: same key + same request_hash returns the stored receipt; same key
+ different request_hash is a 409 conflict.

## outbox_events

Transactional outbox for notifications.

- id (PK)
- aggregate_id (submission_versions.id)
- event_type (e.g. "submission.versioned")
- payload (jsonb)
- published_at (null until a consumer acknowledges)

Written in the same transaction as the version row; consumers are idempotent,
so notifications are eventually consistent with the accepted state.
