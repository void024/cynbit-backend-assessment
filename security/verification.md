# Security headers: observation and verification

## What was observed (passive review, 3-4 Nov 2026)

Response headers were read from the public site and the workspace login page.
These five defense-in-depth headers were absent on both:

- Content-Security-Policy
- X-Frame-Options
- X-Content-Type-Options
- Referrer-Policy
- Permissions-Policy

## Reproduce the observation

Important: an empty result only means "missing" when the request actually
succeeded. A network error, a redirect that was never followed, or a server
that answers HEAD differently can produce no matching output too. So always
look at the status line first, then the headers.

macOS / Linux, with curl (a GET that prints response headers and discards the
body):

    curl -sS -L -D - -o /dev/null https://www.cynbit.in/
    curl -sS -L -D - -o /dev/null https://bitelevate.cynbit.in/

Read the output top to bottom: after any redirects, the final response should
show a 2xx status. Then check for the five header names. To list only matches:

    curl -sS -L -D - -o /dev/null https://www.cynbit.in/ | grep -iE '^(content-security-policy|x-frame-options|x-content-type-options|referrer-policy|permissions-policy):'

No matching lines on a successful final response means none of the five are
present. If curl errors, times out, or the final status is an error, the
observation is UNVERIFIED for that URL - that is not evidence the headers are
missing. The header names are anchored (^...:) so unrelated headers or values
cannot produce a false match.

Windows PowerShell 5.1+ (built in - note that in Windows PowerShell `curl` is
an alias for Invoke-WebRequest, not the real curl):

    (Invoke-WebRequest -Uri https://www.cynbit.in/ -Method Head).Headers
    (Invoke-WebRequest -Uri https://bitelevate.cynbit.in/ -Method Head).Headers

Check the printed headers for the five names. If a server rejects HEAD (405),
drop `-Method Head` for a plain GET and read the same Headers property.

## Proposed fix

`next.config.js.txt` in this folder is a proposed headers block for their
Next.js config. CSP starts in Report-Only mode so violations are collected
before anything is enforced; that minimizes disruption but does not guarantee
nothing breaks.

## How to verify after deploying

Re-run the same commands against the deployed environment and confirm the
expected values: X-Content-Type-Options: nosniff, X-Frame-Options: DENY,
Referrer-Policy: strict-origin-when-cross-origin, Permissions-Policy:
camera=(), microphone=(), geolocation=(), and
Content-Security-Policy-Report-Only: default-src 'self'.

While CSP is Report-Only, violations appear in the browser developer console
(and at a reporting endpoint, if one is configured). Review them and tune the
policy against real traffic before switching to an enforcing
Content-Security-Policy. The workspace login page matters most: an
authentication page without framing protection is the weaker case.
