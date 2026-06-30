#!/usr/bin/env bash
# One-time bootstrap: created GitHub issues #360–#408 on 2026-06-30.
# Do not re-run unless extending the track with new issues.
set -euo pipefail

REPO="diego-torres/nutriconsultas"
OUT="/tmp/ai-assistant-issues.json"
echo "[]" > "$OUT"

append_issue() {
  local num="$1"
  local title="$2"
  local epic="$3"
  local phase="$4"
  jq --arg n "$num" --arg t "$title" --arg e "$epic" --arg p "$phase" \
    '. += [{number: ($n|tonumber), title: $t, epic: ($e|tonumber), phase: $p}]' "$OUT" > "${OUT}.tmp" && mv "${OUT}.tmp" "$OUT"
}

create_epic() {
  local phase="$1"
  local title="$2"
  local body="$3"
  local labels="$4"
  local url num
  url="$(gh issue create --repo "$REPO" \
    --title "[AI Assistant] Epic — $title" \
    --label "$labels" \
    --body "$body")"
  num="${url##*/}"
  append_issue "$num" "[AI Assistant] Epic — $title" "$num" "$phase"
  echo "$num"
}

create_child() {
  local epic="$1"
  local phase="$2"
  local title="$3"
  local body="$4"
  local labels="$5"
  local url num
  url="$(gh issue create --repo "$REPO" \
    --title "[AI Assistant] $title" \
    --label "$labels" \
    --body "$body")"
  num="${url##*/}"
  append_issue "$num" "[AI Assistant] $title" "$epic" "$phase"
  echo "$num"
}

EPIC0="$(create_epic "0" "Discovery and Architecture (Phase 0)" "$(cat <<'EOF'
## Goal

Document the architecture, security model, data flow, and first implementation scope for the AI nutrition assistant.

## Milestone

**Milestone 1 — AI Foundation** (with Phase 1 and Phase 2).

## Child issues

Created as part of this epic; see [`ISSUE-AI-ASSISTANT.md`](https://github.com/diego-torres/nutriconsultas/blob/main/ISSUE-AI-ASSISTANT.md).

## Plan

[`docs/ai/AI-ASSISTANT-PLAN.md`](https://github.com/diego-torres/nutriconsultas/blob/main/docs/ai/AI-ASSISTANT-PLAN.md)
EOF
)" "ai,documentation")"

create_child "$EPIC0" "0" "Define AI Assistant Functional Scope" "$(cat <<EOF
Part of epic #$EPIC0.

## Description

Define the first supported AI assistant workflows:

- Create dish recipe draft
- Create daily menu draft
- Create weekly menu draft
- Create dietary plan draft
- Calculate nutrient totals
- Validate plan constraints

## Acceptance criteria

- [ ] Document supported user prompts
- [ ] Document unsupported prompts
- [ ] Define what the assistant may do automatically
- [ ] Define what requires nutritionist confirmation
- [ ] Include examples for dish, menu, and diet-plan generation
EOF
)" "ai,documentation,nutrition"

create_child "$EPIC0" "0" "Define AI Data Access Rules" "$(cat <<EOF
Part of epic #$EPIC0.

## Description

Document what data the AI assistant may access and under which conditions.

## Acceptance criteria

- [ ] Define nutritionist/clinic scoping rules
- [ ] Define patient-context access rules
- [ ] Define which fields are safe to send to OpenAI
- [ ] Define which fields must be excluded or redacted
- [ ] Define logging and audit requirements
- [ ] Confirm API keys are backend-only
EOF
)" "ai,documentation,security"

create_child "$EPIC0" "0" "Design AI Tool Contract" "$(cat <<EOF
Part of epic #$EPIC0.

## Description

Design backend tools that allow the AI assistant to interact with application data safely.

## Initial tool list

