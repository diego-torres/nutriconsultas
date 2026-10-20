# Guest patients — product & technical plan

Limited **self-serve** mobile patients who have **no nutritionist yet**. They can explore Minutriporcion catalogs, build private dishes, log weight/BMI, pick a nutritionist, and **request** an appointment. Records expire after **one month** unless a nutritionist adopts the guest.

**Issue registry:** [`ISSUE-GUEST-PATIENTS.md`](../../ISSUE-GUEST-PATIENTS.md)  
**Workflow:** [`GUEST-PATIENTS-WORKFLOW.md`](../../GUEST-PATIENTS-WORKFLOW.md)  
**Backend epic:** [#612](https://github.com/diego-torres/nutriconsultas/issues/612)  
**Mobile epic:** [#152](https://github.com/Escanor4323/nutriconsultas-mobile/issues/152)  
**Mobile consumer:** [Escanor4323/nutriconsultas-mobile](https://github.com/Escanor4323/nutriconsultas-mobile) (`ISSUE.md` — Guest mode section)

This is a **new product track**, not a shortcut for App Store Review demo patient [#607](https://github.com/diego-torres/nutriconsultas/issues/607). Public booking ([`ISSUE-PUBLIC-BOOKING.md`](../../ISSUE-PUBLIC-BOOKING.md)) already creates an `ONBOARDING` patient **under** a known nutritionist at book time (#300). Guest mode inverts that: the patient exists first, then requests care.

---

## Goals

1. Let a person use the **patient app without an invitation** and without a nutritionist tenant (`Paciente.userId` unset).
2. Limit the guest API: **no** nutritionist diet, visits, or messages until adopted.
3. Let guests **browse discoverable nutritionists** and **request a scheduled appointment** (reuse public-booking availability; do **not** run the #300 “create patient under nutritionist” path).
4. Let guests **log weight and height**; compute **BMI** server-side; read their own series.
5. Let guests **read** Minutriporcion **system catalogs** (alimentos + platillos with `userId = system:catalog-platillos`) and **calculate nutrients** the same way nutritionist tools do.
6. Let guests **create private platillos** and **private foods**. Those rows must **never** appear in nutritionist catalog lists or diet authoring.
7. Guests **must not** insert or update nutrients on the **shared ingredients / alimento catalog**.
8. Guest `Paciente` rows (and their private foods/dishes/measurements) last **30 days**, then a **platform/maintenance** action can purge them.

User-facing copy: **Spanish (es-MX)**. Never log patient-identifiable data (`LogRedaction`).

---

## Non-goals (v1)

- Replacing the App Review seeded demo patient (#607) or changing invitation TTL.
- Guests authoring or mutating **system** alimentos / platillos / nutrient columns on catalog rows.
- Nutritionists picking guest dishes from the platillo catalog (even after adopt — patient-owned dishes stay patient-owned).
- Guest diet plans, grocery lists, messages, visit history, AI assistant, or push from a nutritionist.
- Sliding TTL (expiry does **not** reset on each login).
- Automatic scheduled purge (v1 is a **manual** platform-admin action on `/admin/platform/maintenance`, same page as #220 revoked-nutritionist retention — **separate button / run type**).
- Public anonymous booking without Auth0 (`/consultas/{id}/agendar-cita` stays as-is).

---

## Actors

| Actor | What they can do |
|-------|------------------|
| **Guest patient** | Sign up (Auth0) without invite; limited mobile API; private foods/dishes; weight/BMI; directory + appointment **request** |
| **Linked patient** (`ACTIVE` / `ONBOARDING` with `userId`) | Unchanged invite/redeem path. Not in this track except **adopt** (guest → owned) |
| **Nutritionist** | See incoming guest appointment requests; accept (adopt + cap #190) or decline |
| **Platform admin** | Run **purge expired guests** on maintenance page |

---

## Recommended decisions (v1)

Record here so implementers do not invent a second model. Change only via a plan amendment on the epic.

| Topic | Decision |
|-------|----------|
| **`Paciente.userId`** | **Nullable** for `GUEST`. Do **not** use a sentinel nutritionist tenant. `countByUserId` / clinic patient caps (#190) **exclude** guests until adopt. |
| **Status** | New `PacienteStatus.GUEST`. Existing `INVITED` / `ONBOARDING` / `ACTIVE` / `REVOKED` unchanged. |
| **Identity** | JWT `sub` → `Paciente.patientAuthSub` (same as #107). Never store the nutritionist `sub` on `userId` while guest. |
| **TTL** | `guest_expires_at = created_at + 30 days` at bootstrap. Property `nutriconsultas.guest-patients.retention-days=30`. **Not sliding.** |
| **Auth0** | Same patient application. Authorize with an explicit guest intent (e.g. `guest=true` custom param). Post-Login Action (#140): valid invitation → invited path; else guest intent → stamp `app_metadata.guest=true` (do **not** `access.deny`); else keep deny (invite-only for accidental first logins). |
| **Directory** | Nutritionists with **public booking enabled** (`NutritionistProfile.publicBookingId`) **and** an **active paid subscription**. Show display name, optional logo, optional cédula. **No** email or phone. |
| **Appointment request** | Authenticated guest books a **real slot** using #246/#248 availability. Creates a **pending request** + calendar hold. Does **not** `PacienteService.save` a new ONBOARDING row under the nutritionist (#300). Cap is checked **on accept**, not on request. |
| **Adopt** | Nutritionist accepts → set `userId`, status `ONBOARDING`, clear `guest_expires_at` / guest metadata. Patient-owned foods/platillos **remain** `paciente_id`-scoped (still excluded from nutritionist catalog). |
| **Progress writes** | **GUEST only** in v1: POST weight + height (BMI computed). Linked `ACTIVE` progress stays nutritionist-authored (#98/#99 GET). |
| **Private foods** | Prefer a **separate table** `paciente_alimento` (clone of catalog nutrient columns + `paciente_id`). Catalog `alimento` queries stay `paciente`-unaware. Guests cannot `POST/PUT/PATCH` catalog `alimento`. |
| **Private platillos** | `platillo.paciente_id` (nullable FK). When set, **exclude** from every nutritionist `findByUserId` / system catalog union. **Never** set `userId` to `system:catalog-platillos` or to a nutritionist `sub`. |
| **Maintenance vs #220** | Same page, **different** action: “Purgar pacientes invitados vencidos”. Do **not** mix into revoked-nutritionist S3 tenant purge. Confirm with **bootstrap-sweetalert**. Log **counts**, not names. |

---

## Data model (illustrative)

Incremental Liquibase only (`docs/db/LIQUIBASE.md`). Never edit merged baseline SQL. Never `ddl-auto=update`.

### `paciente`

| Column | Notes |
|--------|--------|
| `user_id` | Become **nullable**. `GUEST` rows are `NULL`. |
| `status` | Add enum value `GUEST` (PostgreSQL + H2). |
| `guest_expires_at` | `timestamptz`, null for non-guests. Indexed for purge. |

Existing `patient_auth_sub` unique linkage still applies.

### `paciente_alimento` (new)

Owner-scoped foods for dish building. Same nutrient fields as catalog `alimento` / `AbstractNutrible`, plus `paciente_id` (FK, cascade with guest purge). **No** writes to catalog `alimento`.

### `platillo`

| Column | Notes |
|--------|--------|
| `paciente_id` | Nullable FK. Non-null ⇒ patient-private dish. |

Nutritionist list methods must add `paciente_id IS NULL` (or equivalent) so guest/patient dishes never mix with `system:catalog-platillos` or `findByUserId(nutritionistSub)`.

### Appointment request

New table (name illustrative): `guest_appointment_request` — guest `paciente_id`, nutritionist `user_id`, slot start/end, status `PENDING` / `ACCEPTED` / `DECLINED` / `EXPIRED`, FK to `calendar_event` when a hold exists.

---

## API surface (guest JWT)

All under `/rest/mobile/**`, `ApiResponse` envelope (#110), Spanish errors (#111), PHI-safe logs (#115). OpenAPI regen (`docs/api/openapi-mobile.yaml`).

**Allow for `PacienteStatus.GUEST`** (extend `MobilePatientAccessRules` — today only `ACTIVE` gets the full patient API; `ONBOARDING` is profile-only):

| Area | Behavior |
|------|----------|
| `GET/PATCH /rest/mobile/patient/me` | Profile; guest flag + `expiresAt` |
| `POST /rest/mobile/guest/bootstrap` | Idempotent create/link GUEST `Paciente` after Auth0 login |
| Progress GET + **POST weight/height** | Own measurements only |
| Catalog GET | System alimentos + system platillos, read-only |
| Private alimentos CRUD | Own rows only |
| Private platillos CRUD + nutrient calc | Own dishes; ingredients = catalog **or** own foods |
| Nutritionist directory GET | Discoverable profiles |
| Appointment request POST + list own | Slots from existing availability services |

**Deny (403 / empty as specified in OpenAPI):** visits, diet assignments, nutritionist messages, grocery, appointment-questions that assume a linked visit, any other `ACTIVE`-only resource.

IDOR: a guest must never read another `paciente_id`’s foods, dishes, measurements, or requests.

---

## Auth0 Post-Login gate (#140)

Today first login without a valid invitation is **denied**. Guest mode needs an **explicit** bypass:

1. Mobile authorize includes guest intent.
2. Action: invitation token valid → existing invited stamp.
3. Else guest intent → `app_metadata.guest = true`, allow tokens.
4. Else deny (preserve invite-only default).
5. Backend bootstrap is authoritative: no `Paciente` until `POST …/guest/bootstrap` succeeds.

Document the Action diff in `docs/auth0/PATIENT-POST-LOGIN-GATE.md` (do not weaken the invite path).

---

## Nutritionist web

Incoming **guest appointment requests** (calendar or a small inbox). Accept:

1. Entitlement check (#190) — if over cap, Spanish error, leave guest as `GUEST`.
2. Set `userId`, `ONBOARDING`, clear guest expiry.
3. Confirm calendar event on the nutritionist calendar.

Decline: release slot; guest remains `GUEST`.

Admin dialogs: **bootstrap-sweetalert**, Spanish copy.

---

## Platform maintenance

Page: `/admin/platform/maintenance` (`MaintenanceAdminController`, `sbadmin/platform/maintenance/list.html`).

Add a **second** card/action: purge `status = GUEST` and `guest_expires_at < now()` (batch). Delete:

- guest `Paciente` (and satellites)
- `paciente_alimento` for that id
- platillos with that `paciente_id` (+ ingredientes)
- anthropometry / body-mass rows for that patient
- pending guest appointment requests

Do **not** delete catalog `alimento` or `system:catalog-platillos` rows. Do **not** use `NutritionistTenantPurgeService` (#220). Sweetalert confirm. Flash counts only.

---

## Tests (mandatory on feature PRs)

- Schema: Liquibase incremental; H2 `mvn verify`; nullable `user_id` does not break existing nutritionist patient queries.
- Access: `GUEST` allowed paths vs 403 on diet/visits/messages.
- Isolation: nutritionist platillo list never contains `paciente_id != null`; catalog alimento update 403 for guest JWT.
- Caps: guest create does not increment #190 count; adopt does.
- IDOR: guest A cannot GET guest B resources.
- Maintenance: expired guest + foods + dishes removed; unexpired guests kept; catalog untouched.
- Template validator for maintenance page new model attributes.
- OpenAPI + `ALIGNMENT-SPEC` guest appendix.

---

## Suggested delivery waves

| Wave | Backend | Mobile |
|------|---------|--------|
| **A** | Plan/registry | — |
| **B** | Schema + Auth/bootstrap + access rules | API client + guest signup |
| **C** | Catalog read + private foods/dishes + calc + weight/BMI | Home shell + those screens |
| **D** | Directory + appointment request + nutritionist accept | Directory + request appointment |
| **E** | Maintenance purge | Expiry UX |
| **F** | OpenAPI / remaining tests | Widget/e2e mocks |

Mobile issues live in **Escanor4323/nutriconsultas-mobile** and must cross-link backend children.
