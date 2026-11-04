# Proposed architecture: submission pipeline

This document describes OUR proposed design for Cynbit's candidate submission
pipeline. Cynbit's actual internal architecture is not observable from outside
and is not described or implied here.

## Components and flow

1. API layer (Spring Boot 3 / Java 17) validates the request and enforces the
   link rules in code (https + allowlisted hosts). See docs/api-design.md.
2. Idempotency layer: every create carries an Idempotency-Key header. The
   (candidate_id, idempotency_key, request_hash) triple is stored in an
   idempotency_operations table with UNIQUE (candidate_id, idempotency_key).
   The unique constraint - not the read-then-write - is what prevents
   duplicates under concurrency.
3. Submission record: one record per candidate. Every accepted change (new
   link, edited notes) appends an immutable submission_versions row; prior
   versions are never mutated.
4. Receipts: each accepted operation issues its own immutable receipt. The
   latest application state is a separate read, so a receipt never changes
   meaning after the fact.
5. Outbox: every version write and its outbox event commit in the same
   transaction. Consumers are idempotent, so notifications are eventually
   consistent with the accepted state. Email delivery status is tracked
   separately from acceptance - a receipt means accepted, not emailed.

## Concurrency rule and its known limit

Concurrent NEW keys serialize on the candidate's submission record (pessimistic
lock, or optimistic @Version) so version numbers never race.

Known limit: a row lock cannot serialize the FIRST creation - there is no row
to lock yet. Serialize first creation by locking the existing parent candidate
row, or by a UNIQUE constraint on the candidate's submission record with a
whole-transaction retry on violation: on a UNIQUE violation from a concurrent
same key, roll back and retry in a NEW transaction, re-read the committed
operation and return its receipt. Never catch-and-requery inside the failed
transaction.

## Executable proof of the core invariants

`submission/SubmissionIdempotencyDemo.java` models the uniqueness and replay
rules in memory and asserts them under a real thread race (plain Java, no
framework). Its run output is recorded in submission/last-run.txt. The demo proves the invariants hold; it is not the service.
