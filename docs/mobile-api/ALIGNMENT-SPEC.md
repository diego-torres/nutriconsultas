# Mobile API Alignment Spec (canonical — source of truth for all agents)

Date: 2026-06-09. Produced after reading both repos + both planning docs + live GitHub issues.
This file is the SHARED contract. Agents A/B/C must follow it exactly. Do not re-derive.

## Repos
- Backend: `diego-torres/nutriconsultas` (Spring Boot). Issues #91–#99 = `[Mobile API]` endpoints. #101 closed (PDF branding).
- Mobile: `Escanor4323/nutriconsultas-mobile` (Flutter/GetX). Issues #1–#21 already exist.
- Docs (local, in /Users/joelmartinez/Documents/Work): `mobile-api-roadmap-v2.md`, `mobile_api_token_plan.md`.
- Mobile docs (in mobile/docs/): `api-contract.md`, `flutter-architecture.md`, `ui-ux.md`, `backend-notes.md`.

## GROUND-TRUTH FACTS (verified, override any doc that disagrees)

### F1 — Networking stack is Dio, NOT GetConnect.
mobile/pubspec.yaml has `dio: ^5.7.0`. There is NO GetConnect usage. Mobile issue #5 = "Dio ApiClient with
AuthInterceptor and ErrorInterceptor". roadmap-v2 §A3/§A6/§G-pubspec/FL-1 all wrongly use GetConnect — WRONG.
Correct stack: `Dio` instance + `AuthInterceptor` (Bearer) + `ErrorInterceptor` (HTTP→AppException).

