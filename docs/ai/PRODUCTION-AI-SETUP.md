# Production AI setup (#406)

**Issue:** [#406](https://github.com/diego-torres/nutriconsultas/issues/406) · Epic [#404](https://github.com/diego-torres/nutriconsultas/issues/404)  
**Local counterpart:** [`LOCAL-AI-SETUP.md`](LOCAL-AI-SETUP.md) (#405)

How to configure the **AI Nutrition Assistant** on the production **app EC2** host. OpenAI credentials are **server-only** — loaded from `/opt/nutriconsultas/app.env` via systemd, never exposed to browsers or Git.

**Before enabling in production:** complete release checklist **#408**, prompt security epic **#438**, and plan gating **#409** (see [Pre-production gates](#pre-production-gates)).

---

## Where secrets live

| Location | Purpose |
|----------|---------|
| **`/opt/nutriconsultas/app.env`** | Runtime env file on the app EC2 instance (`root:nutri`, mode `640`). Spring reads `AI_*` / `OPENAI_*` via `EnvironmentFile` in `nutriconsultas.service`. |
| **Terraform first boot** | [`infrastructure/templates/app.user_data.sh`](../../infrastructure/templates/app.user_data.sh) writes JDBC, Auth0, Stripe, etc. **OpenAI vars are not in Terraform today** — add them on brownfield hosts via SSM (below). |
| **SSM Parameter Store** | Deploy metadata only: `/{project}/deploy/app_instance_id` (used by scripts to target the app instance). **Do not** store OpenAI API keys in SSM Parameter Store unless you adopt a dedicated secrets workflow; prefer `app.env` + rotation via script. |
| **Git / CI** | **Never** commit `OPENAI_API_KEY`, `terraform.tfvars` secrets, or pipeline env with live keys. |

Shell access: **SSM Session Manager only** (no public SSH). See [`infrastructure/README.md`](../../infrastructure/README.md#connect-to-the-ec2-instances-ssm-session-manager-only).

---

## Backend-only usage

The OpenAI API key must **only** exist in server process memory loaded from `app.env`:

| Layer | Behavior |
|-------|----------|
| **`OpenAiClientServiceImpl`** | Sends `Authorization: Bearer …` on server-side HTTP to OpenAI. |
| **Thymeleaf / JS** | `AiEnabledModelAdvice` exposes **`aiEnabled` boolean only** — never the key or model config. |
| **REST JSON** | Chat responses return assistant text and draft metadata — no OpenAI credentials. |
| **Logs** | `AiConfig` logs model name and flags, **not** the API key (`AiConfigTest` enforces this). |
| **Outbound redaction** | `AiAssistantOutputValidator` strips accidental `sk-…` patterns from model output. |

See [`DATA-ACCESS-RULES.md`](DATA-ACCESS-RULES.md) for PHI and `OPENAI_STORE=false` (default).

---

## Configure production (recommended)

Use the SSM helper script (same pattern as Stripe / reCAPTCHA / patient broker):

```bash
export AWS_PROFILE=minutriporcion AWS_DEFAULT_REGION=us-east-1
export AI_ENABLED=true
export OPENAI_API_KEY='sk-proj-...'
export OPENAI_MODEL='gpt-5-mini'
# Optional:
# export OPENAI_STORE=false
# export AI_CHAT_MESSAGE_RATE_LIMIT=20
# export AI_CHAT_MESSAGE_RATE_WINDOW=1h
bash infrastructure/scripts/ssm-update-ai-openai.sh
```

The script:

1. Resolves the app instance from `/{project}/deploy/app_instance_id`.
2. Upserts vars in `/opt/nutriconsultas/app.env` (secrets base64-encoded in the SSM payload — not echoed in command history on the laptop beyond your shell).
3. Restarts `nutriconsultas` and waits for `/actuator/health`.
4. Prints an **audit line** (`OPENAI_API_KEY=SET(len=…)`, never the value).

### Manual SSM (alternative)

1. Start a session: `aws ssm start-session --target <app_instance_id>`
2. Edit as root: `sudo vi /opt/nutriconsultas/app.env`
3. Add or update:

   ```bash
   AI_ENABLED=true
   OPENAI_API_KEY=sk-proj-...
   OPENAI_MODEL=gpt-5-mini
   OPENAI_STORE=false
   ```

4. `sudo systemctl restart nutriconsultas`
5. `curl -sf http://127.0.0.1:3000/actuator/health`

---

## Environment variables (production)

Same Spring mapping as local — see [`LOCAL-AI-SETUP.md`](LOCAL-AI-SETUP.md#environment-variables).

| Variable | Production notes |
|----------|------------------|
| `AI_ENABLED` | **`false` by default** in `application.properties`. Must be `true` for `/admin/ai`. |
| `OPENAI_API_KEY` | Production project key; rotate via OpenAI dashboard + SSM script. |
| `OPENAI_MODEL` | Choose cost/latency-appropriate model; validate in staging first. |
| `OPENAI_STORE` | Keep **`false`** unless legal approves provider retention. |
| `AI_CHAT_MESSAGE_RATE_LIMIT` | App rate limit per nutritionist (default **20**). |
| `AI_CHAT_MESSAGE_RATE_WINDOW` | Window for above (default **`1h`**). Resilience4j duration format. |
| `AI_MAX_TOOL_CALLS` | Max tool round-trips per message (default **8**). |

---

## Rate limits and spend controls

Defense is **layered** — use both OpenAI account controls and app limits.

### OpenAI account (operator)

| Control | Recommendation |
|---------|----------------|
| **Usage limits / budget alerts** | Set monthly budget and email alerts in [OpenAI billing](https://platform.openai.com/settings/organization/billing). |
| **Project-scoped API keys** | Separate prod vs dev keys; revoke compromised keys immediately. |
| **Model choice** | Prefer smaller/faster models for staging; promote model id to prod after smoke tests. |

### Application (automatic)

| Mechanism | Default | Config |
|-----------|---------|--------|
| **Per-nutritionist message limit** | 20 messages / hour | `AI_CHAT_MESSAGE_RATE_LIMIT`, `AI_CHAT_MESSAGE_RATE_WINDOW` |
| **Tool calls per turn** | 8 max | `AI_MAX_TOOL_CALLS` |
| **Bulk scope guard** | Refuses excessive batch requests | `AI_MAX_DAYS_PER_TURN`, `AI_MAX_DISHES_PER_TURN`, `AI_MAX_MENU_DAYS_PER_TURN` |
| **Scope classifier** | Optional LLM pre-flight | `AI_SCOPE_CLASSIFIER_ENABLED` (default `true`) |
| **User message length** | 4000 chars | `AI_MAX_USER_MESSAGE_LENGTH` |

User-facing rate limit message (Spanish): *“Has alcanzado el límite de mensajes del asistente de IA…”* (`AiChatRateLimiter`).

OpenAI 429 responses map to a separate Spanish saturation message from `OpenAiClientService`.

---

## Rollback and disable

| Scenario | Action | User impact |
|----------|--------|-------------|
| **Emergency off** | `AI_ENABLED=false` via SSM script or manual `app.env` edit + `systemctl restart nutriconsultas` | `/admin/ai` → 404; sidebar link hidden; no OpenAI traffic |
| **Misconfigured enable** | Leave `AI_ENABLED=true` but remove key/model | 503 Spanish message; startup WARN in journal |
| **Key compromise** | Revoke key in OpenAI; deploy new key via SSM script; restart | Brief downtime during restart |
| **Bad model deploy** | Set previous `OPENAI_MODEL` in `app.env`; restart | No JAR redeploy needed |
| **Code rollback** | Redeploy previous JAR via CodePipeline / `deploy-jar-to-ec2-ssm.sh` | `app.env` unchanged — AI flag independent of JAR version |

**Disable without removing secrets** (fast re-enable):

```bash
export AI_ENABLED=false
# OPENAI_API_KEY and OPENAI_MODEL may remain in app.env
bash infrastructure/scripts/ssm-update-ai-openai.sh
```

---

## Verify production

1. **Journal:** `sudo journalctl -u nutriconsultas -n 100 | grep -i 'AI assistant'`
   - Success: `AI assistant enabled (model=…, openai.store=false, …)`
   - Misconfigured: WARN about missing `OPENAI_API_KEY` or `OPENAI_MODEL`
2. **Health:** `curl -sf https://minutriporcion.com/actuator/health` (or localhost from SSM).
3. **UI:** Log in as nutritionist → **Asistente IA** → short Spanish prompt.
4. **No key leakage:** Confirm browser devtools network responses and page source contain no `sk-` strings.

---

## Pre-production gates

Do **not** set `AI_ENABLED=true` on production until:

| Gate | Issue |
|------|-------|
| Release checklist | **#408** |
| Prompt security hardening | Epic **#438** |
| Plan entitlement (Plus + Consultorio) | **#409** |

Staging may enable AI earlier for smoke tests; use a **separate OpenAI key** with lower budget.

---

## Troubleshooting

| Symptom | Likely cause | Fix |
|---------|--------------|-----|
| `/admin/ai` 404 | `AI_ENABLED=false` | Enable via SSM script |
| Chat 503 | Missing key/model | Set both; check journal WARN |
| 429 to user | App or OpenAI limit | Lower usage; tune `AI_CHAT_MESSAGE_RATE_*`; check OpenAI dashboard |
| No sidebar link | Feature flag off | Same as 404 |
| Changes not applied | Forgot restart | `systemctl restart nutriconsultas` |
| SSM script fails | Wrong profile/region/instance | `aws sts get-caller-identity`; verify `app_instance_id` parameter |

---

## Related documents

| Doc | Topic |
|-----|--------|
| [`LOCAL-AI-SETUP.md`](LOCAL-AI-SETUP.md) | Developer `.env` setup |
| [`infrastructure/README.md`](../../infrastructure/README.md) | EC2, SSM, CodePipeline |
| [`DATA-ACCESS-RULES.md`](DATA-ACCESS-RULES.md) | PHI, logging, retention |
| [`PROMPT-SECURITY.md`](PROMPT-SECURITY.md) | Guardrails before prod enable |
| [`../../AI-ASSISTANT-WORKFLOW.md`](../../AI-ASSISTANT-WORKFLOW.md) | Milestones and env summary |
