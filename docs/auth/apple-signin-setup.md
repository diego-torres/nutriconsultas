# Apple Sign-In — Developer Portal & Auth0 Setup

Maintainer runbook for **Sign in with Apple** on Minutriporcion (#510). Auth0 remains the identity broker; this doc covers Apple Developer Portal, Auth0, backend webhook configuration, and production deployment.

**Related:**

- Issue track: [`ISSUE-APPLE-SIGNIN.md`](../../ISSUE-APPLE-SIGNIN.md) (#497–#511)
- Backend roadmap: [`apple-signin-backend-roadmap.md`](apple-signin-backend-roadmap.md)
- Observability & alerts: [`apple-signin-observability.md`](apple-signin-observability.md)
- Deletion runbook: [`apple-signin-deletion-runbook.md`](apple-signin-deletion-runbook.md)
- Production rollout: [`apple-signin-rollout.md`](apple-signin-rollout.md)
- Auth0 patient post-login gate: [`../auth0/PATIENT-POST-LOGIN-GATE.md`](../auth0/PATIENT-POST-LOGIN-GATE.md)

---

## Prerequisites

Before enabling Apple Sign-In in production:

- [ ] Apple Developer Program membership with access to **Certificates, Identifiers & Profiles**
- [ ] Auth0 production tenant (`minutriporcion-prod.us.auth0.com`) with admin access
- [ ] Minutriporcion backend deployed with Liquibase migrations through `028-apple-signin-relay-email` (or later)
- [ ] Public HTTPS endpoint reachable at `https://minutriporcion.com/rest/webhooks/apple/sign-in`
- [ ] Ops vault entry for Apple **Team ID**, **Key ID**, and `.p8` signing key (never commit to git)

---

## Critical: two different URLs

Apple and Auth0 use **different** endpoints. Mixing them breaks either login or lifecycle notifications.

| Purpose | URL | Used by |
|---------|-----|---------|
| **Auth0 OAuth login callback** | `https://minutriporcion-prod.us.auth0.com/login/callback` | Auth0 ↔ Apple during user sign-in |
| **Apple server-to-server notifications** | `https://minutriporcion.com/rest/webhooks/apple/sign-in` | Apple → Minutriporcion backend lifecycle events |

Do **not** point Apple's **Server-to-Server Notification Endpoint** at the Auth0 callback.

```
User sign-in flow:     iOS app → Auth0 → Apple → Auth0 callback → app
Lifecycle webhooks:    Apple  → minutriporcion.com/rest/webhooks/apple/sign-in → backend
```

---

## Production identifier reference

Store actual values in your **ops vault** (1Password, AWS Secrets Manager, etc.). This table lists what each identifier is for — not secret values.

| Identifier | Where used | Notes |
|------------|------------|-------|
| **Apple Team ID** | Auth0 Apple connection, `.p8` key | 10-character team identifier from Apple Developer |
| **Apple Key ID** | Auth0 Apple connection | From the Sign in with Apple key you create |
| **iOS Bundle ID** (App ID) | Native iOS app, Auth0 Apple connection | Must match the Flutter/iOS app `PRODUCT_BUNDLE_IDENTIFIER` |
| **Services ID** (optional) | Web Sign in with Apple via Auth0 | Only if web-based Apple login is required |
| **`APPLE_SIGNIN_EXPECTED_AUDIENCE`** | Backend JWT verification | Must match the Apple **client identifier** Apple puts in notification JWT `aud` (often the Services ID or primary App ID — confirm from a test notification) |
| **Auth0 connection** | Social login | Typically named `apple`; enabled on patient mobile and nutritionist apps as needed |
| **Auth0 `user_id` for Apple users** | Identity mapping (#504) | Pattern: `apple\|{apple_stable_subject}` |

---

## Apple Developer Portal

### 1. App ID (Bundle ID)

1. Open [Apple Developer](https://developer.apple.com/account) → **Certificates, Identifiers & Profiles** → **Identifiers**.
2. Select or create the **App ID** for the Minutriporcion iOS app (native patient app).
3. Under **Capabilities**, enable **Sign in with Apple**.
4. Save. Note the **Bundle ID** — it must match the mobile app's bundle identifier and Auth0 Apple connection settings.

### 2. Sign in with Apple key (`.p8`)

1. **Keys** → **+** to create a new key.
2. Name it (e.g. `Minutriporcion Sign in with Apple`).
3. Enable **Sign in with Apple** and associate it with your primary App ID.
4. **Register** → **Download** the `.p8` file **once** (Apple does not allow re-download).
5. Record **Key ID** (shown on the key detail page) and your **Team ID** (Membership details).

**Never commit** the `.p8` file or paste its contents into git, Terraform, or this documentation.

### 3. Services ID (web / Auth0 only)

Required when Auth0 handles Sign in with Apple for web or when the notification JWT audience is a Services ID.

1. **Identifiers** → **+** → **Services IDs**.
2. Create an identifier (e.g. `com.minutriporcion.app` or your org's Services ID convention).
3. Enable **Sign in with Apple** → **Configure**:
   - **Primary App ID:** select the iOS App ID from step 1.
   - **Domains and Subdomains:** `minutriporcion-prod.us.auth0.com` (Auth0 tenant host).
   - **Return URLs:** `https://minutriporcion-prod.us.auth0.com/login/callback` (and any Auth0-documented variants for your tenant).
4. Save.

### 4. Server-to-server notification endpoint

Apple sends account lifecycle events (email relay changes, consent revoked, account delete) to your **backend**, not Auth0.

1. In Apple Developer Portal, open the **App ID** (or Services ID — follow Apple's current UI for your account type).
2. Under **Sign in with Apple** configuration, find **Server-to-Server Notification Endpoint**.
3. Set:

```text
https://minutriporcion.com/rest/webhooks/apple/sign-in
```

4. Save. Apple may take time to propagate; verify delivery only after the backend webhook is enabled (see below).

**Local development:** Apple cannot POST to `localhost`. Use integration tests (#509) with locally signed JWTs instead of live delivery.

---

## Auth0 configuration

Production tenant: **`minutriporcion-prod.us.auth0.com`** ([#497](https://github.com/diego-torres/nutriconsultas/issues/497) done, 2026-07-07).

### Apple social connection

1. Auth0 Dashboard → **Authentication** → **Social** → **Apple** (connection name is typically `apple`).
2. Configure:
   - **Apple Team ID** — from Membership / ops vault
   - **App ID / Bundle ID** — iOS native bundle ID
   - **Services ID** — if using web Apple login (step 3 above)
   - **Key ID** — from the `.p8` key
   - **Client Secret Signing Key** — paste contents of the `.p8` file (Auth0 stores it encrypted)
3. **Applications** tab: enable the connection for:
   - Patient mobile Auth0 application (`minutriporcion-native` / Flutter `AUTH0_CLIENT_ID`)
   - Nutritionist web application if Apple login is offered there
4. **Settings** → confirm **Allowed Callback URLs** include:

```text
https://minutriporcion-prod.us.auth0.com/login/callback
```

5. Test login with an Apple ID on a staging or production build before enabling webhook processing.

### Auth0 Apple user profile shape (#504)

For this tenant, Apple users are identified by:

| Field | Value |
|-------|-------|
| Auth0 `user_id` | `apple\|{apple_stable_subject}` |
| Apple subject | Stable per user per app; stored on `Paciente.apple_subject` |
| Email | May be a Hide My Email relay (`*@privaterelay.appleid.com`) — **not** the primary identity key |

Inspect a test user in Auth0 Dashboard → **User Management** → user → **Identities** before relying on email for mapping.

### Auth0 Management API (lifecycle — #505)

Machine-to-Machine application with minimal scopes:

```text
read:users
update:users_app_metadata
```

Configured via `AUTH0_MGMT_CLIENT_ID`, `AUTH0_MGMT_CLIENT_SECRET`, `AUTH0_MGMT_DOMAIN` on the app host.

Add `update:users` / `delete:users` only after product/legal approval ([#511](https://github.com/diego-torres/nutriconsultas/issues/511) Phase 4). User deletion remains disabled by default in application code.

---

## Backend environment variables

Mapped in `application.properties` from these environment variables. Set on production at `/opt/nutriconsultas/app.env` or via SSM (see below).

| Variable | Purpose | Production start | Default (local) |
|----------|---------|------------------|-----------------|
| `APPLE_SIGNIN_WEBHOOK_ENABLED` | Enable `POST /rest/webhooks/apple/sign-in` | `false` → `true` when ready | `false` |
| `APPLE_SIGNIN_EXPECTED_AUDIENCE` | JWT `aud` validation (Apple client identifier) | **Required** when webhook enabled | — |
| `APPLE_SIGNIN_EXPECTED_ISSUER` | JWT `iss` validation | `https://appleid.apple.com` | same |
| `APPLE_SIGNIN_JWKS_URL` | Apple public keys | `https://appleid.apple.com/auth/keys` | same |
| `APPLE_SIGNIN_AUTO_PROCESS_DESTRUCTIVE_EVENTS` | Auto revoke / pending-deletion metadata | `false` (observe-only) | `false` |
| `APPLE_SIGNIN_JWKS_CACHE_TTL` | JWKS cache duration | `6h` | `6h` |
| `APPLE_SIGNIN_VERIFICATION_FAILURE_ALERT_THRESHOLD` | Consecutive failures before ERROR alert | `5` | `5` |

Existing Auth0 variables (`AUTH_ISSUER`, `AUTH_CLIENT`, `AUTH0_MGMT_*`, mobile broker keys) are unchanged. See [`.env.example`](../../.env.example) for local dev.

**Never commit:** Apple `.p8` keys, Auth0 client secrets, or Management API credentials.

### Deploy via SSM (EC2)

From a machine with AWS CLI and `minutriporcion` profile:

```bash
export AWS_PROFILE=minutriporcion AWS_DEFAULT_REGION=us-east-1
export APPLE_SIGNIN_WEBHOOK_ENABLED=true
export APPLE_SIGNIN_EXPECTED_AUDIENCE='YOUR_APPLE_CLIENT_IDENTIFIER'
export APPLE_SIGNIN_AUTO_PROCESS_DESTRUCTIVE_EVENTS=false
./infrastructure/scripts/ssm-update-apple-signin.sh
```

Script: [`infrastructure/scripts/ssm-update-apple-signin.sh`](../../infrastructure/scripts/ssm-update-apple-signin.sh) — updates `app.env`, restarts `nutriconsultas`, prints an env audit.

`APPLE_SIGNIN_*` variables are **not** managed by Terraform (see `terraform.tfvars` comment); use SSM for runtime updates.

---

## Post-deploy verification checklist

1. **Health:** `curl -sf https://minutriporcion.com/actuator/health`
2. **Webhook disabled smoke:** `POST /rest/webhooks/apple/sign-in` with empty body → `503` when `APPLE_SIGNIN_WEBHOOK_ENABLED=false`
3. **Webhook enabled:** after enabling, invalid payload → `400`; valid signed test payload → `200` (use integration test JWT helpers locally, or wait for Apple delivery)
4. **Logs:** search for `Apple Sign-In webhook enabled` on startup and `apple_signin_webhook stage=` after events — see [`apple-signin-observability.md`](apple-signin-observability.md)
5. **Admin UI:** platform admin → **Apple Sign-In** (`/admin/platform/apple-signin`) lists received notifications
6. **Metrics:** `apple.signin.webhook.*` counters on `/actuator/metrics` when enabled
7. **Mobile login:** complete Sign in with Apple on a device build; confirm Auth0 user `apple|…` and patient linkage

---

## Local development

- Webhook is **disabled by default** (`APPLE_SIGNIN_WEBHOOK_ENABLED=false`).
- Run integration tests with locally signed JWTs — no live Apple or Auth0 calls:

```bash
mvn test -Dtest='AppleSignIn*IntegrationTest,AppleSignInNotificationPersistenceTest'
```

- Auth0 Apple connection can be tested against a **dev/staging** tenant before production.
- To test webhook logic locally with a tunnel (optional): expose port 3000 via ngrok/cloudflared and temporarily point Apple's notification URL at the tunnel — revert immediately after testing.

---

## Testing limitations

| Limitation | Workaround |
|------------|------------|
| Apple server notifications require **public HTTPS** | Integration tests (#509); production observe-only phase (#511) |
| Cannot receive Apple webhooks on `localhost` | Signed JWT tests; optional HTTPS tunnel for manual checks |
| Auth0 Apple profile shape varies by tenant | Inspect test users in Auth0 Dashboard (#504) |
| Relay email is not a stable identity key | Map by `apple_subject` / `patientAuthSub` (#507) |
| Destructive automation is off by default | `APPLE_SIGNIN_AUTO_PROCESS_DESTRUCTIVE_EVENTS=false` until Phase 3+ (#511) |

---

## Rollback

1. **Disable webhook:** `APPLE_SIGNIN_WEBHOOK_ENABLED=false` via SSM script or edit `app.env` → `systemctl restart nutriconsultas`.
2. **Disable Auth0 Apple connection** in Auth0 Dashboard if login regressions occur (users fall back to other connections).
3. **Revert Apple notification URL** in Developer Portal (or point to a no-op staging endpoint during investigation).
4. **Review audit data** in `apple_signin_notification` and `/admin/platform/apple-signin` — do not hard-delete patient data as part of rollback.
5. **Leave** `APPLE_SIGNIN_AUTO_PROCESS_DESTRUCTIVE_EVENTS=false` unless explicitly rolling forward again.

See also [`apple-signin-deletion-runbook.md`](apple-signin-deletion-runbook.md) if destructive events were processed.

---

## Rollout pointer

Phased production rollout is documented in [`apple-signin-rollout.md`](apple-signin-rollout.md) ([#511](https://github.com/diego-torres/nutriconsultas/issues/511)). Start with webhook **enabled** and `APPLE_SIGNIN_AUTO_PROCESS_DESTRUCTIVE_EVENTS=false` (Phase 1 observe-only).
