# Issue Registry — `[Subscription]` track

Living index of GitHub issues that implement **subscription enforcement**, platform admin RBAC, clinic hierarchy, and plan-gated features on the nutritionist web app. Update when status changes (commit on the PR that closes work).

**Repo:** [diego-torres/nutriconsultas](https://github.com/diego-torres/nutriconsultas)  
**Workflow:** [`SUBSCRIPTION-ENFORCEMENT-WORKFLOW.md`](SUBSCRIPTION-ENFORCEMENT-WORKFLOW.md)  
**Design doc:** [`docs/subscription/SUBSCRIPTION-ENFORCEMENT-PLAN.md`](docs/subscription/SUBSCRIPTION-ENFORCEMENT-PLAN.md)  
**Last updated:** 2026-06-19 — ~~#211~~ **done** (PR [#230](https://github.com/diego-torres/nutriconsultas/pull/230)); **NEXT:** #207 (Stripe provider). Public funnel: #243, #244 registered. Patient MPX **#221–#223:** [`ISSUE-NUTRITIONIST-WEB.md`](ISSUE-NUTRITIONIST-WEB.md). Production **#226** fixed (PR [#227](https://github.com/diego-torres/nutriconsultas/pull/227), invitation base URL).

> **Scope.** This registry tracks `[Subscription]` issues only. The patient mobile API lives in [`ISSUE.md`](ISSUE.md). Patient invitation onboarding (#132–#141) is orthogonal — do not merge nutritionist and patient invitation entities.

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

## Integration prerequisite

| # | Title | URL | State | Blocks |
|---|-------|-----|-------|--------|
| 46 | Implement Liquibase for database change management | https://github.com/diego-torres/nutriconsultas/issues/46 | **done** | — |

Subscription Liquibase changesets land **after** #46 baseline. Issue #183 (platform admin RBAC) can start before #46.

---

## Phase 0 — Schema & entitlements

| # | Title | URL | State | Depends on | Notes |
|---|-------|-----|-------|-----------|-------|
| **180** | Plan catalog, subscription schema & Liquibase | https://github.com/diego-torres/nutriconsultas/issues/180 | **done** | **46** | Merged PR #197 |
| 181 | SubscriptionEntitlementService — plan tier resolution | https://github.com/diego-torres/nutriconsultas/issues/181 | **done** | 180 | Merged PR #199 — central `hasEntitlement()` |
| 182 | Auth0 role sync — nutriologo-* and director-consultorio | https://github.com/diego-torres/nutriconsultas/issues/182 | **done** | 181, 108 | Merged PR #202 |
| 183 | Platform admin RBAC — enforce admin allowlist | https://github.com/diego-torres/nutriconsultas/issues/183 | **done** | — | Merged PR #200; GitHub closed 2026-06-17 |

**Suggested order:** #46 → **#180** → #181 → #182; **#183** in parallel when touching admin UI.

---

## Phase 1 — Billing & lifecycle

| # | Title | URL | State | Depends on | Notes |
|---|-------|-----|-------|-----------|-------|
| 189 | Payment provider integration (Mercado Pago / abstraction) | https://github.com/diego-torres/nutriconsultas/issues/189 | **done** | 180 | MP impl merged; **superseded by Stripe #207** |
| **207** | Payment provider integration (Stripe / abstraction) | https://github.com/diego-torres/nutriconsultas/issues/207 | **NEXT** | 180, 189 | Migrate MP → Stripe |
| 208 | Tramitar integración operativa con Stripe | https://github.com/diego-torres/nutriconsultas/issues/208 | open | 207 | Cuenta, credenciales, webhooks, test/live |
| ~~204~~ | ~~Tramitar integración operativa con Mercado Pago~~ | https://github.com/diego-torres/nutriconsultas/issues/204 | **deferred** | — | Cerrado; reemplazado por #208 |
| 184 | Admin invitations + payment checkout | https://github.com/diego-torres/nutriconsultas/issues/184 | **done** | 180, 182, 183 | Merged [PR #206](https://github.com/diego-torres/nutriconsultas/pull/206); GitHub closed 2026-06-17. Live checkout → Stripe (#207/#208); email delivery → #209 |
| 209 | Invitation email — SES (Terraform) + localhost console sender | https://github.com/diego-torres/nutriconsultas/issues/209 | open | 184 | SES prod; `email.mode=console` for local dev |
| 185 | Subscription lifecycle — grace, payment override, notifications | https://github.com/diego-torres/nutriconsultas/issues/185 | **done** | 180, ~~184~~ ✓ | Merged PR [#215](https://github.com/diego-torres/nutriconsultas/pull/215) |
| 210 | Platform admin revoke nutritionist access and allow re-invite | https://github.com/diego-torres/nutriconsultas/issues/210 | **done** | 184, 182, ~~185~~ ✓ | Merged PR [#224](https://github.com/diego-torres/nutriconsultas/pull/224) |
| **211** | Platform admin change nutritionist subscription plan tier | https://github.com/diego-torres/nutriconsultas/issues/211 | **done** | 181, 182, 184 | Merged PR [#230](https://github.com/diego-torres/nutriconsultas/pull/230) |

---

## Phase 2 — Clinic hierarchy

| # | Title | URL | State | Depends on | Notes |
|---|-------|-----|-------|-----------|-------|
| 186 | Clinic model + director-consultorio administration | https://github.com/diego-torres/nutriconsultas/issues/186 | open | 180, 181 | Seats, suspend members |
| 188 | Director invitations for nutritionists (no payment) | https://github.com/diego-torres/nutriconsultas/issues/188 | open | 186, 185 | Inherits clinic subscription |

---

## Phase 3 — Enforcement

| # | Title | URL | State | Depends on | Notes |
|---|-------|-----|-------|-----------|-------|
| 190 | Enforce patient and nutritionist limits per plan | https://github.com/diego-torres/nutriconsultas/issues/190 | **done** | 181, ~~185~~ ✓ | Merged PR [#216](https://github.com/diego-torres/nutriconsultas/pull/216) |
| 187 | Gate report tiers and PDF export by plan | https://github.com/diego-torres/nutriconsultas/issues/187 | **done** | 181, ~~185~~ ✓, ~~190~~ ✓ | Merged PR [#218](https://github.com/diego-torres/nutriconsultas/pull/218) |

**Suggested order:** ~~#211~~ ✓; **#207** / **#208** (Stripe) in parallel; #220 unblocked after ~~#210~~ ✓.

---

## Phase 4 — Public funnel (marketing → contact → invite)

| # | Title | URL | State | Depends on | Notes |
|---|-------|-----|-------|-----------|-------|
| 243 | Production reCAPTCHA keys for minutriporcion.com | https://github.com/diego-torres/nutriconsultas/issues/243 | open | — | Remove "testing purposes only" banner |
| 244 | Pre-fill contact form when clicking Solicitar Acceso for a plan | https://github.com/diego-torres/nutriconsultas/issues/244 | open | — | Plan slug on `ContactInquiry`; pairs with #184 |

**Suggested order:** #243 → #244 (or parallel). Blocks public booking form (#248) reCAPTCHA reuse.

---

## Phase 5 — Retention & maintenance

| # | Title | URL | State | Depends on | Notes |
|---|-------|-----|-------|-----------|-------|
| 220 | Retention cleanup — purge revoked nutritionist data with S3 backup | https://github.com/diego-torres/nutriconsultas/issues/220 | open | 210, 183, 46 | 90 días post-revoke; UI mantenimiento; backup S3; bitácora |

**Suggested order:** #220 after #210 merged (needs `access.revoke` audit + `CANCELLED` state).

---

## Epic mapping (user requirements → issues)

| Requirement | Issues |
|-------------|--------|
| 1. Platform admin allowlist only | #183 (+ existing `PlatformAdminService`) |
| 2. Admin assigns nutriologo-* / director-consultorio | #182 |
| 3. Admin paid invitations, payment, grace, payment override | #184, #189, #185 |
| 3b. Admin revoke access and change plan tier | ~~#210~~ ✓ PR [#224](https://github.com/diego-torres/nutriconsultas/pull/224), ~~#211~~ ✓ PR [#230](https://github.com/diego-torres/nutriconsultas/pull/230) |
| 3c. Retention purge + S3 backup after revoke | #220 |
| 4. Director invites nutritionists; enable/disable access | #186, #188 |
| 5. Patient & nutritionist limits | #190 — PR [#216](https://github.com/diego-torres/nutriconsultas/pull/216) ✓ |
| 5b. Patient slot rotation (export/import `.mpx`) | #221, #222, #223 — [`ISSUE-NUTRITIONIST-WEB.md`](ISSUE-NUTRITIONIST-WEB.md) |
| 6. Branded / tiered reports | #187 — PR [#218](https://github.com/diego-torres/nutriconsultas/pull/218) ✓ |
| 7. PDF export by plan | #187 — PR [#218](https://github.com/diego-torres/nutriconsultas/pull/218) ✓ |
| 8. Production reCAPTCHA on contact form | #243 |
| 9. Plan-aware Solicitar acceso CTA | #244 |

---

## Plan tier quick reference

| Slug | Patients | Nutritionists | PDF | Branded reports | User admin |
|------|----------|---------------|-----|-----------------|------------|
| `nutriologo-basico` | 10 | 1 | ✗ | ✗ | ✗ |
| `nutriologo-profesional` | 50 | 1 | ✓ | ✓ | ✗ |
| `nutriologo-plus` | ∞ | 1 | ✓ | ✓ | ✗ |
| `director-consultorio` | ∞ | 20 | ✓ | ✓ | ✓ |

Full matrix: [`SUBSCRIPTION-ENFORCEMENT-PLAN.md`](docs/subscription/SUBSCRIPTION-ENFORCEMENT-PLAN.md).

---

**How to update this file**

Same convention as [`ISSUE.md`](ISSUE.md): mark `in-progress` when PR opens, `done` when merged, advance `NEXT` in [`SUBSCRIPTION-ENFORCEMENT-WORKFLOW.md`](SUBSCRIPTION-ENFORCEMENT-WORKFLOW.md). Also update [`AGENT-WORKFLOW.md`](AGENT-WORKFLOW.md) subscription sprint pointer and [`docs/mobile-api/README.md`](docs/mobile-api/README.md) subscription row when status changes.
