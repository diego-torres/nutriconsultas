# Apple Sign-In — safe account deletion runbook (#506)

This runbook describes how Minutriporcion handles **destructive** Apple server-to-server notifications (`consent-revoked`, `account-delete`) without silent data loss.

**Related:** [`apple-signin-backend-roadmap.md`](apple-signin-backend-roadmap.md) · [`apple-signin-setup.md`](apple-signin-setup.md)

---

## Default behavior (production)

| Setting | Default | Effect |
|---------|---------|--------|
| `APPLE_SIGNIN_AUTO_PROCESS_DESTRUCTIVE_EVENTS` | `false` | Webhook verifies, persists, maps identity; **no** local/Auth0 mutations |
| Auth0 user deletion | disabled | `AUTH0_MGMT_USER_DELETE_ENABLED=false` — `deleteUser()` never runs |

When auto-processing is **enabled**, the backend applies a **staged** workflow only:

| Apple event | Local patient (`apple_lifecycle_status`) | Auth0 | Hard delete? |
|-------------|------------------------------------------|-------|--------------|
| `consent-revoked` | `ACCESS_REVOKED` (+ `PacienteStatus.REVOKED`) | `app_metadata` + block flag | **No** |
| `account-delete` | `PENDING_DELETION_REVIEW` | `app_metadata` only | **No** |

---

## Admin review

Platform admins: **Plataforma → Apple Sign-In** (`/admin/platform/apple-signin`).

The grid lists destructive notifications with:

- Event type and received time
- Mapped `paciente_id` (if any)
- Identity mapping status
- Lifecycle action taken (or skipped)

Use `paciente_id` to locate the patient in the nutritionist admin UI. **Do not** use relay email as the primary key.

Relay email forwarding status is stored on `Paciente` (`apple_relay_email`, `apple_relay_forwarding_enabled`) and visible in `/admin/platform/apple-signin` for `email-disabled` / `email-enabled` events (#507).

---

## Manual final deletion (explicit approval only)

Perform these steps only after product/legal sign-off:

1. Confirm the event in `apple_signin_notification` and the admin grid.
2. Document the decision (ticket / internal record).
3. **Patient data:** use existing patient deletion/export tools per clinic policy (`PacienteDeletionService`, MPX export).
4. **Auth0:** manual deletion in Auth0 Dashboard or enable `AUTH0_MGMT_USER_DELETE_ENABLED` temporarily and run a controlled script — never automate from the webhook alone.
5. Mark the notification row for audit (do not delete audit rows).

---

## Idempotency

Repeated Apple notifications for the same subject:

- Duplicate `apple_event_id` → ignored (HTTP 200, no re-processing).
- Same patient already at target `apple_lifecycle_status` → `lifecycle_action=ALREADY_APPLIED`.

---

## Rollout phases (#511)

1. **Observe only** — current production default.
2. **Metadata** — enable auto-processing to mark lifecycle + Auth0 `app_metadata`.
3. **Restricted automation** — block mobile access via `apple_lifecycle_status` (implemented in `MobilePatientAccessRules`).
4. **Optional deletion** — manual only; see above.

---

## Policy notes (open questions)

- **Patients:** staged mark + admin review; no automatic hard-delete.
- **Nutritionists / platform admins:** Apple Sign-In for non-patient account types not yet in scope; webhook mapping targets mobile **patients** via `Paciente.patientAuthSub`.
- **Subscription owners:** deletion does not cancel Stripe automatically — handle billing separately.
