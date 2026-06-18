# Issue Registry — `[Nutritionist Web]` track

Living index of GitHub issues for the **nutritionist Thymeleaf web app** (`/admin/**`) — patient roster, clinical workflows, and plan-slot management. Update when status changes (commit on the PR that closes work).

**Repo:** [diego-torres/nutriconsultas](https://github.com/diego-torres/nutriconsultas)  
**Plan:** [`docs/paciente/PATIENT-MPX-PLAN.md`](docs/paciente/PATIENT-MPX-PLAN.md)  
**Last updated:** 2026-06-18 — MPX export/import epic **#221–#223** registered.

> **Scope.** Nutritionist web features only. Patient mobile API: [`ISSUE.md`](ISSUE.md). Subscription enforcement: [`ISSUE-SUBSCRIPTION.md`](ISSUE-SUBSCRIPTION.md). Do not mix mobile JWT or subscription billing into patient MPX PRs unless explicitly coupled.

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

## Epic — Patient registration export/import (`.mpx`)

Help nutritionists **rotate patient slots** (especially **plan básico** cap via #190) by exporting **registration profile only** to a portable file, deleting the in-app record (and clinical history), and re-importing later.

| Requirement | Issues |
|-------------|--------|
| Export registration YAML to `.mpx` (no history) | #221 |
| Import `.mpx` as new patient (counts toward cap) | #222 |
| UI: Exportar / Eliminar with pre-delete export + history warning | #223 |

**Suggested order:** #221 → #222 → #223.

| # | Title | URL | State | Depends on | Notes |
|---|-------|-----|-------|-----------|-------|
| **221** | Export patient registration to .mpx (YAML, no history) | https://github.com/diego-torres/nutriconsultas/issues/221 | **NEXT** | #156, #190 (context) | Defines `mpxVersion: 1`; `docs/paciente/MPX-FORMAT.md` at implement time |
| 222 | Import patient registration from .mpx file | https://github.com/diego-torres/nutriconsultas/issues/222 | open | **221**, #190 | New `Paciente`; no history restore |
| 223 | Patient export and delete actions with pre-delete backup | https://github.com/diego-torres/nutriconsultas/issues/223 | open | **221**, **222**, #190 | SweetAlert; historial loss copy |

**All tiers:** export/import/delete UI is **not** entitlement-gated; basic plan is the primary use case.

---

## Cross-track links

| Track | Interaction |
|-------|-------------|
| #190 Patient limits | Import and manual alta call `assertCanCreatePatient`; delete frees a slot |
| #109 Mobile linkage | Delete clears `patientAuthSub`; not stored in `.mpx` |
| #220 Retention purge | Platform admin purge of **revoked** nutritionists — orthogonal to nutritionist-initiated patient delete |
| #132 Patient invitations | Onboarding `Paciente.status` — import creates `ACTIVE` patient unless product specifies otherwise |

---

**How to update this file**

Same convention as [`ISSUE.md`](ISSUE.md): mark `in-progress` when PR opens, `done` when merged. Reference this registry from [`AGENT-WORKFLOW.md`](AGENT-WORKFLOW.md) and [`AGENTS.md`](AGENTS.md) when adding nutritionist-web sprint pointers.
