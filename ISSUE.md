# Issue Registry — `[Mobile API]` track

Living index of the GitHub issues that build the **patient mobile API** (`/rest/mobile/patient/**`) on the Spring Boot backend. Update **local and remote** (via a commit on the PR that closes the work) whenever status changes.

**Repo:** [diego-torres/nutriconsultas](https://github.com/diego-torres/nutriconsultas) (Spring Boot · Java 21 · Maven)
**Workflow:** [`AGENT-WORKFLOW.md`](AGENT-WORKFLOW.md) · **Subscription (parallel):** [`ISSUE-SUBSCRIPTION.md`](ISSUE-SUBSCRIPTION.md) · **Nutritionist web (parallel):** [`ISSUE-NUTRITIONIST-WEB.md`](ISSUE-NUTRITIONIST-WEB.md)
**Mobile consumer:** [Escanor4323/nutriconsultas-mobile](https://github.com/Escanor4323/nutriconsultas-mobile) (Flutter/GetX, patient app)
**Canonical contract:** [`docs/mobile-api/ALIGNMENT-SPEC.md`](docs/mobile-api/ALIGNMENT-SPEC.md) (§F8 schema) · [`docs/mobile-api/mobile-api-roadmap-v2.md`](docs/mobile-api/mobile-api-roadmap-v2.md) (endpoint specs)
**Last updated:** 2026-07-16 — #558 opened: `[Mobile API]` Auth0 social login reconcile alignment (planning; cross-repo counterpart to mobile #124). Registered **#529** patient profile photo (S3 + mobile). #353 grocery list endpoint (`mobile-api/353-grocery-list`). ~~#354~~ ~~#352~~ **done** (PR [#357](https://github.com/diego-torres/nutriconsultas/pull/357)). ~~#349~~ **done** (PR [#356](https://github.com/diego-torres/nutriconsultas/pull/356)).

> **Scope of this file.** This registry tracks the `[Mobile API]` issues (#91–#99, #107–#116, #132–#141 invitation onboarding) plus the directly-related `[Dashboard]` IMC gauge (#106) and **integration prerequisites** that gate schema work (#156, #46). The repo's many closed web/admin issues (#1–#90) are nutritionist-web features and are **out of scope** here except where a mobile endpoint reuses their code (cross-referenced in [Data contracts](#data-contracts)).

> **GitHub sync note (2026-06-23):** Phase 2 invitation onboarding **#134–#141 complete** (PRs #319, #324–#333). ~~#133~~ closed (PR #229). ~~#97~~, ~~#111~~ closed (PRs #147, #151).

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
| **`Paciente` entity** | Decomposed via [#156](https://github.com/diego-torres/nutriconsultas/issues/156): projections (Phase A) + body snapshot `@Embeddable` (Phase B) + LAZY `@OneToOne` satellites `paciente_medical_history` / `paciente_energy_preferences` (Phase C, branch `integration/c-paciente-satellites`). Flat accessors via `@Delegate`. Mobile `#98`/`#99` DTOs unchanged. ER: [`docs/integration/paciente-er-phase-c.md`](docs/integration/paciente-er-phase-c.md). |

---

## Legend

| State | Meaning |
|-------|---------|
| `NEXT` | Active — pick this up now |
| `open` | Not started; blocked only if a dependency below is incomplete |
| `in-progress` | Branch exists; PR not merged yet |
| `done` | Merged to `main` |
| `deferred` | Intentionally paused — decision pending |

Phase 0 (#107, #109, #110) is **done**. Endpoints **#91–#99 are done** on `main`. Cross-cutting **#111–#116** done. **#156**, **#46**, **#132–#141** done on `main`. **Phase 2 invitation onboarding complete.**

---

## Phase 0 — Foundation (P0 · blocks every endpoint)

No `/rest/mobile/**` endpoint may be integrated until #107 **and** #110 are `done`. Linkage (#109) gates **live** mobile E2E but not endpoint development (seed `patientAuthSub` for tests).

| # | Title | URL | State | Depends on | Notes |
|---|-------|-----|-------|-----------|-------|
| **107** | Phase 0 — Auth0 JWT resource server + patient Auth0 linkage | https://github.com/diego-torres/nutriconsultas/issues/107 | **done** | — | Merged `aa4db72`: `MobileSecurityConfig` `@Order(1)` on `/rest/mobile/**`; `Paciente.patientAuthSub` + `PatientAuthService` / `PatientLinkageFilter`. |
| 108 | Auth0 API resource + audience + scopes setup | https://github.com/diego-torres/nutriconsultas/issues/108 | open | 107 | Auth0-tenant config: API identifier `https://api.nutriconsultas.minutriporcion.com`, scopes; prod `AUTH_AUDIENCE` deployed via #118. **New items surfaced by #558:** custom email provider → Amazon SES (Dashboard → Branding → Email Provider, for db-connection verification mail); `email_verified` claim mapping into the access token (Auth0 Action/claim rule — resolver has nothing to read otherwise); SPF/DKIM domain authentication on the sending domain (shared prerequisite with Apple private-relay outbound mail, #558 §1c). |
| **109** | Patient-Auth0 account linkage (admin invite/assign flow) | https://github.com/diego-torres/nutriconsultas/issues/109 | **done** | 107 | Merged PR #142: admin Afiliación UI + `POST/DELETE /rest/pacientes/{id}/mobile-auth`. Option A: email lookup via Auth0 Management API. |
| **110** | DTO conventions + `ApiResponse`/`PagedResponse` wrappers | https://github.com/diego-torres/nutriconsultas/issues/110 | **done** | — | Merged on branch `mobile-api/110-dto-wrappers`: `com.nutriconsultas.mobile.dto` records + Jackson ISO-8601. |

### Infra (mobile JWT)

| PR | Title | State | Notes |
|----|-------|-------|-------|
| **118** | deploy `AUTH_AUDIENCE` for mobile JWT validation | **done** | Merged `0437a6c`; prod EC2 `app.env` has `AUTH_AUDIENCE` (verified 2026-06-13). |

## Phase 1 — Cross-cutting foundation (P1 · applies to all endpoints)

| # | Title | URL | State | Depends on | Notes |
|---|-------|-----|-------|-----------|-------|
| 111 | Accept-Language filter + MessageSource i18n for REST errors | https://github.com/diego-torres/nutriconsultas/issues/111 | **done** | 110 | Merged PR #151: `LocaleContextFilter`, `MobileApiErrorResponses`, `MobileApiExceptionHandler` (403/404/400/429). |
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
| 97 | `POST /rest/mobile/patient/messages` — send to nutritionist | https://github.com/diego-torres/nutriconsultas/issues/97 | **done** | Merged PR #147 (`senderRole=PATIENT`, validation); PR #151 adds HTTP 201 + #113 rate limit. |

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
| **156** | `[Integration]` Paciente domain refactor — incremental decomposition | https://github.com/diego-torres/nutriconsultas/issues/156 | **done** | [#121](https://github.com/diego-torres/nutriconsultas/issues/121) GET/TDEE | — (unblocked #46) | Merged PRs [#175](https://github.com/diego-torres/nutriconsultas/pull/175) (projections), [#176](https://github.com/diego-torres/nutriconsultas/pull/176) (embeddables), [#178](https://github.com/diego-torres/nutriconsultas/pull/178) (satellite tables). ER: [`paciente-er-phase-c.md`](docs/integration/paciente-er-phase-c.md). Mobile DTOs unchanged. |
| 46 | Implement Liquibase for database change management | https://github.com/diego-torres/nutriconsultas/issues/46 | **done** | ~~156~~ ✓ | — | Merged [PR #196](https://github.com/diego-torres/nutriconsultas/pull/196): Liquibase baseline (PG + H2), catalog seed changelogs, `ddl-auto=none`. Docs: [`docs/db/LIQUIBASE.md`](docs/db/LIQUIBASE.md). |
| 132 | Invitation & patient onboarding data model | https://github.com/diego-torres/nutriconsultas/issues/132 | **done** | #107, ~~156~~ ✓, ~~46~~ ✓ | — | Merged [PR #214](https://github.com/diego-torres/nutriconsultas/pull/214): `PacienteStatus`, `PatientInvitation`, Liquibase `007`. |

---

## Phase 2 — Invitation onboarding (P1 · ~~#132~~ ✓ ~~#133~~ ✓ → #134–#141)

Invite-only patient onboarding replaces manual Afiliación linkage (#109) for new patients. **Phase 2 complete** — #141 hardening done. ~~#337~~ web landing **done**.

| # | Title | URL | State | Depends on | Notes |
|---|-------|-----|-------|-----------|-------|
| 133 | Invitation token generation & hashing service | https://github.com/diego-torres/nutriconsultas/issues/133 | **done** | ~~132~~ ✓ | Merged PR [#229](https://github.com/diego-torres/nutriconsultas/pull/229): `PatientInvitationTokenService`, human code, SHA-256 hash, optional offline JWS (#140) |
| 134 | `POST /rest/mobile/invitations` — nutritionist creates patient + invitation | https://github.com/diego-torres/nutriconsultas/issues/134 | **done** | ~~132~~ ✓, ~~133~~ ✓ | Merged PR [#319](https://github.com/diego-torres/nutriconsultas/pull/319) |
| 135 | `GET /rest/mobile/invitations/{token}/preview` — public rate-limited preview | https://github.com/diego-torres/nutriconsultas/issues/135 | **done** | ~~133~~ ✓ | Merged PR [#324](https://github.com/diego-torres/nutriconsultas/pull/324) |
| 136 | `POST /rest/mobile/invitations/{token}/redeem` — bind Auth0 sub → patient | https://github.com/diego-torres/nutriconsultas/issues/136 | **done** | ~~132~~ ✓, ~~133~~ ✓, 107 | Merged PR [#325](https://github.com/diego-torres/nutriconsultas/pull/325); extended `POST …/by-code/{code}/redeem` + `POST …/reconcile` (JWT email / optional credential) — PR [#345](https://github.com/diego-torres/nutriconsultas/pull/345) |
| 137 | CurrentPatient resolver + onboarding data gate (403 onboarding required) | https://github.com/diego-torres/nutriconsultas/issues/137 | **done** | ~~132~~ ✓, 107 | Merged PR [#326](https://github.com/diego-torres/nutriconsultas/pull/326) |
| 138 | `PATCH` & `GET /rest/mobile/patient/me` — onboarding profile + status→ACTIVE | https://github.com/diego-torres/nutriconsultas/issues/138 | **done** | ~~132~~ ✓, ~~137~~ ✓ | Merged PR [#328](https://github.com/diego-torres/nutriconsultas/pull/328) |
| 139 | `POST /rest/mobile/invitations/{id}/revoke` — nutritionist invalidates invite | https://github.com/diego-torres/nutriconsultas/issues/139 | **done** | ~~132~~ ✓, 134 | Merged PR [#330](https://github.com/diego-torres/nutriconsultas/pull/330) |
| 140 | Auth0 Post-Login Action gate — first-login invitation validation | https://github.com/diego-torres/nutriconsultas/issues/140 | **done** | ~~133~~ ✓, 136 | Post-Login Action + [`docs/auth0/PATIENT-POST-LOGIN-GATE.md`](docs/auth0/PATIENT-POST-LOGIN-GATE.md); human-code preview fallback in gate script |
| 141 | Invitation security hardening — rate limits, enumeration protection, no-token logging | https://github.com/diego-torres/nutriconsultas/issues/141 | **done** | ~~133~~ ✓, 135, 136 | [`INVITATION-SECURITY-AUDIT.md`](docs/mobile-api/INVITATION-SECURITY-AUDIT.md) acceptance audit |
| 336 | B1 — `GET /rest/mobile/invitations/by-code/{code}/preview` (human-readable code) | https://github.com/diego-torres/nutriconsultas/issues/336 | **done** | ~~135~~ ✓, ~~141~~ ✓ | Public rate-limited preview via `PatientInvitation.humanCode`; mobile #62–#63 shipped |
| 337 | B2 — Invitation web landing page `GET /links/i/{token}` | https://github.com/diego-torres/nutriconsultas/issues/337 | **done** | ~~135~~ ✓ | Public Thymeleaf landing (`/links/i/{token}`, `/i/{token}` alias); hero image, human code, store URLs; `buildInviteUrl()` → `/links/i/` |
| 349 | Invitation preview auth routing hints (signup vs login) | https://github.com/diego-torres/nutriconsultas/issues/349 | **done** | ~~135~~ ✓, ~~141~~ ✓ | Merged PR [#356](https://github.com/diego-torres/nutriconsultas/pull/356): `patientStatus`, `mobileAppLinked`, `authPath`, masked `emailHint` on both preview endpoints |
| **558** | `[Mobile API]` Auth0 social login reconcile alignment — reconcile-by-code security review + contract docs + PATCH `patient/me` (mobile #124) | https://github.com/diego-torres/nutriconsultas/issues/558 | **open** (planning) | ~~136~~ ✓, ~~138~~ ✓, 108 | **Planning/alignment only — no implementation until product sign-off on mobile #124's decisions.** Consolidates the two backend TBD rows from #124's cross-repo tracker: (1) reconcile-by-code security review, (2) reconcile matrix contract documentation — plus `GET`/`PATCH /rest/mobile/patient/me` alignment after social link and `email_verified` end-to-end. Reconcile-by-code **already ships** (`PatientInvitationRedeemServiceImpl.reconcile()`, extended in #136 via PR #345) but is **undocumented** in `ALIGNMENT-SPEC.md`, `mobile-api-roadmap-v2.md`, and `PATIENT-POST-LOGIN-GATE.md` — flagged as an **agent/context-handoff blocker** (§2 below), not just a docs gap. See [Resolved decisions](#558-resolved-decisions) and [`AGENT-WORKFLOW.md`](AGENT-WORKFLOW.md) for implementation insertion points. Cross-repo: mobile [Escanor4323/nutriconsultas-mobile#124](https://github.com/Escanor4323/nutriconsultas-mobile/issues/124). |

**Suggested build order (after ~~#133~~ ✓):** … → ~~#349~~ ✓ → ~~#354~~ ✓ → ~~#352~~ ✓ → #353. **#558** planning runs in parallel, gated on product sign-off of mobile #124's revised social-first flow.

<a id="558-resolved-decisions"></a>
### #558 — resolved decisions (record verbatim; do not re-derive)

Grounded against code audit + mobile #124's planning/design threads. These are agreed proposals pending product sign-off, not yet implemented:

| Topic | Resolved decision |
|-------|-------------------|
| **Reconcile precedence (already shipped)** | `PatientInvitationRedeemServiceImpl.reconcile()` (`:76-116`) — 1) **already-linked** (`findAuthViewByPatientAuthSub`) → idempotent; 2) **token** provided → `redeem(token, sub)`, no email involved; 3) **humanCode** provided → `redeemByHumanCode(humanCode, sub)`, no email involved; 4) **repair path** — invitation `REDEEMED` by this sub but `patientAuthSub` unset → `repairPacienteLinkage`; 5) **last resort** — JWT-email → `PENDING` invitation on `INVITED` Paciente (`findRedeemablePendingByPacienteEmail`). Anti-hijack: `rejectIfSubLinkedElsewhere` → 409 `PatientInvitationRedeemConflictException`. |
| **`email_verified` (§1a)** | Require `email_verified == true` on **step 5 (email-fallback) only**; steps 1–4 stay email-agnostic (credential possession is the proof). Claim must be mapped into the access token via an Auth0 Action (tracked under #108) — resolver has nothing to read otherwise. |
| **Connection-type distinction (§1b)** | Deliberately **none** in linking logic — keep connection-blind (matches #124: "one linked account," social = first-class path). Add structured audit logging only: connection prefix parsed from `sub`, plus which precedence step linked (`TOKEN \| HUMAN_CODE \| REPAIR \| EMAIL_FALLBACK`). |
| **Apple private relay (§1c)** | **Answered — allow with code reconcile** (not block, not in-person verification). Relay addresses are always `email_verified=true` from Apple and functional as a mailbox, but never equal the nutritionist-entered email, so the email-fallback path is structurally dead for these users regardless — Option B (always pass stored humanCode/token post-social-login) routes them through steps 2/3 untouched. Operational prerequisite if the app emails patients: relay outbound mail requires domain/SPF registration in the Apple Developer account's private-relay config — tracked under #108, same domain-auth effort as Auth0's SES email provider. Don't rely on `is_private_email` claim (sometimes missing); match the `@privaterelay.appleid.com` suffix instead. |
| **Nutritionist-entered email (§1d)** | **Hint-only**, not authoritative — matches shipped precedence (step 5 is last-resort) and preview's masked `emailHint` (#349/PR #356). Invitation credential (token/humanCode) is the authoritative proof of invite scope. Residual risk with hint-only is humanCode brute-force posture — confirm rate limiting on `MobileInvitationController.reconcileInvitation` (`:145`). |
| **PATCH `/patient/me` email-lock** | **Needed.** `PatchPatientOnboardingProfileRequest.email` is writable with **no JWT-email check** (`MobilePatientOnboardingService.applyPatch`, `:98-100`) — a client could desync `Paciente.email` from the authenticated identity. Required once the mobile "verify your data" screen renders email read-only: either ignore `email` on this endpoint or reject 400/409 on mismatch vs. JWT `email` claim. |
| **Auth0→SES verification email** | Auth0 natively supports Amazon SES as a custom email provider (Dashboard → Branding → Email Provider) for db-connection signup verification mail. SES is **already the transport** for invitation email (`SesPatientInvitationEmailSender`, `mode=ses`/`console`, `application.properties:106-113`) — the new work is tenant config, not backend code. Cross-ref #108. |
| **Auth0UserLookup** | **Correction:** already fully implemented (Management API `users-by-email` via `Auth0UserLookupImpl`), not a deferred stub. Only `AUTH0_MGMT_*` env wiring remains. |

**Cross-repo tracker:** mobile [#124](https://github.com/Escanor4323/nutriconsultas-mobile/issues/124) ↔ backend **#558** — see also the Data contracts row below.

---

## Phase 3 — Diet plan mobile enhancements (P1)

Extended diet plan DTOs and endpoints for mobile home/diet detail flows.

| # | Title | URL | State | Depends on | Notes |
|---|-------|-----|-------|-----------|-------|
| 354 | Extend `DietPlatilloDto` with id and per-dish macros | https://github.com/diego-torres/nutriconsultas/issues/354 | **done** | ~~94~~ ✓ | Merged PR [#357](https://github.com/diego-torres/nutriconsultas/pull/357) |
| 352 | Patient platillo detail endpoint (ingredients, prep, nutrition) | https://github.com/diego-torres/nutriconsultas/issues/352 | **done** | ~~94~~ ✓, ~~354~~ ✓ | Merged PR [#357](https://github.com/diego-torres/nutriconsultas/pull/357) |
| 353 | Grocery list for patient diet plan | https://github.com/diego-torres/nutriconsultas/issues/353 | **in-progress** | ~~94~~ ✓ | `GET /rest/mobile/patient/diet-plans/{assignmentId}/grocery-list` |
| 355 | Add default platillo image `plato-vacio.jpg` to static resources | https://github.com/diego-torres/nutriconsultas/issues/355 | **in-progress** | — | Serves `/sbadmin/img/plato-vacio.jpg`; OpenAPI `imageUrl` documented |
| 529 | Patient profile photo — S3 upload API + picture resolver for mobile | https://github.com/diego-torres/nutriconsultas/issues/529 | **open** | #241, #107 | Custom photo in S3; falls back to `PacienteAvatarCatalog`; mobile + web upload |

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
| 156 | `Paciente` internal refactor (pre-Liquibase) | — (backend only) | — | **done** — PRs #175/#176/#178 on `main` |
| 132–141 | Invitation onboarding | mobile (future) | Auth + profile | Replaces #109 manual linkage for new patients; gated by #156 |
| 558 | Auth0 social login reconcile alignment (planning) | mobile [#124](https://github.com/Escanor4323/nutriconsultas-mobile/issues/124) | Auth + profile | **open (planning)** — reconcile-by-code security review, reconcile matrix contract docs, `GET`/`PATCH /patient/me` alignment, `email_verified` end-to-end. Reconcile itself already ships via #136/PR #345; see [Resolved decisions](#558-resolved-decisions). |

**Common contract for every #91–#99** (ALIGNMENT-SPEC §"CORRECTED SCOPE"):
- Auth: OAuth2 Resource Server JWT, separate `@Order(1)` `SecurityFilterChain`, stateless.
- Principal: JWT `sub` → `Paciente.patientAuthSub` (#107). 403 if unlinked.
- Envelope: `ApiResponse<T>`; lists in `PagedResponse<T>` (#110); ISO-8601 dates.
- i18n: `Accept-Language` (es-MX default) error bodies (#111).
- PHI-safe logging (`LogRedaction`, #115); no names/emails/DOB at INFO.

**E2E HTTP codes (2026-06-15):**
- **401** — missing/invalid JWT or wrong `aud` (check mobile `AUTH0_AUDIENCE` = `https://api.nutriconsultas.minutriporcion.com`).
- **403** — JWT valid but patient data blocked: unlinked `sub`, `ONBOARDING` on data endpoints, or `REVOKED`/`INVITED` (#137). Localized `ApiResponse` with `error.patient.onboarding.required`. Legacy controller paths may still return `error.patient.not.linked` (#111).
- **404** — linkage OK but resource not found (localized `error.resource.not.found`).
- **400** — validation failure (e.g. blank message body → `error.message.required`).
- **429** — patient write rate limit exceeded (#113); `Retry-After: 60`; localized `error.rate.limit.exceeded`.

---

## How to update this file

**Starting work** — mark `in-progress`, keep `NEXT` on it until the PR merges:

```text
| 109 | Patient-Auth0 account linkage ... | https://... | in-progress | 107 | ... |
```

**After PR merges** — mark `done` and advance `NEXT` to the next unblocked row; update [`AGENT-WORKFLOW.md`](AGENT-WORKFLOW.md), [`docs/mobile-api/README.md`](docs/mobile-api/README.md), [`ALIGNMENT-SPEC.md`](docs/mobile-api/ALIGNMENT-SPEC.md) §F8.3, and [`AGENTS.md`](AGENTS.md) in the same PR:

```text
| 109 | Patient-Auth0 account linkage ... | https://... | done | 107 | ... |
| 110 | DTO conventions ... | https://... | **NEXT** | — | ... |
```

Commit registry updates in the same PR that implements the issue (Phase 9 of [`AGENT-WORKFLOW.md`](AGENT-WORKFLOW.md)). Keep the mobile registry's "Backend cross-reference" in sync when an endpoint's state changes.
