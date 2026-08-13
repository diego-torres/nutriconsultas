# Appointment question reminders — mobile contract (#587)

**Backend:** [#587](https://github.com/diego-torres/nutriconsultas/issues/587)  
**Schema:** Liquibase `037-appointment-question.yaml` (`appointment_question`)  
**OpenAPI twin:** [`../api/openapi-mobile.yaml`](../api/openapi-mobile.yaml) (`/rest/mobile/patient/appointment-questions`)

Patient-owned private reminders of questions to ask the nutritionist at the **next appointment**. This is **not** the nutritionist message thread (`/messages`).

---

## Product rules

| Rule | Detail |
|------|--------|
| Audience | Linked **patient** only (`ACTIVE` after onboarding) |
| Visibility | Private to the patient — nutritionist web does **not** read these in v1 |
| Auth | Patient JWT; `sub` → `Paciente.patientAuthSub` |
| Ownership / IDOR | All queries scoped by `pacienteId`; miss → **404** (not 403) |
| PHI | Never log question `body`; use `LogRedaction.redactAppointmentQuestion(id)` |

---

## AuthZ / error codes

| HTTP | When |
|------|------|
| **401** | Missing or invalid JWT |
| **403** | Patient not linked / onboarding gate |
| **400** | Validation failed (blank `body`, oversized text) |
| **404** | Question id missing or belongs to another patient |
| **429** | Create rate limit (`patientAppointmentQuestions`: 20/min per patient in prod; 2/min in test) |

Envelope: mobile `ApiResponse` with localized `message` + `timestamp` on errors.

---

## Resource model (`AppointmentQuestionDto`)

```json
{
  "id": 42,
  "body": "¿Puedo comer mango?",
  "answered": false,
  "answeredAt": null,
  "createdAt": "2026-08-13T15:00:00Z",
  "updatedAt": "2026-08-13T15:00:00Z"
}
```

| Field | Type | Notes |
|-------|------|-------|
| `id` | long | Server-assigned |
| `body` | string | 1–2000 chars |
| `answered` | boolean | Patient marks as asked/done after the visit |
| `answeredAt` | instant \| null | Set when `answered` becomes `true`; cleared when reopened |
| `createdAt` / `updatedAt` | instant | ISO-8601 |

---

## `GET /rest/mobile/patient/appointment-questions`

**Success:** **200** + `ApiResponse<PagedResponse<AppointmentQuestionDto>>`

| Query | Default | Notes |
|-------|---------|-------|
| `page` | `0` | Zero-based |
| `size` | `20` | Max 100 |
| `answered` | omitted | Optional filter: `true` / `false` |

Sort: `createdAt` descending.

### Example

```bash
curl -sS "$API/rest/mobile/patient/appointment-questions?answered=false" \
  -H "Authorization: Bearer $PATIENT_JWT"
```

---

## `GET /rest/mobile/patient/appointment-questions/{questionId}`

**Success:** **200** + `ApiResponse<AppointmentQuestionDto>`  
**Miss / other patient:** **404**

---

## `POST /rest/mobile/patient/appointment-questions`

**Success:** **201** + `ApiResponse<AppointmentQuestionDto>`

### Request

```json
{
  "body": "¿Debo tomar más agua?"
}
```

| Field | Required | Notes |
|-------|----------|-------|
| `body` | yes | `@NotBlank`, max 2000; trimmed on save |

Creates with `answered=false`.

---

## `PATCH /rest/mobile/patient/appointment-questions/{questionId}`

**Success:** **200** + `ApiResponse<AppointmentQuestionDto>`  
**Miss / other patient:** **404**

### Request (partial)

```json
{
  "body": "Texto actualizado",
  "answered": true
}
```

| Field | Required | Notes |
|-------|----------|-------|
| `body` | no | If present, must be non-blank after trim; max 2000 |
| `answered` | no | `true` sets `answeredAt` (once); `false` clears `answeredAt` |

Omitted fields are unchanged. Empty `{}` is a no-op save of the existing row.

---

## `DELETE /rest/mobile/patient/appointment-questions/{questionId}`

**Success:** **204** No Content  
**Miss / other patient:** **404**

---

## Mobile implementation notes

1. Models: map `AppointmentQuestionDto` + `PagedResponse` / `ApiResponse` envelopes (same as visits).
2. Repository: list with optional `answered` filter; create / get / patch / delete by id.
3. UI suggestion: “Preguntas para mi próxima cita” — open list (`answered=false`), mark asked after the visit, allow edit/delete.
4. Do **not** send these as `/messages`; they are personal reminders only.
5. Rate-limit create; surface **429** with the existing localized mobile error UI.

---

## Example (curl)

```bash
# Create
curl -sS -X POST "$API/rest/mobile/patient/appointment-questions" \
  -H "Authorization: Bearer $PATIENT_JWT" \
  -H "Content-Type: application/json" \
  -d '{"body":"¿Puedo comer mango?"}'

# Mark answered
curl -sS -X PATCH "$API/rest/mobile/patient/appointment-questions/42" \
  -H "Authorization: Bearer $PATIENT_JWT" \
  -H "Content-Type: application/json" \
  -d '{"answered":true}'

# Delete
curl -sS -X DELETE "$API/rest/mobile/patient/appointment-questions/42" \
  -H "Authorization: Bearer $PATIENT_JWT"
```
