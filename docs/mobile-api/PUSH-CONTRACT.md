# Mobile push contract — devices + killed-state alerts (#577)

**Epic:** [#573](https://github.com/diego-torres/nutriconsultas/issues/573)  
**Issues:** [#574](https://github.com/diego-torres/nutriconsultas/issues/574) devices · [#575](https://github.com/diego-torres/nutriconsultas/issues/575) sender · [#576](https://github.com/diego-torres/nutriconsultas/issues/576) emit · [#577](https://github.com/diego-torres/nutriconsultas/issues/577) this doc  
**Mobile:** [Escanor4323/nutriconsultas-mobile#26](https://github.com/Escanor4323/nutriconsultas-mobile/issues/26)

Stable contract for Dio models + register/deregister on login/logout, and for handling killed-state pushes. Machine-readable twin: [`../api/openapi-mobile.yaml`](../api/openapi-mobile.yaml) (`/rest/mobile/patient/devices`). Ops/secrets: [`PUSH-SETUP.md`](PUSH-SETUP.md).

---

## Product rules (read first)

| Rule | Detail |
|------|--------|
| Backend owns messaging | Device tokens register with **our** API; sends go through **our** APNs + FCM HTTP v1 clients |
| No Firebase BaaS | Do **not** use Firebase Auth/Firestore/etc. Android may use a **minimal** Firebase Messaging client **only** to obtain an FCM registration token and display notification messages |
| No PHI in push | Notification title/body are generic Spanish; data payload is type + optional `messageId` only — never message text |
| Auth | Same patient JWT as other `/rest/mobile/patient/**` (`sub` → `Paciente.patientAuthSub`) |

---

## AuthZ / error codes

Applies to both device endpoints (and matches other patient mobile routes).

| HTTP | When |
|------|------|
| **401** | Missing or invalid JWT |
| **403** | Patient not linked / onboarding gate (`PatientLinkageFilter`) — e.g. complete registration required |
| **400** | Validation failed (blank token, missing `platform`, oversized fields) |
| **429** | Write rate limit envelope may appear in OpenAPI shared annotations; devices are not currently rate-limited like messages — treat as optional |

Envelope for JSON error bodies: same mobile `ApiResponse` / localized `message` + `timestamp` as other patient endpoints.

---

## `POST /rest/mobile/patient/devices` — register / upsert

**Auth:** patient JWT  
**Success:** **200** + `ApiResponse<PatientDeviceDto>`

### Request

```json
{
  "platform": "IOS",
  "token": "<apns-or-fcm-registration-token>",
  "appVersion": "1.2.3"
}
```

| Field | Type | Required | Notes |
|-------|------|----------|-------|
| `platform` | enum | yes | `IOS` \| `ANDROID` |
| `token` | string | yes | max 512; APNs device token or FCM registration token |
| `appVersion` | string | no | max 50 |

### Response `data`

```json
{
  "id": 123,
  "platform": "IOS",
  "updatedAt": "2026-08-11T17:00:00Z"
}
```

### Semantics

- Upsert by **token** (globally unique).
- Same token re-POSTed refreshes `appVersion` / `lastSeenAt` / `updatedAt`.
- If the token was registered to another patient, it is **reassigned** to the current patient (device switched accounts).
- Call on login / when the OS grants a new token; refresh whenever the token rotates.

### Example (curl)

```bash
curl -sS -X POST "$API/rest/mobile/patient/devices" \
  -H "Authorization: Bearer $PATIENT_JWT" \
  -H "Content-Type: application/json" \
  -d '{"platform":"ANDROID","token":"fcm-reg-token","appVersion":"1.2.3"}'
```

---

## `DELETE /rest/mobile/patient/devices` — deregister

**Auth:** patient JWT  
**Success:** **204** No Content (idempotent — already gone still 204)  
**Body required:**

```json
{
  "token": "<same token>"
}
```

| Field | Type | Required | Notes |
|-------|------|----------|-------|
| `token` | string | yes | max 512 |

### Semantics

- Deletes only if the token belongs to the **current** patient.
- Token owned by another patient → still **204** (no leak); row unchanged.
- Call on logout / uninstall / before clearing local credentials.

### Example (curl)

```bash
curl -sS -X DELETE "$API/rest/mobile/patient/devices" \
  -H "Authorization: Bearer $PATIENT_JWT" \
  -H "Content-Type: application/json" \
  -d '{"token":"fcm-reg-token"}'
```

---

## Push event (killed / background)

Triggered when a **nutritionist** saves a reply on the web thread (`senderRole = NUTRITIONIST`).  
**Not** triggered for patient-sent messages (`POST /rest/mobile/patient/messages`) or read-flag updates.

### Data payload (custom / FCM `data` / APNs custom keys)

```json
{
  "type": "NEW_MESSAGE",
  "messageId": "12345"
}
```

| Field | Type | Notes |
|-------|------|-------|
| `type` | string | Currently only `NEW_MESSAGE` |
| `messageId` | string (FCM) / number (APNs custom) | Optional correlation; use to open Messages / fetch thread. **Never** treat as body text |

FCM requires **string** data values; mobile should parse `messageId` as string then to int if needed.

### Visible notification copy (OS tray)

Fixed Spanish — **do not** expect message PHI from the server:

| | Value |
|--|-------|
| Title | `Nuevo mensaje` |
| Body | `Nuevo mensaje de tu nutriólogo` |

### Suggested mobile handling

1. Permission + obtain platform token (APNs / FCM).
2. `POST /devices` after successful patient linkage.
3. On push / local tap: route to Messages; optionally refresh thread using `messageId`.
4. `DELETE /devices` on logout.
5. Foreground: existing Phase B poll + local notify remains valid; this contract unlocks **killed-state** delivery.

---

## Android / Firebase clarification

| Allowed | Not allowed |
|---------|-------------|
| Firebase Messaging SDK for **token + displaying** FCM notification messages | Firebase as product BaaS (Auth, Firestore, RTDB, etc.) |
| Register token with Minutriporcion `/devices` | Sending pushes from the mobile app or a Firebase console campaign as the primary path |

iOS uses native APNs token acquisition (no Firebase required for iOS token).

---

## Staging / secrets checklist (pointer)

Deploy enablement and key rotation live in [`PUSH-SETUP.md`](PUSH-SETUP.md):

- `PUSH_ENABLED`, `APNS_*`, `FCM_PROJECT_ID`, `FCM_SERVICE_ACCOUNT_JSON`
- Soft no-op when disabled or misconfigured
- Never commit `.p8` / service-account JSON — use Secrets Manager / SSM / env

Local template placeholders: [`.env.example`](../../.env.example).

---

## Mobile doc cross-links (consumer repo)

Please add reciprocal links in the mobile repo (separate PR is fine):

- [`docs/api-contract.md`](https://github.com/Escanor4323/nutriconsultas-mobile/blob/main/docs/api-contract.md) → this file + OpenAPI devices paths
- [`docs/plans/cross-repo-backend-mobile-map.md`](https://github.com/Escanor4323/nutriconsultas-mobile/blob/main/docs/plans/cross-repo-backend-mobile-map.md) → epic #573 / backend #574–#577

---

## OpenAPI regen

```bash
./scripts/export-openapi-mobile.sh
```

Writes [`docs/api/openapi-mobile.yaml`](../api/openapi-mobile.yaml).
