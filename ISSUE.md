# Issue Registry — `[Mobile API]` track

Living index of the GitHub issues that build the **patient mobile API** (`/rest/mobile/patient/**`) on the Spring Boot backend. Update **local and remote** (via a commit on the PR that closes the work) whenever status changes.

**Repo:** [diego-torres/nutriconsultas](https://github.com/diego-torres/nutriconsultas) (Spring Boot · Java 21 · Maven)
**Workflow:** [`AGENT-WORKFLOW.md`](AGENT-WORKFLOW.md)
**Mobile consumer:** [Escanor4323/nutriconsultas-mobile](https://github.com/Escanor4323/nutriconsultas-mobile) (Flutter/GetX, patient app)
**Canonical contract:** [`docs/mobile-api/ALIGNMENT-SPEC.md`](docs/mobile-api/ALIGNMENT-SPEC.md) (§F8 schema) · [`docs/mobile-api/mobile-api-roadmap-v2.md`](docs/mobile-api/mobile-api-roadmap-v2.md) (endpoint specs)
**Last updated:** 2026-06-15 — #114 nutritionist reply **done** (branch `mobile-api/114-nutritionist-reply`: integration test for admin→mobile loop). #116 `senderDisplayName` **done** on `main`. **NEXT:** #156.

> **Scope of this file.** This registry tracks the `[Mobile API]` issues (#91–#99, #107–#116, #132–#141 invitation onboarding) plus the directly-related `[Dashboard]` IMC gauge (#106) and **integration prerequisites** that gate schema work (#156, #46). The repo's many closed web/admin issues (#1–#90) are nutritionist-web features and are **out of scope** here except where a mobile endpoint reuses their code (cross-referenced in [Data contracts](#data-contracts)).

> **GitHub sync note (2026-06-15):** **#112** closed on GitHub (merged PR #164). **#97** and **#111** are **done on `main`** (PRs #147, #151) but remain **open on GitHub** — close when convenient.

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
| **Greenfield** | Message entity/repo/service/controllers **implemented** (#96/#97, PRs #146–#147, #151). Contact form uses `ContactInquiry` (not patient thread). |
| **Branding** | `NutritionistProfile` (entity @ `228bbc3`: `userId`, `displayName`, `cedulaProfesional`, `logoExtension`, `registro`) already brands diet PDFs (#95) transparently; supplies optional `senderDisplayName` for #96 (#116). |
| **Package note** | Actual entity package is `com.nutriconsultas.paciente` (singular) — issue #107 text says `pacientes`; follow the code. |
| **`Paciente` entity** | Monolithic JPA entity (~44 fields post-#121). Mobile `#98`/`#99` consume **DTOs** mapped from `BodyMetricRecord` + snapshot cache on `Paciente` — not the entity shape. Decompose via [#156](https://github.com/diego-torres/nutriconsultas/issues/156) **before** [#46 Liquibase](https://github.com/diego-torres/nutriconsultas/issues/46). |

---

## Legend

| State | Meaning |
|-------|---------|
| `NEXT` | Active — pick this up now |
| `open` | Not started; blocked only if a dependency below is incomplete |
| `in-progress` | Branch exists; PR not merged yet |
| `done` | Merged to `main` |
| `deferred` | Intentionally paused — decision pending |

Phase 0 (#107, #109, #110) is **done**. Patient linkage (#109) is **done**. Endpoints **#91–#99 are done** on `main` (PR #153). OpenAPI (#112, PR #164) is **done**. **#115 PHI audit done** (PR #168). **#116 `senderDisplayName` done** on `main`. **#114 nutritionist reply done** on `main` (admin REST + web widget since PR #146; integration test on branch `mobile-api/114-nutritionist-reply`). **NEXT:** #156.

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
| 111 | Accept-Language filter + MessageSource i18n for REST errors | https://github.com/diego-torres/nutriconsultas/issues/111 | **done** | 110 | Merged PR #151: `LocaleContextFilter`, `MobileApiErrorResponses`, `MobileApiExceptionHandler` (403/404/400/429). GitHub issue still open — close when convenient. |
| 115 | PHI log redaction audit for all mobile controllers | https://github.com/diego-torres/nutriconsultas/issues/115 | **done** | 110 | Merged [PR #168](https://github.com/diego-torres/nutriconsultas/pull/168): `PhiLogTurboFilter`, `scripts/audit-mobile-logging.sh`, `docs/mobile-api/PHI-LOGGING-AUDIT.md`. |
| 112 | OpenAPI spec for `/rest/mobile/patient/**` | https://github.com/diego-torres/nutriconsultas/issues/112 | **done** | 110, endpoints | Merged [PR #164](https://github.com/diego-torres/nutriconsultas/pull/164): springdoc + `docs/api/openapi-mobile.yaml`. |

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
| **95** | `GET /rest/mobile/patient/diet-plans/{assignmentId}/pdf` — printable PDF | https://github.com/diego-torres/nutriconsultas/issues/95 | **done** | 94 | Merged PR #144: `DietaPdfService.generatePdfForAssignment`, `Content-Disposition`, ownership → 404. |

### Messages — `PatientMessage`

| # | Endpoint | URL | State | Backend source |
|---|----------|-----|-------|----------------|
| 96 | `GET /rest/mobile/patient/messages` — list thread | https://github.com/diego-torres/nutriconsultas/issues/96 | **done** | Merged PR #146: cursor pagination; contact form → `ContactInquiry`; admin inbox |
| 97 | `POST /rest/mobile/patient/messages` — send to nutritionist | https://github.com/diego-torres/nutriconsultas/issues/97 | **done** | Merged PR #147 (`senderRole=PATIENT`, validation); PR #151 adds HTTP 201 + #113 rate limit. GitHub issue still open — close when convenient. |

### Progress — `AnthropometricMeasurement` / `Paciente`

| # | Endpoint | URL | State | Backend source |
|---|----------|-----|-------|----------------|
| 98 | `GET /rest/mobile/patient/progress` — BMI/indicator snapshot | https://github.com/diego-torres/nutriconsultas/issues/98 | **done** | PR #148: `deltaPeso`/`deltaImc`, `imcLabel`, BMR, optional circumferences |
| 99 | `GET /rest/mobile/patient/progress/measurements` — time series | https://github.com/diego-torres/nutriconsultas/issues/99 | **done** | 98 | Merged PR #153: `from`/`to` ISO-8601; `maxRows` cap 365; ASC order; `porcentajeGrasaCorporal`; `truncated` flag |

---

## Cross-cutting / additive (P2)

| # | Title | URL | State | Notes |
|---|-------|-----|-------|-------|
| 113 | Rate limiting on patient write endpoints (Resilience4j) | https://github.com/diego-torres/nutriconsultas/issues/113 | **done** | Merged PR #151: 10/min per `patientAuthSub` on POST #97; 429 + `Retry-After: 60`; localized via #111 |
| 116 | Additive: `senderDisplayName` in message DTOs from `NutritionistProfile` | https://github.com/diego-torres/nutriconsultas/issues/116 | **done** | On `main`: optional `senderDisplayName` on `PatientMessageSummaryDto` when `senderRole=NUTRITIONIST`; sourced from `NutritionistProfile.displayName`; OpenAPI updated. GitHub issue closed. |
| 114 | Nutritionist reply to patient messages | https://github.com/diego-torres/nutriconsultas/issues/114 | **done** | **Web/backend only** — `POST /rest/patient-messages/thread/{pacienteId}` + floating admin widget (`patient-messages-widget`); `senderRole=NUTRITIONIST` server-side; feeds #96 thread. Integration test: `PatientMessageIntegrationTest`. |
| 106 | `[Dashboard]` IMC gauge with color bands (anthropometric tablero) | https://github.com/diego-torres/nutriconsultas/issues/106 | **done** | Web dashboard; shares `NivelPeso` color-band logic the mobile `ImcGauge` (mobile #21) mirrors. Not a `/rest/mobile/**` endpoint. |

---

## Integration prerequisites (pre-Liquibase · P1)

Schema-affecting work must respect mobile DTO contracts (§F8). These issues are **not** `/rest/mobile/**` endpoints but **block** Liquibase adoption and onboarding schema (#132).

| # | Title | URL | State | Depends on | Blocks | Notes |
|---|-------|-----|-------|-----------|--------|-------|
| **156** | `[Integration]` Paciente domain refactor — incremental decomposition | https://github.com/diego-torres/nutriconsultas/issues/156 | **NEXT** | [#121](https://github.com/diego-torres/nutriconsultas/issues/121) GET/TDEE | [#46](https://github.com/diego-torres/nutriconsultas/issues/46) Liquibase, [#132](https://github.com/diego-torres/nutriconsultas/issues/132) timing | Phases A–B (projections + `@Embeddable`, no mobile JSON changes); Phase C optional satellite tables. `#98`/`#99` DTO keys stable; `patientAuthSub` immovable. |
| 46 | Implement Liquibase for database change management | https://github.com/diego-torres/nutriconsultas/issues/46 | open | **156** (Phases A–B min.) | — | First Liquibase baseline must capture post-#156 `Paciente` schema. Until then: `ddl-auto=update` + manual SQL if Phase C. |
| 132 | Invitation & patient onboarding data model | https://github.com/diego-torres/nutriconsultas/issues/132 | open | #107, **156** (Phase B) | — | `Paciente.status` enum + `Invitation` entity; Liquibase via #46 after #156. |

---

## Phase 2 — Invitation onboarding (P1 · gated by #156 → #46 → #132)

Invite-only patient onboarding replaces manual Afiliación linkage (#109) for new patients. **Not active sprint** — start after #156 Phase B and Liquibase baseline (#46). Issues decomposed from `invitation-system-research.md`.

| # | Title | URL | State | Depends on | Notes |
|---|-------|-----|-------|-----------|-------|
| 133 | Invitation token generation & hashing service | https://github.com/diego-torres/nutriconsultas/issues/133 | open | 132 | CSPRNG token; store hash only; constant-time verify; optional signed JWS for Auth0 Action |
| 134 | `POST /rest/mobile/invitations` — nutritionist creates patient + invitation | https://github.com/diego-torres/nutriconsultas/issues/134 | open | 132, 133 | Nutritionist JWT (not patient); creates `Paciente` + `Invitation` |
| 135 | `GET /rest/mobile/invitations/{token}/preview` — public rate-limited preview | https://github.com/diego-torres/nutriconsultas/issues/135 | open | 133 | Public; enumeration protection (#141) |
| 136 | `POST /rest/mobile/invitations/{token}/redeem` — bind Auth0 sub → patient | https://github.com/diego-torres/nutriconsultas/issues/136 | open | 132, 133, 107 | **Authoritative** redeem gate; patient JWT required |
| 137 | CurrentPatient resolver + onboarding data gate (403 onboarding required) | https://github.com/diego-torres/nutriconsultas/issues/137 | open | 132, 107 | Centralizes `sub → Paciente`; 403 until onboarded |
| 138 | `PATCH` & `GET /rest/mobile/patient/me` — onboarding profile + status→ACTIVE | https://github.com/diego-torres/nutriconsultas/issues/138 | open | 132, 137 | Patient completes profile; transitions to `ACTIVE` |
| 139 | `POST /rest/mobile/invitations/{id}/revoke` — nutritionist invalidates invite | https://github.com/diego-torres/nutriconsultas/issues/139 | open | 132, 134 | Nutritionist JWT |
| 140 | Auth0 Post-Login Action gate — first-login invitation validation | https://github.com/diego-torres/nutriconsultas/issues/140 | open | 133, 136 | **Post-Login** (not Pre-User-Registration — social logins skip PUR); docs + Action script |
| 141 | Invitation security hardening — rate limits, enumeration protection, no-token logging | https://github.com/diego-torres/nutriconsultas/issues/141 | open | 133, 135, 136 | Relates #115; never log raw tokens |

**Suggested build order (after #132):** #133 → #134/#135 → #136 → #137 → #138 → #139/#140 → #141.

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
| 116 | `senderDisplayName` (optional) | mobile #19 | Read-only (additive) | **done** on `main` |
| 114 | nutritionist reply | — (web only; feeds #96) | Write (nutritionist) | **done** — `POST /rest/patient-messages/thread/{pacienteId}` + admin widget |
| 156 | `Paciente` internal refactor (pre-Liquibase) | — (backend only) | — | No mobile JSON change; `#98`/`#99` regression required per PR |
| 132–141 | Invitation onboarding | mobile (future) | Auth + profile | Replaces #109 manual linkage for new patients; gated by #156 |

**Common contract for every #91–#99** (ALIGNMENT-SPEC §"CORRECTED SCOPE"):
- Auth: OAuth2 Resource Server JWT, separate `@Order(1)` `SecurityFilterChain`, stateless.
- Principal: JWT `sub` → `Paciente.patientAuthSub` (#107). 403 if unlinked.
- Envelope: `ApiResponse<T>`; lists in `PagedResponse<T>` (#110); ISO-8601 dates.
- i18n: `Accept-Language` (es-MX default) error bodies (#111).
- PHI-safe logging (`LogRedaction`, #115); no names/emails/DOB at INFO.

**E2E HTTP codes (2026-06-15):**
- **401** — missing/invalid JWT or wrong `aud` (check mobile `AUTH0_AUDIENCE` = `https://api.nutriconsultas.minutriporcion.com`).
- **403** — JWT valid, no `Paciente.patientAuthSub` match (#109 linkage required). Localized `ApiResponse` with `error.patient.not.linked` (#111).
- **404** — linkage OK but resource not found (localized `error.resource.not.found`).
- **400** — validation failure (e.g. blank message body → `error.message.required`).
- **429** — patient write rate limit exceeded (#113); `Retry-After: 60`; localized `error.rate.limit.exceeded`.

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
