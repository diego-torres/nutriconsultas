# Local AI setup (#405)

**Issue:** [#405](https://github.com/diego-torres/nutriconsultas/issues/405) · Epic [#404](https://github.com/diego-torres/nutriconsultas/issues/404)  
**Production counterpart:** [`PRODUCTION-AI-SETUP.md`](PRODUCTION-AI-SETUP.md) (#406)

How to enable or disable the **AI Nutrition Assistant** on a developer machine. OpenAI credentials stay **server-side only** — never in Git, the browser, or logs.

---

## Prerequisites

| Requirement | Notes |
|-------------|--------|
| Java **21** | Same as the rest of the app |
| PostgreSQL | `./dev-start.sh` starts Podman Postgres and runs the app |
| Auth0 login | Nutritionist OAuth2 session (same as other `/admin/**` pages) |
| OpenAI account | API key with access to your chosen chat model |

---

## Quick start (enable AI)

1. Copy the env template if you do not have a local `.env` yet:

   ```bash
   cp .env.example .env
   ```

2. Edit **`.env`** (gitignored — see [Never commit keys](#never-commit-keys)) and set at minimum:

   ```bash
   AI_ENABLED=true
   OPENAI_API_KEY=sk-proj-your_key_here
   OPENAI_MODEL=gpt-5-mini
   ```

   `OPENAI_MODEL` must match a model your API key can call (e.g. `gpt-5-mini`, `gpt-4o`). The app does **not** default a model when the variable is empty.

3. Start the stack:

   ```bash
   ./dev-start.sh
   ```

   The script loads `.env` and exports variables before `mvn spring-boot:run`.

4. Confirm startup logs (no API key in output):

   - **OK:** `AI assistant enabled (model=gpt-5-mini, openai.store=false, maxToolCalls=8)`
   - **Misconfigured:** `AI assistant is enabled (AI_ENABLED=true) but OpenAI is not fully configured (OPENAI_API_KEY or OPENAI_MODEL missing)...`

5. Open **`http://localhost:3000/admin/ai`** after logging in as a nutritionist.

---

## Environment variables

Spring maps env vars in `application.properties` under `nutriconsultas.ai.*`.

### Required to run AI locally

| Variable | Spring property | Purpose |
|----------|-----------------|---------|
| `AI_ENABLED` | `nutriconsultas.ai.enabled` | Feature flag. Default **`false`**. Set `true` to expose `/admin/ai` and REST chat endpoints. |
| `OPENAI_API_KEY` | `nutriconsultas.ai.openai.api-key` | Server secret for OpenAI HTTP calls. |
| `OPENAI_MODEL` | `nutriconsultas.ai.openai.model` | Model id sent on every completion (e.g. `gpt-5-mini`). |

**Legacy alias:** `OPEN_API_KEY` is accepted if `OPENAI_API_KEY` is unset (`OPENAI_API_KEY` takes precedence).

### Recommended defaults

| Variable | Default | Purpose |
|----------|---------|---------|
| `OPENAI_STORE` | `false` | Do not enable provider-side conversation retention without legal review ([`DATA-ACCESS-RULES.md`](DATA-ACCESS-RULES.md)). |
| `OPENAI_BASE_URL` | `https://api.openai.com` | Override only for proxies or compatible APIs. |

### Optional tuning

| Variable | Default | Purpose |
|----------|---------|---------|
| `OPENAI_CONNECT_TIMEOUT_MS` | `5000` | HTTP connect timeout |
| `OPENAI_READ_TIMEOUT_MS` | `120000` | HTTP read timeout (long tool loops) |
| `AI_MAX_TOOL_CALLS` | `8` | Max tool round-trips per user message |
| `AI_MAX_USER_MESSAGE_LENGTH` | `4000` | User input cap (matches UI `maxlength`) |
| `AI_MAX_DAYS_PER_TURN` | `14` | Diet-plan day cap per turn |
| `AI_MAX_DISHES_PER_TURN` | `1` | Dish drafts per turn |
| `AI_MAX_MENU_DAYS_PER_TURN` | `7` | Menu day cap per turn |
| `AI_SCOPE_CLASSIFIER_ENABLED` | `true` | LLM pre-flight scope classifier (#448) |
| `AI_SCOPE_CLASSIFIER_MAX_TOKENS` | `200` | Classifier completion budget |

See also [`PROMPT-SECURITY.md`](PROMPT-SECURITY.md) for guardrail-related limits.

---

## Disable AI locally

Use any of these — **`AI_ENABLED=false` is enough** for most developers:

| Approach | Effect |
|----------|--------|
| `AI_ENABLED=false` (or omit / comment out) | Default. `/admin/ai` returns **404**; sidebar AI entry hidden; REST chat unavailable. |
| Omit `OPENAI_API_KEY` / `OPENAI_MODEL` with `AI_ENABLED=true` | Feature “on” but **not operational** — REST returns **503** with Spanish misconfiguration message; startup **WARN** in logs. |
| Tests | `application-test.properties` sets `nutriconsultas.ai.enabled=false` unless a test overrides it. |

Restart the app after changing `.env`.

---

## Never commit keys

| Rule | Detail |
|------|--------|
| **`.env` is gitignored** | Listed in `.gitignore` — keep secrets only there or in your shell profile. |
| **Use `.env.example`** | Commit placeholder names and comments, never real `sk-…` values. |
| **No keys in code or docs** | Do not paste live keys into issues, PRs, Slack, or Cursor chats. |
| **Rotate if exposed** | Revoke the key in the OpenAI dashboard and issue a new one. |
| **Separate test keys** | Prefer a project-scoped key with usage limits for local dev. |

Startup logging intentionally **never** prints the API key (`AiConfigTest` guards this).

---

## Verify the integration

### UI

1. Log in as a nutritionist.
2. Sidebar → **Asistente IA** (visible only when `AI_ENABLED=true`).
3. Send a short prompt in Spanish, e.g. *“Busca avena en mi catálogo.”*
4. Expect a streamed or JSON reply; draft tools show **Borrador IA** labels before accept.

### REST (optional)

With session cookie from browser login:

```bash
curl -s -X POST http://localhost:3000/rest/nutritionist/ai/chat/start \
  -H 'Content-Type: application/json' \
  -d '{"title":"Prueba local"}' \
  --cookie 'JSESSIONID=...'
```

### Automated tests (no live OpenAI)

```bash
mvn test -Dtest=AiDraftFlowIntegrationTest,AiNutritionGoldenPromptTest
```

Golden and E2E tests mock OpenAI; they do not require `OPENAI_API_KEY` in `.env`.

---

## Troubleshooting

| Symptom | Likely cause | Fix |
|---------|--------------|-----|
| `/admin/ai` → 404 | `AI_ENABLED=false` | Set `AI_ENABLED=true` and restart. |
| Chat → 503 “no está disponible” | Missing key or model | Set `OPENAI_API_KEY` and `OPENAI_MODEL`; check startup WARN. |
| Startup WARN about misconfiguration | Same as above | Fill both variables; alias `OPEN_API_KEY` only if you omit `OPENAI_API_KEY`. |
| OpenAI 401 / invalid API key | Wrong or revoked key | Regenerate key; update `.env`; restart. |
| Model not found / 404 from OpenAI | Typo in `OPENAI_MODEL` | Use a model id valid for your account. |
| Rate limit / 429 | OpenAI or app limiter | Wait and retry; app message: *“El servicio de IA está saturado…”* |
| Empty sidebar link | Feature flag off | `AI_ENABLED=true` + restart. |
| Changes ignored | Env not loaded | Use `./dev-start.sh` or `export $(grep -v '^#' .env \| xargs)` before `mvn spring-boot:run`. |
| `.env` not found warning | No file in repo root | `cp .env.example .env` |

For evaluation scenarios without live API, see [`NUTRITION-GOLDEN-PROMPTS.md`](NUTRITION-GOLDEN-PROMPTS.md).

---

## Related documents

| Doc | Topic |
|-----|--------|
| [`AI-ASSISTANT-PLAN.md`](AI-ASSISTANT-PLAN.md) | Architecture and milestones |
| [`DATA-ACCESS-RULES.md`](DATA-ACCESS-RULES.md) | PHI, logging, `OPENAI_STORE` |
| [`FUNCTIONAL-SCOPE.md`](FUNCTIONAL-SCOPE.md) | Supported workflows |
| [`PRODUCTION-AI-SETUP.md`](PRODUCTION-AI-SETUP.md) | Production EC2, SSM, spend controls (#406) |

**Note:** Plan-tier gating (`Entitlement.AI_ASSISTANT`, #409) is not required for basic local enablement today; production rollout must follow #408 and #409.
