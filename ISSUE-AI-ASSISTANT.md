# Issue Registry — `[AI Assistant]` track

Living index of GitHub issues for the **AI Nutrition Assistant** — OpenAI-backed chat for nutritionists to draft recipes, menus, and diet plans using application catalog tools. Update when status changes (commit on the PR that closes work).

**Repo:** [diego-torres/nutriconsultas](https://github.com/diego-torres/nutriconsultas)  
**Plan:** [`docs/ai/AI-ASSISTANT-PLAN.md`](docs/ai/AI-ASSISTANT-PLAN.md)  
**Workflow:** [`AI-ASSISTANT-WORKFLOW.md`](AI-ASSISTANT-WORKFLOW.md)  
**Last updated:** 2026-07-05 — Registered `[AI Assistant]` implementation track **complete** (#360–#450). ~~#395~~ merged (PR #489); epic **#391** (MCP Phase 7) closed.

> **Scope.** AI assistant for **nutritionist web** (`/admin/**`, `/nutritionist/ai/**`). Patient mobile API: [`ISSUE.md`](ISSUE.md). Subscription: [`ISSUE-SUBSCRIPTION.md`](ISSUE-SUBSCRIPTION.md). Do not mix AI orchestration into mobile or subscription PRs unless explicitly coupled.

---

## Track principles

| Principle | Rule |
|-----------|------|
| **Language** | All AI assistant **user-facing** text is **Spanish (es-MX)** — assistant replies, clarifying questions, assumptions, warnings, draft labels, errors, empty states, and upgrade/plan-denial copy. System prompts instruct the model to respond in Spanish. Structured JSON field names may stay English; human-readable strings inside drafts must be Spanish. |
| **Drafts only** | Generated content is a draft until the nutritionist accepts — never auto-assign to patients. |
| **Backend-only secrets** | OpenAI API key never in browser, templates, logs, or Git. |
| **Plan gating** | Plus + Consultorio only (#409) — separate from `REPORTS_ADVANCED`. |

Applies to every issue in this track unless explicitly noted otherwise.

---

## Legend

| State | Meaning |
|-------|---------|
| `NEXT` | Active — pick this up now (if unblocked) |
| `open` | Not started |
| `in-progress` | Branch / PR open |
| `done` | Merged to `main` |
| `deferred` | Paused |

---

## Milestone map

| Milestone | Epics | Issue range | Outcome |
|-----------|-------|-------------|---------|
| **1 — AI Foundation** | Phase 0–2 | #360–#371 | Design + OpenAI backend + DB persistence |
| **2 — Nutrition Tools** | Phase 3–4 | #372–#382 | Catalog tools + draft creation |
| **3 — Chat UX** | Phase 5–6, 6b | #383–#390, **#433–#437**, **#442** | REST API + Thymeleaf chat UI + controls + streaming |
| **4 — MCP** | Phase 7 | #391–#395 | MCP tool server |
| **5 — Safety & Release** | Phase 8–10, 8b | #396–#408, **#438–#441** | Audit, **prompt security**, evaluation, docs, rollout |

**Suggested order:** Milestone 1 → 2 → 3 → 4 (optional parallel after M2) → 5.

---

## Epic — Discovery and Architecture (Phase 0)

Document architecture, security model, data flow, and first implementation scope.

| # | Title | URL | State | Depends on | Notes |
|---|-------|-----|-------|------------|-------|
| **360** | Epic — Discovery and Architecture (Phase 0) | https://github.com/diego-torres/nutriconsultas/issues/360 | **done** | — | ~~#361–#363~~ |
| **361** | Define AI Assistant Functional Scope | https://github.com/diego-torres/nutriconsultas/issues/361 | **done** | **360** | [`docs/ai/FUNCTIONAL-SCOPE.md`](docs/ai/FUNCTIONAL-SCOPE.md) |
| **362** | Define AI Data Access Rules | https://github.com/diego-torres/nutriconsultas/issues/362 | **done** | **360** | [`docs/ai/DATA-ACCESS-RULES.md`](docs/ai/DATA-ACCESS-RULES.md) |
| **363** | Design AI Tool Contract | https://github.com/diego-torres/nutriconsultas/issues/363 | **done** | **360**, **362** | [`docs/ai/TOOL-CONTRACT.md`](docs/ai/TOOL-CONTRACT.md) |

**Suggested order:** #361 + #362 parallel → ~~#363~~.

---

## Epic — Backend OpenAI Integration (Phase 1)

Backend-only OpenAI configuration and client service.

| # | Title | URL | State | Depends on | Notes |
|---|-------|-----|-------|------------|-------|
| **364** | Epic — Backend OpenAI Integration (Phase 1) | https://github.com/diego-torres/nutriconsultas/issues/364 | **done** | **363** | Milestone 1 — ~~#365–#367~~ |
| **365** | Add OpenAI Configuration Properties | https://github.com/diego-torres/nutriconsultas/issues/365 | **done** | **364**, **363** | `AiProperties`, `application.properties` |
| **366** | Add OpenAI Java Client or HTTP Client Integration | https://github.com/diego-torres/nutriconsultas/issues/366 | **done** | **365** | `OpenAiClientService` |
| **367** | Create AI System Prompt Template | https://github.com/diego-torres/nutriconsultas/issues/367 | **done** | **364**, **361** | `AiSystemPromptService`, `ai/system-prompt-base.txt` |

**Suggested order:** ~~#367~~.

---

## Epic — AI Chat Persistence and Draft Storage (Phase 2)

Store chat threads, messages, and generated drafts.

| # | Title | URL | State | Depends on | Notes |
|---|-------|-----|-------|------------|-------|
| **368** | Epic — AI Chat Persistence and Draft Storage (Phase 2) | https://github.com/diego-torres/nutriconsultas/issues/368 | **done** | **364** | Milestone 1 — ~~#369–#371~~ |
| **369** | Add Liquibase Schema for AI Chat Tables | https://github.com/diego-torres/nutriconsultas/issues/369 | **done** | **368**, #46 | `024-ai-chat-schema.yaml` |
| **370** | Implement AI Chat Domain Entities and Repositories | https://github.com/diego-torres/nutriconsultas/issues/370 | **done** | **369** | `com.nutriconsultas.ai` entities/repos |
| **371** | Implement AI Draft Lifecycle | https://github.com/diego-torres/nutriconsultas/issues/371 | **done** | **370** | `AiDraftLifecycleService` |

**Suggested order:** ~~#371~~.

---

## Epic — Backend Nutrition Tool Services (Phase 3)

Read-only catalog and validation tools for AI orchestration.

| # | Title | URL | State | Depends on | Notes |
|---|-------|-----|-------|------------|-------|
| **372** | Epic — Backend Nutrition Tool Services (Phase 3) | https://github.com/diego-torres/nutriconsultas/issues/372 | **done** | **363**, **371** | Milestone 2 — ~~#373–#377~~ |
| **373** | Implement Food Catalog Search Tool | https://github.com/diego-torres/nutriconsultas/issues/373 | **done** | **372** | `search_food_catalog` |
| **374** | Implement Food Nutrient Lookup Tool | https://github.com/diego-torres/nutriconsultas/issues/374 | **done** | **372** | `get_food_nutrients` |
| **375** | Implement Dish Catalog Search Tool | https://github.com/diego-torres/nutriconsultas/issues/375 | **done** | **372** | `search_dish_catalog` |
| **376** | Implement Recipe Nutrient Calculation Tool | https://github.com/diego-torres/nutriconsultas/issues/376 | **done** | **374** | `calculate_recipe_nutrients` |
| **377** | Implement Plan Constraint Validation Tool | https://github.com/diego-torres/nutriconsultas/issues/377 | **done** | **376** | `validate_plan_constraints` |

**Suggested order:** ~~#377~~.

---

## Epic — AI Draft Creation Tools (Phase 4)

Draft-creation tools and acceptance flow (no direct patient assignment).

| # | Title | URL | State | Depends on | Notes |
|---|-------|-----|-------|------------|-------|
| **378** | Epic — AI Draft Creation Tools (Phase 4) | https://github.com/diego-torres/nutriconsultas/issues/378 | **done** | **372** | ~~#379–#382~~ |
| **379** | Implement Dish Draft Creation Tool | https://github.com/diego-torres/nutriconsultas/issues/379 | **done** | **378**, **371** | `create_dish_draft` |
| **380** | Implement Menu Draft Creation Tool | https://github.com/diego-torres/nutriconsultas/issues/380 | **done** | **378**, **371** | `create_menu_draft` |
| **381** | Implement Diet Plan Draft Creation Tool | https://github.com/diego-torres/nutriconsultas/issues/381 | **done** | **378**, **371** | `create_diet_plan_draft` |
| **382** | Implement Draft Acceptance Flow | https://github.com/diego-torres/nutriconsultas/issues/382 | **done** | **379**, **380**, **381** | Convert draft → real record |

**Phase 4 complete.** Epic #383 **done** — ~~#384–#386~~ REST API complete. **NEXT:** #387.

---

## Epic — AI Chat REST API (Phase 5)

Authenticated endpoints for chat and draft management.

| # | Title | URL | State | Depends on | Notes |
|---|-------|-----|-------|------------|-------|
| **383** | Epic — AI Chat REST API (Phase 5) | https://github.com/diego-torres/nutriconsultas/issues/383 | **done** | **366**, **382** | Milestone 3 — ~~#384–#386~~ |
| **384** | Create AI Chat Controller | https://github.com/diego-torres/nutriconsultas/issues/384 | **done** | **383**, **385** | `AiChatRestController` |
| **385** | Implement AI Orchestration Service | https://github.com/diego-torres/nutriconsultas/issues/385 | **done** | **366**, **372**, **378** | `AiOrchestrationService` |
| **386** | Add Rate Limiting for AI Chat | https://github.com/diego-torres/nutriconsultas/issues/386 | **done** | **385** | `AiChatRateLimiter` |

**Suggested order:** ~~#386~~ → **#387**.

---

## Epic — Nutritionist AI Chat UI (Phase 6)

Thymeleaf chat window and draft preview.

| # | Title | URL | State | Depends on | Notes |
|---|-------|-----|-------|------------|-------|
| **387** | Epic — Nutritionist AI Chat UI (Phase 6) | https://github.com/diego-torres/nutriconsultas/issues/387 | **done** | **384** | ~~#388–#390, #442, #433–#437~~ |
| **388** | Add AI Chat Entry Point to Nutritionist UI | https://github.com/diego-torres/nutriconsultas/issues/388 | **done** | **387**, **365** | Sidebar + `/admin/ai`, gated by `AI_ENABLED` |
| **389** | Build AI Chat Window | https://github.com/diego-torres/nutriconsultas/issues/389 | **done** | **388**, **384** | `ai-chat.js` + REST integration |
| **390** | Build Draft Preview UI | https://github.com/diego-torres/nutriconsultas/issues/390 | **done** | **389**, **382** | PR #443 |
| **442** | Floating context-aware AI assistant widget | https://github.com/diego-torres/nutriconsultas/issues/442 | **done** | **388**, **384**, **389** | Merged with PR #432 |

**Suggested order:** ~~#389~~ ~~#390~~ ~~#442~~ ~~#434–#437~~ **done**. Epic **#433** complete.

---

## Epic — AI Chat UX Enhancements (Phase 6b)

Markdown, streaming, and message controls for full-page chat and floating widget.

| # | Title | URL | State | Depends on | Notes |
|---|-------|-----|-------|------------|-------|
| **433** | Epic — AI Chat UX Enhancements (Phase 6b) | https://github.com/diego-torres/nutriconsultas/issues/433 | **done** | **387**, **390** | ~~#434–#437~~ merged |
| **434** | Render markdown in assistant chat responses | https://github.com/diego-torres/nutriconsultas/issues/434 | **done** | **433**, **389** | PR #444 |
| **435** | Stream assistant responses (SSE) | https://github.com/diego-torres/nutriconsultas/issues/435 | **done** | **433**, **385**, **389** | PR #445 |
| **436** | Stop and cancel in-flight AI generation | https://github.com/diego-torres/nutriconsultas/issues/436 | **done** | **433**, **389** | PR #446 |
| **437** | Edit user message and resubmit | https://github.com/diego-torres/nutriconsultas/issues/437 | **done** | **433**, **384**, **389** | PR #451 |

**Suggested order:** Epic **#433** complete. ~~#439~~ → **#440** (prompt security).

---

## Epic — MCP Tool Server (Phase 7)

MCP-compatible exposure of nutrition tools.

| # | Title | URL | State | Depends on | Notes |
|---|-------|-----|-------|------------|-------|
| **391** | Epic — MCP Tool Server for Nutriconsultas (Phase 7) | https://github.com/diego-torres/nutriconsultas/issues/391 | **done** | **372**, **378** | ~~#392–#395~~ PR #486–#489 |
| **392** | Design MCP Server Endpoint | https://github.com/diego-torres/nutriconsultas/issues/392 | **done** | **391**, **363** | PR #486 — [`MCP-SERVER-ENDPOINT.md`](docs/ai/MCP-SERVER-ENDPOINT.md) |
| **393** | Implement MCP Tool Descriptors | https://github.com/diego-torres/nutriconsultas/issues/393 | **done** | **392** | PR #487 |
| **394** | Implement MCP Tool Dispatch | https://github.com/diego-torres/nutriconsultas/issues/394 | **done** | **393**, **372** | PR #488 |
| **395** | Add MCP Security Review | https://github.com/diego-torres/nutriconsultas/issues/395 | **done** | **394** | PR #489 |

**Suggested order:** Epic **#391** complete. Registered MCP track **done**.

---

## Epic — AI Safety and Observability (Phase 8)

Audit logging, metrics, and error UX.

| # | Title | URL | State | Depends on | Notes |
|---|-------|-----|-------|------------|-------|
| **396** | Epic — AI Safety and Observability (Phase 8) | https://github.com/diego-torres/nutriconsultas/issues/396 | **done** | **385** | ~~#397–#399~~ PR #483–#485 |
| **397** | Add AI Audit Logging | https://github.com/diego-torres/nutriconsultas/issues/397 | **done** | **396** | PR #483 — redacted audit logs |
| **398** | Add AI Usage Metrics | https://github.com/diego-torres/nutriconsultas/issues/398 | **done** | **396** | PR #484 — Micrometer metrics |
| **399** | Add AI Error Handling UX | https://github.com/diego-torres/nutriconsultas/issues/399 | **done** | **389**, **385** | PR #485 — Spanish errors + `errorCode` |

**Suggested order:** Epic **#396** complete. Epic **#391** (MCP) complete. Prompt hardening epic **#438** complete.

---

## Epic — Prompt Security Hardening (Phase 8b)

Injection, jailbreak, and defense-in-depth guardrails for orchestration (#385).

| # | Title | URL | State | Depends on | Notes |
|---|-------|-----|-------|------------|-------|
| **438** | Epic — Prompt Security Hardening (Phase 8b) | https://github.com/diego-torres/nutriconsultas/issues/438 | **done** | **396**, **385**, **362** | ~~#439–#441, #447–#450~~ |
| **439** | Prompt injection input guardrails | https://github.com/diego-torres/nutriconsultas/issues/439 | **done** | **438**, **385** | PR #452 — `AiUserMessageGuard`, `docs/ai/PROMPT-SECURITY.md` |
| **440** | Jailbreak and role-override defenses | https://github.com/diego-torres/nutriconsultas/issues/440 | **done** | **438**, **439**, **367** | PR #454 — `AiPromptThreatDetector`, tool hardening |
| **441** | Defense-in-depth prompt engineering guardrails | https://github.com/diego-torres/nutriconsultas/issues/441 | **done** | **438**, **439**, **440**, **372** | PR #464 — delimiters, tool allowlist, output validation |
| **447** | Deterministic request scope limits (bulk generation guard) | https://github.com/diego-torres/nutriconsultas/issues/447 | **done** | **438**, **385** | PR #456 — `AiRequestScopeGuard`, configurable thresholds |
| **448** | LLM scope classifier pre-flight | https://github.com/diego-torres/nutriconsultas/issues/448 | **done** | **438**, **447**, **366** | PR #460 — `AiRequestScopeClassifier`, JSON pre-flight |
| **449** | System prompt volume limits and bulk refusal corpus | https://github.com/diego-torres/nutriconsultas/issues/449 | **done** | **438**, **367**, **447** | PR #458 — `VOLUMEN Y LÍMITES`, `FUNCTIONAL-SCOPE.md`, bulk corpus |
| **450** | Golden prompts for excessive bulk AI requests | https://github.com/diego-torres/nutriconsultas/issues/450 | **done** | **400**, **401**, **447** | PR #462 — `AiBulkScopeGoldenPromptTest`, docs |

**Suggested order:** ~~#401~~ ~~#402~~ ~~#403~~ **done**. Epic **#400** complete. Phase 10: ~~#405~~ ~~#406~~ ~~#407~~ ~~#408~~ ~~#409~~ **done**. Epic **#391** (MCP) complete.

---

## Epic — AI Nutrition Evaluation Suite (Phase 9)

Golden prompts, schema validation, E2E tests.

| # | Title | URL | State | Depends on | Notes |
|---|-------|-----|-------|------------|-------|
| **400** | Epic — AI Nutrition Evaluation Suite (Phase 9) | https://github.com/diego-torres/nutriconsultas/issues/400 | **done** | **382**, **385** | PR #466, #468, #470 — golden prompts, schema validation, E2E |
| **401** | Add Golden Prompt Test Cases | https://github.com/diego-torres/nutriconsultas/issues/401 | **done** | **400**, **361** | PR #466 — `AiNutritionGoldenPromptTest`, docs |
| **402** | Add Structured Output Validation | https://github.com/diego-torres/nutriconsultas/issues/402 | **done** | **379**, **380**, **381** | PR #468 — JSON Schema validation, `AiDraftToolSchemaValidator` |
| **403** | Add End-to-End AI Draft Flow Test | https://github.com/diego-torres/nutriconsultas/issues/403 | **done** | **384**, **390** | PR #470 — `AiDraftFlowIntegrationTest`, IDOR |

**Suggested order:** ~~#401~~ ~~#402~~ ~~#403~~ **done** (PR #466, #468, #470). Epic **#400** complete.

---

## Epic — Documentation and Release (Phase 10)

Setup docs, nutritionist guidance, release checklist.

| # | Title | URL | State | Depends on | Notes |
|---|-------|-----|-------|------------|-------|
| **404** | Epic — AI Assistant Documentation and Release (Phase 10) | https://github.com/diego-torres/nutriconsultas/issues/404 | **done** | **396**, **403** | ~~#405–#409~~ PR #472–#481 |
| **405** | Document Local AI Setup | https://github.com/diego-torres/nutriconsultas/issues/405 | **done** | **365** | PR #472 — [`docs/ai/LOCAL-AI-SETUP.md`](docs/ai/LOCAL-AI-SETUP.md) |
| **406** | Document Production AI Setup | https://github.com/diego-torres/nutriconsultas/issues/406 | **done** | **365** | PR #474 — [`docs/ai/PRODUCTION-AI-SETUP.md`](docs/ai/PRODUCTION-AI-SETUP.md), `ssm-update-ai-openai.sh` |
| **407** | Add Nutritionist User Guidance | https://github.com/diego-torres/nutriconsultas/issues/407 | **done** | **390** | PR #477 — in-app panel + [`NUTRITIONIST-USER-GUIDANCE.md`](docs/ai/NUTRITIONIST-USER-GUIDANCE.md) |
| **408** | Create AI Assistant Release Checklist | https://github.com/diego-torres/nutriconsultas/issues/408 | **done** | **404** | PR #479 — [`RELEASE-CHECKLIST.md`](docs/ai/RELEASE-CHECKLIST.md) |

**Suggested order:** ~~#397~~ ~~#398~~ ~~#399~~ **done**. Epic **#396** complete. Epic **#391** (MCP) complete.

---

## Subscription gating — Plus and Consultorio only

AI assistant is a **new entitlement** — do **not** replace `REPORTS_ADVANCED` (reportes avanzados). That flag is implemented (#187 ✓) for Profesional+ and must stay.

| # | Title | URL | State | Depends on | Notes |
|---|-------|-----|-------|------------|-------|
| **409** | Gate AI assistant by plan — Plus and Consultorio only | https://github.com/diego-torres/nutriconsultas/issues/409 | **done** | #181, **384**, **388** | PR #481 — `Entitlement.AI_ASSISTANT`, pricing row on `eterna/index.html` |

**Suggested order:** ~~#409~~ **done** (PR #481). ~~#397~~ ~~#398~~ ~~#399~~ **done** (PR #483–#485). Epic **#391** (MCP) **done** (PR #486–#489).

---

## Cross-track links

| Track | Interaction |
|-------|-------------|
| #46 Liquibase | All `ai_chat_*` schema via incremental changesets |
| [`ISSUE-NUTRITIONIST-WEB.md`](ISSUE-NUTRITIONIST-WEB.md) | Draft acceptance may create platillos/dietas via existing flows |
| [`ISSUE-SUBSCRIPTION.md`](ISSUE-SUBSCRIPTION.md) | **#409** — new `AI_ASSISTANT` entitlement (Plus + Consultorio); do not reuse `REPORTS_ADVANCED` |
| [`ISSUE.md`](ISSUE.md) | Mobile API orthogonal — no patient AI chat in v1 |
| Multi-tenant | Reuse `Paciente.userId` / clinic scoping patterns |
| `LogRedaction` | AI audit logs must not leak PHI |

---

## Definition of done (project)

See [`docs/ai/AI-ASSISTANT-PLAN.md`](docs/ai/AI-ASSISTANT-PLAN.md#definition-of-done-whole-project).

---

**How to update this file**

Same convention as [`ISSUE.md`](ISSUE.md): mark `in-progress` when PR opens, `done` when merged. Reference from [`AI-ASSISTANT-WORKFLOW.md`](AI-ASSISTANT-WORKFLOW.md) and [`AGENTS.md`](AGENTS.md).
