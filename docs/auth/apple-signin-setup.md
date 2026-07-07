# Apple Sign-In — Developer Portal & Auth0 Setup

Maintainer runbook for **Sign in with Apple** on Minutriporcion. Auth0 remains the identity broker; this doc covers Apple Developer Portal, Auth0, and production environment configuration.

**Related:**

- Issue track: [`ISSUE-APPLE-SIGNIN.md`](../../ISSUE-APPLE-SIGNIN.md) (#497–#511)
- Full backend roadmap: [`apple-signin-backend-roadmap.md`](apple-signin-backend-roadmap.md)
- Auth0 patient post-login gate: [`../auth0/PATIENT-POST-LOGIN-GATE.md`](../auth0/PATIENT-POST-LOGIN-GATE.md)

---

## Critical: two different URLs

| Purpose | URL | Used by |
|---------|-----|---------|
| **Auth0 OAuth login callback** | `https://minutriporcion-prod.us.auth0.com/login/callback` | Auth0 ↔ Apple OAuth during user sign-in |
| **Apple server-to-server notifications** | `https://minutriporcion.com/rest/webhooks/apple/sign-in` | Apple → Minutriporcion backend lifecycle events |

Do **not** point Apple's server-to-server notification endpoint at the Auth0 callback.

---

## Apple Developer Portal

### 1. App ID / Bundle ID

1. Open [Apple Developer](https://developer.apple.com/account) → **Certificates, Identifiers & Profiles** → **Identifiers**.
2. Create or confirm the iOS **App ID** used by the Minutriporcion mobile app.
3. Enable **Sign in with Apple** capability on the App ID.

### 2. Services ID (if web Sign in with Apple is needed)

1. Create a **Services ID** if web-based Sign in with Apple is required (optional for native-only iOS).
2. Enable Sign in with Apple on the Services ID.
3. Configure domains and return URLs per Apple documentation (Auth0 docs list the exact redirect URIs for your Auth0 tenant).

### 3. Sign in with Apple key

1. **Keys** → create a key with **Sign in with Apple** enabled.
2. Download the `.p8` private key once (Apple does not allow re-download).
3. Record **Key ID** and **Team ID**.

### 4. Server-to-server notification endpoint

In the Apple Developer Portal, configure the **Server-to-Server Notification Endpoint** for Sign in with Apple:

```text
https://minutriporcion.com/rest/webhooks/apple/sign-in
```

This endpoint is implemented by the backend (#499). Deploy and enable the webhook (#498) before expecting Apple deliveries in production.

---

## Auth0 configuration — [#497](https://github.com/diego-torres/nutriconsultas/issues/497)

1. Auth0 Dashboard → **Authentication** → **Social** → **Apple**.
2. Configure:
   - **Apple Team ID**
   - **App ID / Bundle ID** (native) and/or **Services ID** (web)
   - **Key ID**
   - **Client Secret Signing Key** (contents of the `.p8` file)
3. Enable the Apple connection for the Minutriporcion application(s).
4. Confirm the Auth0 **Allowed Callback URL** includes:

```text
https://minutriporcion-prod.us.auth0.com/login/callback
```

5. Verify existing connections (Google, email/password) still work after enabling Apple.

### Auth0 Management API (lifecycle handling — [#505](https://github.com/diego-torres/nutriconsultas/issues/505))

Create or reuse a Machine-to-Machine application with minimal scopes:

```text
read:users
update:users_app_metadata
```

Add `update:users` / `delete:users` only after product/legal approval (#511 Phase 4).

---

## Backend environment variables

Set on the production app host (`/opt/nutriconsultas/app.env`) or via SSM remediation script (pattern: `infrastructure/scripts/ssm-update-*.sh`).

| Variable | Purpose | Default (local) |
|----------|---------|-----------------|
| `APPLE_SIGNIN_WEBHOOK_ENABLED` | Enable Apple webhook endpoint | `false` |
| `APPLE_SIGNIN_EXPECTED_ISSUER` | JWT issuer validation | `https://appleid.apple.com` |
| `APPLE_SIGNIN_EXPECTED_AUDIENCE` | Apple client/app identifier (required when webhook enabled) | — |
| `APPLE_SIGNIN_JWKS_URL` | Apple JWKS URL | `https://appleid.apple.com/auth/keys` |
| `APPLE_SIGNIN_AUTO_PROCESS_DESTRUCTIVE_EVENTS` | Allow automated deletion/revocation | `false` |

Existing Auth0 variables (`AUTH_ISSUER`, `AUTH_CLIENT`, mobile broker keys, etc.) are unchanged. See `.env.example` for local dev.

**Never commit** Apple `.p8` keys, Auth0 client secrets, or Management API credentials to git.

---

## Local development

- Webhook is **disabled by default** (`APPLE_SIGNIN_WEBHOOK_ENABLED=false`).
- Use unit/integration tests (#509) with mocked Apple payloads — no live Apple delivery required.
- Auth0 Apple connection can be tested against a dev/staging tenant before production.

---

## Testing limitations

- Apple server-to-server notifications require a **public HTTPS** endpoint; localhost cannot receive them directly.
- Use **Phase 1 (observe only)** rollout (#511) in production before enabling destructive processing.
- Auth0 Apple user profile shape should be inspected in the tenant (#504) before relying on email for mapping.

---

## Rollback

1. **Disable webhook:** set `APPLE_SIGNIN_WEBHOOK_ENABLED=false` and restart the app.
2. **Disable Auth0 Apple connection** in Auth0 Dashboard if login regressions occur.
3. **Revert Apple notification URL** in Developer Portal if needed (or point to a no-op staging endpoint during investigation).
4. Review persisted events in `apple_signin_notification` (#502) for audit; do not hard-delete user data as part of rollback.

---

## TODO (expand in #510)

- [ ] Exact Auth0 connection name and application IDs for production
- [ ] iOS Bundle ID and Apple Team ID values (reference only — store in secure ops vault, not in git)
- [ ] Auth0 Apple `user_id` / `identities` shape for this tenant
- [ ] SSM update script for Apple webhook env vars on EC2
- [ ] Operator alert runbook for verification failures (#508)
