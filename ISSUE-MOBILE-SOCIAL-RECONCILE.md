# Issue Registry — `[Mobile Social Reconcile]` track

Living index of GitHub issues that close planning [#558](https://github.com/diego-torres/nutriconsultas/issues/558) / mobile [Escanor4323/nutriconsultas-mobile#124](https://github.com/Escanor4323/nutriconsultas-mobile/issues/124): Auth0 social invite reconcile, verified-email fallback, and Auth0→Amazon SES verification mail.

**Repo:** [diego-torres/nutriconsultas](https://github.com/diego-torres/nutriconsultas)  
**Planning:** [#558](https://github.com/diego-torres/nutriconsultas/issues/558)  
**Epic:** [#560](https://github.com/diego-torres/nutriconsultas/issues/560)  
**Mobile consumer:** [Escanor4323/nutriconsultas-mobile#124](https://github.com/Escanor4323/nutriconsultas-mobile/issues/124)  
**Last updated:** 2026-07-18 — track issues created (#560–#569). **NEXT:** [#569](https://github.com/diego-torres/nutriconsultas/issues/569) registry/workflow (or ops #561–#563 in parallel).

> **Scope.** Harden `POST /rest/mobile/invitations/reconcile` for social login (credential-first, email hint-only), require `email_verified` on empty-body email fallback, lock PATCH `/patient/me` email vs JWT, document the reconcile matrix, and configure **Auth0 + Amazon SES** (verification mail, domain auth, Apple private relay). Does **not** replace Auth0 or invent a separate social onboarding API. Broader Auth0 API audience/scopes remain [#108](https://github.com/diego-torres/nutriconsultas/issues/108).

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

## Epic — Social invite reconcile + email verification

| Requirement | Issues |
|-------------|--------|
| Epic umbrella | #560 |
| Auth0 → Amazon SES email provider | #561 |
| SES SPF/DKIM + Apple private relay | #562 |
| Auth0 `email_verified` claim + Post-Login Action | #563 |
| Backend verified-email fallback + audit log | #564 |
| Reconcile-by-code rate limits | #565 |
| PATCH `/patient/me` email-lock | #566 |
| Reconcile matrix contract docs | #567 |
| Tests | #568 |
| Track registry / plan / workflow | #569 |

**Suggested order:** #569 → (#561 ∥ #562 ∥ #563) → #564 → (#565 ∥ #566) → #567 → #568.

| # | Title | URL | State | Depends on | Notes |
|---|-------|-----|-------|------------|-------|
| **560** | Epic — Auth0 social invite reconcile + email verification | https://github.com/diego-torres/nutriconsultas/issues/560 | open | #558 (planning) | Close #558 when epic done |
| **561** | Configure Auth0 custom email provider → Amazon SES | https://github.com/diego-torres/nutriconsultas/issues/561 | open | 560 | Tenant + AWS; no Spring code |
| **562** | SES domain SPF/DKIM + Apple private email relay registration | https://github.com/diego-torres/nutriconsultas/issues/562 | open | 560 | Complements #209 SES infra |
| **563** | Auth0 — map `email_verified` into access tokens + Post-Login Action | https://github.com/diego-torres/nutriconsultas/issues/563 | open | 560 | Prerequisite for #564 claim read |
| **564** | Enforce `email_verified` on reconcile email-fallback + audit log | https://github.com/diego-torres/nutriconsultas/issues/564 | open | 563 | Credential paths stay email-agnostic |
| **565** | Confirm/harden rate limits for authenticated reconcile-by-code | https://github.com/diego-torres/nutriconsultas/issues/565 | open | 560 | Existing per-sub limiter may suffice |
| **566** | PATCH `/patient/me` — lock email to JWT claim | https://github.com/diego-torres/nutriconsultas/issues/566 | open | 560 | Ignore or reject mismatch |
| **567** | Document reconcile matrix (ALIGNMENT-SPEC, gate, OpenAPI) | https://github.com/diego-torres/nutriconsultas/issues/567 | open | 564, 566 | Agent context-handoff blocker |
| **568** | Tests — social onboarding, REVOKED, verified-email fallback | https://github.com/diego-torres/nutriconsultas/issues/568 | open | 564–566 | Residual quality gate OK |
| **569** | Track registry + plan + workflow docs | https://github.com/diego-torres/nutriconsultas/issues/569 | **NEXT** | 560 | This file + AGENTS/AGENT-WORKFLOW pointers |

---

## Product decisions (authoritative)

| Decision | Policy |
|----------|--------|
| Nutritionist invite email | **Hint-only** |
| Apple private relay / social email mismatch | **Allow with code reconcile** |
| Empty-body email fallback | Requires `email_verified == true` |
| Connection-type branching | **None** in link logic (audit only) |
| Unverified db-auth + valid invite code | **Proceed** (do not block credential reconcile) |

---

## Cross-track links

| Track / issue | Interaction |
|---------------|-------------|
| [#558](https://github.com/diego-torres/nutriconsultas/issues/558) | Planning / security review source |
| [#108](https://github.com/diego-torres/nutriconsultas/issues/108) | Auth0 API audience/scopes (orthogonal; cross-link claim docs) |
| [#209](https://github.com/diego-torres/nutriconsultas/issues/209) | Invitation SES infra already shipped |
| [`ISSUE-APPLE-SIGNIN.md`](ISSUE-APPLE-SIGNIN.md) | Apple social + private relay lifecycle |
| [`ISSUE.md`](ISSUE.md) | Mobile API invitation onboarding (#132–#141) |
| [`docs/subscription/INVITATION-EMAIL.md`](docs/subscription/INVITATION-EMAIL.md) | SES runbook to extend for Auth0 provider |

---

## Definition of done (track-level)

- [ ] Auth0 verification mail via SES on verified domain
- [ ] SPF/DKIM + Apple relay sources configured
- [ ] Access tokens carry `email_verified`; fallback enforces it
- [ ] Credential reconcile remains connection-blind and email-agnostic
- [ ] Reconcile matrix in ALIGNMENT-SPEC + gate diagram + OpenAPI
- [ ] PATCH email-lock enforced
- [ ] Tests green; #558 and #560 closed

---

**How to update this file**

Same convention as [`ISSUE.md`](ISSUE.md): mark `in-progress` when a PR opens, `done` when merged. Keep **NEXT** on the unblocked issue agents should pick up.
