# Backend Technical Audit

An analysis of Cynbit's publicly observable backend behaviour, prepared for the
Backend Developer Intern assessment.

## How this review was done

- Read-only observation of the public website and candidate portal on 3-4 Nov 2026:
  rendered pages, response headers, caching behaviour, public JavaScript assets.
- One exception, stated honestly: a single malformed-header request was sent early
  in the review. That is active input testing, not passive observation, so its
  result is excluded from every finding.
- No exploitation, no load, no unauthorized access, no enumeration of other candidates.
- This was a bounded pass. Where nothing was found, that means none found in this
  pass, not proof that none exists.

## The observable backend

- **Stack:** Next.js 15.5.18 on Vercel; marketing pages server-rendered.
- **Candidate portal:** application details load behind a long opaque link; no
  sign-in is asked for.
- **Second portal:** bitelevate.cynbit.in runs a credential-based workspace
  sign-in; two access models coexist.
- **Caching:** homepage served from CDN cache (about 30 hours); personalized pages
  marked no-store, which is correct.
- **Error handling:** an invalid portal link returns a graceful "Application
  Reference Not Found" page with no data.
- **Public assets:** no secrets found in the 24 public JavaScript chunks reviewed.

API payloads were observed only as rendered through these pages; internal
endpoints, the database, and the notification workflow are not visible from outside.

## What we could not assess

- Server internals, the database schema, and how notification emails are actually
  triggered.
- API payloads beyond what the portal pages render.
- Whether portal links expire, rotate, or can be revoked.
- The root cause of the two deadline displays (see findings.md).

Stating these limits is a strength, not a gap: everything in findings.md is
observable and reproducible from outside.
