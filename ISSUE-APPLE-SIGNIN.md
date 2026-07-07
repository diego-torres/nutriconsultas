# Issue Registry — `[Apple Sign-In]` track

Living index of GitHub issues that implement **Sign in with Apple** through Auth0 and Apple **server-to-server account notifications** on the Minutriporcion backend. Update when status changes (commit on the PR that closes work).

**Repo:** [diego-torres/nutriconsultas](https://github.com/diego-torres/nutriconsultas)  
**Roadmap:** [`docs/auth/apple-signin-backend-roadmap.md`](docs/auth/apple-signin-backend-roadmap.md)  
**Setup (maintainer runbook):** [`docs/auth/apple-signin-setup.md`](docs/auth/apple-signin-setup.md)  
**Epic comment:** [#497](https://github.com/diego-torres/nutriconsultas/issues/497#issuecomment-4904658287)  
**Last updated:** 2026-07-07 — track registered (#497–#511).

> **Scope.** Auth0 Apple social connection, backend webhook (`POST /rest/webhooks/apple/sign-in`), signed payload verification, notification persistence, identity mapping, and safe lifecycle handling. **Does not** replace Auth0 with custom Apple OAuth. Patient mobile API: [`ISSUE.md`](ISSUE.md). Auth0 patient gate: [`docs/auth0/PATIENT-POST-LOGIN-GATE.md`](docs/auth0/PATIENT-POST-LOGIN-GATE.md).

---

## Key endpoints (do not confuse)

| Purpose | URL |
|---------|-----|
| Auth0 OAuth login callback | `https://minutriporcion-prod.us.auth0.com/login/callback` |
| Apple server-to-server notifications | `https://minutriporcion.com/rest/webhooks/apple/sign-in` |
| Local/dev webhook path | `POST /rest/webhooks/apple/sign-in` |

---

## Legend

| State | Meaning |
|-------|---------|
| `NEXT` | Active — pick this up now (if unblocked) |
| `open` | Not started |
| `in-progress` | Branch / PR open |
| `done` | Merged to `main` |
| `deferred` | Paused |

---

## Implementation order

Suggested order matches [`docs/auth/apple-signin-backend-roadmap.md`](docs/auth/apple-signin-backend-roadmap.md).

| Step | # | Title | URL | State | Depends on | Notes |
|------|---|-------|-----|-------|------------|-------|
| 1 | **497** | Configure Auth0 Apple connection | https://github.com/diego-torres/nutriconsultas/issues/497 | **open** | — | Auth0 tenant + Apple Developer Portal; **NEXT** |
| 2 | 498 | Add webhook configuration properties | https://github.com/diego-torres/nutriconsultas/issues/498 | open | — | `@ConfigurationProperties`; disabled by default |
| 3 | 499 | Add webhook controller | https://github.com/diego-torres/nutriconsultas/issues/499 | open | 498 | `POST /rest/webhooks/apple/sign-in` |
| 4 | 500 | Permit webhook path through Spring Security | https://github.com/diego-torres/nutriconsultas/issues/500 | open | 499 | Public route; verify payload in handler |
| 5 | 501 | Implement signed payload verification | https://github.com/diego-torres/nutriconsultas/issues/501 | open | 498, 499 | JWKS + JWS; consider Nimbus JOSE JWT |
| 6 | 502 | Persist notification events (Liquibase + JPA) | https://github.com/diego-torres/nutriconsultas/issues/502 | open | 498 | `apple_signin_notification`; idempotent |
| 7 | 503 | Create notification processing service | https://github.com/diego-torres/nutriconsultas/issues/503 | open | 501, 502 | Observe-only first; no destructive auto |
| 8 | 504 | Map Apple/Auth0 identity to local users | https://github.com/diego-torres/nutriconsultas/issues/504 | open | 497, 503 | Stable subject over email |
| 9 | 505 | Add Auth0 Management API client methods | https://github.com/diego-torres/nutriconsultas/issues/505 | open | 497 | Narrow scopes; delete disabled by default |
| 10 | 506 | Add safe account deletion workflow | https://github.com/diego-torres/nutriconsultas/issues/506 | open | 503, 504, 505 | Staged; no silent hard-delete |
| 11 | 507 | Handle private relay email changes | https://github.com/diego-torres/nutriconsultas/issues/507 | open | 503, 504 | Relay metadata; no email-as-primary-key |
| 12 | 508 | Add observability and operational alerts | https://github.com/diego-torres/nutriconsultas/issues/508 | open | 499, 503 | Metrics + structured logs |
| 13 | 509 | Add integration tests | https://github.com/diego-torres/nutriconsultas/issues/509 | open | 499–503 | No live Apple/Auth0 in tests |
| 14 | 510 | Document Apple Developer Portal setup | https://github.com/diego-torres/nutriconsultas/issues/510 | open | — | Expand [`apple-signin-setup.md`](docs/auth/apple-signin-setup.md) |
| 15 | 511 | Add production rollout plan | https://github.com/diego-torres/nutriconsultas/issues/511 | open | 497–510 | Phased: observe → metadata → restrict → optional delete |

---

## Rollout phases (#511)

| Phase | Behavior |
|-------|----------|
| **1 — Observe only** | Enable webhook; verify + persist; no Auth0/local mutations |
| **2 — Metadata** | Mark lifecycle metadata; relay changes; admin visibility |
| **3 — Restricted automation** | Block revoked users per policy; hard delete still manual |
| **4 — Optional deletion** | Only after legal/product approval + audit trail |

---

## Open questions

- What exact Auth0 connection name will Apple users use in production?
- Does Auth0 expose Apple stable subject in `user_id`, `identities`, or profile metadata for this tenant?
- Which account types can sign in with Apple: patients, nutritionists, platform admins, or all?
- Legal/product policy for Apple account deletion events?
- Should deletion events block access, soft-delete, anonymize, or only mark for review?
- Should relay email changes notify admins or users?

---

## Definition of done (track-level)

- Auth0 Apple login works for the iOS app
- Apple server-to-server notifications reach Minutriporcion backend
- Notifications are verified, persisted, deduplicated, and observable
- Apple/Auth0/local identity mapping is reliable
- Destructive events follow documented policy
- Tests cover verification, idempotency, mapping, and disabled destructive processing
- Docs distinguish Auth0 callback URL from Apple webhook endpoint
