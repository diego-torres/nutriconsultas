# Apple Sign-In webhook observability (#508)

Operators can detect Apple server-to-server notification problems via structured logs, Micrometer counters, and alert log lines. No emails, Apple subjects, Auth0 user IDs, or signed payloads are logged.

**Related:** [`apple-signin-setup.md`](apple-signin-setup.md), [`apple-signin-rollout.md`](apple-signin-rollout.md), [`ISSUE-APPLE-SIGNIN.md`](../../ISSUE-APPLE-SIGNIN.md)

---

## Micrometer counters

Exposed through Spring Boot Actuator (`/actuator/metrics`) when enabled:

| Metric | When incremented | Tags |
|--------|------------------|------|
| `apple.signin.webhook.received` | Valid POST with non-empty payload | — |
| `apple.signin.webhook.verified` | Payload signature and claims verified | `event_type` |
| `apple.signin.webhook.failed` | Verification or parsing failed | `reason` |
| `apple.signin.webhook.duplicate` | Duplicate `apple_event_id` | `event_type` |
| `apple.signin.webhook.unmapped` | Destructive event without `MAPPED` patient | `event_type`, `mapping_status` |

Tag values are low-cardinality enums only (`CONSENT_REVOKED`, `invalid_signature`, `NO_LOCAL_USER`, etc.).

---

## Structured log stages

Search application logs for `apple_signin_webhook stage=`:

| Stage | Level | Meaning |
|-------|-------|---------|
| `received` | DEBUG | Webhook accepted for processing |
| `verified` | INFO | Signed payload verified (`eventId`, `eventType`) |
| `verification_failed` | WARN | Invalid payload (`reason`, `consecutiveFailures`) |
| `duplicate` | DEBUG | Idempotent duplicate event |
| `processed` | INFO | Persisted notification summary (status, mapping, lifecycle, `pacienteId`) |
| `auth0_update_failed` | WARN | Auth0 Management API metadata update failed |

---

## Operator alerts (log-based)

Configure CloudWatch (or similar) alarms on these log substrings:

| Alert token | Trigger | Suggested action |
|-------------|---------|------------------|
| `APPLE_SIGNIN_ALERT repeated_verification_failures` | `consecutiveFailures` ≥ threshold (default **5**) | Check `APPLE_SIGNIN_EXPECTED_AUDIENCE`, Apple JWKS reachability, clock skew, recent deploy |
| `APPLE_SIGNIN_ALERT unmapped_destructive_event` | Destructive event (`consent-revoked`, `account-delete`) without mapped patient | Review `/admin/platform/apple-signin` and identity mapping (#504) |

### Configuration

```properties
nutriconsultas.apple.signin.verification-failure-alert-threshold=5
```

Environment variable: `APPLE_SIGNIN_VERIFICATION_FAILURE_ALERT_THRESHOLD`

---

## Debugging checklist

1. Confirm `APPLE_SIGNIN_WEBHOOK_ENABLED=true` and audience matches Apple Services ID.
2. Check `apple.signin.webhook.failed` by `reason` tag in metrics.
3. Query `apple_signin_notification` for `processing_status` and `identity_mapping_status`.
4. Review platform admin UI: `/admin/platform/apple-signin`.

---

## Integration tests (#509)

Run the Apple Sign-In integration suite (local RSA keys, mocked Auth0 — no live Apple or Auth0):

```bash
mvn test -Dtest=AppleSignIn*IntegrationTest,AppleSignInNotificationPersistenceTest
```

Coverage includes webhook HTTP flows, signed payload verification, idempotency, relay/destructive service paths, and repository uniqueness.
