# MCP Server Endpoint Design (#392)

**Issue:** [#392](https://github.com/diego-torres/nutriconsultas/issues/392) · Epic [#391](https://github.com/diego-torres/nutriconsultas/issues/391)  
**Related:** [`TOOL-CONTRACT.md`](TOOL-CONTRACT.md) (#363) · [`DATA-ACCESS-RULES.md`](DATA-ACCESS-RULES.md) (#362) · [`AI-ASSISTANT-PLAN.md`](AI-ASSISTANT-PLAN.md)

Design for exposing the existing nutrition tool layer through an **MCP-compatible HTTP endpoint** so external AI agents can reuse the same Spring services as the in-app OpenAI orchestrator (#385).

**Implementation status:** **Shipped** — #393 `McpToolDescriptorCatalog`, #394 `McpNutriconsultasController` + `McpToolDispatchService`, #395 security tests.

---

## Goals

| Goal | Detail |
|------|--------|
| **Reuse tool layer** | MCP `tools/call` delegates to the same services as `AiOrchestrationToolDispatcher` — no duplicate business logic or alternate tenant checks. |
| **Same security bar** | OAuth2 nutritionist session, plan gating (#409), feature flag, rate limits, and PHI rules apply to every MCP call. |
| **Stable tool names** | Dot-separated MCP identifiers (`catalog.search_foods`) version independently from internal OpenAI snake_case names (`search_food_catalog`). |
| **Draft safety** | Draft tools persist `ai_generated_draft` only; final catalog/patient writes still require nutritionist accept in the web UI (#382). |

---

## Endpoint

| Property | Value |
|----------|-------|
| **URL** | `POST /mcp/nutriconsultas` |
| **Base path** | Same origin as the Spring Boot app (port 3000 local, production host behind ALB) |
| **Content-Type** | `application/json` (JSON-RPC 2.0 body) |
| **Session** | Cookie-based OAuth2 session (same as `/admin/**` and `/rest/**`) |

Future versions may add a version prefix (`/mcp/v1/nutriconsultas`) when breaking descriptor or auth changes ship. v1 uses the path above.

---

## Transport choice

### Selected: **Streamable HTTP** (MCP remote server profile)

Nutriconsultas is a **remote HTTP server**, not a local subprocess. The MCP server profile that fits Spring Boot is:

1. **Primary (v1):** [Streamable HTTP transport](https://modelcontextprotocol.io/specification/2025-03-26/basic/transports#streamable-http) — single `POST` endpoint accepts JSON-RPC 2.0 messages (`initialize`, `tools/list`, `tools/call`, `ping`).
2. **Response mode:** Synchronous JSON response for v1 tool calls (typical `tools/call` completes in &lt; 30 s). Long-running work stays in the existing AI chat SSE path (#435), not MCP v1.
3. **Not in v1:** `stdio` transport (desktop/local agent hosts), WebSocket-only transports, or a separate MCP port.

### Why not duplicate REST chat?

| Concern | Chat REST/SSE (#384, #435) | MCP (#392–#394) |
|---------|---------------------------|-----------------|
| **Consumer** | Minutriporcion Thymeleaf UI | External agent hosts (Cursor, Claude Desktop, custom agents) |
| **Protocol** | Custom JSON + SSE | MCP JSON-RPC tool surface |
| **OpenAI** | Server calls OpenAI with tool loop | Agent brings its own model; server exposes tools only |

Both paths call the **same** Spring tool services after auth context is established.

### JSON-RPC envelope (v1)

**Request:**

```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "method": "tools/call",
  "params": {
    "name": "catalog.search_foods",
    "arguments": {
      "query": "avena",
      "limit": 10
    }
  }
}
```

**Success response:**

```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "result": {
    "content": [
      {
        "type": "text",
        "text": "{\"success\":true,\"data\":{\"items\":[],\"totalReturned\":0}}"
      }
    ],
    "isError": false
  }
}
```

**Error response** (tool or protocol failure):

```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "result": {
    "content": [
      {
        "type": "text",
        "text": "{\"success\":false,\"errorCode\":\"NOT_FOUND\",\"message\":\"No se encontró el alimento solicitado.\"}"
      }
    ],
    "isError": true
  }
}
```

Tool results use the **same envelope** as [`TOOL-CONTRACT.md`](TOOL-CONTRACT.md#common-response-envelope). MCP wraps that JSON as a `text` content block so agents parse one consistent structure.

Protocol-level failures (malformed JSON-RPC, unknown method) return standard JSON-RPC `error` objects with HTTP **400** / **404** and **no stack traces** in the body.

---

## Authentication approach

### v1 — OAuth2 session (nutritionist web login)

| Check | Mechanism |
|-------|-----------|
| **Identity** | Spring Security OAuth2 login (Auth0). MCP requests include the **same session cookie** as `/rest/nutritionist/ai/**`. |
| **Principal** | `OidcUser.getSubject()` → `nutritionistId` injected server-side on every tool dispatch. |
| **Unauthenticated** | HTTP **401** JSON-RPC error; Spanish message: *«Sesión no válida.»* |
| **CSRF** | MCP is under `/mcp/**` and requires authentication; CSRF policy follows existing `/rest/**` rules (CSRF disabled for API-style endpoints in `SecurityConfig`). |

MCP **does not** accept `nutritionistId` from the client arguments or headers. Tenant identity always comes from the server session.

### Authorization gates (after auth)

Same pre-checks as chat orchestration ([`DATA-ACCESS-RULES.md`](DATA-ACCESS-RULES.md#authentication-and-plan-gating)):

| Gate | Failure |
|------|---------|
| `AI_ENABLED=true` | **503** — Spanish misconfiguration message |
| `Entitlement.AI_ASSISTANT` (Plus + Consultorio, #409) | **403** — subscription upgrade message |
| Per-nutritionist rate limit (#386) | **429** — Spanish rate-limit message |

### Patient context (optional)

Patient data is **never** exposed through MCP tool arguments by default.

| Rule | Detail |
|------|--------|
| **No patient search tool** | MCP v1 does not offer `search_patients` or clinical record reads ([`TOOL-CONTRACT.md` explicit non-tools](TOOL-CONTRACT.md#explicit-non-tools-v1)). |
| **Thread-scoped context** | When #394 wires MCP to chat threads, `patientContext` loads only if the caller supplies a valid `threadId` owned by the session nutritionist **and** the thread has an owned `patient_id` ([`DATA-ACCESS-RULES.md`](DATA-ACCESS-RULES.md#patient-context-access-rules)). |
| **PHI minimization** | Allowed patient fields match `AiPatientContext` — numeric targets, pathology booleans, truncated allergies. Names, email, phone, and clinical narrative text are **denied**. |
| **Cross-tenant** | Wrong `threadId` / `patientId` → **404** `NOT_FOUND` (IDOR-safe). |

External agents must not receive patient identifiers unless the nutritionist has already bound a patient to an AI thread through the authorized web UI.

### Deferred (post-v1)

| Approach | When |
|----------|------|
| **OAuth2 client-credentials / M2M JWT** | Separate issue if headless agents need non-cookie auth |
| **Per-agent API keys** | Not planned for v1 — would duplicate Auth0 and complicate revocation |

---

## MCP session context (#394)

Tool dispatch requires server-injected context (not part of MCP tool JSON Schema):

| Field | Source | Required |
|-------|--------|----------|
| `nutritionistId` | OAuth `sub` | always |
| `threadId` | MCP session metadata or `tools/call` `_meta.threadId` | required for **draft** tools; recommended for read tools when patient-aware validation is needed |

**v1 recommendation:** MCP clients call `initialize` with optional metadata:

```json
{
  "jsonrpc": "2.0",
  "id": 0,
  "method": "initialize",
  "params": {
    "protocolVersion": "2025-03-26",
    "capabilities": {},
    "clientInfo": { "name": "example-agent", "version": "1.0.0" },
    "_meta": {
      "threadId": 42
    }
  }
}
```

If `threadId` is omitted, **read-only** catalog tools still work. **Draft** tools return `VALIDATION` — *«Se requiere una conversación activa para crear borradores.»*

Creating a thread remains a **web or REST chat** operation (`POST /rest/nutritionist/ai/chat/start`); MCP v1 does not add thread-management tools.

---

## Available tools (v1)

Eight tools match the **current** OpenAI orchestrator implementation (`AiOpenAiToolCatalog` / `AiOrchestrationToolDispatcher`). MCP names are stable dot-separated identifiers for #393.

### Read-only tools

Safe to call without nutritionist confirmation in the UI. They query catalog or compute nutrients; **no durable writes**.

| MCP name | Internal name | Purpose |
|----------|---------------|---------|
| `catalog.search_foods` | `search_food_catalog` | Search authorized food catalog |
| `catalog.get_food_nutrients` | `get_food_nutrients` | Nutrients for one food ID |
| `catalog.search_dishes` | `search_dish_catalog` | Search authorized platillos |
| `nutrition.calculate_recipe` | `calculate_recipe_nutrients` | Sum nutrients for ingredient list |
| `nutrition.validate_plan` | `validate_plan_constraints` | Check menu/plan against kcal/macro targets |

### Draft-creation tools (approval required before final save)

These **write** `ai_generated_draft` rows linked to the MCP session `threadId`. They do **not** create final `Platillo`, `Dieta`, or patient assignments.

| MCP name | Internal name | Purpose |
|----------|---------------|---------|
| `draft.create_dish` | `create_dish_draft` | Persist platillo borrador |
| `draft.create_menu` | `create_menu_draft` | Persist menú borrador |
| `draft.create_diet_plan` | `create_diet_plan_draft` | Persist plan alimentario borrador |

**App-side confirmation:** Nutritionist must **Aceptar** or **Descartar** in `/admin/ai` (#390, #382). MCP agents must surface the label *«Borrador IA — revisión del nutriólogo requerida»* when presenting draft results.

### Documented but not in MCP v1

These appear in [`TOOL-CONTRACT.md`](TOOL-CONTRACT.md) but are **not** registered in `AiOpenAiToolCatalog` yet. Add to MCP when backend handlers ship:

| Internal name | Planned MCP name |
|---------------|------------------|
| `get_dish_recipe` | `catalog.get_dish_recipe` |
| `get_diet_templates` | `catalog.get_diet_templates` |

### Explicitly excluded

Same forbidden surface as OpenAI orchestration ([`TOOL-CONTRACT.md` — explicit non-tools](TOOL-CONTRACT.md#explicit-non-tools-v1)): patient search, clinical records, catalog delete/update, direct patient diet assignment, mobile messaging.

---

## Input and output schemas

MCP tool descriptors (#393) expose JSON Schema for each tool’s **arguments**. Results use the shared tool envelope documented in [`TOOL-CONTRACT.md`](TOOL-CONTRACT.md).

| Topic | Reference |
|-------|-----------|
| Per-tool input/output JSON Schema | [`TOOL-CONTRACT.md`](TOOL-CONTRACT.md) sections per internal tool name |
| Shared types (`NutrientSummary`, `RecipeIngredientInput`, …) | [`TOOL-CONTRACT.md` — Shared types](TOOL-CONTRACT.md#shared-types) |
| Draft payload shapes | [`TOOL-CONTRACT.md`](TOOL-CONTRACT.md) + [`DRAFT-SCHEMA-VALIDATION.md`](DRAFT-SCHEMA-VALIDATION.md) |
| Error codes | [`TOOL-CONTRACT.md` — Common response envelope](TOOL-CONTRACT.md#common-response-envelope) |

**Schema policy for MCP (#393):**

- Property names stay **English** (same as OpenAI function schemas).
- Titles and descriptions in descriptors use **Spanish (es-MX)** for agent UX parity with the web assistant.
- `additionalProperties: false` on all tool inputs.
- Read-only tools include `"annotations": { "readOnlyHint": true }` in MCP descriptors where the host supports hints.

**Mapping rule:** MCP argument objects match OpenAI tool arguments **field-for-field**; only the tool **name** changes (e.g. `catalog.search_foods` arguments = `search_food_catalog` arguments in `TOOL-CONTRACT.md`).

---

## Security configuration (Spring)

Implemented in `SecurityConfig` (#394):

```java
.requestMatchers("/mcp/**").authenticated()
```

`McpNutriconsultasController` + `McpToolDispatchService`:

1. Resolve `OidcUser` from the security context (reject unauthenticated with OAuth redirect or JSON-RPC 401 when principal is null).
2. Assert `AiProperties.isEnabled()` and subscription `AI_ASSISTANT` via `AiChatRequestGuards`.
3. Build `AiOrchestrationContext` from session + optional `_meta.threadId` (required for draft tools).
4. Map MCP name → internal name → `AiOrchestrationToolDispatcher`.
5. Log via `AiAuditLogger.logMcpToolCall` — tool names + redacted nutritionist ID only at INFO (#395).

No alternate code paths that bypass `AiToolAllowlist`, `AiDraftToolSchemaValidator`, or tenant checks (#441, #402).

---

## Rate limiting and observability

| Control | Behavior |
|---------|----------|
| **App rate limit** | MCP `tools/call` does **not** use `AiChatRateLimiter` in v1 — only REST chat messages are rate-limited (#386). Add a dedicated MCP limiter in a follow-up issue if agent traffic needs caps. |
| **Max tool calls** | Bulk scope guard `nutriconsultas.ai.max-tool-calls` applies per agent turn when MCP is used inside a hosted orchestration loop. |
| **Metrics** | MCP tool calls audited via `AiAuditLogger.logMcpToolCall`; distinct Micrometer `source=mcp` tag deferred. |
| **Audit** | `AiAuditLogger.logMcpToolCall` — MCP + internal tool names, redacted nutritionist ID, no argument JSON at INFO (#395). |

---

## Client integration sketch

1. Nutritionist logs into Minutriporcion in a browser (or agent reuses exported session — **discouraged**; prefer future M2M issue).
2. Agent starts or reuses an AI chat thread via REST (`/rest/nutritionist/ai/chat/start`) → obtains `threadId`.
3. Agent `POST /mcp/nutriconsultas` with `initialize` + `_meta.threadId`.
4. Agent `tools/list` → receives eight descriptors (#393).
5. Agent `tools/call` → receives JSON envelope in `content[0].text`.
6. For drafts, nutritionist reviews in web UI; agent polls `GET /rest/nutritionist/ai/chat/{threadId}/drafts` if needed.

---

## Testing expectations (#395)

| Area | Tests | Class |
|------|-------|-------|
| **Auth** | Unauthenticated denied; wrong plan → 403 JSON-RPC; `AI_ENABLED=false` → 503 | `McpNutriconsultasSecurityIntegrationTest`, `McpAiDisabledSecurityIntegrationTest` |
| **Tenant** | User A cannot pass user B’s `threadId` | `McpNutriconsultasSecurityIntegrationTest`, `McpToolDispatchServiceTest` |
| **Descriptors** | Eight tools, schemas, read-only vs draft | `McpToolDescriptorCatalogTest`, `McpSecurityReviewTest` |
| **Dispatch** | Each MCP name maps to correct service; unknown name → error | `McpToolDispatchServiceTest` |
| **Draft** | Draft tools without `threadId` → validation error | `McpToolDispatchServiceTest`, `McpSecurityReviewTest` |
| **Redaction** | Logs contain no full user id or argument JSON at INFO | `AiAuditLoggerTest`, `McpSecurityReviewTest` |

---

## Acceptance checklist (#392)

- [x] Document transport choice (Streamable HTTP, JSON-RPC 2.0)
- [x] Document authentication approach (OAuth2 session, server-injected `nutritionistId`)
- [x] Document available tools (eight v1 tools + MCP name mapping)
- [x] Document read-only vs approval-required (draft) tools
- [x] Document input/output schemas (reference `TOOL-CONTRACT.md`)
- [x] Document rule: no patient data without authorization (thread + ownership + PHI allow list)

---

## Related documents

| Doc | Issue |
|-----|-------|
| [`TOOL-CONTRACT.md`](TOOL-CONTRACT.md) | Tool schemas and MCP name table |
| [`DATA-ACCESS-RULES.md`](DATA-ACCESS-RULES.md) | PHI and tenant rules |
| [`PROMPT-SECURITY.md`](PROMPT-SECURITY.md) | Not applicable to MCP tool-only path, but agents should not bypass scope |
| [`RELEASE-CHECKLIST.md`](RELEASE-CHECKLIST.md) | MCP optional for initial chat rollout; required for external agent integrations |
