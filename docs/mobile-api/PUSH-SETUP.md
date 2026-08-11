# Mobile push setup — APNs + FCM HTTP v1 (#575)

**Issue:** [#575](https://github.com/diego-torres/nutriconsultas/issues/575) · Epic [#573](https://github.com/diego-torres/nutriconsultas/issues/573)  
**Mobile:** [Escanor4323/nutriconsultas-mobile#26](https://github.com/Escanor4323/nutriconsultas-mobile/issues/26)

Server-side push gateway for **killed/background** patient message alerts. Our API owns the device registry and send path — Firebase is only Google’s push pipe for Android tokens, not a BaaS.

---

## Product rules

| Rule | Detail |
|------|--------|
| No PHI in payload | Data is `{ "type": "NEW_MESSAGE", "messageId": "<id>" }` only |
| Visible copy | Fixed Spanish: title `Nuevo mensaje`, body `Nuevo mensaje de tu nutriólogo` |
| Soft disable | `PUSH_ENABLED=false` or missing creds → sender no-ops (local/dev safe) |
| Invalid tokens | APNs `BadDeviceToken`/`Unregistered` and FCM `UNREGISTERED` delete the `patient_device` row |

Device register/deregister: `POST`/`DELETE` `/rest/mobile/patient/devices` (#574). Emit-on-reply is [#576](https://github.com/diego-torres/nutriconsultas/issues/576).

---

## Environment variables

Copy from `.env.example` into local `.env` (gitignored).

| Variable | Purpose |
|----------|---------|
| `PUSH_ENABLED` | Master switch (default `false`) |
| `APNS_KEY_ID` | Apple `.p8` Key ID |
| `APNS_TEAM_ID` | Apple Team ID |
| `APNS_BUNDLE_ID` | iOS app bundle id (`apns-topic`) |
| `APNS_P8_KEY` | PKCS8 PEM private key; use `\n` for newlines in a single env value |
| `APNS_PRODUCTION` | `true` → `api.push.apple.com`; `false` → sandbox |
| `FCM_PROJECT_ID` | Firebase/Google Cloud project id |
| `FCM_SERVICE_ACCOUNT_JSON` | Full service account JSON with FCM send permission |

Spring maps these under `nutriconsultas.push.*` in `application.properties`.

---

## Local / staging checklist

1. Keep `PUSH_ENABLED=false` until APNs and/or FCM creds are present.
2. Register a device via the mobile app (or curl) against `#574` endpoints.
3. Call `PatientPushSender.send(pacienteId, PushEvent.newMessage(messageId))` from a temporary admin/test path, or wait for #576.
4. Confirm startup log: `Patient push enabled (apnsConfigured=..., fcmConfigured=...)` — never logs key material or tokens.

---

## Rotate credentials (runbook)

### APNs `.p8` key

1. In [Apple Developer](https://developer.apple.com/account/resources/authkeys/list) create a new **Apple Push Notifications** key (or revoke the old one after cutover).
2. Download the `.p8` once; store Key ID + Team ID + PEM in Secrets Manager / SSM / staging secrets — **not** in git.
3. Update `APNS_KEY_ID` and `APNS_P8_KEY` (PEM with `\n` newlines).
4. Redeploy / restart the app. Old JWTs expire within ~1 hour; new sends use the new key immediately after restart.
5. Revoke the previous key only after verifying a sandbox/production test notification.

### FCM service account

1. In Google Cloud Console → IAM → Service Accounts, create (or rotate keys on) an account with **Firebase Cloud Messaging API Admin** (or the minimal role that can call `messages:send`).
2. Create a new JSON key; store the full JSON in `FCM_SERVICE_ACCOUNT_JSON` (secret store).
3. Set `FCM_PROJECT_ID` to the matching project.
4. Redeploy / restart. Delete the previous JSON key in GCP after a successful Android test send.

---

## Android note

Obtaining an FCM *registration token* on-device usually needs a minimal Firebase Messaging client. That does **not** make Firebase our messaging product — tokens register with `/rest/mobile/patient/devices` and sends go through this FCM HTTP v1 client.
