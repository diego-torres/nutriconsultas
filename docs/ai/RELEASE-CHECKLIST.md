# AI Assistant release checklist (#408)

**Issue:** [#408](https://github.com/diego-torres/nutriconsultas/issues/408) · Epic [#404](https://github.com/diego-torres/nutriconsultas/issues/404)  
**Purpose:** Gate before setting `AI_ENABLED=true` on **production**. Complete every item; record sign-off at the bottom.

**Related:** [`PRODUCTION-AI-SETUP.md`](PRODUCTION-AI-SETUP.md) · [`LOCAL-AI-SETUP.md`](LOCAL-AI-SETUP.md) · [`NUTRITIONIST-USER-GUIDANCE.md`](NUTRITIONIST-USER-GUIDANCE.md) · [`PROMPT-SECURITY.md`](PROMPT-SECURITY.md)

---

## Hard blockers (do not enable production without these)

| Gate | Issue | Verify |
|------|-------|--------|
| **Plan entitlement** | [#409](https://github.com/diego-torres/nutriconsultas/issues/409) | Only **Plus** and **Consultorio** tiers can access `/admin/ai` and REST chat; Básico/Profesional receive 403. |
| **Prompt security** | Epic [#438](https://github.com/diego-torres/nutriconsultas/issues/438) | #439–#441, #447–#450 merged; golden security tests green (see [Security tests](#security-tests)). |
| **This checklist** | #408 | All sections below checked and signed off. |

Staging may enable AI earlier for smoke tests using a **separate OpenAI key** with a lower budget.

---

## Release checklist

### 1. AI disabled by default

- [ ] `application.properties` default: `nutriconsultas.ai.enabled=${AI_ENABLED:false}`.
- [ ] Production `/opt/nutriconsultas/app.env` has **`AI_ENABLED=false`** (or omits the var) until sign-off.
- [ ] `/admin/ai` returns **404** and sidebar **Asistente IA** is hidden when disabled.
- [ ] **Verify:** `grep AI_ENABLED /opt/nutriconsultas/app.env` → `false` or absent before rollout.

**Reference:** [`LOCAL-AI-SETUP.md`](LOCAL-AI-SETUP.md#disable-ai-locally) · [`PRODUCTION-AI-SETUP.md`](PRODUCTION-AI-SETUP.md#environment-variables-production)

---

### 2. API key in staging/production secret store

- [ ] `OPENAI_API_KEY` set only in **`/opt/nutriconsultas/app.env`** on the app EC2 host (not Git, CI, Thymeleaf, or browser).
- [ ] Production uses a **project-scoped** key (separate from developer keys).
- [ ] `OPENAI_MODEL` set and validated in staging first.
- [ ] `OPENAI_STORE=false` unless legal approves provider retention.
- [ ] SSM deploy script run without echoing key value: [`ssm-update-ai-openai.sh`](../../infrastructure/scripts/ssm-update-ai-openai.sh).
- [ ] **Verify:** journal shows `OPENAI_API_KEY=SET(len=…)` audit line, never `sk-…`; browser network tab has no `sk-` strings.

**Reference:** [`PRODUCTION-AI-SETUP.md`](PRODUCTION-AI-SETUP.md#where-secrets-live) · [`DATA-ACCESS-RULES.md`](DATA-ACCESS-RULES.md#api-key-handling-confirmation)

---

### 3. Rate limiting enabled

- [ ] Resilience4j instance `aiChatMessage` configured (default **20 messages / hour** per nutritionist).
- [ ] Production env documents limits if non-default: `AI_CHAT_MESSAGE_RATE_LIMIT`, `AI_CHAT_MESSAGE_RATE_WINDOW`.
- [ ] OpenAI account **budget alerts** configured in [OpenAI billing](https://platform.openai.com/settings/organization/billing).
- [ ] Bulk scope guards active: `AI_MAX_TOOL_CALLS`, `AI_MAX_DAYS_PER_TURN`, `AI_MAX_DISHES_PER_TURN`, `AI_MAX_MENU_DAYS_PER_TURN`.
- [ ] **Verify (staging):** exceed app limit → Spanish message *«Has alcanzado el límite de mensajes del asistente de IA…»*.

**Reference:** [`PRODUCTION-AI-SETUP.md`](PRODUCTION-AI-SETUP.md#rate-limits-and-spend-controls) · `AiChatRateLimiterTest`

---

### 4. Audit logging enabled

- [ ] Chat persistence: user, assistant, and **tool audit** rows in `ai_chat_message` / `ai_generated_draft`.
- [ ] Application logs follow [`DATA-ACCESS-RULES.md`](DATA-ACCESS-RULES.md#logging-and-audit-397): thread IDs and tool names at INFO; **no PHI**, no API keys, no full prompt bodies at INFO.
- [ ] Blocked injection attempts log **category + length only** (not message body) — [`PROMPT-SECURITY.md`](PROMPT-SECURITY.md).
- [ ] Draft accept/discard audited via draft lifecycle (status transitions in DB).
- [ ] **Verify:** send one chat message in staging → rows in `ai_chat_message`; journal has no patient names/emails.

**Note:** Structured audit logging via `AiAuditLogger` (#397) supplements DB persistence; redacted INFO lines only.

---

### 5. Draft approval flow tested

- [ ] Integration test green: `AiDraftFlowIntegrationTest` (create draft → accept → catalog materialization; discard; IDOR).
- [ ] Manual staging: generate platillo or menú borrador → review panel → **Aceptar** creates catalog entity → **Descartar** removes draft only.
- [ ] SweetAlert confirmations on accept/discard (no native `confirm()`).
- [ ] Draft label visible: *«Borrador IA — revisión del nutriólogo requerida»*.
- [ ] **Verify:** accepted platillo/dieta editable in normal catalog UI; never auto-assigned to patient.

**Reference:** [`E2E-DRAFT-FLOW-TEST.md`](E2E-DRAFT-FLOW-TEST.md) · [`NUTRITIONIST-USER-GUIDANCE.md`](NUTRITIONIST-USER-GUIDANCE.md)

---

### 6. Security tests passing

Run from repo root:

```bash
mvn test -Dtest=AiSecurityGoldenPromptTest,AiUserMessageGuardTest,AiPromptThreatDetectorTest,\
AiToolAllowlistTest,AiAssistantOutputValidatorTest,AiToolResultSanitizerTest,\
AiRequestScopeGuardTest,AiBulkScopeGoldenPromptTest,AiDraftFlowIntegrationTest
```

- [ ] All tests above pass on the release commit.
- [ ] Full CI green on `main` (lint, Thymeleaf validation, SpotBugs, PMD).
- [ ] IDOR: user A cannot read user B threads/drafts (`AiDraftFlowIntegrationTest`, `AiChatRestControllerTest`).
- [ ] Golden prompts: no tool execution on injection/jailbreak/bulk abuse scenarios.

**Reference:** [`SECURITY-GOLDEN-PROMPTS.md`](SECURITY-GOLDEN-PROMPTS.md) · [`BULK-SCOPE-GOLDEN-PROMPTS.md`](BULK-SCOPE-GOLDEN-PROMPTS.md)

---

### 7. Liquibase migration tested

- [ ] Changeset `024-ai-chat-schema.yaml` (and any later AI changesets) applied on staging PostgreSQL.
- [ ] `LiquibaseMigrationTest.testAiChatSchemaTablesExist` passes in CI.
- [ ] Brownfield prod: Liquibase runs on deploy; no manual DDL.
- [ ] **Verify on staging DB:**

  ```sql
  SELECT COUNT(*) FROM ai_chat_thread;
  SELECT COUNT(*) FROM ai_chat_message;
  SELECT COUNT(*) FROM ai_generated_draft;
  ```

**Reference:** [`docs/db/LIQUIBASE.md`](../db/LIQUIBASE.md) · `src/main/resources/db/changelog/changes/024-ai-chat-schema.yaml`

---

### 8. Rollback plan documented

- [ ] Team knows **emergency disable:** `AI_ENABLED=false` + `systemctl restart nutriconsultas` (SSM script or manual).
- [ ] Key compromise procedure: revoke in OpenAI dashboard → deploy new key → restart.
- [ ] Bad model rollback: revert `OPENAI_MODEL` in `app.env` → restart (no JAR redeploy).
- [ ] Code rollback: redeploy previous JAR via CodePipeline; `app.env` AI vars independent of JAR.

**Reference:** [`PRODUCTION-AI-SETUP.md`](PRODUCTION-AI-SETUP.md#rollback-and-disable)

---

### 9. Nutritionist guidance published

- [ ] In-app panel **«Cómo usar el asistente»** on `/admin/ai` (drafts, prompts, limits, accept/discard).
- [ ] Doc published: [`NUTRITIONIST-USER-GUIDANCE.md`](NUTRITIONIST-USER-GUIDANCE.md).
- [ ] **Verify:** `AiChatGuidanceContentTest` passes; expand panel in staging and spot-check Spanish copy.

---

## Pre-enable smoke test (staging)

After checklist complete on **staging** with `AI_ENABLED=true`:

1. Log in as Plus/Consultorio nutritionist → open **Asistente IA**.
2. Send: *«Genera un desayuno alto en proteína usando alimentos de mi catálogo.»*
3. Confirm borrador in right panel; accept or discard.
4. Confirm no `sk-` in page source or REST JSON.
5. Check OpenAI usage dashboard for expected token spend.

---

## Production enable procedure

Only after **all checklist items** and **#409** are done:

```bash
export AWS_PROFILE=minutriporcion AWS_DEFAULT_REGION=us-east-1
export AI_ENABLED=true
export OPENAI_API_KEY='sk-proj-...'
export OPENAI_MODEL='gpt-5-mini'
bash infrastructure/scripts/ssm-update-ai-openai.sh
```

Post-enable monitoring (first 24–48 h):

- OpenAI billing / usage alerts
- App journal: `journalctl -u nutriconsultas -f | grep -i 'AI '`
- Error rate on `/rest/ai/**` (503, 429)
- Support channel for nutritionist feedback

---

## Sign-off

| Role | Name | Date | Notes |
|------|------|------|-------|
| Engineering | | | CI commit SHA: |
| Security / privacy | | | #409 + #438 verified |
| Product / clinical | | | Guidance reviewed |
| Operations | | | SSM + rollback rehearsed |

**Production `AI_ENABLED=true` authorized:** ☐ Yes · ☐ No — date: ___________
