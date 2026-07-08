# Apple Sign-In production rollout plan (#511)

**Issue:** [#511](https://github.com/diego-torres/nutriconsultas/issues/511) · Epic [#497](https://github.com/diego-torres/nutriconsultas/issues/497)  
**Purpose:** Roll out Apple server-to-server lifecycle handling **gradually** on production. Destructive automation stays off until operators verify real traffic and product/legal sign off.

**Related:** [`apple-signin-setup.md`](apple-signin-setup.md) · [`apple-signin-observability.md`](apple-signin-observability.md) · [`apple-signin-deletion-runbook.md`](apple-signin-deletion-runbook.md) · [`ISSUE-APPLE-SIGNIN.md`](../../ISSUE-APPLE-SIGNIN.md)

---

## Hard blockers (do not enable the webhook without these)

| Gate | Issue | Verify |
|------|-------|--------|
| **Auth0 Apple connection** | [#497](https://github.com/diego-torres/nutriconsultas/issues/497) | iOS patient can sign in with Apple via Auth0 prod tenant |
| **Backend on `main`** | #498–#510 | PRs [#513](https://github.com/diego-torres/nutriconsultas/pull/513)–[#519](https://github.com/diego-torres/nutriconsultas/pull/519) deployed to production EC2 |
| **Liquibase schema** | #502 | `apple_signin_notification` and relay/lifecycle columns exist on prod PostgreSQL |
| **Apple notification URL** | #510 | Developer Portal points to `https://minutriporcion.com/rest/webhooks/apple/sign-in` (**not** the Auth0 callback) |
| **Audience configured** | #501 | `APPLE_SIGNIN_EXPECTED_AUDIENCE` matches JWT `aud` from a test notification |
| **Integration tests green** | #509 | `mvn test -Dtest=AppleSignIn*IntegrationTest,AppleSignInNotificationPersistenceTest` on release commit |

---

## Phase summary

| Phase | `APPLE_SIGNIN_WEBHOOK_ENABLED` | `APPLE_SIGNIN_AUTO_PROCESS_DESTRUCTIVE_EVENTS` | Destructive events (`consent-revoked`, `account-delete`) | Relay email events (`email-enabled`, `email-disabled`) |
|-------|-------------------------------|------------------------------------------------|----------------------------------------------------------|--------------------------------------------------------|
| **0 — Default (pre-rollout)** | `false` | `false` | Webhook returns **503**; nothing persisted | Same |
| **1 — Observe only** | `true` | `false` | Verified, persisted, mapped; `lifecycle_action=SKIPPED_OBSERVE_ONLY`; **no** lifecycle metadata or access changes | Verified, persisted; **relay metadata applied** when patient is mapped (#507) |
| **2 — Metadata + restrict** | `true` | `true` | Local `apple_lifecycle_status` + Auth0 `app_metadata`; `consent-revoked` sets `PacienteStatus.REVOKED` and blocks Auth0 login | Same as Phase 1 |
| **3 — Restricted automation** | `true` | `true` | Mobile API access blocked for non-`NONE` `apple_lifecycle_status` (`MobilePatientAccessRules`) | Same |
| **4 — Optional hard delete** | `true` | `true` | **Manual only** — see deletion runbook; never enable Auth0 auto-delete from webhook | Same |

Phases **2 and 3** share the same configuration flag today: enabling `APPLE_SIGNIN_AUTO_PROCESS_DESTRUCTIVE_EVENTS=true` applies metadata **and** mobile access restrictions together. Phase **4** is a **process** gate (legal/product), not an additional env var.

---

## Configuration reference

| Variable | Phase 0 | Phase 1+ | Notes |
|----------|---------|----------|-------|
| `APPLE_SIGNIN_WEBHOOK_ENABLED` | `false` | `true` | Master switch for `POST /rest/webhooks/apple/sign-in` |
| `APPLE_SIGNIN_EXPECTED_AUDIENCE` | — | **Required** when webhook enabled | Apple client identifier in notification JWT `aud` |
| `APPLE_SIGNIN_AUTO_PROCESS_DESTRUCTIVE_EVENTS` | `false` | `false` → `true` for Phase 2+ | Default `false` in `application.properties` |
| `APPLE_SIGNIN_VERIFICATION_FAILURE_ALERT_THRESHOLD` | `5` | `5` | Consecutive verification failures before ERROR alert |
| `AUTH0_MGMT_USER_DELETE_ENABLED` | `false` | `false` | **Never** enable for webhook-driven deletion (Phase 4 manual only) |

Deploy runtime changes via SSM (not Terraform):

```bash
export AWS_PROFILE=minutriporcion AWS_DEFAULT_REGION=us-east-1
bash infrastructure/scripts/ssm-update-apple-signin.sh
```

---

## Phase 1 — Observe only (recommended first production step)

### Goals

- Receive and cryptographically verify real Apple notifications.
- Persist rows in `apple_signin_notification` with identity mapping status.
- Record destructive events **without** applying lifecycle metadata or revoking access.
- Surface traffic in metrics, logs, and `/admin/platform/apple-signin`.

### Pre-flight checklist

- [ ] Production JAR includes Apple Sign-In code (CodePipeline deploy from `main`).
- [ ] Apple **Server-to-Server Notification Endpoint** is `https://minutriporcion.com/rest/webhooks/apple/sign-in`.
- [ ] Auth0 Management API credentials on EC2 allow `read:users` and `update:users` for relay metadata (#507).
- [ ] Ops vault has confirmed `APPLE_SIGNIN_EXPECTED_AUDIENCE` (from a staging/test JWT or Apple docs for your App/Services ID).
- [ ] CloudWatch (or log aggregator) can search for `APPLE_SIGNIN_ALERT` and `apple_signin_webhook stage=`.

### Enable procedure

```bash
export AWS_PROFILE=minutriporcion AWS_DEFAULT_REGION=us-east-1
export APPLE_SIGNIN_WEBHOOK_ENABLED=true
export APPLE_SIGNIN_EXPECTED_AUDIENCE='YOUR_APPLE_CLIENT_IDENTIFIER'
export APPLE_SIGNIN_AUTO_PROCESS_DESTRUCTIVE_EVENTS=false
bash infrastructure/scripts/ssm-update-apple-signin.sh
```

### Verification (within 24–48 h)

- [ ] `curl -sf https://minutriporcion.com/actuator/health` → `UP`.
- [ ] Disabled webhook smoke no longer applies: empty `POST` to webhook should **not** return `503`.
- [ ] Trigger or wait for a real Apple event (relay change, test user consent flow, or Apple sandbox where available).
- [ ] Admin UI **Plataforma → Apple Sign-In** shows new rows with `processing_status=PROCESSED`.
- [ ] Destructive events show `lifecycle_action=SKIPPED_OBSERVE_ONLY` (not `APPLIED_*`).
- [ ] Metrics: `apple.signin.webhook.verified` increments; no sustained `apple.signin.webhook.failed`.
- [ ] Logs: no `APPLE_SIGNIN_ALERT repeated_verification_failures` after initial tuning.

### Soak criteria before Phase 2

- [ ] At least one successfully verified notification per event type you expect in production (or documented “no traffic yet” with monitoring in place).
- [ ] Identity mapping rate acceptable (`MAPPED` vs `NO_LOCAL_USER` understood).
- [ ] No false-positive verification failures after audience fix.
- [ ] On-call knows how to disable webhook (rollback below).

---

## Phase 2 — Metadata updates

### Goals

- Apply `apple_lifecycle_status` on mapped patients for destructive events.
- Write Auth0 `app_metadata` lifecycle fields.
- Keep **hard deletion** manual.

### Enable procedure

Only after Phase 1 soak sign-off:

```bash
export AWS_PROFILE=minutriporcion AWS_DEFAULT_REGION=us-east-1
export APPLE_SIGNIN_WEBHOOK_ENABLED=true
export APPLE_SIGNIN_EXPECTED_AUDIENCE='YOUR_APPLE_CLIENT_IDENTIFIER'
export APPLE_SIGNIN_AUTO_PROCESS_DESTRUCTIVE_EVENTS=true
bash infrastructure/scripts/ssm-update-apple-signin.sh
```

### Expected behavior

| Apple event | Local patient | Auth0 | Hard delete |
|-------------|---------------|-------|-------------|
| `consent-revoked` | `ACCESS_REVOKED`, `PacienteStatus.REVOKED` | `app_metadata` + block flag | **No** |
| `account-delete` | `PENDING_DELETION_REVIEW` | `app_metadata` only | **No** |

### Verification

- [ ] Staging integration test green: `AppleSignInDestructiveAutoProcessIntegrationTest`.
- [ ] Replay or new destructive test event → `lifecycle_action=APPLIED_ACCESS_REVOKED` or `APPLIED_PENDING_DELETION_REVIEW`.
- [ ] Admin grid shows `paciente_id` and lifecycle action.
- [ ] Idempotency: duplicate `apple_event_id` → HTTP 200, no double mutation.

---

## Phase 3 — Restricted automation (mobile access)

### Goals

- Block patient mobile API access when `apple_lifecycle_status` is not `NONE`.
- Keep nutritionist web and manual review paths available per policy.

### Configuration

Same as Phase 2 (`APPLE_SIGNIN_AUTO_PROCESS_DESTRUCTIVE_EVENTS=true`). Access rules are enforced in `MobilePatientAccessRules` when lifecycle status is set.

### Verification

- [ ] Mapped patient with `ACCESS_REVOKED` receives onboarding/block response on `/rest/mobile/patient/**` (except allowed onboarding paths).
- [ ] `MobilePatientAccessRulesTest` green in CI.
- [ ] Support runbook updated for “Apple consent revoked” patient tickets.

---

## Phase 4 — Optional hard deletion (manual only)

### Goals

- Satisfy Apple account deletion policy with **human approval**, audit trail, and rollback documentation.
- **Never** enable automatic Auth0 or local hard-delete from the webhook.

### Requirements before any hard delete

- [ ] Written product/legal approval recorded (ticket link in sign-off table).
- [ ] Operator followed [`apple-signin-deletion-runbook.md`](apple-signin-deletion-runbook.md) manual steps.
- [ ] Patient data handled via existing export/deletion tools (`PacienteDeletionService`, MPX).
- [ ] `AUTH0_MGMT_USER_DELETE_ENABLED` remains `false` unless a **one-off** controlled Auth0 deletion is explicitly approved.

---

## Monitoring (all phases)

| Signal | Where | Action |
|--------|-------|--------|
| Verification failures | `APPLE_SIGNIN_ALERT repeated_verification_failures` | Fix audience, JWKS, clock skew |
| Unmapped destructive events | `APPLE_SIGNIN_ALERT unmapped_destructive_event` | Review identity mapping (#504), admin grid |
| Throughput / errors | `/actuator/metrics` → `apple.signin.webhook.*` | Dashboard or CloudWatch metric filters |
| Audit rows | `apple_signin_notification` table | SQL or `/admin/platform/apple-signin` |
| Auth0 metadata failures | `auth0_update_failed` log stage | Check Management API scopes and rate limits |

See [`apple-signin-observability.md`](apple-signin-observability.md) for full detail.

---

## Rollback

| Situation | Action |
|-----------|--------|
| Bad audience / verification storm | `APPLE_SIGNIN_WEBHOOK_ENABLED=false` → SSM script → restart |
| Destructive false positives | `APPLE_SIGNIN_AUTO_PROCESS_DESTRUCTIVE_EVENTS=false` → SSM script; manually correct affected patients |
| Login regressions | Disable Auth0 Apple connection in Auth0 Dashboard (users use other connections) |
| Apple sending to wrong host | Revert notification URL in Developer Portal |
| Investigation | **Do not** delete `apple_signin_notification` audit rows |

Emergency disable (same as Phase 0):

```bash
export AWS_PROFILE=minutriporcion AWS_DEFAULT_REGION=us-east-1
export APPLE_SIGNIN_WEBHOOK_ENABLED=false
bash infrastructure/scripts/ssm-update-apple-signin.sh
```

---

## Integration tests (release gate)

Run on the commit before each phase advance:

```bash
mvn test -Dtest=AppleSignInWebhookIntegrationTest,AppleSignInNotificationFlowIntegrationTest,\
AppleSignInDestructiveAutoProcessIntegrationTest,AppleSignInNotificationPersistenceTest,\
AppleSignInWebhookSecurityTest,AppleSignInWebhookMetricsTest
```

- [ ] All tests pass (local RSA keys; no live Apple or Auth0).
- [ ] Full CI green on `main`.

---

## Sign-off

| Phase | Engineering | Product / legal | Operations | Date | Notes |
|-------|-------------|-----------------|------------|------|-------|
| **1 — Observe** | | | | | CI SHA: |
| **2 — Metadata** | | | | | |
| **3 — Restrict** | | | | | |
| **4 — Hard delete (manual)** | | | | | Ticket: |

**Phase 1 webhook enable authorized:** ☐ Yes · ☐ No — date: ___________  
**Phase 2+ destructive auto-process authorized:** ☐ Yes · ☐ No — date: ___________
