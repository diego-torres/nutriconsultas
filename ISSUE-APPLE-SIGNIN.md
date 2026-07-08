# Issue Registry — `[Apple Sign-In]` track

Living index of GitHub issues that implement **Sign in with Apple** through Auth0 and Apple **server-to-server account notifications** on the Minutriporcion backend. Update when status changes (commit on the PR that closes work).

**Repo:** [diego-torres/nutriconsultas](https://github.com/diego-torres/nutriconsultas)  
**Roadmap:** [`docs/auth/apple-signin-backend-roadmap.md`](docs/auth/apple-signin-backend-roadmap.md)  
**Setup (maintainer runbook):** [`docs/auth/apple-signin-setup.md`](docs/auth/apple-signin-setup.md)  
**Deletion runbook:** [`docs/auth/apple-signin-deletion-runbook.md`](docs/auth/apple-signin-deletion-runbook.md)  
**Observability:** [`docs/auth/apple-signin-observability.md`](docs/auth/apple-signin-observability.md)  
**Production rollout:** [`docs/auth/apple-signin-rollout.md`](docs/auth/apple-signin-rollout.md)  
**Epic comment:** [#497](https://github.com/diego-torres/nutriconsultas/issues/497#issuecomment-4904658287)  
**Last updated:** 2026-07-08 — ~~#511~~ **done** on `apple-signin/511-rollout-plan`. Apple Sign-In backend track **complete** (#497–#511).

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
| 1 | 497 | Configure Auth0 Apple connection | https://github.com/diego-torres/nutriconsultas/issues/497 | **done** | — | Auth0 prod tenant (`minutriporcion-prod.us.auth0.com`) + Apple social connection (2026-07-07) |
| 2 | 498 | Add webhook configuration properties | https://github.com/diego-torres/nutriconsultas/issues/498 | **done** | — | PR #513 |
| 3 | 499 | Add webhook controller | https://github.com/diego-torres/nutriconsultas/issues/499 | **done** | 498 | PR #513 |
| 4 | 500 | Permit webhook path through Spring Security | https://github.com/diego-torres/nutriconsultas/issues/500 | **done** | 499 | PR #513 |
| 5 | 501 | Implement signed payload verification | https://github.com/diego-torres/nutriconsultas/issues/501 | **done** | 498, 499 | PR #513 |
| 6 | 502 | Persist notification events (Liquibase + JPA) | https://github.com/diego-torres/nutriconsultas/issues/502 | **done** | 498 | PR #513 |
| 7 | 503 | Create notification processing service | https://github.com/diego-torres/nutriconsultas/issues/503 | **done** | 501, 502 | PR #513 |
| 8 | 504 | Map Apple/Auth0 identity to local users | https://github.com/diego-torres/nutriconsultas/issues/504 | **done** | 497, 503 | PR #514 |
| 9 | 505 | Add Auth0 Management API client methods | https://github.com/diego-torres/nutriconsultas/issues/505 | **done** | 497 | PR #514 |
| 10 | **506** | Add safe account deletion workflow | https://github.com/diego-torres/nutriconsultas/issues/506 | **done** | 503, 504, 505 | PR #515 |
| 11 | **507** | Handle private relay email changes | https://github.com/diego-torres/nutriconsultas/issues/507 | **done** | 503, 504 | PR #516 |
| 12 | **508** | Add observability and operational alerts | https://github.com/diego-torres/nutriconsultas/issues/508 | **done** | 499, 503 | PR #517 |
| 13 | **509** | Add integration tests | https://github.com/diego-torres/nutriconsultas/issues/509 | **done** | 499–503 | PR #518 |
| 14 | **510** | Document Apple Developer Portal setup | https://github.com/diego-torres/nutriconsultas/issues/510 | **done** | — | PR #519 |
| 15 | **511** | Add production rollout plan | https://github.com/diego-torres/nutriconsultas/issues/511 | **done** | 497–510 | [`apple-signin-rollout.md`](docs/auth/apple-signin-rollout.md) |

---

## Rollout phases (#511)

Documented in [`docs/auth/apple-signin-rollout.md`](docs/auth/apple-signin-rollout.md).

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
