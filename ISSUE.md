# Issue Registry — `[Mobile API]` track

Living index of the GitHub issues that build the **patient mobile API** (`/rest/mobile/patient/**`) on the Spring Boot backend. Update **local and remote** (via a commit on the PR that closes the work) whenever status changes.

**Repo:** [diego-torres/nutriconsultas](https://github.com/diego-torres/nutriconsultas) (Spring Boot · Java 21 · Maven)
**Workflow:** [`AGENT-WORKFLOW.md`](AGENT-WORKFLOW.md)
**Mobile consumer:** [Escanor4323/nutriconsultas-mobile](https://github.com/Escanor4323/nutriconsultas-mobile) (Flutter/GetX, patient app)
**Canonical contract:** [`docs/mobile-api/ALIGNMENT-SPEC.md`](docs/mobile-api/ALIGNMENT-SPEC.md) (§F8 schema) · [`docs/mobile-api/mobile-api-roadmap-v2.md`](docs/mobile-api/mobile-api-roadmap-v2.md) (endpoint specs)
**Last updated:** 2026-06-13 — **#95 in-progress** on branch `mobile-api/95-patient-diet-plan-pdf`.

> **Scope of this file.** This registry tracks the `[Mobile API]` issues (#91–#99, #107–#116) plus the directly-related `[Dashboard]` IMC gauge (#106). The repo's many closed web/admin issues (#1–#90) are nutritionist-web features and are **out of scope** here except where a mobile endpoint reuses their code (cross-referenced in [Data contracts](#data-contracts)).

---

## Audience reconciliation (authoritative — ALIGNMENT-SPEC §F7)

| Decision | Status |
|----------|--------|
| Mobile API audience | **Patient-only** — `/rest/mobile/patient/**` serves a patient their **own** visits, diet plans, progress, messages. Never another patient's, never cross-tenant. |
| Nutritionist mobile app | **Out of scope** — roster/consultation/meal-plan authoring stays on the existing Thymeleaf web app. No `[Mobile API]` endpoint exposes authoring. |
| `[Mobile API] #114` (nutritionist reply) | **Web/backend feature** — lives in this repo, but it is **not** consumed by the patient mobile app. It feeds the patient `messages` thread (#96). |
| Patient identity | JWT `sub` resolves to **`Paciente.patientAuthSub`** (#107). **Never `Paciente.userId`** — that is the NUTRITIONIST's Auth0 sub / tenant owner (ALIGNMENT-SPEC §F2). 403 if no linked `Paciente`. |
| Linkage mechanism | **Option A (nutritionist-initiated)** — #109: nutritionist links from **Afiliación** via patient email (Auth0 Management API lookup) or manual `sub` assign; unlink clears `patientAuthSub`. Requires optional `AUTH0_MGMT_*` env for email lookup. |

## Design & schema ground truth (ALIGNMENT-SPEC §F8 — verified at `228bbc3`)

| Topic | Ground truth |
|-------|-------------|
| **DTO field map** | Serialize aliases, **no schema changes**: `Dieta.nombre→dietaName`, `Dieta.energia→totalKcal`, `Dieta.proteina→totalProteina`, `Dieta.lipidos→totalGrasas`, `Dieta.hidratosDeCarbono→totalCarbohidratos`; `Ingesta.nombre→tipo`; `PlatilloIngesta.name/portions/energia→nombre/porciones/kcal`. |
| **Enums (expose all values)** | `EventStatus = SCHEDULED / COMPLETED / CANCELLED` (`calendar/EventStatus.java`); `PacienteDietaStatus = ACTIVE / COMPLETED / CANCELLED` — **no INACTIVE** (`paciente/PacienteDietaStatus.java`); `NivelPeso = BAJO / NORMAL / ALTO / SOBREPESO` → service maps to `imcLabel` display string. |
| **Body-fat field** | Prefer **`bodyComposition.porcentajeGrasaCorporal`** (patient-facing %) over `indiceGrasaCorporal` for #98/#99 consistency. |
| **Computed fields** | `deltaPeso` / `deltaImc` are **computed at query time**, not stored — aggregate in the service layer (#98). |
| **Greenfield** | **No** message entity/repo/service/controller exists — #96/#97 are built from scratch. |
| **Branding** | `NutritionistProfile` (entity @ `228bbc3`: `userId`, `displayName`, `cedulaProfesional`, `logoExtension`, `registro`) already brands diet PDFs (#95) transparently; supplies optional `senderDisplayName` for #96 (#116). |
| **Package note** | Actual entity package is `com.nutriconsultas.paciente` (singular) — issue #107 text says `pacientes`; follow the code. |

---

## Legend

| State | Meaning |
|-------|---------|
| `NEXT` | Active — pick this up now |
| `open` | Not started; blocked only if a dependency below is incomplete |
| `in-progress` | Branch exists; PR not merged yet |
| `done` | Merged to `main` |
| `deferred` | Intentionally paused — decision pending |

Phase 0 JWT (#107) and DTO wrappers (#110) are **done** on stacked branches. Patient linkage (#109) is **pushed**. Visits (#91–#92) and diet plan list (#93) land on `mobile-api/93-patient-diet-plans`.

---

## Phase 0 — Foundation (P0 · blocks every endpoint)

No `/rest/mobile/**` endpoint may be integrated until #107 **and** #110 are `done`. Linkage (#109) gates **live** mobile E2E but not endpoint development (seed `patientAuthSub` for tests).

| # | Title | URL | State | Depends on | Notes |
|---|-------|-----|-------|-----------|-------|
| **107** | Phase 0 — Auth0 JWT resource server + patient Auth0 linkage | https://github.com/diego-torres/nutriconsultas/issues/107 | **done** | — | Merged `aa4db72`: `MobileSecurityConfig` `@Order(1)` on `/rest/mobile/**`; `Paciente.patientAuthSub` + `PatientAuthService` / `PatientLinkageFilter`. |
| 108 | Auth0 API resource + audience + scopes setup | https://github.com/diego-torres/nutriconsultas/issues/108 | open | 107 | Auth0-tenant config: API identifier `https://api.nutriconsultas.minutriporcion.com`, scopes; prod `AUTH_AUDIENCE` deployed via #118. |
| **109** | Patient-Auth0 account linkage (admin invite/assign flow) | https://github.com/diego-torres/nutriconsultas/issues/109 | **done** | 107 | Merged PR #142: admin Afiliación UI + `POST/DELETE /rest/pacientes/{id}/mobile-auth`. Option A: email lookup via Auth0 Management API. |
| **110** | DTO conventions + `ApiResponse`/`PagedResponse` wrappers | https://github.com/diego-torres/nutriconsultas/issues/110 | **done** | — | Merged on branch `mobile-api/110-dto-wrappers`: `com.nutriconsultas.mobile.dto` records + Jackson ISO-8601. |

### Infra (mobile JWT)

| PR | Title | State | Notes |
|----|-------|-------|-------|
| **118** | deploy `AUTH_AUDIENCE` for mobile JWT validation | **done** | Merged `0437a6c`; prod EC2 `app.env` has `AUTH_AUDIENCE` (verified 2026-06-13). |

## Phase 1 — Cross-cutting foundation (P1 · applies to all endpoints)

| # | Title | URL | State | Depends on | Notes |
|---|-------|-----|-------|-----------|-------|
| 111 | Accept-Language filter + MessageSource i18n for REST errors | https://github.com/diego-torres/nutriconsultas/issues/111 | open | 110 | `LocaleContextFilter` (es-MX default) + `MessageSource` error bodies. Mobile sends `Accept-Language` (mobile #25). |
| 115 | PHI log redaction audit for all mobile controllers | https://github.com/diego-torres/nutriconsultas/issues/115 | open | 110 | Audit every `/rest/mobile/**` controller against `util/LogRedaction`; CI gate `scripts/audit-logging.sh`. No names/emails/DOB at INFO. |
| 112 | OpenAPI spec for `/rest/mobile/patient/**` | https://github.com/diego-torres/nutriconsultas/issues/112 | open | 110, endpoints | springdoc spec; mobile reads it as the integration contract. Track per-endpoint as they land. |

---

## Endpoints (P0/P1 · each depends on #107 + #110)

### Visits — `CalendarEvent`

| # | Endpoint | URL | State | Backend source |
|---|----------|-----|-------|----------------|
| **91** | `GET /rest/mobile/patient/visits` — list session summaries | https://github.com/diego-torres/nutriconsultas/issues/91 | **done** | 107, 110 | Branch `mobile-api/91-patient-visits`: paged `VisitSummaryDto`, filters (`status`, `from`, `to`), `ApiResponse`/`PagedResponse` envelope; `@AuthenticationPrincipal Jwt`. |
| **92** | `GET /rest/mobile/patient/visits/{visitId}` — single detail | https://github.com/diego-torres/nutriconsultas/issues/92 | **done** | 91 | Branch `mobile-api/92-patient-visit-detail`: `VisitDetailDto`, `findByIdAndPacienteId` IDOR guard → 404. |

### Diet Plans — `PacienteDieta` / `Dieta`

| # | Endpoint | URL | State | Backend source |
|---|----------|-----|-------|----------------|
| **93** | `GET /rest/mobile/patient/diet-plans` — list assigned plans | https://github.com/diego-torres/nutriconsultas/issues/93 | **done** | 110 | Merged PR #142: paged `DietPlanSummaryDto`, `activeOnly` filter, macro aliases per §F8. |
| **94** | `GET /rest/mobile/patient/diet-plans/{assignmentId}` — structured meal JSON | https://github.com/diego-torres/nutriconsultas/issues/94 | **done** | 93 | Merged PR #143: `DietPlanDetailDto` + ingesta/platillo/alimento tree; `findByIdAndPacienteId` IDOR guard → 404. |
| **95** | `GET /rest/mobile/patient/diet-plans/{assignmentId}/pdf` — printable PDF | https://github.com/diego-torres/nutriconsultas/issues/95 | **in-progress** | 94 | Branch `mobile-api/95-patient-diet-plan-pdf`: reuses `DietaPdfService.generatePdfForAssignment`, `Content-Disposition`, ownership → 404. |

### Messages — **greenfield** (no entity exists)

| # | Endpoint | URL | State | Backend source |
|---|----------|-----|-------|----------------|
| 96 | `GET /rest/mobile/patient/messages` — list thread | https://github.com/diego-torres/nutriconsultas/issues/96 | open | NEW entity/repo/service; **cursor** pagination (not offset); never log body; optional `senderDisplayName` (#116) |
| 97 | `POST /rest/mobile/patient/messages` — send to nutritionist | https://github.com/diego-torres/nutriconsultas/issues/97 | open | `senderRole=PATIENT` from JWT only; `@Valid @Size(max=2000)`; rate limit via #113 |

### Progress — `AnthropometricMeasurement` / `Paciente`

| # | Endpoint | URL | State | Backend source |
|---|----------|-----|-------|----------------|
| 98 | `GET /rest/mobile/patient/progress` — BMI/indicator snapshot | https://github.com/diego-torres/nutriconsultas/issues/98 | open | Aggregate in service; `deltaPeso`/`deltaImc` vs prior measurement; `NivelPeso`→`imcLabel`; BMI/BMR on `Paciente` |
| 99 | `GET /rest/mobile/patient/progress/measurements` — time series | https://github.com/diego-torres/nutriconsultas/issues/99 | open | `from`/`to` ISO-8601; cap `maxRows=365`; ASC order; prefer `porcentajeGrasaCorporal` |

---

## Cross-cutting / additive (P2)

| # | Title | URL | State | Notes |
|---|-------|-----|-------|-------|
| 113 | Rate limiting on patient write endpoints (Resilience4j) | https://github.com/diego-torres/nutriconsultas/issues/113 | open | 10/min on #97; returns 429 + i18n message (#111) |
| 116 | Additive: `senderDisplayName` in message DTOs from `NutritionistProfile` | https://github.com/diego-torres/nutriconsultas/issues/116 | open | Additive to #96 DTO; **non-blocking** for mobile #19. Source `NutritionistProfile.displayName`. |
| 114 | Nutritionist reply to patient messages | https://github.com/diego-torres/nutriconsultas/issues/114 | open | **Web/backend only** — not a mobile-app endpoint; writes into the same thread #96 reads. |
| 106 | `[Dashboard]` IMC gauge with color bands (anthropometric tablero) | https://github.com/diego-torres/nutriconsultas/issues/106 | open | Web dashboard; shares `NivelPeso` color-band logic the mobile `ImcGauge` (mobile #21) mirrors. Not a `/rest/mobile/**` endpoint. |

---

## Data contracts

Each mobile feature consumes these backend endpoints. Two-way linking with the mobile registry ([`mobile/ISSUE.md`](../mobile/ISSUE.md) → "Backend cross-reference").

| Backend # | Patient feature | Mobile consumer (issue) | Read / write |
|-----------|-----------------|-------------------------|--------------|
| 91, 92 | Visitas | mobile #17 (feature), #10 (repo), #1 (models) | Read-only |
| 93, 94, 95 | Planes de Dieta (+ PDF) | mobile #18, #12, #3 | Read-only |
| 96, 97 | Mensajes | mobile #19, #14, #6 | Read + patient send |
| 98, 99 | Progreso | mobile #16/#20, #15, #8 | Read-only |
| 107, 108, 109 | Auth / linkage | mobile #5, #7, #13, #22 | Auth |
| 116 | `senderDisplayName` (optional) | mobile #19 | Read-only (additive) |
| 114 | nutritionist reply | — (web only; feeds #96) | — |

**Common contract for every #91–#99** (ALIGNMENT-SPEC §"CORRECTED SCOPE"):
- Auth: OAuth2 Resource Server JWT, separate `@Order(1)` `SecurityFilterChain`, stateless.
- Principal: JWT `sub` → `Paciente.patientAuthSub` (#107). 403 if unlinked.
- Envelope: `ApiResponse<T>`; lists in `PagedResponse<T>` (#110); ISO-8601 dates.
- i18n: `Accept-Language` (es-MX default) error bodies (#111).
- PHI-safe logging (`LogRedaction`, #115); no names/emails/DOB at INFO.

**E2E HTTP codes (2026-06-13):**
- **401** — missing/invalid JWT or wrong `aud` (check mobile `AUTH0_AUDIENCE` = `https://api.nutriconsultas.minutriporcion.com`).
- **403** `patient_not_linked` — JWT valid, no `Paciente.patientAuthSub` match (#109 linkage required).
- **404** — linkage OK but endpoint not implemented yet (#91+).

---

## How to update this file

**Starting work** — mark `in-progress`, keep `NEXT` on it until the PR merges:

```text
| 109 | Patient-Auth0 account linkage ... | https://... | in-progress | 107 | ... |
```

**After PR merges** — mark `done` and advance `NEXT` to the next unblocked row (Phase 0 order: #109 → #110 → #108 → endpoints):

```text
| 109 | Patient-Auth0 account linkage ... | https://... | done | 107 | ... |
| 110 | DTO conventions ... | https://... | **NEXT** | — | ... |
```

Commit registry updates in the same PR that implements the issue (Phase 9 of [`AGENT-WORKFLOW.md`](AGENT-WORKFLOW.md)). Keep the mobile registry's "Backend cross-reference" in sync when an endpoint's state changes.
