# Mobile API contract docs

Canonical cross-repo contracts for the `[Mobile API]` track. Indexed from [`../../AGENT-WORKFLOW.md`](../../AGENT-WORKFLOW.md) and [`../../ISSUE.md`](../../ISSUE.md).

## Mobile track

| File | What it is |
|------|-----------|
| [`ALIGNMENT-SPEC.md`](ALIGNMENT-SPEC.md) | Source-of-truth contract — §F7 audience, §F8 schema/enum map, verified gaps, invitation gate §F8.6 |
| [`mobile-api-roadmap-v2.md`](mobile-api-roadmap-v2.md) | Per-endpoint (#91–#99) request/response JSON and field mappings |
| [`PHI-LOGGING-AUDIT.md`](PHI-LOGGING-AUDIT.md) | Completed PHI logging audit for `/rest/mobile/**` (#115, PR #168) |
| [`MOBILE-E2E-STATUS.md`](MOBILE-E2E-STATUS.md) | Live E2E status, Auth0 setup, HTTP code matrix |
| [`../auth0/PATIENT-POST-LOGIN-GATE.md`](../auth0/PATIENT-POST-LOGIN-GATE.md) | Auth0 Post-Login invitation gate (#140) — Action script + deployment |
| [`../api/openapi-mobile.yaml`](../api/openapi-mobile.yaml) | OpenAPI 3.1 export (#112, PR #164); regen: `scripts/export-openapi-mobile.sh` |

**Status (2026-06-22):** ~~#132~~ ~~#133~~ ~~#134~~ ~~#135~~ ~~#136~~ ~~#137~~ ~~#138~~ ~~#139~~ ~~#140~~ done ([`docs/auth0/PATIENT-POST-LOGIN-GATE.md`](../auth0/PATIENT-POST-LOGIN-GATE.md)). **NEXT:** [#141](https://github.com/diego-torres/nutriconsultas/issues/141) invitation hardening.

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

**Nutritionist web NEXT:** [#271](https://github.com/diego-torres/nutriconsultas/issues/271)–[#272](https://github.com/diego-torres/nutriconsultas/issues/272) system catalog create. ~~#285~~ done (`issue-285-platillo-inline-cantidad`, 340a318). ~~#281~~ done (`issue-281-ingesta-platillo-ingredient-edit`). ~~#280~~ done (PR [#284](https://github.com/diego-torres/nutriconsultas/pull/284)). ~~#238~~ done (PR [#279](https://github.com/diego-torres/nutriconsultas/pull/279)). ~~#237~~ done (PR [#278](https://github.com/diego-torres/nutriconsultas/pull/278)). ~~#236~~ done (PR [#274](https://github.com/diego-torres/nutriconsultas/pull/274)). ~~#259~~ done (PR [#270](https://github.com/diego-torres/nutriconsultas/pull/270)); platillo ownership ~~#257–#258~~ done; diet catalog ~~#232–#235~~ done; ~~#221–#223~~ MPX epic done.

**Subscription NEXT:** Registered track **complete** (~~#244~~ ✓ on `subscription/244-contact-form-prefill`; ~~#314~~ ~~#188~~ ~~#186~~ ~~#220~~ ~~#207~~ ~~#208~~ ~~#209~~ ~~#211~~ ~~#210~~ ~~#187~~ ~~#190~~ on `main`).

## Provenance / drift

Mobile consumer registry: [Escanor4323/nutriconsultas-mobile](https://github.com/Escanor4323/nutriconsultas-mobile) → `ISSUE.md`.

When sprint state changes, update this README, `ALIGNMENT-SPEC.md` §F8.3, `mobile-api-roadmap-v2.md` header, `MOBILE-E2E-STATUS.md` footer, [`ISSUE.md`](../../ISSUE.md), and [`AGENT-WORKFLOW.md`](../../AGENT-WORKFLOW.md) in the same PR.
