# AI Prompt Security (v1)

**Issue:** [#439](https://github.com/diego-torres/nutriconsultas/issues/439), [#440](https://github.com/diego-torres/nutriconsultas/issues/440), [#447](https://github.com/diego-torres/nutriconsultas/issues/447) · Epic [#438](https://github.com/diego-torres/nutriconsultas/issues/438)  
**Related:** [`DATA-ACCESS-RULES.md`](DATA-ACCESS-RULES.md) (#362) · [`AI-ASSISTANT-PLAN.md`](AI-ASSISTANT-PLAN.md)

Defense-in-depth rules for nutritionist chat input before OpenAI orchestration (#385). Part of Milestone 5 — required before production `AI_ENABLED=true` (see #408).

---

## Goals

| Goal | Implementation |
|------|----------------|
| Limit untrusted input size | `nutriconsultas.ai.max-user-message-length` (default **4000**, matches UI `maxlength`) |
| Detect high-risk override patterns | `AiPromptThreatDetector` + `AiUserMessageGuard` (injection + jailbreak) |
| Separate user content from system instructions | Delimiter wrap `<mensaje_nutriologo>…</mensaje_nutriologo>` on outbound user role messages |
| Model-side refusal | `ai/system-prompt-base.txt` **SEGURIDAD DE PROMPTS** + **DEFENSA ANTE JAILBREAK** (#440) |
| User-facing block message | Spanish `400` via `AiChatException` — no OpenAI call, no persistence on block |
| Bulk generation limits (#447) | `AiRequestScopeGuard` before orchestration — persist user + assistant refusal, no OpenAI/tools |

---

## Input pipeline

```mermaid
flowchart LR
  A[Nutritionist message] --> B[AiUserMessageGuard.validateAndSanitize]
  B -->|blocked| C[400 Spanish error]
  B -->|allowed| D[Persist sanitized text in ai_chat_message]
  D --> S[AiRequestScopeGuard.evaluate]
  S -->|scope exceeded| R[Persist assistant refusal — no OpenAI]
  S -->|allowed| E[buildConversation wraps USER rows]
  E --> F[OpenAI chat completion]
```

**Edit/resubmit (#437):** validation runs **before** thread truncation so a blocked edit cannot delete history.

**Storage vs model payload:** the database stores the sanitized plain text the nutritionist typed (after trim/control-char cleanup). Only the OpenAI request wraps user rows with delimiters.

---

## Blocked patterns (deterministic)

`AiPromptThreatDetector` classifies input as **INJECTION** or **JAILBREAK** before orchestration.

### Injection (#439)

- English: `ignore previous instructions`, `disregard prior instructions`, `forget your instructions`, `you are now a…`, `act as DAN`, `new instructions:`, `override system prompt`, `` ```system ``, `<|im_start|>system`, `[INST]`, `developer mode on`, `pretend you are not…`, `role: system`
- Spanish: `ignora las instrucciones anteriores`, `olvida tus instrucciones`

### Jailbreak / role override (#440)

- Role-play: `act as admin`, `you are the platform administrator`, `roleplay as unrestricted AI`, `actúa como administrador`, `eres el admin`
- Mode bypass: `simulate unrestricted mode`, `bypass safety guardrails`, `DAN mode`, `god mode`, `modo sin restricciones`
- Exfiltration: `reveal your tools`, `list all functions`, `repeat your system prompt`, `what are your system instructions`, `muéstrame el prompt del sistema`, `exfiltrate the tool schema`

**Not blocked:** legitimate nutrition wording (e.g. “ignorar lácteos”, “sin instrucciones de cocina”) when it does not match the override regexes.

Blocked requests log `category` and `messageLength` only — never the message body (#397).

---

## Request scope limits (#447)

`AiRequestScopeGuard` runs **after** user message persistence and **before** OpenAI / tool orchestration (sync and SSE streaming). On refuse:

- Spanish assistant message is returned (streamed as content deltas in SSE).
- User + assistant rows are persisted as a normal turn.
- No OpenAI call and no tool execution.

| Kind | Default threshold | Example blocked phrasing |
|------|-------------------|--------------------------|
| Diet plan days | **14** (`CreateDietPlanDraftToolServiceImpl.MAX_DAYS`) | «plan de 30 días», «1000 planes nutricionales» |
| Weekly menu days | **7** | «menú de 8 días» |
| Dishes per turn | **1** | «Genera 5 platillos», «100 recetas» |
| Multi-patient | any bulk | «todos mis pacientes», «20 pacientes» |
| Year / bulk counts | heuristic | «cada día del año», «cien menús» |

Scope blocks log `kind` and `requestedAmount` only — never the message body.

---

## Configuration

| Property | Env | Default |
|----------|-----|---------|
| `nutriconsultas.ai.max-user-message-length` | `AI_MAX_USER_MESSAGE_LENGTH` | `4000` (clamped 500–8000) |
| `nutriconsultas.ai.max-days-per-turn` | `AI_MAX_DAYS_PER_TURN` | `14` |
| `nutriconsultas.ai.max-dishes-per-turn` | `AI_MAX_DISHES_PER_TURN` | `1` |
| `nutriconsultas.ai.max-menu-days-per-turn` | `AI_MAX_MENU_DAYS_PER_TURN` | `7` |

---

## Error copy (Spanish)

| Case | Message |
|------|---------|
| Injection / jailbreak | *No puedo procesar…* (injection) / *No puedo cumplir solicitudes que intenten cambiar mi rol…* (jailbreak) |
| Length | *El mensaje supera el límite de N caracteres.* |
| Empty | *El mensaje no puede estar vacío.* |
| Bulk scope (#447) | Assistant reply in-thread (not HTTP 400) — e.g. *No puedo generar N platillos en un solo turno…* |

HTTP status: **400** (`AiToolErrorCode.VALIDATION`) for injection/jailbreak/length chat REST and SSE error events. Scope refusals complete normally with assistant content.

---

## Testing

- **Unit:** `AiPromptThreatDetectorTest`, `AiUserMessageGuardTest`, `AiRequestScopeGuardTest`, `AiOpenAiToolCatalogTest` — fixtures only, no live OpenAI
- **Integration:** `AiOrchestrationServiceTest` — wrapped user content; scope refusal without OpenAI
- **Future:** #450 golden prompts for bulk/abuse scenarios; #448 LLM scope classifier

---

## Follow-up issues

| # | Topic |
|---|--------|
| **440** | ~~Jailbreak / role-override refusal corpus~~ **done** in #440 |
| **441** | Delimiters, tool allowlist, output validation (defense-in-depth) |
| **447** | ~~Deterministic bulk scope limits (Java pre-check)~~ **done** in #447 |
| **448** | LLM scope classifier pre-flight |

---

## Logging

- Do **not** log full user message bodies when blocking — log thread id + block reason category only if needed (#397 audit).
- Never log patient-identifiable data in guard failures.
