# Apple Sign-In Backend Integration Roadmap

This document tracks backend improvements required to support Sign in with Apple through Auth0 and to correctly receive Apple server-to-server account notifications.

**Issue registry:** [`ISSUE-APPLE-SIGNIN.md`](../../ISSUE-APPLE-SIGNIN.md) (#497–#511)

---

## Context

Minutriporcion uses Auth0 for authentication. The Auth0 tenant callback URL should continue to be used for the OAuth login flow:

```text
https://minutriporcion-prod.us.auth0.com/login/callback
```

Apple's **Server-to-Server Notification Endpoint** is different from the Auth0 login callback. It should point to a Minutriporcion backend endpoint that can receive Apple account lifecycle events.

Recommended production endpoint:

```text
https://minutriporcion.com/rest/webhooks/apple/sign-in
```

Recommended local/dev path:

```text
POST /rest/webhooks/apple/sign-in
```

---

## Goals

- Keep Auth0 as the identity broker for Sign in with Apple.
- Add a backend webhook for Apple server-to-server notifications.
- Verify Apple notification payloads before processing.
- Track notification delivery and deduplicate repeated events.
- Map Apple/Auth0 identities to Minutriporcion patient or nutritionist accounts.
- Safely handle account deletion and email relay changes.
- Avoid destructive automation until the event flow has been verified in production.

## Non-goals

- Do not replace Auth0 with a custom Apple OAuth implementation.
- Do not use Auth0's `/login/callback` endpoint as the Apple server-to-server notification endpoint.
- Do not immediately hard-delete local users on the first webhook implementation.
- Do not trust unsigned or unverified webhook payloads.

---

# Issue List

## Issue 1: Configure Auth0 Apple connection — [#497](https://github.com/diego-torres/nutriconsultas/issues/497) — **done** (2026-07-07)

Auth0 production tenant `minutriporcion-prod.us.auth0.com` with Apple social connection enabled for Minutriporcion applications. See [`apple-signin-setup.md`](apple-signin-setup.md).

## Issue 2: Add Apple webhook configuration properties — [#498](https://github.com/diego-torres/nutriconsultas/issues/498)

### Problem

The backend needs explicit configuration for Apple webhook validation and safe rollout behavior.

### Suggested properties

Add to `src/main/resources/application.properties`:

```properties
# Apple Sign-In server-to-server notifications
nutriconsultas.apple.signin.webhook.enabled=${APPLE_SIGNIN_WEBHOOK_ENABLED:false}
nutriconsultas.apple.signin.expected-issuer=${APPLE_SIGNIN_EXPECTED_ISSUER:https://appleid.apple.com}
nutriconsultas.apple.signin.expected-audience=${APPLE_SIGNIN_EXPECTED_AUDIENCE:}
nutriconsultas.apple.signin.jwks-url=${APPLE_SIGNIN_JWKS_URL:https://appleid.apple.com/auth/keys}

# Start safely: log and persist, but do not delete/update users automatically.
nutriconsultas.apple.signin.auto-process-destructive-events=${APPLE_SIGNIN_AUTO_PROCESS_DESTRUCTIVE_EVENTS:false}
```

### Tasks

- Add a typed Spring configuration properties class.
- Validate required values when the webhook is enabled.
- Keep webhook disabled by default for local development.
- Document required production environment variables.

### Acceptance criteria

- App starts with the webhook disabled by default.
- App fails fast if webhook is enabled without required audience/client configuration.
- Configuration is covered by unit tests.

---

## Issue 3: Add webhook controller — [#499](https://github.com/diego-torres/nutriconsultas/issues/499)

### Problem

Apple needs a public HTTPS endpoint where it can send Sign in with Apple account lifecycle notifications.

### Proposed endpoint

```text
POST /rest/webhooks/apple/sign-in
```

### Suggested class

```text
src/main/java/com/nutriconsultas/auth/apple/AppleSignInWebhookController.java
```

### Suggested request DTO

```java
record AppleSignInWebhookRequest(String payload) {
}
```

### Tasks

- Add controller under an auth/apple package.
- Accept `POST /rest/webhooks/apple/sign-in`.
- Return `200 OK` only after the payload is syntactically valid and safely recorded.
- Return `400 Bad Request` for missing payload.
- Return `401 Unauthorized` or `400 Bad Request` for invalid signatures or invalid claims.
- Do not expose detailed verification failures in the response body.

### Acceptance criteria

- Endpoint accepts Apple-style JSON payloads.
- Endpoint rejects empty or malformed requests.
- Endpoint logs only safe metadata.
- Endpoint does not log raw tokens or sensitive payloads.

---

## Issue 4: Permit webhook path through Spring Security — [#500](https://github.com/diego-torres/nutriconsultas/issues/500)

### Problem

Apple cannot authenticate using the app's normal user session or Auth0 JWT. The webhook route must be publicly reachable while still verifying Apple's signed payload inside the handler.

### Tasks

- Update security configuration to permit:

```java
requestMatchers(HttpMethod.POST, "/rest/webhooks/apple/sign-in").permitAll()
```

- Keep all other protected routes unchanged.
- Ensure CSRF configuration does not block the webhook, if CSRF is enabled for POST routes.
- Add tests proving the endpoint is reachable without a user session.

### Acceptance criteria

- Apple webhook route is reachable without Auth0 login.
- Other protected backend routes remain protected.
- Invalid webhook payloads are rejected by payload verification, not by session auth.

---

## Issue 5: Implement Apple signed payload verification — [#501](https://github.com/diego-torres/nutriconsultas/issues/501)

### Problem

The backend must verify that webhook notifications actually came from Apple.

### Tasks

- Add service:

```text
AppleSignInNotificationVerifier
```

- Fetch Apple public keys from Apple's JWKS endpoint.
- Cache JWKS responses with a reasonable TTL.
- Verify the signed payload:
  - Signature is valid.
  - Algorithm is expected.
  - Issuer is Apple.
  - Audience matches the configured Apple client/app identifier.
  - Expiration and issued-at claims are valid.
- Parse event claims into a typed internal DTO.
- Reject unknown or invalid payloads.

### Suggested dependency review

Evaluate whether existing dependencies can verify JWT/JWS safely. The current project has `jjwt` 0.9.1, but Apple payload verification may be easier and safer with a modern JOSE/JWT library such as Nimbus JOSE JWT.

### Acceptance criteria

- Valid Apple test payloads are accepted.
- Invalid signature is rejected.
- Wrong issuer is rejected.
- Wrong audience is rejected.
- Expired payload is rejected.
- JWKS retrieval failures are handled without crashing the app.

---

## Issue 6: Persist Apple notification events — [#502](https://github.com/diego-torres/nutriconsultas/issues/502)

### Problem

Apple may retry notifications, and backend processing must be idempotent. The app needs an audit trail before performing account updates.

### Suggested table

```text
apple_signin_notification
```

### Suggested fields

```text
id
apple_event_id
event_type
apple_subject
auth0_user_id
email
email_verified
is_private_email
raw_claims_json
processing_status
processing_error
received_at
processed_at
created_at
updated_at
```

### Suggested statuses

```text
RECEIVED
VERIFIED
IGNORED
PROCESSED
FAILED
```

### Tasks

- Add Liquibase changeset.
- Add JPA entity.
- Add repository.
- Enforce uniqueness on `apple_event_id` when present.
- Store raw claims only if they do not include secrets.
- Avoid storing full signed payload unless required for audit.

### Acceptance criteria

- Duplicate Apple notifications do not trigger duplicate processing.
- Every verified notification is auditable.
- Failed processing can be inspected later.
- Tests cover duplicate event handling.

---

## Issue 7: Create notification processing service — [#503](https://github.com/diego-torres/nutriconsultas/issues/503)

### Problem

Webhook receipt and business actions should be separated. The controller should verify and record; a service should decide what to do.

### Suggested service

```text
AppleSignInNotificationService
```

### Tasks

- Normalize Apple event types into internal enum values.
- Route each event type to a handler.
- Unknown event types should be safely recorded and ignored.
- Destructive events should be gated by configuration.
- Add structured logging for event type, status, and mapped user IDs.

### Suggested first-pass behavior

| Event category                        | First release behavior                                          |
| ------------------------------------- | --------------------------------------------------------------- |
| Email relay preference changed        | Record event, mark user for review if mapped                    |
| App account deleted / consent revoked | Record event, mark linked local account as Apple access revoked |
| Apple Account permanently deleted     | Record event, queue manual review                               |
| Unknown event                         | Record and ignore                                               |

### Acceptance criteria

- Controller remains thin.
- Processing is idempotent.
- Unknown events do not fail the whole webhook.
- Destructive behavior is disabled unless explicitly enabled.

---

## Issue 8: Map Apple/Auth0 identity to local users — [#504](https://github.com/diego-torres/nutriconsultas/issues/504)

### Problem

The backend needs a reliable way to map an Apple notification to an Auth0 user and then to a Minutriporcion patient or nutritionist account.

### Tasks

- Inspect Auth0 Apple user profile shape in production/test tenant.
- Confirm Auth0 `user_id` pattern for Apple identities.
- Confirm whether Apple subject appears in:
  - Auth0 `user_id`
  - Auth0 `identities[].user_id`
  - Auth0 app metadata
  - Auth0 user profile email
- Add lookup service:

```text
AppleIdentityMappingService
```

- Prefer stable identifiers over email.
- Store Apple subject on local user linkage when available.
- Backfill Apple subject for existing Apple users if needed.

### Acceptance criteria

- Apple notification subject can be mapped to Auth0 user when possible.
- Auth0 user can be mapped to local patient/nutritionist account when possible.
- Private relay email changes do not break identity mapping.
- Ambiguous matches are recorded for manual review, not guessed.

---

## Issue 9: Add Auth0 Management API client methods — [#505](https://github.com/diego-torres/nutriconsultas/issues/505)

### Problem

Some Apple lifecycle events may require updating or deleting Auth0 users. The app already has Auth0 Management API configuration placeholders, but should expose narrow, safe methods.

### Suggested service

```text
Auth0ManagementUserService
```

### Tasks

- Add method to search users by provider identity or email.
- Add method to update `app_metadata`.
- Add method to block a user.
- Add method to delete a user, but keep it disabled behind configuration.
- Add retries and timeout handling.
- Avoid broad scopes where possible.

### Required Auth0 scopes

Start with:

```text
read:users
update:users_app_metadata
```

Only add later if necessary:

```text
update:users
delete:users
```

### Acceptance criteria

- Backend can read Auth0 users for Apple mapping.
- Backend can mark users with Apple lifecycle metadata.
- User deletion is not enabled by default.
- Management API failures are recorded and retried or surfaced for manual review.

---

## Issue 10: Add safe account deletion workflow — [#506](https://github.com/diego-torres/nutriconsultas/issues/506)

### Problem

Apple notifications may indicate that a user deleted app access or permanently deleted their Apple Account. The backend must avoid accidental data loss.

### Recommended approach

Use a staged workflow:

1. Receive Apple notification.
2. Verify signature.
3. Persist event.
4. Map to Auth0/local user.
5. Mark local account as `APPLE_ACCESS_REVOKED` or `PENDING_DELETION_REVIEW`.
6. Disable login or block access if required.
7. Perform hard deletion only after explicit product/legal decision.

### Tasks

- Define deletion policy for:
  - Patients
  - Nutritionists
  - Platform admins
  - Subscription owners
- Add local account status if missing.
- Add admin review screen or report for pending deletion events.
- Add manual runbook for final deletion.
- Add tests for "do not delete immediately" behavior.

### Acceptance criteria

- Apple deletion events do not silently hard-delete production data.
- Admins can identify affected users.
- Repeated deletion notifications stay idempotent.
- Data retention behavior is documented.

---

## Issue 11: Handle private relay email changes — [#507](https://github.com/diego-torres/nutriconsultas/issues/507)

### Problem

Sign in with Apple users may use private relay email addresses. Apple notifications may indicate forwarding changes. The app should not treat email as the permanent identity key.

### Tasks

- Store Apple subject and Auth0 user ID as primary identity keys.
- Store email relay metadata when available:
  - Current email
  - Private relay status
  - Forwarding enabled/disabled
- Avoid overwriting verified user contact email without review.
- Notify admins if delivery to relay email becomes disabled.

### Acceptance criteria

- App does not lose account mapping when relay email changes.
- Email forwarding changes are recorded.
- Email delivery behavior can be inspected by admins.

---

## Issue 12: Add observability and operational alerts — [#508](https://github.com/diego-torres/nutriconsultas/issues/508)

### Problem

Webhook failures can silently break Apple account lifecycle handling.

### Tasks

- Add structured logs for:
  - Receipt
  - Verification success/failure
  - Duplicate event
  - Processing status
  - Mapping status
  - Auth0 API outcome
- Add metrics if actuator/metrics are available:
  - `apple.signin.webhook.received`
  - `apple.signin.webhook.verified`
  - `apple.signin.webhook.failed`
  - `apple.signin.webhook.duplicate`
  - `apple.signin.webhook.unmapped`
- Add alerting for repeated verification failures.
- Add alerting for unmapped destructive events.

### Acceptance criteria

- Production can detect webhook delivery problems.
- Failed notifications can be debugged without exposing secrets.
- Unmapped destructive events are visible to operators.

---

## Issue 13: Add integration tests — [#509](https://github.com/diego-torres/nutriconsultas/issues/509)

### Problem

Webhook and account lifecycle behavior must be tested without relying on live Apple delivery.

### Tasks

- Add controller tests for:
  - Missing payload
  - Malformed payload
  - Invalid signature
  - Valid payload
  - Duplicate payload
- Add service tests for:
  - Unknown event type
  - Relay email change event
  - Account deletion event with destructive processing disabled
  - Account deletion event with destructive processing enabled in a test profile
- Add repository tests for idempotency.
- Mock Auth0 Management API calls.

### Acceptance criteria

- `mvn test` passes.
- Security tests prove the webhook is public but verified.
- No test requires real Apple credentials.
- No test calls live Auth0.

---

## Issue 14: Document Apple Developer Portal setup — [#510](https://github.com/diego-torres/nutriconsultas/issues/510)

### Problem

Future maintainers need exact setup steps and must not confuse the Auth0 callback with the Apple webhook endpoint.

### Tasks

Expand [`apple-signin-setup.md`](apple-signin-setup.md) with:

- Apple App ID setup.
- Apple Sign in with Apple capability.
- Apple server-to-server notification endpoint.
- Auth0 callback URL.
- Auth0 Apple social connection settings.
- Production environment variables.
- Testing limitations.
- Rollback steps.

### Acceptance criteria

- Documentation clearly separates Auth0 login callback from Apple webhook endpoint.
- Production setup can be repeated from the docs.
- Required secrets are listed but not committed.

---

## Issue 15: Add production rollout plan — [#511](https://github.com/diego-torres/nutriconsultas/issues/511)

### Problem

Apple lifecycle handling is security-sensitive. It should be rolled out gradually.

### Phase 1: Observe only

- Enable webhook.
- Verify payloads.
- Persist notifications.
- Do not mutate Auth0 or local users.

### Phase 2: Metadata updates

- Mark Auth0/local users with Apple lifecycle metadata.
- Record relay changes.
- Surface pending deletion events to admins.

### Phase 3: Restricted automation

- Block or disable access for confirmed revoked users if product policy requires it.
- Keep hard deletion manual.

### Phase 4: Optional deletion automation

- Enable only after legal/product approval.
- Require idempotency, audit trail, and rollback documentation.

### Acceptance criteria

- Webhook can be enabled without destructive side effects.
- Operators can monitor real Apple notifications.
- Destructive actions require explicit configuration and documentation.

---

# Suggested Implementation Order

1. ~~[#497](https://github.com/diego-torres/nutriconsultas/issues/497)~~ — Configure Auth0 Apple connection (**done**, 2026-07-07).
2. ~~[#498](https://github.com/diego-torres/nutriconsultas/issues/498)~~ — Add backend configuration properties (**done**, PR #513).
3. ~~[#499](https://github.com/diego-torres/nutriconsultas/issues/499)~~ — Add webhook controller (**done**, PR #513).
4. ~~[#500](https://github.com/diego-torres/nutriconsultas/issues/500)~~ — Permit webhook route in security config (**done**, PR #513).
5. ~~[#501](https://github.com/diego-torres/nutriconsultas/issues/501)~~ — Implement payload verification (**done**, PR #513).
6. ~~[#502](https://github.com/diego-torres/nutriconsultas/issues/502)~~ — Persist notification events (**done**, PR #513).
7. ~~[#503](https://github.com/diego-torres/nutriconsultas/issues/503)~~ — Add notification processing service (**done**, PR #513).
8. ~~[#504](https://github.com/diego-torres/nutriconsultas/issues/504)~~ — Add Apple/Auth0/local identity mapping (**done**, PR #514).
9. ~~[#505](https://github.com/diego-torres/nutriconsultas/issues/505)~~ — Add Auth0 Management API methods (**done**, PR #514).
10. ~~[#506](https://github.com/diego-torres/nutriconsultas/issues/506)~~ — Add safe account deletion workflow (**done**, PR #515).
11. ~~[#507](https://github.com/diego-torres/nutriconsultas/issues/507)~~ — Handle private relay email changes (**done**, PR #516).
12. [#508](https://github.com/diego-torres/nutriconsultas/issues/508) — Add observability (**in progress**).
13. [#509](https://github.com/diego-torres/nutriconsultas/issues/509) — Add integration tests.
14. [#510](https://github.com/diego-torres/nutriconsultas/issues/510) — Document Apple Developer Portal setup.
15. [#511](https://github.com/diego-torres/nutriconsultas/issues/511) — Roll out in observe-only mode.

---

# Production Values

## Apple server-to-server notification endpoint

```text
https://minutriporcion.com/rest/webhooks/apple/sign-in
```

## Auth0 callback URL

```text
https://minutriporcion-prod.us.auth0.com/login/callback
```

---

# Security Notes

- The webhook route may be public, but the payload must be cryptographically verified.
- Do not use shared static secrets as the primary trust mechanism if Apple sends signed payloads.
- Do not process destructive events until verification, idempotency, and audit logging are complete.
- Do not rely on email as the primary identity key for Apple users.
- Do not log raw signed tokens, authorization codes, refresh tokens, or Auth0 Management API tokens.
- Prefer soft-delete or pending-review states over immediate hard deletion.

---

# Open Questions

- What exact Auth0 connection name will Apple users use in production?
- Does Auth0 expose Apple's stable subject in `user_id`, `identities`, or profile metadata for this tenant?
- Which Minutriporcion account types can sign in with Apple: patients, nutritionists, platform admins, or all?
- What is the legal/product policy for Apple account deletion events?
- Should Apple deletion events block access, soft-delete, anonymize, or only mark for review?
- Should relay email changes notify admins or users?

---

# Definition of Done

Apple Sign-In backend integration is complete when:

- Auth0 Apple login works for the iOS app.
- Apple server-to-server notifications are delivered to Minutriporcion backend.
- Notifications are verified, persisted, deduplicated, and observable.
- Apple/Auth0/local user identity mapping is reliable.
- Destructive events are handled according to documented policy.
- Tests cover verification, idempotency, mapping, and disabled destructive processing.
- Production docs clearly distinguish the Auth0 callback URL from the Apple webhook endpoint.
