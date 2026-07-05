# AI Nutrition Assistant — Data Access Rules (v1)

**Issue:** [#362](https://github.com/diego-torres/nutriconsultas/issues/362) · Epic [#360](https://github.com/diego-torres/nutriconsultas/issues/360)  
**Related:** [`FUNCTIONAL-SCOPE.md`](FUNCTIONAL-SCOPE.md) (#361) · [`AI-ASSISTANT-PLAN.md`](AI-ASSISTANT-PLAN.md)

Rules for what the AI assistant may read, send to OpenAI, persist, and log. Implementation follows in orchestration (#385), tools (#372–#378), and audit (#397).

---

## Principles

| Rule | Detail |
|------|--------|
| **Backend gatekeeper** | Only Spring services build outbound OpenAI payloads; browser never sees API keys or raw patient rows. |
| **Minimum necessary** | Send the smallest structured subset needed for drafting; prefer IDs + numeric constraints over free text. |
| **Tenant isolation** | Same boundaries as nutritionist web: `Paciente.userId`, catalog ownership, plan tier (#409). |
| **No silent writes** | Tools may create **drafts** only; no final patient assignment or catalog mutation without nutritionist accept (#382). |
| **Spanish comms** | User-facing assistant text is es-MX; internal JSON field names may stay English. |

---

## Authentication and plan gating

| Check | When | Failure |
|-------|------|---------|
| OAuth2 session (nutritionist) | Every `/nutritionist/ai/**` and MCP call | 401 / redirect to login |
| `AI_ENABLED=true` | Feature flag | 503 friendly Spanish message |
| `Entitlement.AI_ASSISTANT` (#409) | Plus + Consultorio only | 403 upgrade message (Spanish) |
| Rate limit (#386) | Per nutritionist / window | 429 friendly message |

OpenAI credentials (`OPENAI_API_KEY`, legacy `OPEN_API_KEY`) exist **only** in server env / `app.env` — never in Thymeleaf, JavaScript, REST JSON, Git, or application logs.

---

## Nutritionist and clinic scoping

### v1 — owning nutritionist (default)

All AI data access uses the authenticated nutritionist’s OAuth **`sub`** as `userId`.

| Resource | Access rule | Repository / pattern |
|----------|-------------|----------------------|
| **Patient** | `paciente.userId == principal.sub` | `findByIdAndUserId(id, userId)` + `verifyPatientOwnership()` |
| **AI chat thread** | `thread.nutritionistId == principal.sub` | Scoped repository lookup |
| **Platillo catalog** | System rows + rows where `platillo.userId == userId` (or platform admin) | `PlatilloAuthorization` |
| **Dieta catalog** | System templates + owned + patient copies for owned patients | `DietaAuthorization`, `pacienteId` check |
| **Alimento catalog** | Global reference catalog (read); tenant does not split alimentos by user in v1 | Existing `AlimentosRepository` |
| **Drafts** | `draft.thread.nutritionistId == userId` | Join through `ai_chat_thread` |

**Cross-tenant access is forbidden.** Another nutritionist’s patients, threads, drafts, or owned platillos must return **404** (prefer over 403 for IDOR on patient/thread IDs).

### Clinic / director (v1)

**Out of scope for v1.** A `director-consultorio` does **not** automatically receive another member’s patients in AI chat. Directors use existing clinic admin flows; AI threads are always owned by the **nutritionist who created them**.

Future issue may allow director read-only oversight with explicit audit — not in v1.

### Platform admin

Platform admins (`PlatformAdminService`) are not AI super-users in v1. AI assistant remains a **nutritionist workflow** tool.

---

## Patient-context access rules

Patient context is **optional**. It is loaded only when:

1. The nutritionist starts or continues a thread with `patient_id` set, **and**
2. `findByIdAndUserId(patientId, userId)` succeeds.

If the patient is not owned by the user, **do not** attach context and return 404 on thread access.

### Caloric target resolution

Reuse existing diet-assignment logic, extended for stress:

| Field | Include in OpenAI context | Notes |
|-------|---------------------------|-------|
| `requerimientoKcal` | yes | `totalAdjustedKcal` if set, else `getKcal` — same as [`resolveRequerimientoKcal`](../../src/main/java/com/nutriconsultas/paciente/PacienteController.java) |
| `finalTotalKcal` | yes, when `physiologicalStressActive == true` | Preferred planning target under stress |
| `bmr`, `getKcal`, `tefKcal`, `stressKcal` | optional breakdown | Helps assistant explain assumptions |
| `peso`, `estatura`, `imc`, `nivelPeso` | yes | Numeric / enum only |
| `gender` | yes | `M` / `F` — needed for some calculations |
| `pregnancy` | yes | Boolean |

Do **not** send `dob` or age derived to OpenAI in v1 unless a later issue requires it; nutritionist can state age band in chat if needed.

### Pathology and restrictions

| Field | OpenAI v1 | Notes |
|-------|-----------|-------|
| Pathology booleans (`diabetes`, `hipertension`, …) | **Allow** | From `PacienteMedicalHistory` |
| `alergias` | **Allow** (truncated) | Max **500 characters**; strip control chars; required for exclusion lists |
| `historialAlimenticio` | **Deny** default | High PHI density; nutritionist can paraphrase in chat |
| `antecedentesPatologicosPersonales` / `Familiares` | **Deny** | Use booleans + chat instead |
| `complicaciones`, `desarrolloPsicomotor`, prenatal/natal text | **Deny** | |
| `tipoSanguineo` | **Deny** | Not needed for menu drafting in v1 |
| `responsibleName`, `parentesco` | **Deny** | Contact PHI |

### Energy preferences

| Field | OpenAI v1 |
|-------|-----------|
| `physicalActivityLevel`, `activityFactor` | Allow |
| `physiologicalStressActive`, `physiologicalStressType` | Allow (enum names) |
| `tefMethod`, `tefFixedPercent`, macro TEF percents | Allow |
| `stressValidFrom` / `stressValidUntil` | Deny (dates) |

### Identifiers in OpenAI payload

| Field | OpenAI v1 |
|-------|-----------|
| `patientId` (internal Long) | **Allow** — opaque ID, not PHI by itself |
| `assignedId` | **Deny** — often human-readable clinical code |
| `name`, `displayName` | **Deny** |
| `email`, `emailHint`, `phone` | **Deny** |
| `patientAuthSub` | **Deny** |
| `userId` (nutritionist) | **Deny** in patient DTO — already implied by session |

### Proposed DTO shape (implementation #385)

Server builds `AiPatientContext` (name illustrative):

```json
{
  "patientId": 42,
  "gender": "F",
  "pregnancy": false,
  "requerimientoKcal": 2457.0,
  "finalTotalKcal": 2600.0,
  "physiologicalStressActive": true,
  "imc": 23.7,
  "nivelPeso": "NORMAL",
  "pathologyFlags": {
    "diabetes": false,
    "hipertension": true,
    "obesidad": false
  },
  "alergias": "Mariscos, nueces",
  "activityLevel": "MODERATE"
}
```

Never serialize the full `Paciente` entity to OpenAI.

---

## Catalog and tool data boundaries

| Tool class | Data returned to model | Scoping |
|------------|------------------------|---------|
| Read-only catalog | Food/dish IDs, names, nutrients, units | Authorized rows only; cap result count (e.g. 10–25) |
| Nutrient calculation | Computed numbers from DB | Input food IDs validated against catalog |
| Draft creation | Draft JSON stored in PostgreSQL | `nutritionistId` from session |
| Patient clinical timeline | **Not exposed** | No `CalendarEvent`, `ClinicalExam`, `AnthropometricMeasurement` series, `PatientMessage` tools in v1 |
| Mobile / `patientAuthSub` | **Not exposed** | |

System catalog platillos/dietas are readable like today’s web app; mutating system rows remains platform-admin only (unchanged).

---

## OpenAI payload hygiene

### Always exclude from outbound requests

- API keys, webhook secrets, Auth0 secrets
- Patient name, email, phone, DOB, responsible contact
- Free-text antecedents and clinical notes
- Full chat history from **other** nutritionists or patients
- Other tenants’ catalog rows
- Raw SQL, stack traces, or internal stack metadata

### Conversation history sent to OpenAI

- Messages from **current thread** only, owned by caller
- System prompt (#367) includes safety + Spanish + tool-use rules
- Optional: truncate older turns beyond token budget (implementation #385); persist full history locally regardless

### `OPENAI_STORE`

Default **`false`** (`nutriconsultas.ai.openai.store=${OPENAI_STORE:false}`). Do not enable provider-side conversation retention for PHI-bearing threads without legal review.

---

## Logging and audit (#397)

Align with [`PHI-LOGGING-AUDIT.md`](../mobile-api/PHI-LOGGING-AUDIT.md) and [`LogRedaction`](../../src/main/java/com/nutriconsultas/util/LogRedaction.java).

### May log (INFO)

| Event | Example |
|-------|---------|
| Thread created | `AI thread id=123 nutritionist=[redacted-user]` |
| Message sent | `AI message threadId=123 role=user` (not full body at INFO) |
| Tool invoked | `AI tool search_food_catalog threadId=123 resultCount=8` |
| Draft created | `AI draft id=456 type=DIET_PLAN threadId=123` |
| Draft accept/discard | `AI draft id=456 status=ACCEPTED` |
| OpenAI error class | `AI openai_error type=rate_limit threadId=123` |
| Plan denied | `AI access_denied reason=missing_entitlement` |

### Must not log (any level unless DEBUG with redaction policy)

- OpenAI API key or request `Authorization` header
- Full user/assistant message bodies containing `alergias` or patient context at INFO
- Patient `name`, `email`, `phone`, `dob`
- Entire `Paciente` or `PacienteMedicalHistory` objects
- Raw OpenAI request/response JSON at INFO

### DEBUG

- If DEBUG logging of prompts is needed in development, gate behind profile `dev` + explicit property `nutriconsultas.ai.debug-log-prompts=false` default **false** in all deployed environments.

### Audit trail (database)

Persist in `ai_chat_message` / `ai_generated_draft` (#369). Retention: follow general application backup policy; document in #405/#406.

### Usage metrics (#398)

Micrometer counters via `AiUsageMetrics` (Spring Boot Actuator). Exposed at `/actuator/metrics` when enabled. **No PHI in tags** — only low-cardinality labels (`mode`, `type`, `kind`, `source`, `tool`).

| Metric | Tags | When incremented |
|--------|------|------------------|
| `ai.chat.messages` | `mode` | Chat request (send/stream/edit) |
| `ai.drafts.created` | `type` | Draft tool creates borrador |
| `ai.drafts.accepted` | — | Draft accepted |
| `ai.drafts.discarded` | — | Draft discarded |
| `ai.openai.errors` | `kind` | OpenAI client failure |
| `ai.rate_limited` | `source=chat\|openai` | App or OpenAI rate limit |
| `ai.openai.tokens` | `kind=prompt\|completion` | Orchestration token usage |
| `ai.tool.calls` | `tool` | Tool invoked in orchestration |

---

## API key handling (confirmation)

| Location | Allowed |
|----------|---------|
| EC2 `/opt/nutriconsultas/app.env`, local `.env` (gitignored) | yes |
| Spring `Environment` / `@Value` in server beans | yes |
| Thymeleaf model attributes | **no** |
| Browser JS, HTML comments, REST responses | **no** |
| INFO/WARN/ERROR logs | **no** |
| Git, CI logs, error trackers | **no** |
| OpenAI request from browser | **no** — all calls server-side |

Rotate keys if exposed. Use separate test keys locally; never commit `.env`.

---

## Security test expectations (#395, #403)

- User A cannot read user B’s thread, drafts, or patient-linked context (404)
- Profesional/Básico user receives 403 on AI endpoints when #409 enforced
- Patient context omitted when `patient_id` null
- Outbound OpenAI payload snapshot tests assert deny-list fields absent
- Logs under test do not contain sample patient name/email from fixtures

---

## Related documents

| Doc | Issue |
|-----|-------|
| [`FUNCTIONAL-SCOPE.md`](FUNCTIONAL-SCOPE.md) | Workflows and patient-linked UX |
| System prompt (#367) | `AiSystemPromptService`, `ai/system-prompt-base.txt` |
| Audit logging implementation | ~~#397~~ — `AiAuditLogger`, `AiAuditRedaction` |
| Production setup | #406 |

---

## Acceptance checklist (#362)

- [x] Nutritionist/clinic scoping rules defined
- [x] Patient-context access rules defined
- [x] Safe vs excluded OpenAI fields defined
- [x] Logging and audit requirements defined
- [x] API keys backend-only confirmed
