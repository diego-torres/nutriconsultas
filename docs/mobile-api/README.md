# Mobile API contract docs

Canonical cross-repo contracts for the `[Mobile API]` track. Indexed from [`../../AGENT-WORKFLOW.md`](../../AGENT-WORKFLOW.md) and [`../../ISSUE.md`](../../ISSUE.md).

## Mobile track

| File | What it is |
|------|-----------|
| [`ALIGNMENT-SPEC.md`](ALIGNMENT-SPEC.md) | Source-of-truth contract — §F7 audience, §F8 schema/enum map, verified gaps, invitation gate §F8.6 |
| [`mobile-api-roadmap-v2.md`](mobile-api-roadmap-v2.md) | Per-endpoint (#91–#99) request/response JSON and field mappings |
| [`PHI-LOGGING-AUDIT.md`](PHI-LOGGING-AUDIT.md) | Completed PHI logging audit for `/rest/mobile/**` (#115, PR #168) |
| [`MOBILE-E2E-STATUS.md`](MOBILE-E2E-STATUS.md) | Live E2E status, Auth0 setup, HTTP code matrix |
| [`../api/openapi-mobile.yaml`](../api/openapi-mobile.yaml) | OpenAPI 3.1 export (#112, PR #164); regen: `scripts/export-openapi-mobile.sh` |

**Status (2026-06-19):** ~~#132~~ ~~#133~~ done on `main` (PRs #214, [#229](https://github.com/diego-torres/nutriconsultas/pull/229)). **NEXT:** [#134](https://github.com/diego-torres/nutriconsultas/issues/134) create invitation.

## Related registries (same repo)

| File | What it is |
|------|-----------|
| [`../../ISSUE.md`](../../ISSUE.md) | Mobile issue registry (#91–#141, #156, #46) |
| [`../../AGENT-WORKFLOW.md`](../../AGENT-WORKFLOW.md) | Agent workflow — phases, CI, sprint pointer |
| [`../../AGENTS.md`](../../AGENTS.md) | Agent onboarding summary |
| [`../db/LIQUIBASE.md`](../db/LIQUIBASE.md) | Liquibase baseline + incremental changesets (#46) |

## Parallel track (subscription)

| File | What it is |
|------|-----------|
| [`../../ISSUE-SUBSCRIPTION.md`](../../ISSUE-SUBSCRIPTION.md) | Subscription issue registry (#180–#211) |
| [`../../SUBSCRIPTION-ENFORCEMENT-WORKFLOW.md`](../../SUBSCRIPTION-ENFORCEMENT-WORKFLOW.md) | Subscription agent workflow |
| [`../subscription/SUBSCRIPTION-ENFORCEMENT-PLAN.md`](../subscription/SUBSCRIPTION-ENFORCEMENT-PLAN.md) | Plan tiers, entitlements, lifecycle |

## Parallel track (nutritionist web)

| File | What it is |
|------|-----------|
| [`../../ISSUE-NUTRITIONIST-WEB.md`](../../ISSUE-NUTRITIONIST-WEB.md) | Patient MPX epic (#221–#223) |
| [`../paciente/PATIENT-MPX-PLAN.md`](../paciente/PATIENT-MPX-PLAN.md) | Export/import plan |

**Nutritionist web NEXT:** [#223](https://github.com/diego-torres/nutriconsultas/issues/223) export/delete UI. ~~#221~~ export · ~~#222~~ import done (PR [#261](https://github.com/diego-torres/nutriconsultas/pull/261)).

**Subscription NEXT:** [#207](https://github.com/diego-torres/nutriconsultas/issues/207) Stripe payment provider (~~#211~~ PR [#230](https://github.com/diego-torres/nutriconsultas/pull/230), ~~#210~~ PR [#224](https://github.com/diego-torres/nutriconsultas/pull/224), ~~#187~~ PR [#218](https://github.com/diego-torres/nutriconsultas/pull/218), ~~#190~~ PR [#216](https://github.com/diego-torres/nutriconsultas/pull/216) on `main`).

## Provenance / drift

Mobile consumer registry: [Escanor4323/nutriconsultas-mobile](https://github.com/Escanor4323/nutriconsultas-mobile) → `ISSUE.md`.

When sprint state changes, update this README, `ALIGNMENT-SPEC.md` §F8.3, `mobile-api-roadmap-v2.md` header, `MOBILE-E2E-STATUS.md` footer, [`ISSUE.md`](../../ISSUE.md), and [`AGENT-WORKFLOW.md`](../../AGENT-WORKFLOW.md) in the same PR.
