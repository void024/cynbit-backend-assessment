# Submission pipeline: executable demo

`SubmissionIdempotencyDemo.java` demonstrates the core invariants of the
proposed submission pipeline in plain Java - no framework, no database:

1. An idempotency key resent with the SAME payload replays the original
   receipt and leaves the version history untouched.
2. An idempotency key resent with a CHANGED payload is rejected (409-style
   conflict) and leaves the version history untouched.
3. Payload identity uses exact equality, never a hash: a regression case
   proves two payloads that collide under String.hashCode ("Aa" / "BB") are
   still treated as different.
4. Concurrent FIRST creation (real thread race, distinct keys) produces
   exactly one submission record, one immutable version per accepted key, and
   worker failures are surfaced, never swallowed.
5. A concurrent race on the SAME key returns the one original receipt to every
   caller and adds exactly one version in total.

Run it:

    cd submission
    java SubmissionIdempotencyDemo.java

Expected output ends with:

    PASS: idempotency demo - replay, 409 conflict, collision regression and concurrency invariants hold

What this deliberately is NOT: it is not the proposed Spring Boot service. The
full design (HTTP layer, database, transactions, outbox) is documented in
docs/. The in-memory store here models the database's UNIQUE constraint and
append-only versions so the behaviour is executable and checkable on any
JDK 11+.

The recorded output of the most recent run is in `last-run.txt` (this folder).