### F2 — Patient sub mapping bug (SECURITY-RELEVANT). Appears in TWO places:
- roadmap-v2 §G2 flags it correctly: patient JWT `sub` must map to NEW field `Paciente.patientAuthSub`,
  NOT `Paciente.userId` (which is the NUTRITIONIST's Auth0 sub / tenant owner).
- mobile/docs/api-contract.md line ~3 is WRONG: it says "Auth0 `sub` claim maps to `Paciente.userId`".
  MUST be corrected to `Paciente.patientAuthSub`.

### F3 — Project structure is feature-first GetX, NOT clean-arch data/domain/presentation.
Actual mobile lib/ layout:
  lib/core/{constants,utils,models,services}  (services: auth_service, api_service, notification_service, storage_service)
  lib/app/{theme,bindings,controllers,routes,translations}
  lib/shared/{layouts,widgets}
  lib/features/<feature>/{bindings,controllers,views,widgets}
mobile/docs/flutter-architecture.md adds `lib/core/{network,auth,exceptions,di}` and `lib/data/{models,repositories}`.
That (Dio-based, feature-first + thin data layer) is the TARGET. roadmap-v2's `domain/{entities,usecases,repositories}`
clean-arch layer is the OUTLIER — drop the domain/usecases layer; use data/ + features/ only.

### F4 — Translations already exist: lib/app/translations/{app_translations.dart, locales/es_MX.dart, locales/en_US.dart}.
So roadmap "FL-2 i18n foundation" is largely DONE. Remaining: locale toggle persistence + Accept-Language header on Dio.

### F5 — pubspec currently HAS: get, flutter_screenutil, get_storage, dio, intl, flutter_local_notifications,
permission_handler, flutter_svg.
pubspec currently LACKS (must be added for the roadmap): auth0_flutter, flutter_secure_storage, fl_chart,
flutter_pdfview, connectivity_plus, envied (env config). (notification deps already present → FL-9 push is partly enabled.)

### F6 — Backend auth (updated 2026-06-14)
`spring-boot-starter-oauth2-resource-server` + `MobileSecurityConfig` `@Order(1)` on `/rest/mobile/**` (**#107 done**, PR #117). Patient JWT `sub` → `Paciente.patientAuthSub`; `PatientLinkageFilter` returns localized 403 (#111).

## RECONCILIATION MAP — roadmap FL-N  ↔  existing mobile issues #1–#21  ↔  backend issues #91–#99

| Roadmap FL | Existing mobile issue(s) | Backend issue(s) | Notes |
|-----------|--------------------------|------------------|-------|
| FL-0 Auth (Auth0 PKCE+storage) | #7 AuthService(Auth0)+AuthMiddleware, #13 Auth feature | Phase 0 | secure_storage dep needed |
| FL-1 ApiClient + interceptor   | #5 Dio ApiClient (Auth+Error interceptor), #9 AppException | Phase 0 | Dio, NOT GetConnect |
| FL-2 i18n                      | (translations already exist) | — | only toggle+Accept-Language remain (NEW small issue) |
| FL-3 Network status / offline  | (none — GAP) #9 related | — | NEW issue + connectivity_plus dep |
| FL-4 Visits screens            | #17 Visits feature, #1 visit models, #10 VisitsRepository | #91, #92 | |
| FL-5 Diet screens + PDF        | #18 Diet feature(list/detail/PDF), #3 diet models, #12 DietRepository | #93, #94, #95 | flutter_pdfview dep |
| FL-6 Progress + chart          | #16 Progress snapshot, #20 Progress chart(fl_chart), #8 progress models, #15 ProgressRepository | #98, #99 | fl_chart dep |
| FL-7 Messaging                 | #19 Messages feature, #6 message model, #14 MessagesRepository | #96, #97 | |
| FL-8 PDF download/open         | part of #18 | #95 | flutter_pdfview/share |
| FL-9 Push (FCM)                | (none — GAP; notification_service exists) | #96 | NEW issue |
| infra                          | #2 deps, #4 routes, #11 DI bootstrap, #21 component lib | Phase 0 | |

## BACKEND ENDPOINT ↔ MOBILE CONSUMER cross-reference (for two-way linking)
- #91 visits list      → mobile #17/#10/#1
- #92 visit detail     → mobile #17/#1
- #93 diet list        → mobile #18/#12/#3
- #94 diet detail      → mobile #18/#12/#3
- #95 diet PDF         → mobile #18
- #96 messages list    → mobile #19/#14/#6
- #97 send message     → mobile #19/#14/#6
- #98 progress snapshot→ mobile #16/#15/#8
- #99 progress series  → mobile #20/#15/#8

## CORRECTED SCOPE for backend [Mobile API] issues (apply as edits/comments)
Common to all #91–#99:
- Auth: OAuth2 Resource Server JWT for `/rest/mobile/**` (separate @Order(1) SecurityFilterChain), stateless.
- Patient principal: resolve JWT `sub` → `Paciente.patientAuthSub` (NEW field), NOT `userId`. 403 if unlinked.
- DTO convention: wrap in `ApiResponse<T>`; lists in `PagedResponse<T>`; ISO-8601 date strings.
- i18n: respect `Accept-Language` (es-MX default) via LocaleContextFilter + MessageSource for errors.
- PHI-safe logging (LogRedaction); no names/emails/DOB at INFO.
- All depend on a NEW "Phase 0" issue (blocked-by).
Per-issue extras:
- #92: IDOR guard — return 404 (not 403) on ownership miss (don't leak existence).
- #93: `activeOnly` filter param.
- #94: strip nutritionist-internal fields; define DietPlanDetailDto tree.
- #95: `Content-Disposition`, ownership check, depends on #101/PDF branding for cédula/logo.
- #96: cursor pagination (not offset); never log body.
- #97: senderRole=PATIENT from JWT only; @Valid @Size(max=2000); Resilience4j rate limit 10/min.
- #98: aggregate in service layer; deltas vs prior measurement.
- #99: from/to ISO-8601; cap maxRows=365; ASC order.

## NEW BACKEND ISSUES to create (from roadmap Part D)
1. `[Mobile API] Phase 0 — Auth0 JWT resource server + patient Auth0 linkage` (P0; blocks #91–#99)
2. `[Mobile API] Auth0 API resource + audience + scopes setup` (P0)
3. `[Mobile API] Patient-Auth0 account linkage (admin invite/assign flow)` (P0)
4. `[Mobile API] DTO conventions + ApiResponse/PagedResponse wrappers` (P0)
5. `[Mobile API] Accept-Language filter + MessageSource i18n for REST errors` (P1)
6. `[Mobile API] OpenAPI spec for /rest/mobile/patient/**` (P1)
7. `[Mobile API] Rate limiting on patient write endpoints (Resilience4j)` (P2; for #97)
8. `[Mobile API] Nutritionist reply to patient messages` (P2; for #96/#97)
9. `[Mobile API] PHI log redaction audit for all mobile controllers` (P1)

## NEW MOBILE ISSUES to create (gaps not covered by #1–#21)
1. `[Mobile] Phase 0 dependency — backend Auth0 JWT resource server (BLOCKS all API integration)`
   - tracks backend Phase 0; links the backend Phase-0 issue; blocks #5,#7,#10-20.
2. `[Mobile] Fix API contract: patient sub maps to patientAuthSub, not userId`
   - corrects docs/api-contract.md; coordinate with backend #91/Phase 0.
3. `[Mobile] Add roadmap deps to pubspec: auth0_flutter, flutter_secure_storage, fl_chart, flutter_pdfview, connectivity_plus, envied`
   - (or augment existing #2 — see AGENT C instructions).
4. `[Mobile] FL-3 Network status controller + offline error state` (connectivity_plus)
5. `[Mobile] i18n: locale toggle persistence + Accept-Language header on Dio`
6. `[Mobile] FL-9 Push notifications (FCM) for new nutritionist messages` (extends notification_service)
All new mobile issues must reference the backend endpoint(s) they consume per the cross-reference table.

## AUDIENCE RECONCILIATION (F7 — added 2026-06-09, AUTHORITATIVE)
The app MUST be PATIENT-FACING. Confirmed by README, backend `/rest/mobile/patient/**` (#91–#99),
all 21 mobile issues, and both roadmaps. A patient consumes ONLY their own data:
Visitas (own visits), Planes de Dieta (own diet plans), Progreso (own progress), Mensajes (chat with their nutritionist).

BUT the currently-IMPLEMENTED mobile app is NUTRITIONIST-FACING scaffold that contradicts this:
- Bottom nav tabs: Home · Patients · Consultations · Settings  → MUST become: Inicio/Home · Visitas · Planes · Progreso · Mensajes (patient set).
- Floating "+" → patientForm (create patient)  → MUST be removed (a patient cannot create patients).
- features/patients/ (list ALL patients, search, full CRUD form name/phone/email/goal/notes/DOB) → RETIRE (roster mgmt is nutritionist-only).
- features/consultations/ with consultForm/consultDetail (author consultations) → RETIRE for patient app (patient only VIEWS own visits = Visitas).
- features/meal_plans/ authoring → patient only VIEWS assigned diet plans (read-only).
- dashboard "todayConsultations"/"consults today" banner → replace with patient home (next visit, active plan, progress snapshot).
- auth_service has NO role/audience gating → patient app authenticates as patient (Auth0 API audience), resolves to patientAuthSub.

DIRECTION: All issues in BOTH registries must reflect the patient-facing dynamic. Nutritionist-facing scaffold
(patients/consultations/meal_plans CRUD, roster nav, "+" create) must be tracked for retirement/repurposing.
Backend #114 (Nutritionist reply) is CORRECT as a backend-for-WEB feature and stays — it is not a mobile-app feature.

## AUGMENT existing mobile issues #1–#21 (add a cross-ref note, do not rewrite)
Add to each a short "Backend dependency" line per the cross-reference table, e.g.:
- #17 Visits → "Consumes backend `diego-torres/nutriconsultas` #91 (list), #92 (detail). Requires Phase 0."
- #5 Dio ApiClient → "Base URL serves `/rest/mobile/patient/**`; Bearer JWT audience per backend Phase 0; map 401→logout."
- #7 AuthService → "patient JWT sub → backend `patientAuthSub`; audience = Auth0 API identifier (backend Phase 0)."
(Use the cross-ref table for #10/#12/#14/#15/#16/#18/#19/#20/#1/#3/#6/#8.)

## SCHEMA RECONCILIATION (F8 — added 2026-06-11, AUTHORITATIVE; backend verified at commit 228bbc3)

### F8.1 — Backend↔contract field-name map (DTO serialization aliases; NO schema changes needed)
| Backend entity.field | Mobile contract key |
|---|---|
| `Dieta.nombre` | `dietaName` |
| `Dieta.energia` | `totalKcal` |
| `Dieta.proteina` | `totalProteina` |
| `Dieta.lipidos` | `totalGrasas` |
| `Dieta.hidratosDeCarbono` | `totalCarbohidratos` |
| `Ingesta.nombre` | `tipo` |
| `PlatilloIngesta.name` / `portions` / `energia` | `nombre` / `porciones` / `kcal` |
| `PlatilloIngesta.hidratosDeCarbono` / `lipidos` | `carbohidratos` / `grasas` |
| `AlimentoIngesta.*` | same translations as PlatilloIngesta |

### F8.2 — Enums (mobile must handle ALL values)
- `EventStatus`: SCHEDULED / COMPLETED / CANCELLED
- `PacienteDietaStatus`: ACTIVE / COMPLETED / CANCELLED (no INACTIVE exists)
- `NivelPeso`: BAJO / NORMAL / ALTO / SOBREPESO → service translates to `imcLabel` display string

### F8.3 — Verified gaps (backend, updated 2026-06-14)
- **Phase 0 DONE:** #107 (PR #117), #109 (PR #142), #110 (DTO envelope).
- **Endpoints on `main`:** #91–#99 (visits, diet plans + PDF, messages, progress snapshot + measurements time series); #96/#97 messaging with HTTP 201 + Resilience4j 10/min (#113, PR #151).
- **i18n (#111) DONE** (PR #151): `LocaleContextFilter`, `MobileApiErrorResponses`, localized 403/404/400/429.
- **Cross-cutting NEXT:** #116 `senderDisplayName`. ~~#115~~ PHI log redaction audit **in progress** (`docs/mobile-api/PHI-LOGGING-AUDIT.md`). ~~#112~~ OpenAPI spec **done** (PR #164, `docs/api/openapi-mobile.yaml`).
- `deltaPeso`/`deltaImc` are computed-at-query, not stored.
- Progress `grasa` ambiguity: prefer `bodyComposition.porcentajeGrasaCorporal` (patient-facing %) over `indiceGrasaCorporal`.
- Template dietas (seed `system:template-dietas`) have 4 ingestas incl. Colación — contract examples show only 3.

### F8.4 — NutritionistProfile (NEW entity, 228bbc3)
`userId` (unique, nutritionist sub), `displayName`, `cedulaProfesional`, `logoExtension`, `registro`.
- #95 PDF endpoint now serves branded PDFs transparently (no scope change).
- Optional additive: `senderDisplayName` in message DTOs (#96) from `NutritionistProfile.displayName` — track as separate issue.

### F8.5 — Design source of truth
Canonical visual design: `mobile/.claude/MiNutriporcion-2/` (light editorial palette per JSX + 01-p-dashboard.png; the dark welcome/login screenshots are an OLD iteration — ignore).
Mobile branch `style/editorial-redesign` implements it; auth dedup done (AuthFlowController canonical, PR #48's AuthController/LoginView retired). UI field inventory: `mobile/docs/schema-planning.md` §10.
