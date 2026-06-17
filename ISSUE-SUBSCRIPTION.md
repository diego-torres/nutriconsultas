# Issue Registry — `[Subscription]` track

Living index of GitHub issues that implement **subscription enforcement**, platform admin RBAC, clinic hierarchy, and plan-gated features on the nutritionist web app. Update when status changes (commit on the PR that closes work).

**Repo:** [diego-torres/nutriconsultas](https://github.com/diego-torres/nutriconsultas)  
**Workflow:** [`SUBSCRIPTION-ENFORCEMENT-WORKFLOW.md`](SUBSCRIPTION-ENFORCEMENT-WORKFLOW.md)  
**Design doc:** [`docs/subscription/SUBSCRIPTION-ENFORCEMENT-PLAN.md`](docs/subscription/SUBSCRIPTION-ENFORCEMENT-PLAN.md)  
**Last updated:** 2026-06-17 — #184 in progress on `subscription/184-admin-invitations`.

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
| 183 | Platform admin RBAC — enforce admin allowlist | https://github.com/diego-torres/nutriconsultas/issues/183 | **done** | — | Merged PR #200 |

**Suggested order:** #46 → **#180** → #181 → #182; **#183** in parallel when touching admin UI.

---

## Phase 1 — Billing & lifecycle

| # | Title | URL | State | Depends on | Notes |
|---|-------|-----|-------|-----------|-------|
| 189 | Payment provider integration (Mercado Pago / abstraction) | https://github.com/diego-torres/nutriconsultas/issues/189 | **done** | 180 | Mercado Pago + webhook idempotency |
| 204 | Tramitar integración operativa con Mercado Pago | https://github.com/diego-torres/nutriconsultas/issues/204 | open | 189 | Cuenta, credenciales, webhooks, sandbox/prod |
| 184 | Admin invitations + payment checkout | https://github.com/diego-torres/nutriconsultas/issues/184 | **in-progress** | 180, 182, 183, 189 | Branch `subscription/184-admin-invitations` |
| 185 | Subscription lifecycle — grace, payment override, notifications | https://github.com/diego-torres/nutriconsultas/issues/185 | open | 180, 184 | ACTIVE→GRACE→SUSPENDED |

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
| 190 | Enforce patient and nutritionist limits per plan | https://github.com/diego-torres/nutriconsultas/issues/190 | open | 181, 185 | 10 / 50 / unlimited; 20 nutritionists |
| 187 | Gate report tiers and PDF export by plan | https://github.com/diego-torres/nutriconsultas/issues/187 | open | 181, 185 | Branded PDFs via `NutritionistProfile` |

**Suggested order:** #190 and #187 can run in parallel after #181 + #185.

---

## Epic mapping (user requirements → issues)

| Requirement | Issues |
|-------------|--------|
| 1. Platform admin allowlist only | #183 (+ existing `PlatformAdminService`) |
| 2. Admin assigns nutriologo-* / director-consultorio | #182 |
| 3. Admin paid invitations, payment, grace, payment override | #184, #189, #185 |
| 4. Director invites nutritionists; enable/disable access | #186, #188 |
| 5. Patient & nutritionist limits | #190 |
| 6. Branded / tiered reports | #187 |
| 7. PDF export by plan | #187 |

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

## How to update

Same convention as [`ISSUE.md`](ISSUE.md): mark `in-progress` when PR opens, `done` when merged, advance `NEXT` in [`SUBSCRIPTION-ENFORCEMENT-WORKFLOW.md`](SUBSCRIPTION-ENFORCEMENT-WORKFLOW.md).
