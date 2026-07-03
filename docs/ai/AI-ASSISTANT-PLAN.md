# AI Nutrition Assistant — Plan

Canonical design for the **AI-powered chat assistant** for licensed nutritionists on the Minutriporcion / Nutriconsultas web app.

**Issue registry:** [`ISSUE-AI-ASSISTANT.md`](../../ISSUE-AI-ASSISTANT.md)  
**Agent workflow:** [`AI-ASSISTANT-WORKFLOW.md`](../../AI-ASSISTANT-WORKFLOW.md)  
**Epic range:** GitHub #360–#442

---

## Language (track-wide)

All AI assistant communications are **in Spanish (es-MX)**:

- Model replies, follow-up questions, assumptions, and warnings
- Draft preview copy (“borrador IA — revisión requerida”, nutrient labels shown to nutritionists)
- Chat UI strings, loading/error/empty states, SweetAlert confirmations
- Plan-gating and rate-limit messages (#409, #386, #399)
- Golden prompt evaluation cases may use Spanish prompts (#401)

Implementation: system prompt (#367) must require Spanish output; UI follows existing admin `messages_es_MX.properties` patterns. Tests should assert Spanish for representative flows (#401, #403).

---

## Objective

Add an AI chat feature so nutritionists can draft:

- Dish recipes
- Daily menus
- Weekly menus
- Dietary plan drafts

The assistant uses existing **food, dish, recipe, nutrient, diet, and patient-context** data through controlled backend tools. It must **not** directly save or assign final diets to patients without explicit nutritionist review and approval.

Functional scope (workflows, prompts, patient context): [`FUNCTIONAL-SCOPE.md`](FUNCTIONAL-SCOPE.md) (#361).

---

## Core product behavior

Example prompts:

- “Create a 7-day 1800 kcal plan for a patient who wants weight loss.”
- “Generate a high-protein breakfast using foods from my catalog.”
- “Create a low-sodium menu with 3 meals and 2 snacks.”
- “Build a recipe using chicken, rice, vegetables, and calculate nutrients.”

The assistant must:

1. Ask follow-up questions when data is missing.
2. Search application catalogs instead of inventing nutrient values.
3. Calculate nutrients using application data.
4. Return structured drafts.
5. Show assumptions and warnings.
6. Let the nutritionist edit, approve, discard, or save the draft.
7. Never expose the OpenAI API key to the browser.
8. Never directly assign a diet/menu to a patient without nutritionist confirmation.

---

## Architecture

```text
Nutritionist browser chat UI
        ↓
Spring Boot AI chat controller
        ↓
AI orchestration service
        ↓
OpenAI Responses API
        ↓
Application tool handlers / MCP tools
        ↓
Nutriconsultas PostgreSQL catalogs and patient context
        ↓
Structured draft returned to nutritionist
```

The backend owns all sensitive logic. The frontend is only a chat interface and draft viewer.

---

## Security

| Rule | Detail |
|------|--------|
| API key | `OPENAI_API_KEY` (or legacy `OPEN_API_KEY`) **server-only** — never in JS, Thymeleaf, REST responses, logs, or Git |
| Auth | All AI endpoints require authenticated nutritionist access |
| Scoping | Catalog and patient lookups scoped to authenticated nutritionist / clinic |
| Isolation | No cross-tenant patient, recipe, menu, or catalog exposure |

---

## Privacy

- Send **minimum** patient data to OpenAI — see [`DATA-ACCESS-RULES.md`](DATA-ACCESS-RULES.md) (#362).
- Avoid names, emails, phone numbers, free-text notes unless strictly required.
- Prefer internal IDs and structured clinical/nutrition constraints.
- Store chat history and drafts in PostgreSQL with retention rules.
- Mark all generated plans **“AI draft — nutritionist review required.”**

---

## Medical / nutrition safety

- Drafting tool only — not an autonomous clinical decision-maker.
- Nutritionist approval required before save or patient assignment.
- Warnings when constraints are missing, contradictory, or outside catalog data.
- Clarifying questions for allergies, calorie target, meal count, restrictions, disease constraints, portions.

---

## Configuration

```properties
nutriconsultas.ai.enabled=${AI_ENABLED:false}
nutriconsultas.ai.openai.api-key=${OPENAI_API_KEY:}
nutriconsultas.ai.openai.model=${OPENAI_MODEL:}
nutriconsultas.ai.openai.store=${OPENAI_STORE:false}
nutriconsultas.ai.max-tool-calls=${AI_MAX_TOOL_CALLS:8}
```

Local `.env` / EC2 `app.env`: see issues #365 and #405.

---

## Subscription gating (Plus+)

Issue **#409**: dedicated `Entitlement.AI_ASSISTANT` for **Plus and Consultorio** only.

**Do not replace `REPORTS_ADVANCED`.** Reportes avanzados is already implemented (`assertCanAccessAdvancedReports`, #187) and included on **Profesional, Plus, and Consultorio**. AI assistant is a separate Plus+ differentiator — add a new pricing-table row on `eterna/index.html`, not a swap.

---

## Tool layer (initial)

| Tool | Type | Purpose |
|------|------|---------|
| `search_food_catalog` | read | Search authorized foods |
| `get_food_nutrients` | read | Nutrients for food + quantity + unit |
| `search_dish_catalog` | read | Search authorized dishes |
| `get_dish_recipe` | read | Dish recipe detail |
| `calculate_recipe_nutrients` | read | Total / per-serving nutrients |
| `get_diet_templates` | read | Template diets |
| `validate_plan_constraints` | read | Calorie/macro/allergy validation |
| `create_dish_draft` | draft | Store recipe draft |
| `create_menu_draft` | draft | Store daily/weekly menu draft |
| `create_diet_plan_draft` | draft | Store diet plan draft |

**No tool** may assign a final plan to a patient. Schemas and auth rules: [`TOOL-CONTRACT.md`](TOOL-CONTRACT.md) (#363).

MCP names (Phase 7): `catalog.search_foods`, `catalog.get_food_nutrients`, `catalog.search_dishes`, `nutrition.calculate_recipe`, `nutrition.validate_menu`, `draft.create_dish`, `draft.create_menu`, `draft.create_diet_plan`.

---

## Data model (draft persistence)

```text
ai_chat_thread       — nutritionist_id, clinic_id, patient_id?, title, timestamps
ai_chat_message      — thread_id, role, content, tool_name?, created_at
ai_generated_draft   — thread_id, draft_type, status (DRAFT|ACCEPTED|DISCARDED), json_payload, timestamps
```

Liquibase incremental changesets only — see [`docs/db/LIQUIBASE.md`](../db/LIQUIBASE.md).

---

## REST API (proposed)

```http
POST /nutritionist/ai/chat/start
POST /nutritionist/ai/chat/message
GET  /nutritionist/ai/chat/{threadId}
GET  /nutritionist/ai/chat/{threadId}/drafts
POST /nutritionist/ai/drafts/{draftId}/accept
POST /nutritionist/ai/drafts/{draftId}/discard
```

MCP (Phase 7): `POST /mcp/nutriconsultas`

---

## Milestones

| Milestone | Phases | Outcome |
|-----------|--------|---------|
| **1 — AI Foundation** | 0, 1, 2 (#360–#371) | Secure OpenAI backend + chat/draft persistence |
| **2 — Nutrition Tools** | 3, 4 (#372–#382) | Catalog search, nutrient calc, draft creation |
| **3 — Chat UX** | 5, 6 (#383–#390) | Nutritionist chat window + draft review |
| **4 — MCP** | 7 (#391–#395) | MCP-compatible tool server |
| **5 — Safety & Release** | 8, 9, 10 (#396–#408) | Audit, evaluation, docs, rollout |

---

## Definition of done (whole project)

- [ ] Nutritionists can open AI chat and request dish/menu/diet drafts
- [ ] AI uses application tools for catalogs and nutrients
- [ ] Clarifying questions when required
- [ ] Drafts stored; accept/discard by owner nutritionist
- [ ] No patient assignment without explicit approval
- [ ] API key server-side only; endpoints authenticated and scoped
- [ ] Liquibase migrations tested
- [ ] Unit, integration, template, and security tests pass
- [ ] Local, production, and nutritionist documentation complete
- [ ] Feature disable via `AI_ENABLED=false`
