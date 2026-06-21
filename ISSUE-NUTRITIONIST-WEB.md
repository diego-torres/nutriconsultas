# Issue Registry — `[Nutritionist Web]` track

Living index of GitHub issues for the **nutritionist Thymeleaf web app** (`/admin/**`) — patient roster, clinical workflows, diet/platillo management, and plan-slot rotation. Update when status changes (commit on the PR that closes work).

**Repo:** [diego-torres/nutriconsultas](https://github.com/diego-torres/nutriconsultas)  
**Plan (MPX):** [`docs/paciente/PATIENT-MPX-PLAN.md`](docs/paciente/PATIENT-MPX-PLAN.md)  
**Last updated:** 2026-06-21 — ~~#242~~ **done** (branch `issue-242-anthropometric-field-edit`). ~~#241~~ **done** (PR [#291](https://github.com/diego-torres/nutriconsultas/pull/291)). Patient UX epic **complete** (#241–#242). ~~#272~~ **done** (PR [#289](https://github.com/diego-torres/nutriconsultas/pull/289)). ~~#271~~ **done** (PR [#288](https://github.com/diego-torres/nutriconsultas/pull/288)).

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

**Suggested order:** #232 → #233 + #234 + #235 (parallel after #232). **Epic complete** after #235.

| # | Title | URL | State | Depends on | Notes |
|---|-------|-----|-------|-----------|-------|
| **232** | Platform admins can edit system template diets | https://github.com/diego-torres/nutriconsultas/issues/232 | **done** | #183 | PR [#263](https://github.com/diego-torres/nutriconsultas/pull/263); `DietaAuthorization` |
| **233** | Diet list grid — edit action | https://github.com/diego-torres/nutriconsultas/issues/233 | **done** | — | PR [#264](https://github.com/diego-torres/nutriconsultas/pull/264); `DietaAuthorization.canModify` |
| **234** | Diet list grid — delete action with patient-usage guard | https://github.com/diego-torres/nutriconsultas/issues/234 | **done** | — | PR [#265](https://github.com/diego-torres/nutriconsultas/pull/265); `DietaDeletionService` |
| **235** | Diet list grid — filter all, system, or own diets | https://github.com/diego-torres/nutriconsultas/issues/235 | **done** | — | PR [#267](https://github.com/diego-torres/nutriconsultas/pull/267); `ownershipFilter`; default **todas** |

---

## Epic — Platillo catalog ownership

Lock **system** catalog platillos for nutritionists; **owned** platillos editable by creator; **copy** to fork without mutating shared rows.

| Requirement | Issues |
|-------------|--------|
| System platillos read-only for non-admin | #257 |
| Creator can edit/delete own platillos | #258 |
| Copy platillo into owned catalog row | #259 |

**Suggested order:** #257 → #258 → #259 (ownership schema before copy). **Epic complete** (PR [#270](https://github.com/diego-torres/nutriconsultas/pull/270)).

| # | Title | URL | State | Depends on | Notes |
|---|-------|-----|-------|-----------|-------|
| **257** | Lock system catalog platillos for non-admin users | https://github.com/diego-torres/nutriconsultas/issues/257 | **done** | #183, #46 | PR [#268](https://github.com/diego-torres/nutriconsultas/pull/268); `PlatilloAuthorization`; `user_id` backfill; UI read-only |
| **258** | Nutritionist-owned platillos — creator can edit and delete | https://github.com/diego-torres/nutriconsultas/issues/258 | **done** | **257**, #46 | PR [#269](https://github.com/diego-torres/nutriconsultas/pull/269); ownership + catalog filter + delete guard |
| **259** | Copy platillo — duplicate into nutritionist-owned catalog row | https://github.com/diego-torres/nutriconsultas/issues/259 | **done** | **257**, **258** | PR [#270](https://github.com/diego-torres/nutriconsultas/pull/270); `Fixes #259`; duplicate endpoint + SweetAlert UI |

---

## Epic — System catalog authoring (platform admin create)

Platform admins can **edit** seeded system rows (#232 diets, #257 platillos) but **create** paths still assign the OAuth `sub` as owner. New issues cover admin-only creation of shared catalog entries without Liquibase deploys.

| Requirement | Issues |
|-------------|--------|
| Platform admin create system catalog platillo | #271 |
| Platform admin create system template diet | #272 |

**Suggested order:** #271 + #272 parallel (after #257 and #232 respectively).

| # | Title | URL | State | Depends on | Notes |
|---|-------|-----|-------|-----------|-------|
| **271** | Platform admin — create system catalog platillo | https://github.com/diego-torres/nutriconsultas/issues/271 | **done** | #183, **257** | PR [#288](https://github.com/diego-torres/nutriconsultas/pull/288); `system:catalog-platillos` on create |
| **272** | Platform admin — create system template diet | https://github.com/diego-torres/nutriconsultas/issues/272 | **done** | #183, **232** | PR [#289](https://github.com/diego-torres/nutriconsultas/pull/289); `system:template-dietas` on create |

---

## Epic — Nutritionist branding (profile & PDF)

| Requirement | Issues |
|-------------|--------|
| Show logo on profile after upload | #236 |
| PDF logo standard size (~1.5 × 1.5 in) | #237 |

**Suggested order:** #236 → #237 (or parallel).

| # | Title | URL | State | Depends on | Notes |
|---|-------|-----|-------|-----------|-------|
| **236** | Show uploaded logo on nutritionist profile page | https://github.com/diego-torres/nutriconsultas/issues/236 | **done** | #187 (context) | PR [#274](https://github.com/diego-torres/nutriconsultas/pull/274); `GET /admin/perfil/logo` |
| **237** | PDF reports — standard logo size (~1.5 × 1.5 in) | https://github.com/diego-torres/nutriconsultas/issues/237 | **done** | #187 | PR [#278](https://github.com/diego-torres/nutriconsultas/pull/278); `PdfLogoDimensions` + explicit pt sizing for Flying Saucer |

---

## Epic — Diet & platillo authoring UX

| Requirement | Issues |
|-------------|--------|
| Diet macro table below caloric distribution | #238 |
| Add-ingredient dialog: recalc weight from portion qty | #239 |
| Round fractions to ½, ¼, ⅓ in platillo table & PDFs | #240 |
| Platillo image upload size limit + user-facing oversize error | #275 |

**Suggested order:** #238 independent; ~~#239~~ **done** → ~~#240~~ **done** (PR [#283](https://github.com/diego-torres/nutriconsultas/pull/283)). ~~#275~~ **done** (PR [#276](https://github.com/diego-torres/nutriconsultas/pull/276)). **Epic complete** after #240.

| # | Title | URL | State | Depends on | Notes |
|---|-------|-----|-------|-----------|-------|
| **238** | Diet detail — macronutrients table below caloric distribution | https://github.com/diego-torres/nutriconsultas/issues/238 | **done** | — | PR [#279](https://github.com/diego-torres/nutriconsultas/pull/279); macro table + layout |
| **239** | Add-ingredient dialog — recalculate weight from portion quantity | https://github.com/diego-torres/nutriconsultas/issues/239 | **done** | — | `issue-239-ingredient-weight-recalc` (bd07fb4); platillo + dietas modals; ingesta card scroll |
| **240** | Round ingredient fractions to ½, ¼, or ⅓ in UI and meal-plan PDFs | https://github.com/diego-torres/nutriconsultas/issues/240 | **done** | — | PR [#283](https://github.com/diego-torres/nutriconsultas/pull/283); `AbstractFraccionable` + gram display for `unidad = g` |
| **275** | Platillo image upload — raise size limit and show user-facing error | https://github.com/diego-torres/nutriconsultas/issues/275 | **done** | — | PR [#276](https://github.com/diego-torres/nutriconsultas/pull/276); 10MB limit; `AdminMultipartExceptionHandler` |

---

## Epic — Diet detail nutrients & ingesta platillo editing

Full nutrient visibility and inline platillo customization on the diet form without leaving the ingesta grid.

| Requirement | Issues |
|-------------|--------|
| Full nutrients modal from macronutrients table | #280 |
| In-row platillo ingredient editor + diet nutrient refresh | #281 |

**Suggested order:** #280 independent; #281 after **#239** (weight recalc in sub-row add-ingredient dialog). #281 should refresh macro table (#238) and full-nutrient modal (#280) when both exist. **Epic complete** after #281.

| # | Title | URL | State | Depends on | Notes |
|---|-------|-----|-------|-----------|-------|
| **280** | Diet detail — full nutrients modal from macronutrients table | https://github.com/diego-torres/nutriconsultas/issues/280 | **done** | **238** | PR [#284](https://github.com/diego-torres/nutriconsultas/pull/284); modal from macro table + per-ingesta tabs |
| **281** | Diet ingesta grid — in-row platillo ingredient editing with nutrient refresh | https://github.com/diego-torres/nutriconsultas/issues/281 | **done** | **238**, **239** | Branch `issue-281-ingesta-platillo-ingredient-edit`; diet-scoped REST + accordion sub-row |

---

## Epic — Platillo form inline ingredient editing

Inline **cantidad** editing on the catalog platillo form (`/admin/platillos/{id}`) without opening the add-ingredient modal.

| Requirement | Issues |
|-------------|--------|
| Inline cantidad edit in platillo ingredient list | #285 |

**Suggested order:** ~~#285~~ **done** after **#239** (weight recalc) and **#257** (ownership). **Epic complete.**

| # | Title | URL | State | Depends on | Notes |
|---|-------|-----|-------|-----------|-------|
| **285** | Platillo form — inline ingredient cantidad editing | https://github.com/diego-torres/nutriconsultas/issues/285 | **done** | **239**, **257** | Branch `issue-285-platillo-inline-cantidad` (340a318); catalog `#ingredientesGrid` + diet ingesta inline cantidad; PUT + peso recalc |

---

## Epic — Patient profile & clinical corrections

| Requirement | Issues |
|-------------|--------|
| Selectable patient avatars | #241 |
| Per-field anthropometric correction + recalc | #242 |

**Suggested order:** parallel. **Epic complete** after #242.

| # | Title | URL | State | Depends on | Notes |
|---|-------|-----|-------|-----------|-------|
| 241 | Patient profile — selectable patient avatars | https://github.com/diego-torres/nutriconsultas/issues/241 | **done** | #46 | PR [#291](https://github.com/diego-torres/nutriconsultas/pull/291); `avatar_id` + 20 PNG avatars |
| 242 | Anthropometrics — per-field correction with recalculation | https://github.com/diego-torres/nutriconsultas/issues/242 | **done** | #161 (context) | Branch `issue-242-anthropometric-field-edit`; PUT `/rest/pacientes/{id}/antropometricos/{mid}/fields`; SweetAlert confirm |

---

## Bugs

| # | Title | URL | State | Depends on | Notes |
|---|-------|-----|-------|-----------|-------|
| **250** | Diet ingesta platillo link uses `PlatilloIngesta.id` instead of catalog `Platillo.id` | https://github.com/diego-torres/nutriconsultas/issues/250 | **done** | #46 | PR [#256](https://github.com/diego-torres/nutriconsultas/pull/256); `sourcePlatilloId` + Liquibase backfill |
| **275** | Platillo image upload — raise size limit and show user-facing error | https://github.com/diego-torres/nutriconsultas/issues/275 | **done** | — | PR [#276](https://github.com/diego-torres/nutriconsultas/pull/276); Spanish flash `errorMessage` on platillo form |

**Repro (#250):** Menú vegetal 02 → Cena → "Frijoles con tortilla" links to `/admin/platillos/32` (wrong catalog row) instead of `/admin/platillos/97`. Name displays correctly; portion REST APIs unaffected.

**Fix sketch (#250):** persist `sourcePlatilloId` on `PlatilloIngesta` in `PlatilloIngestaMapping`; template uses catalog id for `/admin/platillos/{id}` only.

**Repro (#275):** Upload a platillo photo > ~1 MB on `/admin/platillos/{id}` → HTTP 413, blank page, no `errorMessage`; app log: `MaxUploadSizeExceededException`. Small files upload OK.

**Fix sketch (#275):** `spring.servlet.multipart.max-file-size` (e.g. 10MB); `@ControllerAdvice` Spanish oversize message on platillo form.

---

## Cross-track links

| Track | Interaction |
|-------|-------------|
| #190 Patient limits | Import and manual alta call `assertCanCreatePatient`; delete frees a slot |
| #109 Mobile linkage | Delete clears `patientAuthSub`; not stored in `.mpx` |
| #220 Retention purge | Platform admin purge of **revoked** nutritionists — orthogonal to nutritionist-initiated patient delete |
| #132 Patient invitations | Onboarding `Paciente.status` — import creates `ACTIVE` patient unless product specifies otherwise |
| #183 Platform admin | System diet edit (#232), create (#272); system platillo edit (#257), create (#271) |
| #187 Branded PDF | Logo profile (#236) and PDF sizing (#237) |
| #198 Diet templates | System diets seeded; editable (#232) and creatable (#272) by admin |
| #280 / #281 Diet nutrients | Full nutrient modal (#280); in-row platillo edit on ingesta grid (#281) — refresh dieta rollup |
| ~~#285~~ Platillo form | Inline cantidad edit on catalog `#ingredientesGrid` + diet ingesta grid — branch `issue-285-platillo-inline-cantidad` |
| ~~#243~~ / #244 Subscription | reCAPTCHA **done**; Solicitar acceso pre-fill — public funnel, not admin UI |
| #245–#248 Public booking | ~~#246~~ done (branch `issue-246-working-hours`); nutritionist hours in profile — separate track |

---

**How to update this file**

Same convention as [`ISSUE.md`](ISSUE.md): mark `in-progress` when PR opens, `done` when merged. Reference this registry from [`AGENT-WORKFLOW.md`](AGENT-WORKFLOW.md) and [`AGENTS.md`](AGENTS.md) when adding nutritionist-web sprint pointers.
