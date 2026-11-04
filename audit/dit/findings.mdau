# Security and Performance Findings

Categorized findings from the observable behaviour described in technical-audit.md.

## Data handling: one deadline, two displays

The same assessment deadline appears as 07:28 AM on 5 Nov in the assignment email
(no timezone shown) and as 12:58 PM on 5 Nov on the candidate portal. The displays
are 5 hours 30 minutes apart, exactly the IST offset, which is consistent with one
stored instant rendered in two zones without labels. The root cause was not
confirmed; the inconsistency itself is reproducible - see
`deadline/DeadlineDisplayReproTest.java`, which renders one stored instant both
ways and matches both displays. A candidate who trusts the later display could
miss the real cutoff.

## Security: missing defense-in-depth headers

CSP, X-Frame-Options, X-Content-Type-Options, Referrer-Policy, and
Permissions-Policy were absent on the main site and on the workspace login page.
An authentication page without framing protection is the weaker case of the two.
Reproduction and post-fix verification steps are in `security/verification.md`.

## Authentication: the bearer-link tradeoff

Anyone holding a candidate portal link can view that candidate's application
details; the link travels by email and can be forwarded or logged. Whether the
links expire, rotate, or can be revoked was not assessed, and no unauthorized
access was found or attempted. This is a design tradeoff (zero-friction access
versus link exposure), presented as an improvement opportunity, not a vulnerability.

## Performance: caching posture

Personalized routes correctly send private, no-store; the static homepage is
CDN-cached for about 30 hours. No performance defect was found in this pass; this
is the posture a report can acknowledge as sound.

## Error handling: a positive, and one design question

Invalid links fail gracefully with no data exposure. The one question worth
raising internally: what status semantics an unknown portal token should return,
and why. A team choice, not a defect.
