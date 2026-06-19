# MPX file format (v1)

**Product:** Minutriporcion / nutriconsultas  
**Track:** `[Nutritionist Web]` — export/import patient registration  
**Issues:** [#221](https://github.com/diego-torres/nutriconsultas/issues/221) export · [#222](https://github.com/diego-torres/nutriconsultas/issues/222) import · [#223](https://github.com/diego-torres/nutriconsultas/issues/223) UI

---

## Overview

An **`.mpx`** (Minutriporción patient export) file is a UTF-8 YAML document that holds a nutritionist-owned **registration profile only**. It lets nutritionists rotate patient slots on capped plans without retyping demographics, body snapshot, energy preferences, and medical history.

Clinical timeline data (consultations, exams, measurements, diet assignments, messages, mobile linkage) is **never** included.

---

## File rules

| Topic | Rule |
|-------|------|
| Extension | `.mpx` |
| Encoding | UTF-8 |
| Format | YAML, block style |
| Version | Required top-level `mpxVersion: 1` |
| Download | Authenticated HTTPS session (`GET /admin/pacientes/{id}/export.mpx`) |
| Filename | `{assignedId-or-slug}-{yyyyMMdd-HHmmss}.mpx` (UTC), slug derived from `assignedId` or patient name |
| PHI | File contains PHI — do not log contents |

---

## Top-level envelope

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `mpxVersion` | integer | yes | Must be `1` for this spec |
| `exportedAt` | string (ISO-8601 instant) | yes | UTC export timestamp, e.g. `2026-06-18T12:00:00Z` |
| `sourceApp` | string | yes | Always `nutriconsultas` |
| `patient` | object | yes | Registration payload (see below) |

---

## `patient` object

Maps to the same data shown on patient alta/edición: core demographics plus embedded satellites.

### Core demographics

| Field | Type | Notes |
|-------|------|-------|
| `name` | string | Required in app; export preserves value |
| `dob` | string (`yyyy-MM-dd`) | Date of birth |
| `email` | string | Optional |
| `phone` | string | Optional |
| `gender` | string | `M` or `F` |
| `responsibleName` | string | Optional |
| `parentesco` | string | Optional |
| `pregnancy` | boolean | Optional; default `false` |

### Excluded from export (by design)

| Field | Reason |
|-------|--------|
| `id` | DB primary key — import creates a new row |
| `userId` | Nutritionist tenant — set from authenticated user on import |
| `patientAuthSub` | Mobile Auth0 linkage — not portable |
| `registro` | Server registration timestamp |
| `status` | Onboarding lifecycle — import sets `ACTIVE` (#222) |
| `assignedId` | Clinic identifier — reassigned on import |
| `displayName`, `emailHint` | Mobile/onboarding hints |

### `bodySnapshot` object

Cached metrics from `PacienteBodySnapshot`:

| Field | Type |
|-------|------|
| `peso` | number |
| `estatura` | number |
| `imc` | number |
| `bmr` | number |
| `getKcal` | number |
| `nivelPeso` | enum string (`NivelPeso` name) |
| `tefKcal` | number |
| `totalAdjustedKcal` | number |
| `stressKcal` | number |
| `finalTotalKcal` | number |

### `energyPreferences` object

All fields from `PacienteEnergyPreferences` except `id` and `paciente` FK. Enum fields serialize as enum **names** (e.g. `HARRIS_BENEDICT`, `PROMEDIO`, `FIXED`). Date fields `stressValidFrom` / `stressValidUntil` use `yyyy-MM-dd`.

### `medicalHistory` object

All fields from `PacienteMedicalHistory` except `id` and `paciente` FK — text antecedents, `tipoSanguineo`, pathology booleans, `historialAlimenticio`, `desarrolloPsicomotor`, `alergias`.

---

## Example (illustrative)

```yaml
mpxVersion: 1
exportedAt: "2026-06-18T12:00:00Z"
sourceApp: nutriconsultas
patient:
  name: Juan Perez
  dob: "1990-01-15"
  email: juan@example.com
  phone: "5551234"
  gender: M
  responsibleName: Maria Perez
  parentesco: Madre
  pregnancy: false
  bodySnapshot:
    peso: 72.5
    estatura: 1.75
    imc: 23.7
    bmr: 1650.0
    getKcal: 2275.0
    nivelPeso: NORMAL
    tefKcal: 182.0
    totalAdjustedKcal: 2457.0
    stressKcal: 0.0
    finalTotalKcal: 2457.0
  energyPreferences:
    activityFactorScale: HARRIS_BENEDICT
    preferredBmrFormula: PROMEDIO
    physicalActivityLevel: MODERATE
    activityFactor: 1.55
    tefMethod: FIXED
  medicalHistory:
    tipoSanguineo: O+
    antecedentesPatologicosPersonales: Ninguno
    hipertension: false
    diabetes: false
```

---

## Versioning

- Importers must reject unknown `mpxVersion` values.
- Future versions may add optional fields; v1 importers should ignore unknown keys where safe.

---

## Import rules (#222)

| Topic | Rule |
|-------|------|
| Endpoint | `POST /admin/pacientes/importar.mpx` (multipart field `mpxFile`) |
| Extension | Must end with `.mpx` |
| Version | Reject `mpxVersion` other than `1` with Spanish error |
| Tenant | Always set `userId` from authenticated nutritionist — never from file |
| New row | Always creates a **new** `Paciente`; never merges into an existing record |
| Status | Set `PacienteStatus.ACTIVE` |
| `registro` | Set to server time at import (not restored from file) |
| Excluded on import | `id`, `userId`, `patientAuthSub`, `assignedId`, `displayName`, `emailHint`, all history entities |
| Plan cap | `SubscriptionEntitlementService.assertCanCreatePatient(userId)` before save (#190) |
| Validation | Same Jakarta constraints as manual alta (`@NotBlank` name/gender, `@NotNull` dob, `@ValidPregnancy`) |
| Duplicate hint | Soft warning (SweetAlert) when same `name` + `dob` already exists for the nutritionist; import still proceeds |
| Success UX | Redirect to `/admin/pacientes/{id}` with success SweetAlert; duplicate warning shown after success |
| Errors | Spanish messages; invalid file/version/validation shown via SweetAlert on listado |

---

## Related

- [`PATIENT-MPX-PLAN.md`](PATIENT-MPX-PLAN.md) — product goals and implementation order
- [`ISSUE-NUTRITIONIST-WEB.md`](../../ISSUE-NUTRITIONIST-WEB.md) — issue registry
