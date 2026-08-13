# Mobile API contract docs

Canonical cross-repo contracts for the `[Mobile API]` track. Indexed from [`../../AGENT-WORKFLOW.md`](../../AGENT-WORKFLOW.md) and [`../../ISSUE.md`](../../ISSUE.md).

## Mobile track

| File | What it is |
|------|-----------|
| [`ALIGNMENT-SPEC.md`](ALIGNMENT-SPEC.md) | Source-of-truth contract — §F7 audience, §F8 schema/enum map, verified gaps, invitation gate §F8.6 |
| [`mobile-api-roadmap-v2.md`](mobile-api-roadmap-v2.md) | Per-endpoint (#91–#99) request/response JSON and field mappings |
| [`PHI-LOGGING-AUDIT.md`](PHI-LOGGING-AUDIT.md) | Completed PHI logging audit for `/rest/mobile/**` (#115, PR #168) |
| [`INVITATION-SECURITY-AUDIT.md`](INVITATION-SECURITY-AUDIT.md) | #141 acceptance audit — rate limits, enumeration, no-token logging |
| [`MOBILE-E2E-STATUS.md`](MOBILE-E2E-STATUS.md) | Live E2E status, Auth0 setup, HTTP code matrix |
| [`../auth0/PATIENT-POST-LOGIN-GATE.md`](../auth0/PATIENT-POST-LOGIN-GATE.md) | Auth0 Post-Login invitation gate (#140) — Action script + deployment |
| [`../api/openapi-mobile.yaml`](../api/openapi-mobile.yaml) | OpenAPI 3.1 export (#112, PR #164); regen: `scripts/export-openapi-mobile.sh` |
| [`PUSH-SETUP.md`](PUSH-SETUP.md) | APNs + FCM HTTP v1 credentials, local enable, key rotation (#575) |
| [`PUSH-CONTRACT.md`](PUSH-CONTRACT.md) | Devices API + push payload contract for mobile (#577); OpenAPI twin |
| [`APPOINTMENT-QUESTIONS-CONTRACT.md`](APPOINTMENT-QUESTIONS-CONTRACT.md) | Appointment question reminders CRUD (#587); OpenAPI twin |

**Status (2026-08-13):** [#587](https://github.com/diego-torres/nutriconsultas/issues/587) appointment question reminders — backend `in-progress` (`mobile-api/appointment-questions`).

**Status (2026-08-11):** Epic [#573](https://github.com/diego-torres/nutriconsultas/issues/573) mobile push — ~~#574~~–~~#577~~ backend contract/docs. (Older grocery status below retained.)

**Status (2026-06-30):** ~~#353~~ **in-progress** — grocery list endpoint. ~~#354~~ ~~#352~~ **done** (PR [#357](https://github.com/diego-torres/nutriconsultas/pull/357)). ~~#349~~ **done** (PR [#356](https://github.com/diego-torres/nutriconsultas/pull/356)).

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

**Nutritionist web NEXT:** None — all registered epics complete (~~#271~~–~~#272~~ system catalog create; ~~#285~~ done; ~~#281~~ done; ~~#280~~ done; ~~#238~~ done; ~~#237~~ done; ~~#236~~ done; ~~#259~~ done; platillo ownership ~~#257–#258~~ done; diet catalog ~~#232–#235~~ done; ~~#221–#223~~ MPX epic done).

**Subscription NEXT:** Registered track **complete** (~~#244~~ ✓ on `subscription/244-contact-form-prefill`; ~~#314~~ ~~#188~~ ~~#186~~ ~~#220~~ ~~#207~~ ~~#208~~ ~~#209~~ ~~#211~~ ~~#210~~ ~~#187~~ ~~#190~~ on `main`).

## Provenance / drift

Mobile consumer registry: [Escanor4323/nutriconsultas-mobile](https://github.com/Escanor4323/nutriconsultas-mobile) → `ISSUE.md`.

When sprint state changes, update this README, `ALIGNMENT-SPEC.md` §F8.3, `mobile-api-roadmap-v2.md` header, `MOBILE-E2E-STATUS.md` footer, [`ISSUE.md`](../../ISSUE.md), and [`AGENT-WORKFLOW.md`](../../AGENT-WORKFLOW.md) in the same PR.
