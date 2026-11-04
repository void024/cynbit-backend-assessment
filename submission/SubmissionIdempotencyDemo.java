import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;

// Plain-Java demonstration of the proposed submission pipeline's core
// invariants: idempotent replay, 409 on key reuse with a changed payload, no
// duplicate record under concurrent FIRST creation, and an append-only
// immutable version history.
//
// In the proposed Spring Boot service these guarantees come from a UNIQUE
// constraint, transactions and a retry rule (see docs/). Here an in-memory
// store models them so the behavior is executable on any JDK 11+, with no
// framework and no database.
public class SubmissionIdempotencyDemo {

    static final class Receipt {
        final long submissionId;
        final int version;
        final String payload; // the service stores a SHA-256 request hash; the demo keeps the canonical payload

        Receipt(long submissionId, int version, String payload) {
            this.submissionId = submissionId;
            this.version = version;
            this.payload = payload;
        }
    }

    // Maps to HTTP 409 in the proposed API (see docs/api-design.md).
    static final class ConflictException extends RuntimeException {
        ConflictException(String msg) { super(msg); }
    }

    // Models UNIQUE (candidate_id, idempotency_key) and one record per candidate.
    static final class SubmissionStore {
        private final Map<String, Receipt> operations = new ConcurrentHashMap<>();       // candidateId|idemKey -> receipt
        private final Map<Long, List<String>> versions = new ConcurrentHashMap<>();      // submissionId -> append-only payload history
        private final Map<Long, Long> submissionByCandidate = new ConcurrentHashMap<>(); // candidateId -> submissionId
        private final AtomicLong ids = new AtomicLong(1000);

        Receipt record(long candidateId, String idemKey, String payload) {
            String opKey = candidateId + "|" + idemKey;

            Receipt prior = operations.get(opKey);
            if (prior != null) {
                // Exact payload equality, never a hash comparison (see the
                // "Aa"/"BB" regression below): two payloads that collide under
                // String.hashCode must still be treated as different.
                if (!prior.payload.equals(payload))
                    throw new ConflictException("key reused with new payload");
                return prior; // replay: the same operation returns its original receipt
            }

            synchronized (this) { // models the database uniqueness check + transaction
                Receipt recheck = operations.get(opKey);
                if (recheck != null) {
                    if (!recheck.payload.equals(payload))
                        throw new ConflictException("key reused with new payload");
                    return recheck;
                }
                // one submission record per candidate, no matter how many keys race
                long submissionId = submissionByCandidate.computeIfAbsent(candidateId, c -> ids.incrementAndGet());
                List<String> history = versions.computeIfAbsent(submissionId, s -> new CopyOnWriteArrayList<>());
                history.add(payload); // append-only: existing entries are never modified or removed
                Receipt r = new Receipt(submissionId, history.size(), payload);
                operations.put(opKey, r);
                return r;
            }
        }

        List<String> versionHistory(long submissionId) { return versions.get(submissionId); }
        int recordCount() { return submissionByCandidate.size(); }
        long submissionFor(long candidateId) { return submissionByCandidate.get(candidateId); }
    }

    public static void main(String[] args) throws Exception {
        // 1. same key + same payload -> replays the original receipt, changes nothing
        SubmissionStore store = new SubmissionStore();
        Receipt first = store.record(1, "key-A", "link=https://github.com/example/repo;notes=v1");
        List<String> beforeReplay = new ArrayList<>(store.versionHistory(store.submissionFor(1)));
        Receipt replay = store.record(1, "key-A", "link=https://github.com/example/repo;notes=v1");
        check(first == replay, "same key + same payload replays the original receipt");
        check(beforeReplay.equals(store.versionHistory(store.submissionFor(1))),
              "a replay leaves the version history untouched");

        // 2. same key + changed payload -> conflict (409), changes nothing
        boolean conflict = false;
        try {
            store.record(1, "key-A", "link=https://github.com/example/repo;notes=EDITED");
        } catch (ConflictException e) {
            conflict = true;
        }
        check(conflict, "same key + changed payload is rejected with a conflict (409)");
        check(beforeReplay.equals(store.versionHistory(store.submissionFor(1))),
              "a rejected operation leaves the version history untouched");

        // 3. regression: "Aa" and "BB" collide under String.hashCode - a hash
        // comparison would wrongly treat them as identical
        check("Aa".hashCode() == "BB".hashCode(),
              "regression premise holds: Aa and BB share a String.hashCode");
        SubmissionStore collisionStore = new SubmissionStore();
        collisionStore.record(7, "key-C", "Aa");
        boolean collisionConflict = false;
        try {
            collisionStore.record(7, "key-C", "BB");
        } catch (ConflictException e) {
            collisionConflict = true;
        }
        check(collisionConflict,
              "payloads colliding under String.hashCode are still treated as different (exact equality)");

        // 4. concurrent FIRST creation with distinct keys -> exactly one record
        SubmissionStore raceStore = new SubmissionStore();
        int racers = 8;
        List<Throwable> raceFailures = race(raceStore, 42, racers, false);
        check(raceFailures.isEmpty(), "no worker thread failed during the first-creation race");
        check(raceStore.recordCount() == 1,
              "concurrent first creation produced exactly one submission record");
        check(raceStore.versionHistory(raceStore.submissionFor(42)).size() == racers,
              "each accepted new key appended exactly one immutable version");

        // 5. concurrent SAME key + same payload -> one receipt, one version
        SubmissionStore replayStore = new SubmissionStore();
        List<Throwable> replayFailures = race(replayStore, 43, racers, true);
        check(replayFailures.isEmpty(), "no worker thread failed during the same-key race");
        check(replayStore.versionHistory(replayStore.submissionFor(43)).size() == 1,
              "a concurrent same-key race adds exactly one version in total");

        System.out.println("PASS: idempotency demo - replay, 409 conflict, collision regression and concurrency invariants hold");
    }

    // Runs `racers` threads that all start together. When sameKey is true every
    // thread uses one shared key and payload (replay race); otherwise each
    // thread gets a distinct key and payload (first-creation race). Every
    // thread also asserts the receipt it got is the one stored receipt.
    // Worker exceptions are collected and returned, never swallowed.
    static List<Throwable> race(SubmissionStore store, long candidateId, int racers, boolean sameKey) throws InterruptedException {
        CountDownLatch start = new CountDownLatch(1);
        List<Thread> threads = new ArrayList<>();
        List<Throwable> failures = new CopyOnWriteArrayList<>();
        List<Receipt> receipts = new CopyOnWriteArrayList<>();
        for (int i = 0; i < racers; i++) {
            final int n = i;
            Thread t = new Thread(() -> {
                try {
                    start.await();
                    Receipt r = sameKey
                        ? store.record(candidateId, "key-shared", "shared-payload")
                        : store.record(candidateId, "key-race-" + n, "payload-" + n);
                    receipts.add(r);
                } catch (Throwable e) {
                    failures.add(e);
                }
            });
            threads.add(t);
            t.start();
        }
        start.countDown();
        for (Thread t : threads) t.join();

        if (sameKey && failures.isEmpty() && !receipts.isEmpty()) {
            Receipt expected = receipts.get(0);
            for (Receipt r : receipts)
                if (r != expected)
                    failures.add(new AssertionError("same-key race returned more than one distinct receipt"));
        }
        return failures;
    }

    static void check(boolean ok, String what) {
        if (!ok) throw new AssertionError("FAILED: " + what);
        System.out.println("ok - " + what);
    }
}
