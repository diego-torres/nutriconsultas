# Nutrition golden prompts (#401)

**Issue:** [#401](https://github.com/diego-torres/nutriconsultas/issues/401) · Epic [#400](https://github.com/diego-torres/nutriconsultas/issues/400)  
**Related:** [`FUNCTIONAL-SCOPE.md`](FUNCTIONAL-SCOPE.md) (#361) · [`TOOL-CONTRACT.md`](TOOL-CONTRACT.md) (#363) · [`BULK-SCOPE-GOLDEN-PROMPTS.md`](BULK-SCOPE-GOLDEN-PROMPTS.md) (#450)

Documented evaluation scenarios for **supported nutrition workflows** in Spanish (es-MX). Tests run in CI **without live OpenAI** — mocked orchestration simulates expected tool usage and warning copy.

---

## Scenario matrix

| ID | Prompt (es-MX) | Category | Expectation | Expected tools |
|----|----------------|----------|-------------|----------------|
| `high-protein-breakfast` | Genera un desayuno alto en proteína usando alimentos de mi catálogo. | DISH | Use tools | `search_food_catalog` → `calculate_recipe_nutrients` → `create_dish_draft` |
| `low-sodium-day-menu` | Menú bajo en sodio de un día: desayuno, comida, cena y dos colaciones. | MENU_DAILY | Use tools | `search_food_catalog`, `search_dish_catalog`, `validate_plan_constraints`, `create_menu_draft` |
| `7-day-weight-loss-menu` | Menú semanal de 7 días para pérdida de peso, 1600 kcal. | MENU_WEEKLY | Use tools | `search_food_catalog`, `search_dish_catalog`, `validate_plan_constraints`, `create_diet_plan_draft` |
| `egg-allergy-plan` | Plan de 7 días sin huevo para el paciente vinculado. | DIET_PLAN | Use tools + patient context | `search_food_catalog`, `validate_plan_constraints`, `create_diet_plan_draft` |
| `diabetic-friendly-menu` | Menú de un día amigable para diabetes, 1800 kcal, bajo índice glucémico. | MENU_DAILY | Use tools | `search_food_catalog`, `search_dish_catalog`, `validate_plan_constraints`, `create_menu_draft` |
| `vegetarian-weekly-plan` | Plan semanal vegetariano de 7 días, lunes a viernes iguales. | MENU_WEEKLY | Use tools | `search_food_catalog`, `search_dish_catalog`, `create_diet_plan_draft` |
| `unsupported-ingredient` | Receta con pitahaya del catálogo. | CATALOG_LOOKUP | Search only | `search_food_catalog` (no draft if missing) |
| `menu-missing-calorie-target` | Arma un menú de un día con desayuno, comida y cena. | CLARIFY | Ask first | *(none on first turn)* |

---

## Expected warnings (Spanish)

| ID | Warning themes |
|----|----------------|
| `high-protein-breakfast` | *Borrador IA*, *revisión del nutriólogo* |
| `low-sodium-day-menu` | *sodio*, draft label |
| `7-day-weight-loss-menu` | Calorie target echoed (*1600*), draft label |
| `egg-allergy-plan` | *huevo*, *alergia*, draft label |
| `diabetic-friendly-menu` | *revisión profesional* (no diagnosis), draft label |
| `vegetarian-weekly-plan` | *vegetariano*, *supuesto*, draft label |
| `unsupported-ingredient` | *catálogo*, *no se encontró*, *sustituto* — must not invent foods |
| `menu-missing-calorie-target` | Ask for *objetivo calórico* / *kcal* before generating |

---

## Assertions (automated)

| Check | Implementation |
|-------|----------------|
| In scope (#447) | All prompts pass `AiRequestScopeGuard` |
| Valid tool names | Expected tools ⊆ `AiToolAllowlist` |
| Tool loop (MUST_USE_TOOLS) | Mocked OpenAI returns tool calls; `AiOrchestrationToolDispatcher` invoked for each |
| Clarify (MUST_CLARIFY) | No tool dispatch; assistant asks for missing kcal |
| Catalog miss (MUST_SEARCH_CATALOG) | Search only; no `create_dish_draft` when ingredient absent |

---

## Test classes

| Class | Role |
|-------|------|
| `AiNutritionGoldenPrompt` | Scenario fixtures (ids, prompts, tools, warnings) |
| `AiNutritionGoldenPromptTest` | Parameterized golden evaluation (#401) |

Run:

```bash
mvn test -Dtest=AiNutritionGoldenPromptTest
```

---

## Manual evaluation (optional)

For live OpenAI smoke tests (not CI), use the prompts above in `/admin/ai` with `AI_ENABLED=true` and verify:

1. Assistant responds in **Spanish**.
2. Tool audit rows match expected tools (or clarifying question when applicable).
3. Draft preview shows **Borrador IA — revisión del nutriólogo requerida**.
4. No patient assignment or catalog save without accept flow (#382).

---

## Adding scenarios

1. Add a row to the matrix and a `Scenario` in `AiNutritionGoldenPrompt.java`.
2. Reference tool names from [`TOOL-CONTRACT.md`](TOOL-CONTRACT.md).
3. Extend `AiNutritionGoldenPromptTest` if a new `Expectation` type is needed.

Security and bulk-scope scenarios live in [`SECURITY-GOLDEN-PROMPTS.md`](SECURITY-GOLDEN-PROMPTS.md) and [`BULK-SCOPE-GOLDEN-PROMPTS.md`](BULK-SCOPE-GOLDEN-PROMPTS.md).
