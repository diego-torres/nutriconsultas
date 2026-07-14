# Issue Registry — `[Support]` track

Living index of GitHub issues for **in-app support tickets** and the topbar user menu (**Perfil / Soporte / Acerca de / Salir**). Update when status changes (commit on the PR that closes work).

**Repo:** [diego-torres/nutriconsultas](https://github.com/diego-torres/nutriconsultas)  
**Plan:** [`docs/support/SUPPORT-TICKETS-PLAN.md`](docs/support/SUPPORT-TICKETS-PLAN.md)  
**Workflow:** [`SUPPORT-WORKFLOW.md`](SUPPORT-WORKFLOW.md)  
**Last updated:** 2026-07-14 — ~~#541~~ topbar **done** (this PR). **NEXT:** [#542](https://github.com/diego-torres/nutriconsultas/issues/542) Acerca de version (∥). #544 service in PR [#551](https://github.com/diego-torres/nutriconsultas/pull/551).

> **Scope.** Authenticated nutritionist web (`/admin/**`): support ticket create/list for users; platform-admin triage (user, subscription, title; update/close; active/closed filter); **Acerca de** version modal. **Does not** replace public `ContactInquiry`. Patient mobile API: [`ISSUE.md`](ISSUE.md). Platform admin patterns: contact inquiries / subscription admin.

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

## Epic — In-app support + user menu

| Requirement | Issues |
|-------------|--------|
| Epic umbrella | #540 |
| Topbar: Perfil, Soporte, Acerca de, Salir | #541 |
| Acerca de modal + version + README bump docs | #542 |
| Ticket schema (Liquibase) | #543 |
| Service + access control | #544 |
| Nutritionist Soporte UI | #545 |
| Platform admin Soporte UI | #546 |
| Tests + template validators | #547 |
| Track registry / plan / workflow docs | #548 |

**Suggested order:** #548 → #543 → #544 → (#541 ∥ #542) → #545 → #546 → #547 (tests also land with each feature PR).

| # | Title | URL | State | Depends on | Notes |
|---|-------|-----|-------|------------|-------|
| **540** | Epic — In-app support tickets + user menu | https://github.com/diego-torres/nutriconsultas/issues/540 | **open** | — | Umbrella; close when children done |
| **541** | Topbar user menu — Perfil, Soporte, Acerca de, Salir | https://github.com/diego-torres/nutriconsultas/issues/541 | **done** | 540 | `#aboutModal` contract; Soporte → `/admin/soporte` |
| **542** | Acerca de modal — app version + README bump process | https://github.com/diego-torres/nutriconsultas/issues/542 | **NEXT** | 540, ~~541~~ | Maven `project.version` → `appVersion` |
| **543** | Support ticket schema — Liquibase + entity + repository | https://github.com/diego-torres/nutriconsultas/issues/543 | **done** | 540 | Liquibase `034`; `SupportTicket` + repository |
| **544** | Support ticket service — create, list, update, close, filter | https://github.com/diego-torres/nutriconsultas/issues/544 | **in-progress** | ~~**543**~~ | PR [#551](https://github.com/diego-torres/nutriconsultas/pull/551) |
| **545** | Nutritionist Soporte page — own tickets + create form | https://github.com/diego-torres/nutriconsultas/issues/545 | **open** | **544**, 541 | `/admin/soporte` user view |
| **546** | Platform admin Soporte — list, filter, update, close | https://github.com/diego-torres/nutriconsultas/issues/546 | **open** | **544** | User + subscription + title columns |
| **547** | Tests + template validators for Soporte / Acerca de | https://github.com/diego-torres/nutriconsultas/issues/547 | **open** | 541–546 | May close incrementally with feature PRs |
| **548** | Plan + registry docs for support tickets track | https://github.com/diego-torres/nutriconsultas/issues/548 | **done** | 540 | `ISSUE-SUPPORT.md`, plan, workflow, AGENTS/README pointers |

---

## Cross-track links

| Track / area | Interaction |
|--------------|-------------|
| Platform admin | `PlatformAdminService`, `AbstractPlatformAdminController` (same as contact inquiries) |
| Subscription | Resolve creator plan/tier for admin list columns (do not store on ticket in v1) |
| ContactInquiry | Orthogonal public marketing inbox — keep separate |
| Liquibase #46 | All schema via incremental changesets |

---

## Definition of done (track-level)

- [ ] User menu: Perfil, Soporte, Acerca de, Salir
- [ ] Nutritionists create and list only their tickets
- [ ] Platform admins filter activos/cerrados; update and close; see user + subscription + title
- [ ] Acerca de shows version; README documents bump for `main` releases
- [ ] Liquibase + tests + template validators
- [ ] This registry marked `done` for all children; epic #540 closed

---

**How to update this file**

Same convention as [`ISSUE.md`](ISSUE.md): mark `in-progress` when a PR opens, `done` when merged. Keep **NEXT** on the unblocked issue agents should pick up.
