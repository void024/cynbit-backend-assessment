# Cynbit Backend Assessment: Technical Audit and Recommendations

Backend Developer Intern assessment: an analysis of Cynbit's publicly observable
backend behaviour, categorized findings, and recommended improvements - with
executable proof where proof is possible, and clearly-labeled design proposals
where it is not.

## Repository map

    README.md                 - you are here
    audit/
      technical-audit.md      - how the review was done and the observable backend
      findings.md             - security and performance findings, by category
      recommendations.md      - recommended improvements, tested vs proposed
    deadline/                 - EXECUTED: deadline contract test + display reproduction
    submission/               - EXECUTED: idempotency demo for the proposed pipeline
    security/                 - proposed headers config + observation/verification steps
    docs/                     - proposed design: architecture, API, data model

Start with audit/technical-audit.md, then findings.md, then recommendations.md.

## Running the executed artifacts

Requirements: any JDK 11 or newer. No build tool, no dependencies - each file
runs with the JDK single-file source launcher.

    java deadline/DeadlineContractTest.java
    java deadline/DeadlineDisplayReproTest.java
    java submission/SubmissionIdempotencyDemo.java

Each prints a PASS line on success. Recorded run outputs are in
deadline/last-run.txt and submission/last-run.txt.

Windows note: the .java files must be saved as UTF-8 without BOM. A BOM (added
by some tools, including older PowerShell Set-Content -Encoding UTF8) makes
`java File.java` fail with an "illegal character" error on the first line. The
unpack instructions that shipped with this package already write without a BOM.

## What is tested versus proposed

- **Executed locally and passing:** the deadline formatter contract test, the
  two-display reproduction, and the submission idempotency demo - plain Java on
  OpenJDK 11, no framework, no database.
- **Proposed, not executed:** the full Spring Boot submission service
  (docs/) and the Next.js security header block (security/). These are design
  material. The header snippet keeps a .txt extension deliberately, so no editor
  treats it as part of a buildable app.
- Nothing here is deployed, and nothing in docs/ describes Cynbit's actual
  internals - those are not observable from outside, and the audit says so
  plainly.

## Method in one line

Read-only observation of the public website and candidate portal on 3-4 Nov 2026
(one disclosed malformed-header request, excluded from findings); no
exploitation, no load, no unauthorized access.
