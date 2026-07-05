# AI Nutrition Assistant — Tool Contract (v1)

**Issue:** [#363](https://github.com/diego-torres/nutriconsultas/issues/363) · Epic [#360](https://github.com/diego-torres/nutriconsultas/issues/360)  
**Related:** [`DATA-ACCESS-RULES.md`](DATA-ACCESS-RULES.md) (#362) · [`FUNCTIONAL-SCOPE.md`](FUNCTIONAL-SCOPE.md) (#361)

Backend tools the OpenAI orchestrator (#385) may invoke. Each tool is a Spring service method with **fixed JSON input/output** — the model never writes SQL or calls repositories directly.

---

## Design principles

| Principle | Rule |
|-----------|------|
| **Read vs draft** | Seven **read-only** tools query catalog/nutrition data; three **draft** tools persist `ai_generated_draft` rows only. |
| **No patient assignment** | No tool creates `PacienteDieta`, mutates live patient diets, or saves final `Platillo`/`Dieta` catalog rows. Accept flow is #382. |
| **Tenant scoping** | Every handler receives `nutritionistId` from the OAuth session — never from model arguments. |
| **Catalog authorization** | Same rules as web: system + owned platillos/dietas; global alimentos; patient-assigned dietas only when patient is owned (#362). |
| **Spanish descriptions** | OpenAI `description` fields use **es-MX** so the model understands tool purpose in Spanish. JSON property names stay English. |
| **Bounded results** | Search tools cap at **25** rows; oversized inputs rejected with `VALIDATION`. |

---

## Tool registry

| Tool | Class | Mutates DB | Implementation |
|------|-------|------------|----------------|
| `search_food_catalog` | read | no | #373 |
| `get_food_nutrients` | read | no | #374 |
| `search_dish_catalog` | read | no | #375 |
| `get_dish_recipe` | read | no | #375 |
| `calculate_recipe_nutrients` | read | no | #376 |
| `get_diet_templates` | read | no | #372 (Phase 3) |
| `validate_plan_constraints` | read | no | #377 |
| `get_patient_appointments` | read | no | patient calendar (scoped) |
| `create_dish_draft` | draft | yes (`ai_generated_draft`) | #379 |
| `create_menu_draft` | draft | yes (`ai_generated_draft`) | #380 |
| `create_diet_plan_draft` | draft | yes (`ai_generated_draft`) | #381 |

**Forbidden in v1:** delete/update catalog rows, assign diets to patients, read clinical timeline, cross-tenant IDs.

---

## Orchestration context (server-injected)

These fields are **not** part of OpenAI tool schemas. The orchestrator attaches them on every dispatch:

| Field | Source | Required |
|-------|--------|----------|
| `nutritionistId` | `OidcUser.getSubject()` | always |
| `threadId` | active `ai_chat_thread.id` | always |
| `patientContext` | `AiPatientContext` when thread has owned `patient_id` | optional |

Pre-checks before any tool runs (#362, #409):

1. Authenticated nutritionist session  
2. `AI_ENABLED=true`  
3. `Entitlement.AI_ASSISTANT` (Plus + Consultorio)  
4. Thread belongs to `nutritionistId`  
5. Rate limit not exceeded (#386)

---

## Common response envelope

### Success

```json
{
  "success": true,
  "data": {}
}
```

### Error

```json
{
  "success": false,
  "errorCode": "NOT_FOUND",
  "message": "No se encontró el alimento solicitado."
}
```

| `errorCode` | HTTP (REST) | When |
|-------------|-------------|------|
| `NOT_FOUND` | 404 | ID not found or not authorized (prefer 404 over 403 for IDOR) |
| `FORBIDDEN` | 403 | Missing entitlement or feature disabled |
| `VALIDATION` | 400 | Invalid arguments, empty query, limit exceeded |
| `RATE_LIMIT` | 429 | Per-nutritionist AI quota (#386) |
| `INTERNAL` | 500 | Unexpected failure — generic Spanish message to user |

---

## Shared types

### `NutrientSummary`

Macros and common micros returned by nutrient tools (aligned with `AbstractNutrible` / `AbstractMacroNutrible`):

```json
{
  "energiaKcal": 0,
  "proteinaG": 0.0,
  "lipidosG": 0.0,
  "hidratosDeCarbonoG": 0.0,
  "fibraG": 0.0,
  "sodioMg": 0.0,
  "potasioMg": 0.0
}
```

Null DB fields omit from output or serialize as `null` — implementation must not invent values.

### `RecipeIngredientInput`

Used by `calculate_recipe_nutrients` and embedded in draft payloads:

```json
{
  "alimentoId": 123,
  "cantidad": "1/2",
  "pesoNetoG": 75,
  "unidad": "taza"
}
```

| Property | Type | Required | Notes |
|----------|------|----------|-------|
| `alimentoId` | integer | yes | Must exist in `Alimento` catalog |
| `cantidad` | string | yes | Fractional quantity (e.g. `1`, `1/2`, `2/3`) |
| `pesoNetoG` | integer | no | Gram weight; used when unit is weight-based |
| `unidad` | string | no | Defaults from catalog `Alimento.unidad` |

Calculation reuses `IngredienteFromAlimentoCalculator` logic (#376).

### `IngestaSlot`

Meal slot in menu/diet drafts:

```json
{
  "nombre": "Desayuno",
  "orden": 1,
  "items": [
    {
      "type": "PLATILLO",
      "platilloId": 42,
      "portions": 1
    },
    {
      "type": "ALIMENTO",
      "alimentoId": 100,
      "portions": 1
    }
  ]
}
```

| `items[].type` | Meaning |
|----------------|---------|
| `PLATILLO` | Reference authorized catalog platillo |
| `ALIMENTO` | Standalone alimento in ingesta |
| `RECIPE` | Inline recipe (same shape as dish draft `ingredients`) — for not-yet-saved combinations |

---

## Read-only tools

### `search_food_catalog`

**Description (OpenAI):** Busca alimentos en el catálogo autorizado por nombre o clasificación. Devuelve IDs para consultar nutrientes. No inventes alimentos.

**Auth / scoping:** Global `Alimento` catalog (read-only). No tenant filter in v1.

**Input schema:**

```json
{
  "type": "object",
  "properties": {
    "query": {
      "type": "string",
      "minLength": 2,
      "maxLength": 120,
      "description": "Texto de búsqueda (nombre o clasificación)"
    },
    "clasificacion": {
      "type": "string",
      "maxLength": 80,
      "description": "Filtro opcional por clasificación"
    },
    "limit": {
      "type": "integer",
      "minimum": 1,
      "maximum": 25,
      "default": 10
    }
  },
  "required": ["query"],
  "additionalProperties": false
}
```

**Output schema (`data`):**

```json
{
  "type": "object",
  "properties": {
    "items": {
      "type": "array",
      "items": {
        "type": "object",
        "properties": {
          "alimentoId": { "type": "integer" },
          "nombreAlimento": { "type": "string" },
          "clasificacion": { "type": "string" },
          "unidad": { "type": "string" },
          "cantSugerida": { "type": "number" },
          "energiaKcalPorPorcion": { "type": "integer" }
        },
        "required": ["alimentoId", "nombreAlimento"]
      }
    },
    "totalReturned": { "type": "integer" },
    "truncated": { "type": "boolean" }
  },
  "required": ["items", "totalReturned", "truncated"]
}
```

---

### `get_food_nutrients`

**Description (OpenAI):** Obtiene nutrientes calculados de un alimento del catálogo para una cantidad y unidad específicas. Usa siempre este tool en lugar de estimar.

**Auth / scoping:** `alimentoId` must exist. No PHI.

**Input schema:**

```json
{
  "type": "object",
  "properties": {
    "alimentoId": { "type": "integer" },
    "cantidad": {
      "type": "string",
      "description": "Cantidad fraccionaria, ej. 1, 1/2, 2"
    },
    "pesoNetoG": { "type": "integer", "minimum": 1 },
    "portions": {
      "type": "integer",
      "minimum": 1,
      "default": 1,
      "description": "Multiplicador de porciones sobre el cálculo base"
    }
  },
  "required": ["alimentoId", "cantidad"],
  "additionalProperties": false
}
```

**Output schema (`data`):**

```json
{
  "type": "object",
  "properties": {
    "alimentoId": { "type": "integer" },
    "nombreAlimento": { "type": "string" },
    "cantidad": { "type": "string" },
    "pesoNetoG": { "type": "integer" },
    "nutrientsPerCalculation": { "$ref": "#/definitions/NutrientSummary" },
    "nutrientsTotal": { "$ref": "#/definitions/NutrientSummary" }
  },
  "required": ["alimentoId", "nombreAlimento", "nutrientsTotal"]
}
```

---

### `search_dish_catalog`

**Description (OpenAI):** Busca platillos del catálogo del sistema y del nutriólogo autenticado. Devuelve IDs para leer recetas.

**Auth / scoping:** System platillos (`PlatilloCatalogConstants`) **or** `platillo.userId == nutritionistId`. Exclude other tenants' owned rows.

**Input schema:**

```json
{
  "type": "object",
  "properties": {
    "query": {
      "type": "string",
      "minLength": 2,
      "maxLength": 120
    },
    "ingestasSugeridas": {
      "type": "string",
      "maxLength": 80,
      "description": "Filtro opcional, ej. Desayuno, Comida"
    },
    "limit": {
      "type": "integer",
      "minimum": 1,
      "maximum": 25,
      "default": 10
    }
  },
  "required": ["query"],
  "additionalProperties": false
}
```

**Output schema (`data`):**

```json
{
  "type": "object",
  "properties": {
    "items": {
      "type": "array",
      "items": {
        "type": "object",
        "properties": {
          "platilloId": { "type": "integer" },
          "name": { "type": "string" },
          "ingestasSugeridas": { "type": "string" },
          "energiaKcal": { "type": "integer" },
          "proteinaG": { "type": "number" },
          "ownedByNutritionist": { "type": "boolean" },
          "systemCatalog": { "type": "boolean" }
        },
        "required": ["platilloId", "name"]
      }
    },
    "totalReturned": { "type": "integer" }
  },
  "required": ["items", "totalReturned"]
}
```

---

### `get_dish_recipe`

**Description (OpenAI):** Lee la receta completa de un platillo autorizado: ingredientes, cantidades, nutrientes por porción y pasos si existen.

**Auth / scoping:** Same as `search_dish_catalog` for `platilloId`. Return `NOT_FOUND` if unauthorized.

**Input schema:**

```json
{
  "type": "object",
  "properties": {
    "platilloId": { "type": "integer" },
    "portions": {
      "type": "integer",
      "minimum": 1,
      "default": 1
    }
  },
  "required": ["platilloId"],
  "additionalProperties": false
}
```

**Output schema (`data`):**

```json
{
  "type": "object",
  "properties": {
    "platilloId": { "type": "integer" },
    "name": { "type": "string" },
    "description": { "type": "string" },
    "portions": { "type": "integer" },
    "ingredients": {
      "type": "array",
      "items": {
        "type": "object",
        "properties": {
          "alimentoId": { "type": "integer" },
          "nombreAlimento": { "type": "string" },
          "cantidad": { "type": "string" },
          "pesoNetoG": { "type": "integer" },
          "unidad": { "type": "string" },
          "nutrients": { "$ref": "#/definitions/NutrientSummary" }
        },
        "required": ["alimentoId", "cantidad"]
      }
    },
    "nutrientsPerPortion": { "$ref": "#/definitions/NutrientSummary" },
    "nutrientsTotal": { "$ref": "#/definitions/NutrientSummary" }
  },
  "required": ["platilloId", "name", "ingredients", "nutrientsPerPortion"]
}
```

---

### `calculate_recipe_nutrients`

**Description (OpenAI):** Calcula nutrientes totales y por porción de una lista de ingredientes del catálogo. Usa antes de proponer un platillo o validar un menú.

**Auth / scoping:** Every `alimentoId` validated against catalog. No writes.

**Input schema:**

```json
{
  "type": "object",
  "properties": {
    "ingredients": {
      "type": "array",
      "minItems": 1,
      "maxItems": 40,
      "items": { "$ref": "#/definitions/RecipeIngredientInput" }
    },
    "portions": {
      "type": "integer",
      "minimum": 1,
      "default": 1
    },
    "label": {
      "type": "string",
      "maxLength": 120,
      "description": "Nombre provisional del platillo (solo para contexto)"
    }
  },
  "required": ["ingredients"],
  "additionalProperties": false
}
```

**Output schema (`data`):**

```json
{
  "type": "object",
  "properties": {
    "portions": { "type": "integer" },
    "ingredientResults": {
      "type": "array",
      "items": {
        "type": "object",
        "properties": {
          "alimentoId": { "type": "integer" },
          "nutrients": { "$ref": "#/definitions/NutrientSummary" },
          "warnings": {
            "type": "array",
            "items": { "type": "string" }
          }
        }
      }
    },
    "nutrientsPerPortion": { "$ref": "#/definitions/NutrientSummary" },
    "nutrientsTotal": { "$ref": "#/definitions/NutrientSummary" }
  },
  "required": ["nutrientsPerPortion", "nutrientsTotal"]
}
```

---

### `get_diet_templates`

**Description (OpenAI):** Lista o lee plantillas de dieta autorizadas (sistema y del nutriólogo). Excluye copias asignadas a pacientes salvo que el paciente vinculado al chat sea dueño.

**Auth / scoping:**

- Include: system templates (`DietaCatalogConstants.SYSTEM_TEMPLATE_USER_ID`), `dieta.userId == nutritionistId`, `pacienteId == null`  
- Include patient-assigned copy **only** when `patientContext.patientId` matches `dieta.pacienteId` and patient is owned  
- Use `DietaAuthorization.canView()` pattern

**Input schema:**

```json
{
  "type": "object",
  "properties": {
    "query": {
      "type": "string",
      "minLength": 0,
      "maxLength": 120,
      "description": "Búsqueda por nombre; vacío lista plantillas recientes"
    },
    "dietaId": {
      "type": "integer",
      "description": "Si se envía, devuelve detalle de una plantilla"
    },
    "includeIngestas": {
      "type": "boolean",
      "default": true
    },
    "limit": {
      "type": "integer",
      "minimum": 1,
      "maximum": 25,
      "default": 10
    }
  },
  "additionalProperties": false
}
```

**Output schema (`data`) — list mode:**

```json
{
  "type": "object",
  "properties": {
    "items": {
      "type": "array",
      "items": {
        "type": "object",
        "properties": {
          "dietaId": { "type": "integer" },
          "nombre": { "type": "string" },
          "energiaKcal": { "type": "integer" },
          "systemTemplate": { "type": "boolean" },
          "ingestaCount": { "type": "integer" }
        },
        "required": ["dietaId", "nombre"]
      }
    }
  },
  "required": ["items"]
}
```

**Output schema (`data`) — detail mode (`dietaId` set):** adds `ingestas[]` with `IngestaSlot`-compatible structure and per-ingesta `NutrientSummary`.

---

### `validate_plan_constraints`

**Description (OpenAI):** Valida un borrador de menú o plan contra objetivos calóricos, macros, alergias del paciente vinculado y restricciones declaradas. Devuelve cumplimiento y advertencias en español.

**Auth / scoping:** Read-only. Uses injected `patientContext` when present; never loads patient by ID from model args.

**Input schema:**

```json
{
  "type": "object",
  "properties": {
    "planType": {
      "type": "string",
      "enum": ["MENU", "DIET_PLAN", "DISH"]
    },
    "targetKcal": { "type": "number", "minimum": 500, "maximum": 10000 },
    "targetProteinaG": { "type": "number", "minimum": 0 },
    "targetLipidosG": { "type": "number", "minimum": 0 },
    "targetHidratosG": { "type": "number", "minimum": 0 },
    "maxSodioMg": { "type": "number", "minimum": 0 },
    "excludedAlimentoIds": {
      "type": "array",
      "items": { "type": "integer" }
    },
    "menu": {
      "type": "object",
      "description": "Menú de un día",
      "properties": {
        "ingestas": {
          "type": "array",
          "items": { "$ref": "#/definitions/IngestaSlot" }
        }
      }
    },
    "dietPlan": {
      "type": "object",
      "description": "Plan multi-día",
      "properties": {
        "days": {
          "type": "array",
          "maxItems": 14,
          "items": {
            "type": "object",
            "properties": {
              "dayIndex": { "type": "integer", "minimum": 1 },
              "label": { "type": "string" },
              "ingestas": {
                "type": "array",
                "items": { "$ref": "#/definitions/IngestaSlot" }
              }
            },
            "required": ["dayIndex", "ingestas"]
          }
        }
      }
    },
    "dish": {
      "type": "object",
      "properties": {
        "ingredients": {
          "type": "array",
          "items": { "$ref": "#/definitions/RecipeIngredientInput" }
        },
        "portions": { "type": "integer", "minimum": 1, "default": 1 }
      }
    },
    "toleranceKcal": {
      "type": "number",
      "minimum": 0,
      "default": 50,
      "description": "Margen ± kcal aceptable"
    }
  },
  "required": ["planType"],
  "additionalProperties": false
}
```

**Output schema (`data`):**

```json
{
  "type": "object",
  "properties": {
    "valid": { "type": "boolean" },
    "computedNutrients": { "$ref": "#/definitions/NutrientSummary" },
    "warnings": {
      "type": "array",
      "items": {
        "type": "object",
        "properties": {
          "code": {
            "type": "string",
            "enum": [
              "KCAL_OUT_OF_RANGE",
              "PROTEIN_LOW",
              "SODIUM_HIGH",
              "ALLERGY_RISK",
              "MISSING_NUTRIENT_DATA",
              "PATHOLOGY_NOTE"
            ]
          },
          "message": { "type": "string" },
          "severity": { "type": "string", "enum": ["INFO", "WARNING", "ERROR"] }
        },
        "required": ["code", "message", "severity"]
      }
    },
    "patientContextApplied": { "type": "boolean" }
  },
  "required": ["valid", "warnings"]
}
```

When `patientContext.alergias` is set, flag ingredients whose catalog names match excluded terms (implementation heuristic in #377).

---

## Draft-creation tools

All draft tools:

1. Persist row in `ai_generated_draft` with `status = DRAFT`, `draft_type` ∈ `DISH | MENU | DIET_PLAN`  
2. Set `thread_id`, `nutritionist_id` from session — **ignore** any model-supplied user IDs  
3. Return `draftId` for UI preview (#390)  
4. Include `"label": "Borrador IA — revisión del nutriólogo requerida"` in assistant-facing summary  
5. **Never** insert into `platillo`, `dieta`, `paciente_dieta`

### `create_dish_draft`

**Description (OpenAI):** Guarda un borrador de platillo/receta para revisión del nutriólogo. No guarda en el catálogo final.

**Auth / scoping:** Draft owned by session nutritionist. Ingredient `alimentoId`s must exist.

**Input schema:**

```json
{
  "type": "object",
  "properties": {
    "name": { "type": "string", "minLength": 2, "maxLength": 120 },
    "description": { "type": "string", "maxLength": 2000 },
    "preparationSteps": {
      "type": "array",
      "maxItems": 30,
      "items": { "type": "string", "maxLength": 500 }
    },
    "ingestasSugeridas": { "type": "string", "maxLength": 80 },
    "ingredients": {
      "type": "array",
      "minItems": 1,
      "maxItems": 40,
      "items": { "$ref": "#/definitions/RecipeIngredientInput" }
    },
    "portions": { "type": "integer", "minimum": 1, "default": 1 },
    "nutrientsPerPortion": { "$ref": "#/definitions/NutrientSummary" },
    "assumptions": {
      "type": "array",
      "items": { "type": "string", "maxLength": 300 },
      "description": "Supuestos en español mostrados al nutriólogo"
    },
    "warnings": {
      "type": "array",
      "items": { "type": "string", "maxLength": 300 }
    }
  },
  "required": ["name", "ingredients"],
  "additionalProperties": false
}
```

**Output schema (`data`):**

```json
{
  "type": "object",
  "properties": {
    "draftId": { "type": "integer" },
    "draftType": { "type": "string", "const": "DISH" },
    "status": { "type": "string", "const": "DRAFT" },
    "summary": { "type": "string" }
  },
  "required": ["draftId", "draftType", "status"]
}
```

---

### `create_menu_draft`

**Description (OpenAI):** Guarda un borrador de menú de un día (varias ingestas). No asigna al paciente.

**Auth / scoping:** Referenced `platilloId` / `alimentoId` must be authorized. Optional `patientContext` for display only in `assumptions`.

**Input schema:**

```json
{
  "type": "object",
  "properties": {
    "title": { "type": "string", "maxLength": 120 },
    "targetKcal": { "type": "number" },
    "ingestas": {
      "type": "array",
      "minItems": 1,
      "maxItems": 12,
      "items": { "$ref": "#/definitions/IngestaSlot" }
    },
    "nutrientsTotal": { "$ref": "#/definitions/NutrientSummary" },
    "validationSummary": { "type": "string", "maxLength": 1000 },
    "assumptions": {
      "type": "array",
      "items": { "type": "string", "maxLength": 300 }
    },
    "warnings": {
      "type": "array",
      "items": { "type": "string", "maxLength": 300 }
    }
  },
  "required": ["ingestas"],
  "additionalProperties": false
}
```

**Output schema (`data`):**

```json
{
  "type": "object",
  "properties": {
    "draftId": { "type": "integer" },
    "draftType": { "type": "string", "const": "MENU" },
    "status": { "type": "string", "const": "DRAFT" },
    "summary": { "type": "string" }
  },
  "required": ["draftId", "draftType", "status"]
}
```

---

### `create_diet_plan_draft`

**Description (OpenAI):** Guarda un borrador de plan alimenticio multi-día. No asigna al paciente.

**Auth / scoping:** Same as `create_menu_draft` for all referenced IDs.

**Input schema:**

```json
{
  "type": "object",
  "properties": {
    "title": { "type": "string", "maxLength": 120 },
    "dayCount": { "type": "integer", "minimum": 1, "maximum": 14 },
    "targetKcalPerDay": { "type": "number" },
    "days": {
      "type": "array",
      "minItems": 1,
      "maxItems": 14,
      "items": {
        "type": "object",
        "properties": {
          "dayIndex": { "type": "integer", "minimum": 1 },
          "label": { "type": "string", "maxLength": 80 },
          "ingestas": {
            "type": "array",
            "items": { "$ref": "#/definitions/IngestaSlot" }
          },
          "nutrientsTotal": { "$ref": "#/definitions/NutrientSummary" }
        },
        "required": ["dayIndex", "ingestas"]
      }
    },
    "weeklyAverageNutrients": { "$ref": "#/definitions/NutrientSummary" },
    "validationSummary": { "type": "string", "maxLength": 2000 },
    "assumptions": {
      "type": "array",
      "items": { "type": "string", "maxLength": 300 }
    },
    "warnings": {
      "type": "array",
      "items": { "type": "string", "maxLength": 300 }
    }
  },
  "required": ["days"],
  "additionalProperties": false
}
```

**Output schema (`data`):**

```json
{
  "type": "object",
  "properties": {
    "draftId": { "type": "integer" },
    "draftType": { "type": "string", "const": "DIET_PLAN" },
    "status": { "type": "string", "const": "DRAFT" },
    "summary": { "type": "string" }
  },
  "required": ["draftId", "draftType", "status"]
}
```

---

## OpenAI function-calling registration

Orchestrator (#385) registers tools with OpenAI **Responses / Chat Completions** `tools[]` using:

- `type`: `function`  
- `function.name`: snake_case names exactly as above  
- `function.description`: Spanish text from each tool section  
- `function.parameters`: input schema (JSON Schema subset)

Tool results are sent back as `role: tool` messages with JSON string body = full response envelope.

Max tool calls per user turn: `nutriconsultas.ai.max-tool-calls` (default **8**, #385).

---

## MCP name mapping (Phase 7)

Stable MCP identifiers (#393) map 1:1 to Spring handlers:

| Internal tool | MCP name |
|---------------|----------|
| `search_food_catalog` | `catalog.search_foods` |
| `get_food_nutrients` | `catalog.get_food_nutrients` |
| `search_dish_catalog` | `catalog.search_dishes` |
| `get_dish_recipe` | `catalog.get_dish_recipe` |
| `calculate_recipe_nutrients` | `nutrition.calculate_recipe` |
| `get_diet_templates` | `catalog.get_diet_templates` |
| `validate_plan_constraints` | `nutrition.validate_plan` |
| `create_dish_draft` | `draft.create_dish` |
| `create_menu_draft` | `draft.create_menu` |
| `create_diet_plan_draft` | `draft.create_diet_plan` |

MCP dispatch (#394) validates the same auth/scoping as orchestrator — no alternate code paths that bypass tenant checks.

---

## Explicit non-tools (v1)

The model must **not** be offered functions for:

| Prohibited action | Reason |
|-------------------|--------|
| `assign_diet_to_patient` | Patient assignment is manual UI only |
| `save_platillo` / `save_dieta` | Use draft + accept (#382) |
| `delete_*` / `update_*` catalog | Destructive — not exposed |
| `search_patients` / `get_patient_record` | PHI minimization (#362) |
| `send_mobile_message` | Out of scope |

---

## Testing expectations (#401–#403)

- JSON Schema validation unit tests per tool input/output  
- Authorization tests: cross-tenant `platilloId`, `dietaId`, `threadId` → `NOT_FOUND`  
- Draft tools assert no rows in `platillo` / `dieta` / `paciente_dieta`  
- Golden prompts (#401) reference tool names from this contract  

---

## Related documents

| Doc | Issue |
|-----|-------|
| [`DATA-ACCESS-RULES.md`](DATA-ACCESS-RULES.md) | Scoping and PHI |
| [`FUNCTIONAL-SCOPE.md`](FUNCTIONAL-SCOPE.md) | UX and confirmation flows |
| MCP endpoint design | [`MCP-SERVER-ENDPOINT.md`](MCP-SERVER-ENDPOINT.md) | #392 |
| Draft accept mapping to entities | #382 |

---

## Acceptance checklist (#363)

- [x] Each tool has name, description, input schema, and output schema
- [x] Read-only tools separated from draft-creation tools
- [x] No tool directly assigns final plans to patients
- [x] All tools include auth and nutritionist scoping requirements
