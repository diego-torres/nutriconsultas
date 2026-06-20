# Issue Registry — `[Nutritionist Web]` track

Living index of GitHub issues for the **nutritionist Thymeleaf web app** (`/admin/**`) — patient roster, clinical workflows, diet/platillo management, and plan-slot rotation. Update when status changes (commit on the PR that closes work).

**Repo:** [diego-torres/nutriconsultas](https://github.com/diego-torres/nutriconsultas)  
**Plan (MPX):** [`docs/paciente/PATIENT-MPX-PLAN.md`](docs/paciente/PATIENT-MPX-PLAN.md)  
**Last updated:** 2026-06-20 — ~~#221~~ **done** (PR [#254](https://github.com/diego-torres/nutriconsultas/pull/254), deployed EC2). ~~#222~~ **done** (PR [#261](https://github.com/diego-torres/nutriconsultas/pull/261)). ~~#250~~ **done** (PR [#256](https://github.com/diego-torres/nutriconsultas/pull/256)). ~~#223~~ **done** (PR [#262](https://github.com/diego-torres/nutriconsultas/pull/262)). ~~#232~~ **done** (PR [#263](https://github.com/diego-torres/nutriconsultas/pull/263)). ~~#233~~ **done** (PR [#264](https://github.com/diego-torres/nutriconsultas/pull/264)). ~~#234~~ **done** (PR [#265](https://github.com/diego-torres/nutriconsultas/pull/265)). **NEXT:** [#235 diet grid filter](https://github.com/diego-torres/nutriconsultas/issues/235). Epics **#232–#242**, **#257–#259** (platillo ownership) registered.

> **Scope.** Nutritionist web features only. Patient mobile API: [`ISSUE.md`](ISSUE.md). Subscription enforcement: [`ISSUE-SUBSCRIPTION.md`](ISSUE-SUBSCRIPTION.md). Public booking: [`ISSUE-PUBLIC-BOOKING.md`](ISSUE-PUBLIC-BOOKING.md). Do not mix mobile JWT, subscription billing, or public booking into unrelated PRs unless explicitly coupled.

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
| **221** | Export patient registration to .mpx (YAML, no history) | https://github.com/diego-torres/nutriconsultas/issues/221 | **done** | #156, #190 (context) | PR [#254](https://github.com/diego-torres/nutriconsultas/pull/254); `GET /admin/pacientes/{id}/export.mpx` |
| **222** | Import patient registration from .mpx file | https://github.com/diego-torres/nutriconsultas/issues/222 | **done** | **221**, #190 | PR [#261](https://github.com/diego-torres/nutriconsultas/pull/261); `POST /admin/pacientes/importar.mpx` |
| **223** | Patient export and delete actions with pre-delete backup | https://github.com/diego-torres/nutriconsultas/issues/223 | **done** | **221**, **222**, #190 | PR [#262](https://github.com/diego-torres/nutriconsultas/pull/262); SweetAlert; `DELETE /rest/pacientes/{id}` |

**All tiers:** export/import/delete UI is **not** entitlement-gated; basic plan is the primary use case.

---

## Epic — Diet catalog management

System template diets (`userId = system:template-dietas`), grid actions, and ownership filters.

| Requirement | Issues |
|-------------|--------|
| Platform admins edit system diets | #232 |
| Grid: edit action | #233 |
| Grid: delete with patient-assignment guard | #234 |
| Grid: filter todas / sistema / propias | #235 |

**Suggested order:** #232 → #233 + #234 + #235 (parallel after #232).

| # | Title | URL | State | Depends on | Notes |
|---|-------|-----|-------|-----------|-------|
| **232** | Platform admins can edit system template diets | https://github.com/diego-torres/nutriconsultas/issues/232 | **done** | #183 | PR [#263](https://github.com/diego-torres/nutriconsultas/pull/263); `DietaAuthorization` |
| **233** | Diet list grid — edit action | https://github.com/diego-torres/nutriconsultas/issues/233 | **done** | — | PR [#264](https://github.com/diego-torres/nutriconsultas/pull/264); `DietaAuthorization.canModify` |
| **234** | Diet list grid — delete action with patient-usage guard | https://github.com/diego-torres/nutriconsultas/issues/234 | **done** | — | PR [#265](https://github.com/diego-torres/nutriconsultas/pull/265); `DietaDeletionService` |
| 235 | Diet list grid — filter all, system, or own diets | https://github.com/diego-torres/nutriconsultas/issues/235 | open | — | Server-side filter on `/rest/dietas` grid |

---

## Epic — Platillo catalog ownership

Lock **system** catalog platillos for nutritionists; **owned** platillos editable by creator; **copy** to fork without mutating shared rows.

| Requirement | Issues |
|-------------|--------|
| System platillos read-only for non-admin | #257 |
| Creator can edit/delete own platillos | #258 |
| Copy platillo into owned catalog row | #259 |

**Suggested order:** #257 → #258 → #259 (ownership schema before copy).

| # | Title | URL | State | Depends on | Notes |
|---|-------|-----|-------|-----------|-------|
| 257 | Lock system catalog platillos for non-admin users | https://github.com/diego-torres/nutriconsultas/issues/257 | open | #183, #46 | `userId` / system sentinel; UI read-only; server guards |
| 258 | Nutritionist-owned platillos — creator can edit and delete | https://github.com/diego-torres/nutriconsultas/issues/258 | open | **257**, #46 | Multi-tenant like `Dieta.userId`; grid filter own + system |
| 259 | Copy platillo — duplicate into nutritionist-owned catalog row | https://github.com/diego-torres/nutriconsultas/issues/259 | open | **257**, **258** | SweetAlert optional; clone ingredients; complements #250 `sourcePlatilloId` |

---

## Epic — Nutritionist branding (profile & PDF)

| Requirement | Issues |
|-------------|--------|
| Show logo on profile after upload | #236 |
| PDF logo standard size (~1.5 × 1.5 in) | #237 |

**Suggested order:** #236 → #237 (or parallel).

| # | Title | URL | State | Depends on | Notes |
|---|-------|-----|-------|-----------|-------|
| 236 | Show uploaded logo on nutritionist profile page | https://github.com/diego-torres/nutriconsultas/issues/236 | open | #187 (context) | `NutritionistProfile` / S3 preview |
| 237 | PDF reports — standard logo size (~1.5 × 1.5 in) | https://github.com/diego-torres/nutriconsultas/issues/237 | open | #187 | `DietaPdfService`, Flying Saucer CSS |

---

## Epic — Diet & platillo authoring UX

| Requirement | Issues |
|-------------|--------|
| Diet macro table below caloric distribution | #238 |
| Add-ingredient dialog: recalc weight from portion qty | #239 |
| Round fractions to ½, ¼, ⅓ in platillo table & PDFs | #240 |

**Suggested order:** #238 independent; #239 → #240.

| # | Title | URL | State | Depends on | Notes |
|---|-------|-----|-------|-----------|-------|
| 238 | Diet detail — macronutrients table below caloric distribution | https://github.com/diego-torres/nutriconsultas/issues/238 | open | — | Match platillo macro table style |
| 239 | Add-ingredient dialog — recalculate weight from portion quantity | https://github.com/diego-torres/nutriconsultas/issues/239 | open | — | Platillo + dietas modals |
| 240 | Round ingredient fractions to ½, ¼, or ⅓ in UI and meal-plan PDFs | https://github.com/diego-torres/nutriconsultas/issues/240 | open | — | Extend `AbstractFraccionable` |

---

## Epic — Patient profile & clinical corrections

| Requirement | Issues |
|-------------|--------|
| Selectable patient avatars | #241 |
| Per-field anthropometric correction + recalc | #242 |

**Suggested order:** parallel.

| # | Title | URL | State | Depends on | Notes |
|---|-------|-----|-------|-----------|-------|
| 241 | Patient profile — selectable patient avatars | https://github.com/diego-torres/nutriconsultas/issues/241 | open | #46 | Liquibase `avatarId` on `Paciente` |
| 242 | Anthropometrics — per-field correction with recalculation | https://github.com/diego-torres/nutriconsultas/issues/242 | open | #161 (context) | Edit icon per field; derived metrics |

---

## Bugs

| # | Title | URL | State | Depends on | Notes |
|---|-------|-----|-------|-----------|-------|
| **250** | Diet ingesta platillo link uses `PlatilloIngesta.id` instead of catalog `Platillo.id` | https://github.com/diego-torres/nutriconsultas/issues/250 | **done** | #46 | PR [#256](https://github.com/diego-torres/nutriconsultas/pull/256); `sourcePlatilloId` + Liquibase backfill |

**Repro:** Menú vegetal 02 → Cena → "Frijoles con tortilla" links to `/admin/platillos/32` (wrong catalog row) instead of `/admin/platillos/97`. Name displays correctly; portion REST APIs unaffected.

**Fix sketch:** persist `sourcePlatilloId` on `PlatilloIngesta` in `PlatilloIngestaMapping`; template uses catalog id for `/admin/platillos/{id}` only.

---

## Cross-track links

| Track | Interaction |
|-------|-------------|
| #190 Patient limits | Import and manual alta call `assertCanCreatePatient`; delete frees a slot |
| #109 Mobile linkage | Delete clears `patientAuthSub`; not stored in `.mpx` |
| #220 Retention purge | Platform admin purge of **revoked** nutritionists — orthogonal to nutritionist-initiated patient delete |
| #132 Patient invitations | Onboarding `Paciente.status` — import creates `ACTIVE` patient unless product specifies otherwise |
| #183 Platform admin | System diet edit (#232); system platillo edit (#257) |
| #187 Branded PDF | Logo profile (#236) and PDF sizing (#237) |
| #198 Diet templates | System diets seeded; editable by admin via #232 |
| #243 / #244 Subscription | reCAPTCHA + Solicitar acceso pre-fill — public funnel, not admin UI |
| #245–#248 Public booking | Nutritionist hours in profile (#246) — separate track |

---

**How to update this file**

Same convention as [`ISSUE.md`](ISSUE.md): mark `in-progress` when PR opens, `done` when merged. Reference this registry from [`AGENT-WORKFLOW.md`](AGENT-WORKFLOW.md) and [`AGENTS.md`](AGENTS.md) when adding nutritionist-web sprint pointers.