\`\`\`text
search_food_catalog
get_food_nutrients
search_dish_catalog
get_dish_recipe
calculate_recipe_nutrients
get_diet_templates
validate_plan_constraints
create_dish_draft
create_menu_draft
create_diet_plan_draft
\`\`\`

## Acceptance criteria

- [ ] Each tool has a name, description, input schema, and output schema
- [ ] Read-only tools are separated from draft-creation tools
- [ ] No tool directly assigns final plans to patients
- [ ] All tools include auth and clinic/nutritionist scoping requirements
EOF
)" "ai,documentation,mcp,backend"

EPIC1="$(create_epic "1" "Backend OpenAI Integration (Phase 1)" "$(cat <<'EOF'
## Goal

Add backend-only OpenAI configuration and a service capable of sending chat requests and receiving structured responses.

## Milestone

**Milestone 1 — AI Foundation** (with Phase 0 and Phase 2).

## Security

- `OPENAI_API_KEY` / `OPEN_API_KEY` stored only in server environment variables
- Never expose API key to browser, logs, or Git
EOF
)" "ai,backend,security")"

create_child "$EPIC1" "1" "Add OpenAI Configuration Properties" "$(cat <<EOF
Part of epic #$EPIC1.

## Description

Add backend configuration for OpenAI.

## Required properties

\`\`\`properties
nutriconsultas.ai.enabled=\${AI_ENABLED:false}
nutriconsultas.ai.openai.api-key=\${OPENAI_API_KEY:}
nutriconsultas.ai.openai.model=\${OPENAI_MODEL:}
nutriconsultas.ai.openai.store=\${OPENAI_STORE:false}
nutriconsultas.ai.max-tool-calls=\${AI_MAX_TOOL_CALLS:8}
\`\`\`

## Acceptance criteria

- [ ] Properties added to \`application.properties\`
- [ ] \`.env.example\` documents the new variables
- [ ] App fails safely when AI is enabled but API key is missing
- [ ] API key is never logged
- [ ] Unit test covers missing/disabled config behavior
EOF
)" "ai,backend,tests"

create_child "$EPIC1" "1" "Add OpenAI Java Client or HTTP Client Integration" "$(cat <<EOF
Part of epic #$EPIC1.

## Description

Add the backend integration used to call OpenAI from Spring Boot.

## Acceptance criteria

- [ ] Add required Maven dependency or implement Spring HTTP client
- [ ] Create \`AiClient\` or \`OpenAiClientService\`
- [ ] Support sending user messages and system instructions
- [ ] Support structured output or tool/function calls
- [ ] Add timeout handling
- [ ] Add error handling for rate limits, auth failures, and unavailable model
- [ ] Add unit tests with mocked responses
EOF
)" "ai,backend,integration,tests"

create_child "$EPIC1" "1" "Create AI System Prompt Template" "$(cat <<EOF
Part of epic #$EPIC1.

## Description

Create a server-side prompt template for nutrition drafting.

## Prompt requirements

The assistant should:

- Help licensed nutrition professionals
- Prefer application catalog tools over memory
- Ask clarifying questions when needed
- Return structured recipe/menu/diet drafts
- Include assumptions and warnings
- Never claim medical appropriateness without nutritionist review
- Never directly assign a diet to a patient

## Acceptance criteria

- [ ] Prompt template stored server-side
- [ ] Prompt supports locale, nutritionist context, and optional patient constraints
- [ ] Unit tests verify key safety instructions are included
EOF
)" "ai,backend,nutrition,tests"

EPIC2="$(create_epic "2" "AI Chat Persistence and Draft Storage (Phase 2)" "$(cat <<'EOF'
## Goal

Store AI chat threads, messages, and generated drafts in the database.

## Milestone

**Milestone 1 — AI Foundation** (with Phase 0 and Phase 1).
EOF
)" "ai,backend,liquibase,db")"

create_child "$EPIC2" "2" "Add Liquibase Schema for AI Chat Tables" "$(cat <<EOF
Part of epic #$EPIC2.

## Description

Create incremental Liquibase changesets for AI chat persistence.

## Proposed tables

\`\`\`text
ai_chat_thread — id, nutritionist_id, clinic_id, patient_id (nullable), title, created_at, updated_at
ai_chat_message — id, thread_id, role, content, tool_name (nullable), created_at
ai_generated_draft — id, thread_id, draft_type, status, json_payload, created_at, accepted_at (nullable)
\`\`\`

## Acceptance criteria

- [ ] New Liquibase changeset under \`db/changelog/changes/\`
- [ ] Master changelog includes the new changeset
- [ ] PostgreSQL migration works
- [ ] H2/test migration works or is covered by integration test
- [ ] No existing baseline changesets edited
- [ ] Entity mappings and repositories added
- [ ] Migration test added
EOF
)" "ai,liquibase,db,tests"

create_child "$EPIC2" "2" "Implement AI Chat Domain Entities and Repositories" "$(cat <<EOF
Part of epic #$EPIC2.

Depends on Liquibase schema issue in this epic.

## Acceptance criteria

- [ ] Entities for chat thread, message, and generated draft
- [ ] Repositories with scoped lookup methods
- [ ] Created/updated timestamps
- [ ] Draft status enum (\`DRAFT\`, \`ACCEPTED\`, \`DISCARDED\`)
- [ ] Draft type enum
- [ ] Unit tests cover persistence behavior
EOF
)" "ai,backend,db,tests"

create_child "$EPIC2" "2" "Implement AI Draft Lifecycle" "$(cat <<EOF
Part of epic #$EPIC2.

## Acceptance criteria

- [ ] Drafts created as \`DRAFT\`
- [ ] Drafts can be accepted by owning nutritionist
- [ ] Drafts can be discarded by owning nutritionist
- [ ] Drafts cannot be accepted by another nutritionist
- [ ] Accepted draft records are immutable or audit-safe
- [ ] Service tests added
EOF
)" "ai,backend,tests"

EPIC3="$(create_epic "3" "Backend Nutrition Tool Services (Phase 3)" "$(cat <<'EOF'
## Goal

Create safe backend services that the AI can call to search catalogs, calculate nutrients, and validate constraints.

## Milestone

**Milestone 2 — Nutrition Tools** (with Phase 4).
EOF
)" "ai,backend,nutrition,mcp")"

create_child "$EPIC3" "3" "Implement Food Catalog Search Tool (search_food_catalog)" "$(cat <<EOF
Part of epic #$EPIC3.

## Tool name

\`search_food_catalog\`

## Acceptance criteria

- [ ] Searches foods available to authenticated nutritionist
- [ ] Supports query text and optional filters
- [ ] Limits result count
- [ ] Returns food ID, name, category, default unit, summary nutrients
- [ ] Does not expose unauthorized catalog rows
- [ ] Service tests included
EOF
)" "ai,backend,nutrition,tests"

create_child "$EPIC3" "3" "Implement Food Nutrient Lookup Tool (get_food_nutrients)" "$(cat <<EOF
Part of epic #$EPIC3.

## Tool name

\`get_food_nutrients\`

## Acceptance criteria

- [ ] Accepts food ID, quantity, and unit
- [ ] Converts or validates units where supported
- [ ] Returns calories, macros, and available micronutrients
- [ ] Returns validation error when unit unsupported
- [ ] Uses application database values, not model guesses
- [ ] Unit tests included
EOF
)" "ai,backend,nutrition,tests"

create_child "$EPIC3" "3" "Implement Dish Catalog Search Tool (search_dish_catalog)" "$(cat <<EOF
Part of epic #$EPIC3.

## Tool name

\`search_dish_catalog\`

## Acceptance criteria

- [ ] Searches only authorized dishes
- [ ] Returns dish ID, name, tags/category, serving info, nutrient summary
- [ ] Supports max result limit
- [ ] Tests for scoping included
EOF
)" "ai,backend,nutrition,tests"

create_child "$EPIC3" "3" "Implement Recipe Nutrient Calculation Tool (calculate_recipe_nutrients)" "$(cat <<EOF
Part of epic #$EPIC3.

## Tool name

\`calculate_recipe_nutrients\`

## Acceptance criteria

- [ ] Accepts ingredient list with food IDs, quantities, and units
- [ ] Calculates total nutrients
- [ ] Calculates per-serving nutrients
- [ ] Returns warnings for missing nutrient values
- [ ] Rejects unknown food IDs or unsupported units
- [ ] Deterministic tests included
EOF
)" "ai,backend,nutrition,tests"

create_child "$EPIC3" "3" "Implement Plan Constraint Validation Tool (validate_plan_constraints)" "$(cat <<EOF
Part of epic #$EPIC3.

## Tool name

\`validate_plan_constraints\`

## Acceptance criteria

- [ ] Accepts structured draft and constraint object
- [ ] Returns pass/warn/fail status
- [ ] Returns specific warnings
- [ ] Does not make clinical claims
- [ ] Tests for common constraints included
EOF
)" "ai,backend,nutrition,tests"

EPIC4="$(create_epic "4" "AI Draft Creation Tools (Phase 4)" "$(cat <<'EOF'
## Goal

Allow AI to create application draft records that nutritionists can review before saving final records.

## Milestone

**Milestone 2 — Nutrition Tools** (with Phase 3).

## Rule

No tool directly assigns final plans to patients.
EOF
)" "ai,backend,nutrition")"

create_child "$EPIC4" "4" "Implement Dish Draft Creation Tool (create_dish_draft)" "$(cat <<EOF
Part of epic #$EPIC4.

## Acceptance criteria

- [ ] Accepts structured recipe JSON
- [ ] Stores draft as \`DRAFT\`
- [ ] Includes ingredients, instructions, servings, nutrients, assumptions, warnings
- [ ] Does not create final dish record
- [ ] Requires authenticated nutritionist
- [ ] Tests included
EOF
)" "ai,backend,nutrition,tests"

create_child "$EPIC4" "4" "Implement Menu Draft Creation Tool (create_menu_draft)" "$(cat <<EOF
Part of epic #$EPIC4.

## Acceptance criteria

- [ ] Accepts structured menu JSON
- [ ] Supports daily and weekly menus
- [ ] Includes meals, snacks, dishes, portions, nutrients, assumptions, warnings
- [ ] Stores draft as \`DRAFT\`
- [ ] Does not assign menu to patient
- [ ] Tests included
EOF
)" "ai,backend,nutrition,tests"

create_child "$EPIC4" "4" "Implement Diet Plan Draft Creation Tool (create_diet_plan_draft)" "$(cat <<EOF
Part of epic #$EPIC4.

## Acceptance criteria

- [ ] Accepts structured diet-plan JSON
- [ ] Supports patient-context constraints when authorized
- [ ] Includes meals, portions, nutrients, assumptions, warnings
- [ ] Stores draft as \`DRAFT\`
- [ ] Does not assign plan to patient
- [ ] Tests included
EOF
)" "ai,backend,nutrition,tests"

create_child "$EPIC4" "4" "Implement Draft Acceptance Flow" "$(cat <<EOF
Part of epic #$EPIC4.

## Acceptance criteria

- [ ] Nutritionist can accept own draft
- [ ] Nutritionist can edit before saving where supported by existing UI patterns
- [ ] Accepted dish draft creates real dish/recipe record
- [ ] Accepted menu/diet draft creates appropriate application record
- [ ] Draft status changes to \`ACCEPTED\`
- [ ] Operation is audited
- [ ] Controller and service tests included
EOF
)" "ai,backend,nutrition,tests,ux"

EPIC5="$(create_epic "5" "AI Chat REST API (Phase 5)" "$(cat <<'EOF'
## Goal

Expose authenticated backend endpoints for the frontend chat window.

## Milestone

**Milestone 3 — Chat UX** (with Phase 6).
EOF
)" "ai,backend,security")"

create_child "$EPIC5" "5" "Create AI Chat Controller" "$(cat <<EOF
Part of epic #$EPIC5.

## Proposed endpoints

\`\`\`http
POST /nutritionist/ai/chat/start
POST /nutritionist/ai/chat/message
GET /nutritionist/ai/chat/{threadId}
GET /nutritionist/ai/chat/{threadId}/drafts
POST /nutritionist/ai/drafts/{draftId}/accept
POST /nutritionist/ai/drafts/{draftId}/discard
\`\`\`

## Acceptance criteria

- [ ] All endpoints require authenticated nutritionist access
- [ ] Thread access scoped to owner/clinic
- [ ] Message endpoint calls AI orchestration service
- [ ] Draft accept/discard endpoints validate ownership
- [ ] Controller integration tests added
EOF
)" "ai,backend,security,tests"

create_child "$EPIC5" "5" "Implement AI Orchestration Service" "$(cat <<EOF
Part of epic #$EPIC5.

## Acceptance criteria

- [ ] Saves user message
- [ ] Calls OpenAI with system prompt and conversation context
- [ ] Executes allowed tools
- [ ] Enforces max tool-call limit
- [ ] Saves assistant response
- [ ] Saves tool call summaries where useful
- [ ] Handles OpenAI errors gracefully
- [ ] Unit tests with mocked AI client and tools
EOF
)" "ai,backend,integration,tests"

create_child "$EPIC5" "5" "Add Rate Limiting for AI Chat" "$(cat <<EOF
Part of epic #$EPIC5.

## Acceptance criteria

- [ ] Resilience4j rate limiter for AI chat
- [ ] Limit configurable through application properties
- [ ] Friendly error when limit exceeded
- [ ] Tests for rate-limited behavior
EOF
)" "ai,backend,security,tests"

EPIC6="$(create_epic "6" "Nutritionist AI Chat UI (Phase 6)" "$(cat <<'EOF'
## Goal

Add a chat window in the nutritionist web interface.

## Milestone

**Milestone 3 — Chat UX** (with Phase 5).
EOF
)" "ai,frontend,ux")"

create_child "$EPIC6" "6" "Add AI Chat Entry Point to Nutritionist UI" "$(cat <<EOF
Part of epic #$EPIC6.

## Acceptance criteria

- [ ] Navigation/button/link added where appropriate
- [ ] Only visible when \`AI_ENABLED=true\`
- [ ] Hidden or disabled when AI is off
- [ ] Does not expose API keys or backend configuration
- [ ] Template validation updated
EOF
)" "ai,frontend,ux,tests"

create_child "$EPIC6" "6" "Build AI Chat Window" "$(cat <<EOF
Part of epic #$EPIC6.

## Acceptance criteria

- [ ] Displays conversation messages
- [ ] Sends user messages to backend endpoint
- [ ] Shows assistant responses
- [ ] Shows loading state and errors
- [ ] Supports starting new thread
- [ ] Supports selecting existing thread if implemented
- [ ] Accessible labels and keyboard-friendly behavior
EOF
)" "ai,frontend,ux,tests"

create_child "$EPIC6" "6" "Build Draft Preview UI" "$(cat <<EOF
Part of epic #$EPIC6.

## Acceptance criteria

- [ ] Shows draft type
- [ ] Shows ingredients, meals, portions, nutrients, assumptions, warnings
- [ ] Accept and Discard actions with explicit confirmation
- [ ] Displays "AI draft, nutritionist review required"
- [ ] Template validator updated
- [ ] Controller tests added
EOF
)" "ai,frontend,ux,tests"

EPIC7="$(create_epic "7" "MCP Tool Server for Nutriconsultas (Phase 7)" "$(cat <<'EOF'
## Goal

Expose selected nutrition tools through an MCP-compatible server endpoint so the same backend tool layer can be reused by AI agents.

## Milestone

**Milestone 4 — MCP and Agent Readiness**.
EOF
)" "ai,mcp,backend,security")"

create_child "$EPIC7" "7" "Design MCP Server Endpoint" "$(cat <<EOF
Part of epic #$EPIC7.

## Proposed endpoint

\`POST /mcp/nutriconsultas\`

## Acceptance criteria

- [ ] Document transport choice
- [ ] Document authentication approach
- [ ] Document available tools
- [ ] Document read-only vs approval-required tools
- [ ] Document input/output schemas
- [ ] Do not expose patient data without authorization
EOF
)" "ai,mcp,documentation,security"

create_child "$EPIC7" "7" "Implement MCP Tool Descriptors" "$(cat <<EOF
Part of epic #$EPIC7.

## Initial tools

\`\`\`text
catalog.search_foods
catalog.get_food_nutrients
catalog.search_dishes
nutrition.calculate_recipe
nutrition.validate_menu
draft.create_dish
draft.create_menu
draft.create_diet_plan
\`\`\`

## Acceptance criteria

- [ ] Each descriptor has name, title, description, input schema
- [ ] Read-only tools marked read-only
- [ ] Draft-creation tools require app-side confirmation before final save
- [ ] Tool names stable and versionable
- [ ] Tests verify descriptor output
EOF
)" "ai,mcp,backend,tests"

create_child "$EPIC7" "7" "Implement MCP Tool Dispatch" "$(cat <<EOF
Part of epic #$EPIC7.

## Acceptance criteria

- [ ] Dispatch maps MCP tool names to Spring services
- [ ] Input validation enforced
- [ ] Auth context required
- [ ] Structured error responses
- [ ] Tool calls logged with redaction
- [ ] Tests cover success and failure
EOF
)" "ai,mcp,backend,security,tests"

create_child "$EPIC7" "7" "Add MCP Security Review" "$(cat <<EOF
Part of epic #$EPIC7.

## Acceptance criteria

- [ ] MCP endpoint requires authentication
- [ ] Tool calls scoped to authenticated nutritionist/clinic
- [ ] No global catalog or patient data leaks
- [ ] No destructive write actions exposed
- [ ] Draft creation allowed only as draft, not final save
- [ ] Security tests added
EOF
)" "ai,mcp,security,tests"

EPIC8="$(create_epic "8" "AI Safety and Observability (Phase 8)" "$(cat <<'EOF'
## Goal

Make AI usage safe, observable, auditable, and debuggable.

## Milestone

**Milestone 5 — Safety and Release** (with Phase 9 and Phase 10).
EOF
)" "ai,security,backend")"

create_child "$EPIC8" "8" "Add AI Audit Logging" "$(cat <<EOF
Part of epic #$EPIC8.

## Acceptance criteria

- [ ] Log chat request metadata
- [ ] Log tool names called
- [ ] Log draft creation and accept/discard events
- [ ] Redact API keys and sensitive content
- [ ] Tests for redaction behavior
EOF
)" "ai,security,backend,tests"

create_child "$EPIC8" "8" "Add AI Usage Metrics" "$(cat <<EOF
Part of epic #$EPIC8.

## Acceptance criteria

- [ ] Track chat messages, generated drafts, accepted drafts
- [ ] Track OpenAI errors and rate-limited requests
- [ ] Metrics do not include PHI
EOF
)" "ai,backend,tests"

create_child "$EPIC8" "8" "Add AI Error Handling UX" "$(cat <<EOF
Part of epic #$EPIC8.

## Acceptance criteria

- [ ] Handle missing API key, disabled AI, OpenAI timeout, rate limit
- [ ] Handle malformed tool response and unavailable catalog data
- [ ] UI displays helpful error without stack trace
EOF
)" "ai,frontend,ux,tests"

EPIC9="$(create_epic "9" "AI Nutrition Evaluation Suite (Phase 9)" "$(cat <<'EOF'
## Goal

Create repeatable tests and sample prompts to evaluate quality and safety.

## Milestone

**Milestone 5 — Safety and Release** (with Phase 8 and Phase 10).
EOF
)" "ai,tests,nutrition")"

create_child "$EPIC9" "9" "Add Golden Prompt Test Cases" "$(cat <<EOF
Part of epic #$EPIC9.

## Suggested cases

High-protein breakfast; low-sodium day menu; 7-day weight-loss menu; egg allergy; diabetic-friendly menu; vegetarian weekly plan; unsupported ingredient; menu missing calorie target.

## Acceptance criteria

- [ ] Test cases documented
- [ ] Expected tool usage documented
- [ ] Expected warnings documented
- [ ] Cases runnable manually or via automated tests
EOF
)" "ai,tests,nutrition,documentation"

create_child "$EPIC9" "9" "Add Structured Output Validation" "$(cat <<EOF
Part of epic #$EPIC9.

## Acceptance criteria

- [ ] Dish, menu, and diet-plan draft JSON schemas exist
- [ ] Invalid JSON rejected
- [ ] Missing required fields rejected
- [ ] Validation errors shown to nutritionist or handled gracefully
EOF
)" "ai,backend,tests,nutrition"

create_child "$EPIC9" "9" "Add End-to-End AI Draft Flow Test" "$(cat <<EOF
Part of epic #$EPIC9.

## Acceptance criteria

- [ ] Mock OpenAI response
- [ ] Mock tool calls or use test catalog data
- [ ] Send chat message; verify assistant response stored
- [ ] Verify generated draft stored
- [ ] Verify draft accept/discard
- [ ] Verify unauthorized nutritionist cannot access
EOF
)" "ai,tests,backend,security"

EPIC10="$(create_epic "10" "AI Assistant Documentation and Release (Phase 10)" "$(cat <<'EOF'
## Goal

Document setup, configuration, behavior, and rollout steps.

## Milestone

**Milestone 5 — Safety and Release** (with Phase 8 and Phase 9).
EOF
)" "ai,documentation,infrastructure")"

create_child "$EPIC10" "10" "Document Local AI Setup" "$(cat <<EOF
Part of epic #$EPIC10.

## Acceptance criteria

- [ ] Document \`OPENAI_API_KEY\`, \`OPENAI_MODEL\`, \`AI_ENABLED\`
- [ ] Document how to disable AI locally
- [ ] Warn not to commit keys
- [ ] Troubleshooting steps included
EOF
)" "ai,documentation"

create_child "$EPIC10" "10" "Document Production AI Setup" "$(cat <<EOF
Part of epic #$EPIC10.

## Acceptance criteria

- [ ] Where production key is stored (EC2 \`app.env\`, SSM scripts)
- [ ] Backend-only usage explained
- [ ] Rate limits and spend controls documented
- [ ] Rollback/disable strategy documented
EOF
)" "ai,documentation,infrastructure,security"

create_child "$EPIC10" "10" "Add Nutritionist User Guidance" "$(cat <<EOF
Part of epic #$EPIC10.

## Acceptance criteria

- [ ] AI outputs are drafts; nutritionist review required
- [ ] Examples of good prompts
- [ ] Limitations explained
- [ ] How to accept/discard drafts
EOF
)" "ai,documentation,ux,nutrition"

create_child "$EPIC10" "10" "Create AI Assistant Release Checklist" "$(cat <<EOF
Part of epic #$EPIC10.

## Checklist must include

- [ ] AI disabled by default
- [ ] API key in staging/production secret store
- [ ] Rate limiting enabled
- [ ] Audit logging enabled
- [ ] Draft approval flow tested
- [ ] Security tests passing
- [ ] Liquibase migration tested
- [ ] Rollback plan documented
- [ ] Nutritionist guidance published
EOF
)" "ai,documentation,infrastructure,security"

echo "Created issues. Output: $OUT"
cat "$OUT"
