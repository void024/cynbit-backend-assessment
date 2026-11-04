# Recommended Improvements

Three recommendations, each tied to a finding in findings.md. Code that was
executed is marked; design material is marked as proposed.

## A. One deadline, rendered one way - executed locally: PASS

Store the deadline once as a canonical instant, render it through a single
service with an explicit display zone, and add a contract test so two surfaces
can never disagree again.

- `deadline/DeadlineContractTest.java` - the contract test (executed; run output
  in `deadline/last-run.txt`):

```
PASS: email and portal render the same deadline -> Thu, Nov 5, 12:58 PM IST
```

- `deadline/DeadlineDisplayReproTest.java` - reproduces both observed displays
  from one stored instant (executed). This shows the two-zone reading is
  consistent with the observations; the actual cause on Cynbit's side remains
  unconfirmed.

## B. A reliable submission pipeline - core invariants executed, full service proposed

How submissions are stored internally is not observable, so this is a proposal
(target: Java 17 / Spring Boot 3). Its core behaviour IS executable and tested:

- `submission/SubmissionIdempotencyDemo.java` - plain Java, no dependencies
  (executed). Demonstrates under real thread races: an idempotency key with
  the same payload replays the original receipt and changes nothing; the same
  key with a changed payload is rejected with a conflict (409); payload
  identity uses exact equality (a regression case covers String.hashCode
  collisions); concurrent FIRST creation produces exactly one record; and the
  version history is append-only.

The full proposed design - endpoints, data model, and the service logic with a
transactional outbox - is in `docs/` (proposed, not executed as a service):

- Validated intake; the link is enforced in code (https + allowlisted hosts).
- Duplicate-safe retry via `Idempotency-Key` + request hash, backed by a UNIQUE
  (candidate_id, idempotency_key) constraint - the constraint, not the
  read-then-write, is what prevents duplicates under concurrency.
- Immutable versions; concurrent new keys serialize so version numbers never
  race. Known limit, stated in docs/architecture.md: a row lock cannot
  serialize the first creation (no row exists yet), so first creation needs a
  parent-row lock or a unique constraint plus whole-transaction retry.
- Immutable receipts, separate from the latest-state read.
- Notification consistency via a transactional outbox; a receipt means
  accepted, not emailed.

Proposed tests for the full service (not executed): malformed payloads return
400 with field errors; the same idempotency key replays the original receipt
and adds no version; the same key with a changed payload returns 409;
concurrent first submissions race and the unique constraint prevents
duplicates. The first three of these are exactly what the executable demo
already covers in memory.

If a link-checking worker is added later: fetch only allowlisted hosts, block
private IP ranges, re-resolve after redirects, hard timeouts and size caps,
isolated worker with restricted egress. Link metadata is never a quality gate
without an explicit rubric.

## C. Security headers - proposed config plus verification steps

Their site is Next.js, so the fix lives in one config block:
`security/next.config.js.txt` (proposed - merge into the real config). Start
CSP in Report-Only mode to collect violations before enforcing, which minimizes
disruption but does not guarantee nothing breaks; the policy still needs tuning
against real traffic. `security/verification.md` shows how the missing headers
were observed and how to confirm the fix after deployment. (If any service
were Spring, Spring Security sets the same headers in its security filter
chain.)

## Tested versus illustrative

- **Executed locally and passing:** `deadline/DeadlineContractTest.java`,
  `deadline/DeadlineDisplayReproTest.java`, `submission/SubmissionIdempotencyDemo.java`
  - plain Java on OpenJDK 11, no framework, no database (run outputs in
  `deadline/last-run.txt` and `submission/last-run.txt`).
- **Proposed, not executed:** the full Spring Boot submission service and the
  Next.js header config. They are documented in `docs/` and `security/` as
  design material, and the header snippet keeps a .txt extension so no editor
  treats it as part of a buildable app.
- Nothing here is deployed; these are recommendations, not a running service.
