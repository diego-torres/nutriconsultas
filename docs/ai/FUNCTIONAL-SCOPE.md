# AI Nutrition Assistant — Functional Scope (v1)

**Issue:** [#361](https://github.com/diego-torres/nutriconsultas/issues/361) · Epic [#360](https://github.com/diego-torres/nutriconsultas/issues/360)  
**Status:** Phase 0 design — implementation follows Milestones 1–3  
**Language:** All assistant **user-facing** output is **Spanish (es-MX)** ([track principle](../../ISSUE-AI-ASSISTANT.md#track-principles))

This document defines what the AI assistant **may** do in v1, what it **must not** do, and how nutritionist confirmation fits the workflow.

---

## Purpose

Help **licensed nutritionists** (plan **Plus** or **Consultorio**, #409) draft:

| Workflow | Draft type | Maps to application concept |
|----------|------------|----------------------------|
| Create dish / recipe | `DISH` | Catalog [`Platillo`](../../src/main/java/com/nutriconsultas/platillos/Platillo.java) + [`Ingrediente`](../../src/main/java/com/nutriconsultas/platillos/Ingrediente.java) |
| Create daily menu | `MENU_DAILY` | One day of meals/snacks (ingestas) with platillos or foods |
| Create weekly menu | `MENU_WEEKLY` | Seven `MENU_DAILY` structures |
| Create dietary plan draft | `DIET_PLAN` | [`Dieta`](../../src/main/java/com/nutriconsultas/dieta/Dieta.java) structure: ingestas → platillos/alimentos |
| Calculate nutrient totals | (embedded in drafts) | Uses catalog nutrient data via tools — never model guesses |
| Validate plan constraints | (validation result) | Calorie/macro/allergy checks via `validate_plan_constraints` |

The assistant is a **drafting aid**. It does **not** replace professional judgment or directly modify production patient records.

---

## Supported workflows (v1)

### 1. Create dish / recipe draft

Nutritionist describes a platillo; assistant searches **authorized** foods (`search_food_catalog`), proposes ingredients with quantities and units, calculates nutrients (`calculate_recipe_nutrients`), and stores a **`DISH` draft** (`create_dish_draft`).

**Includes:** name, description, ingredients (food IDs from catalog), preparation steps, servings, per-serving and total nutrients, assumptions, warnings.

**Does not include:** saving to catalog, uploading images/videos, or publishing to patients.

### 2. Create daily menu draft

One calendar day with a configured number of **ingestas** (e.g. desayuno, comida, cena, colaciones). Each slot may reference catalog platillos, new dish drafts, or direct food portions.

**Includes:** meal labels (Spanish), platillo/food references, portions, day-level nutrient summary, constraint validation when targets provided.

### 3. Create weekly menu draft

Seven daily menus with optional repetition (e.g. “lunes a viernes iguales, fin de semana distinto”). Same structure as daily, aggregated weekly totals/averages.

### 4. Create dietary plan draft

Multi-day **plan de alimentación** aligned with [`Dieta`](../../src/main/java/com/nutriconsultas/dieta/Dieta.java) + [`Ingesta`](../../src/main/java/com/nutriconsultas/dieta/Ingesta.java) model. May use **optional patient context** when the nutritionist explicitly links a patient (see #362).

**Includes:** plan name suggestion, ingestas with platillos/alimentos, macro distribution notes, assumptions, warnings.

**Does not include:** assigning [`PacienteDieta`](../../src/main/java/com/nutriconsultas/dieta/PacienteDieta.java) or activating a plan for a patient.

### 5. Calculate nutrient totals

On demand within any workflow: totals and per-serving/per-meal/per-day breakdown from **application catalog data only**.

### 6. Validate plan constraints

When the nutritionist supplies targets (kcal, macros, sodium, allergies, excluded foods, meal count), assistant runs validation and returns **pass / warn / fail** with specific Spanish messages — no clinical diagnosis language.

---

## Supported user prompts (examples)

Nutritionists write prompts in **Spanish**. The assistant responds in **Spanish**.

### Dish / recipe

| Prompt (example) | Expected behavior |
|------------------|-------------------|
| «Genera un desayuno alto en proteína usando alimentos de mi catálogo.» | Search foods, propose recipe draft, show nutrients |
| «Arma una receta con pechuga de pollo, arroz y verduras; calcula nutrientes por porción.» | Map to catalog IDs, calculate, draft |
| «Necesito un platillo bajo en sodio para la cena, 2 porciones.» | Search + validate sodium constraint if data available |

### Daily menu

| Prompt (example) | Expected behavior |
|------------------|-------------------|
| «Menú de un día de 1800 kcal con 3 comidas y 2 colaciones.» | Ask missing details if needed, daily menu draft |
| «Menú bajo en sodio: desayuno, comida, cena y dos colaciones.» | Daily draft + validation warnings |
| «Plan del día vegetariano usando mis platillos existentes.» | `search_dish_catalog` + compose menu |

### Weekly menu

| Prompt (example) | Expected behavior |
|------------------|-------------------|
| «Menú semanal de 7 días para pérdida de peso, 1600 kcal.» | Clarify allergies/preferences, weekly draft |
| «Semana alta en proteína, lunes a viernes iguales.» | Weekly structure with repetition note in assumptions |

### Diet plan

| Prompt (example) | Expected behavior |
|------------------|-------------------|
| «Borrador de plan de 7 días, 1800 kcal, orientado a pérdida de peso.» | Diet plan draft, no patient assignment |
| «Plan para paciente con restricción sin huevo — usa el contexto del paciente vinculado.» | Requires linked patient + authorized context (#362) |
| «Adapta la dieta plantilla #123 a 2200 kcal.» | `get_diet_templates` + adjusted draft |

### Nutrients & validation only

| Prompt (example) | Expected behavior |
|------------------|-------------------|
| «¿Cuántas kcal tiene 150 g de pechuga de pollo del catálogo?» | `get_food_nutrients` |
| «Valida si este menú cumple 140 g de proteína y menos de 2000 mg de sodio.» | `validate_plan_constraints` on supplied draft |

---

## Unsupported prompts (v1)

The assistant **must decline or redirect** (politely, in Spanish) for:

| Category | Examples | Reason |
|----------|----------|--------|
| **Direct patient assignment** | «Asigna esta dieta al paciente María.» | Requires nutritionist confirmation via accept flow — no auto-assign |
| **Clinical diagnosis / treatment** | «Diagnostica diabetes y prescribe insulina.» | Out of scope — not a clinical decision system |
| **Medical emergencies** | «El paciente tiene hipoglucemia, ¿qué hace?» | Redirect to professional / emergency care |
| **Invented nutrient data** | «Estima las calorías del aguacate sin buscar en catálogo.» | Must use catalog tools |
| **Cross-tenant data** | «Usa los platillos del nutriólogo X.» | Multi-tenant violation |
| **Bulk destructive actions** | «Elimina todos mis platillos.» | No destructive tools exposed to AI |
| **Non-nutrition tasks** | «Redacta un correo de marketing.» | Out of product scope |
| **Bypass plan tier** | (Profesional user) «Genera un menú con IA.» | Blocked by #409 — upgrade message |
| **Final save without review** | «Guarda ya este platillo en mi catálogo.» | Draft only until nutritionist accepts |
| **Patient mobile / public API** | «Envía el plan al app del paciente.» | v1 is nutritionist web only |
| **Unsupported languages** | Prompts insisting on English-only replies | Track policy: user-facing comms in es-MX |

When declining, the assistant explains **what it can do instead** (e.g. «Puedo generar un borrador para que lo revises y guardes manualmente.»).

---

## Automatic vs confirmation-required actions

### May run automatically (no extra confirmation)

These happen inside an active chat session after the nutritionist sends a message:

| Action | Tool / behavior |
|--------|-----------------|
| Search authorized food catalog | `search_food_catalog` |
| Search authorized dish catalog | `search_dish_catalog` |
| Read food nutrients | `get_food_nutrients` |
| Read dish recipe / template diet | `get_dish_recipe`, `get_diet_templates` |
| Calculate recipe or menu nutrients | `calculate_recipe_nutrients` |
| Validate constraints on a draft | `validate_plan_constraints` |
| Ask clarifying questions | Model reply only |
| Create **draft** records | `create_dish_draft`, `create_menu_draft`, `create_diet_plan_draft` |
| Persist chat messages | Backend persistence (#369–#371) |

Drafts are always stored with status **`DRAFT`** and label **«Borrador IA — revisión del nutriólogo requerida»**.

### Requires explicit nutritionist confirmation

| Action | How |
|--------|-----|
| Save dish to **catalog** (`Platillo`) | Accept dish draft → nutritionist reviews/edits → confirm (#382) |
| Save menu / diet to **catalog** (`Dieta`) | Accept menu/diet draft → confirm (#382) |
| **Assign plan to patient** (`PacienteDieta`) | **Never** from AI tools; nutritionist uses existing assignment UI after review |
| Delete or overwrite existing catalog rows | Not available via AI |
| Change another user’s data | Blocked by auth/scoping |

**UI pattern:** SweetAlert confirmation (Spanish) before accept — consistent with admin UI ([`AGENTS.md`](../../AGENTS.md) Thymeleaf section).

---

## End-to-end examples

### Example A — Dish draft

1. **Nutriólogo:** «Quiero un platillo de huevos con espinaca para 1 porción, alto en proteína.»
2. **Asistente:** Busca huevo y espinaca en catálogo; si falta alguno, pregunta sustituto o pide agregar al catálogo manualmente.
3. **Asistente:** Propone ingredientes con gramos, pasos de preparación, kcal y macros **desde catálogo**.
4. **Asistente:** Crea borrador `DISH`, muestra advertencias si algún nutriente falta en BD.
5. **Nutriólogo:** Revisa, edita si hace falta, **Aceptar** o **Descartar**.
6. **Sistema (solo si acepta):** Crea `Platillo` en catálogo del nutriólogo.

### Example B — Daily menu draft

1. **Nutriólogo:** «Menú de hoy: 1800 kcal, 3 comidas y 2 colaciones, bajo en sodio.»
2. **Asistente:** Confirma si hay alergias o alimentos excluidos.
3. **Asistente:** Compone ingestas (Desayuno, Colación AM, Comida, Colación PM, Cena) con platillos/alimentos del catálogo.
4. **Asistente:** `validate_plan_constraints` → muestra «Cumple objetivo calórico» o advertencias.
5. **Nutriólogo:** Acepta borrador → puede convertirse en `Dieta` plantilla o copia editable (#382).

### Example C — Weekly diet plan draft

1. **Nutriólogo:** «Plan de 7 días, 1600 kcal, para bajar de peso; paciente vinculado sin mariscos.»
2. **Asistente:** Lee restricciones del contexto autorizado del paciente (#362); pregunta número de ingestas si no está claro.
3. **Asistente:** Genera borrador `DIET_PLAN` con 7 días, totales diarios y semanal promedio.
4. **Asistente:** Advertencia visible: «Borrador IA — no sustituye evaluación clínica.»
5. **Nutriólogo:** Acepta → crea `Dieta` borrador/plantilla; **asignación al paciente** es paso separado en UI existente.

---

## Clarifying questions (when to ask)

The assistant **should ask** before generating a large draft when any of these are missing and materially affect the result:

- Objetivo calórico (kcal/día o por comida)
- Número de ingestas y colaciones
- Alergias, intolerancias, alimentos excluidos
- Estilo dietético (vegetariano, bajo sodio, alto proteína, etc.)
- Porciones o personas a servir
- Duración (1 día vs 7 días vs otro)
- Si debe priorizar platillos existentes vs recetas nuevas
- Paciente vinculado (solo si el nutriólogo menciona contexto clínico)

The assistant **should not** ask for patient name, email, or teléfono unless product explicitly requires it later (#362 will forbid most PHI).

---

## Patient-linked chat context

When the nutritionist opens AI chat **from a patient record** (e.g. perfil, asignar dieta, plan alimentario del paciente), the backend should attach **`patient_id`** to `ai_chat_thread` (#369) and load **authorized** patient data to pre-fill constraints — same tenant rules as `findByIdAndUserId` + `verifyPatientOwnership`.

### Where pathologies live today

Pathology and clinical history are on **`PacienteMedicalHistory`** (satellite table), exposed on [`Paciente`](../../src/main/java/com/nutriconsultas/paciente/Paciente.java) via `@Delegate`:

| Field | Type | UI / use |
|-------|------|----------|
| `alergias` | text | `sbadmin/pacientes/desarrollo.html` |
| `hipertension`, `diabetes`, `hipotiroidismo`, `obesidad`, `anemia`, `bulimia`, `anorexia`, `enfermedadesHepaticas` | boolean | Pathology checkboxes on desarrollo |
| `antecedentesPatologicosPersonales` / `Familiares` | text | `antecedentes.html` |
| `historialAlimenticio`, `complicaciones`, `tipoSanguineo`, … | text | Registration / antecedentes forms |

[`PhysiologicalStressCatalog.suggestFromPathologies`](../../src/main/java/com/nutriconsultas/paciente/calculation/PhysiologicalStressCatalog.java) already maps flags (diabetes, obesidad, embarazo, etc.) → suggested stress types for energy calculations.

### Where caloric requirements live today

Cached on **`PacienteBodySnapshot`** (embedded on `Paciente`):

| Field | Meaning |
|-------|---------|
| `bmr` | TMB (kcal/día) |
| `getKcal` | GET / TDEE (kcal/día) |
| `tefKcal` | Termogénesis inducida por dietas |
| `totalAdjustedKcal` | GET + TEF |
| `stressKcal` | Incremento por estrés fisiológico |
| `finalTotalKcal` | GET + TEF + estrés |
| `nivelPeso`, `imc`, `peso`, `estatura` | Anthropometrics |

**Energy preferences** (activity factor, TEF method, stress type/active) live on [`PacienteEnergyPreferences`](../../src/main/java/com/nutriconsultas/paciente/satellite/PacienteEnergyPreferences.java).

### Existing “requerimiento calórico” for diet design

The app **already** resolves a single target for diet matching:

```142:147:src/main/java/com/nutriconsultas/paciente/PacienteController.java
	private Double resolveRequerimientoKcal(final Paciente paciente) {
		if (paciente.getTotalAdjustedKcal() != null) {
			return paciente.getTotalAdjustedKcal();
		}
		return paciente.getGetKcal();
	}
```

Used when:

- **Asignar dieta** — [`/admin/pacientes/{id}/dietas/asignar`](../../src/main/resources/templates/sbadmin/pacientes/asignar-dieta.html) shows requerimiento and sorts picker diets by [`DietaCaloricFit`](../../src/main/java/com/nutriconsultas/dieta/DietaCaloricFit.java)
- **Patient diet editor** — [`DietaController`](../../src/main/java/com/nutriconsultas/dieta/DietaController.java) when `Dieta.pacienteId` is set
- **Diet picker REST** — `GET /rest/dietas/picker?requerimientoKcal=…` via [`DietasRestController`](../../src/main/java/com/nutriconsultas/dieta/DietasRestController.java)

**Note:** `resolveRequerimientoKcal` uses **GET+TEF** (`totalAdjustedKcal`), not `finalTotalKcal` (which adds stress). When stress is active, AI context should prefer `finalTotalKcal` — see [`DATA-ACCESS-RULES.md`](DATA-ACCESS-RULES.md).

### Suggested AI patient context (for prompt / tools)

When `patient_id` is set on the thread, backend builds **`AiPatientContext`** per [`DATA-ACCESS-RULES.md`](DATA-ACCESS-RULES.md) (#362) — not the full `Paciente` entity.

| Include | Source | Example use |
|---------|--------|-------------|
| `requerimientoKcal` | Same as `resolveRequerimientoKcal` | Default calorie target for menus/plans |
| `finalTotalKcal` | `Paciente.finalTotalKcal` | When `physiologicalStressActive` |
| Pathology booleans | `PacienteMedicalHistory` | Constraint validation, stress awareness |
| `alergias` | text (max 500 chars) | Excluded foods |
| `pregnancy`, `nivelPeso`, `imc`, `gender` | demographics / snapshot | Clarifying questions |
| Activity / stress enums | `PacienteEnergyPreferences` | Explain GET calculation assumptions |

**Exclude** name, email, phone, DOB, `assignedId`, `patientAuthSub`, and free-text antecedents — see deny list in DATA-ACCESS-RULES.

### Entry points (v1 UI)

Launch AI chat with `patientId` query param or hidden field from:

- Patient profile (`/admin/pacientes/{id}`)
- Assign diet flow (`/admin/pacientes/{id}/dietas/asignar`) — reuses existing `requerimientoKcal` banner copy
- Patient-specific diet form (`/admin/dietas/{id}` when `patientDiet`)

Orchestration (#385) merges this context into the system prompt (#367) so the assistant **does not re-ask** kcal target or known allergies when already present.

---

## v1 boundaries (deferred)

| Feature | Target |
|---------|--------|
| Patient mobile chat | Out of scope — [`ISSUE.md`](../../ISSUE.md) |
| Auto-rotation / meal-plan scheduling | Future issue |
| Image generation for platillos | Future issue |
| Import from photo or PDF | Future issue |
| Multi-language assistant | Future — v1 es-MX only |
| Clinic director AI on member patients | Future — v1 owner nutritionist only ([#362](DATA-ACCESS-RULES.md)) |

---

## Related documents

| Doc | Issue |
|-----|-------|
| [`AI-ASSISTANT-PLAN.md`](AI-ASSISTANT-PLAN.md) | Architecture |
| [`DATA-ACCESS-RULES.md`](DATA-ACCESS-RULES.md) | PHI, scoping, logging (#362) |
| Tool JSON schemas (planned) | #363 |
| System prompt requirements | #367 |
| Plan gating | #409 |

---

## Acceptance checklist (#361)

- [x] Supported user prompts documented
- [x] Unsupported prompts documented
- [x] Automatic actions defined
- [x] Nutritionist confirmation actions defined
- [x] Examples for dish, menu, and diet-plan generation
