# Mobile E2E — Backend & Auth0 status

**Date:** 2026-06-14  
**Audience:** [nutriconsultas-mobile](https://github.com/Escanor4323/nutriconsultas-mobile) team  
**Backend:** https://minutriporcion.com  
**Auth0 tenant:** `dev-imd1udg26uvzvfto.us.auth0.com`

This document summarizes what the backend team verified after Auth0 CLI authentication and production checks. Use it to resume live E2E testing with `MOCK_AUTH=false`.

---

## Quick verdict

| Area | Status | Action for mobile team |
|------|--------|------------------------|
| Auth0 API + audience | ✅ Ready | Keep `AUTH0_AUDIENCE=https://api.nutriconsultas.minutriporcion.com` |
| Native client + client grant | ✅ Ready | Use `CmZdUaMZ3Oqs4JSbUBdYZYmKoqDUiLBk` |
| Google login | ✅ Ready | `connection=google-oauth2` |
| Apple login | ✅ Ready (dev keys) | Connection `apple` enabled on `minutriporcion-native` — uses Auth0 dev keys until Apple Developer creds are added |
| JWT validation on prod (`AUTH_AUDIENCE`) | ✅ Ready | Valid token should **not** return 401 |
| Patient linkage (prod test user) | ✅ Ready | Paciente linked to Google account below |
| Visits / diet-plans / messages / progress API | ✅ Deployed on `main` (incl. #99 via PR #153) | Expect **200** (POST messages **201**) after login + linkage |
| Localized error envelope (#111) | ✅ Deployed | 403/404/400/429 return `ApiResponse` with localized `message` |
| Message rate limit (#113) | ✅ Deployed | POST messages: 10/min per `patientAuthSub`; **429** + `Retry-After: 60` |

---

## Auth0 configuration (verified 2026-06-14)

### Tenant & API (#108)

| Setting | Value |
|---------|-------|
| API name | `nutriconsultas-mobile-api` |
| API identifier (audience) | `https://api.nutriconsultas.minutriporcion.com` |
| Signing algorithm | RS256 |
| Scopes | `read:visits`, `read:diet-plans`, `read:progress`, `read:messages`, `write:messages` |

Mobile `.env` **must** use the same audience on `login()`:

```env
AUTH0_AUDIENCE=https://api.nutriconsultas.minutriporcion.com
```

### Native application

| Setting | Value |
|---------|-------|
| Name | `minutriporcion-native` |
| Client ID | `CmZdUaMZ3Oqs4JSbUBdYZYmKoqDUiLBk` |
| Type | Native |
| Client grant | ✅ Authorized for `nutriconsultas-mobile-api` with all scopes above |

**Connections enabled on native app:**

| Connection | Enabled | Notes |
|------------|---------|-------|
| `google-oauth2` | ✅ | Use for Android + iOS Google button |
| `Username-Password-Authentication` | ✅ | Email/password flow |
| `apple` | ✅ | Enabled on native app; **Auth0 dev keys** (blank Client ID) — OK for E2E, not for production |

### Callback URLs (Auth0 dashboard)

Currently registered:

```
com.example.mobile://dev-imd1udg26uvzvfto.us.auth0.com/ios/com.example.mobile/callback
com.example.mobile://dev-imd1udg26uvzvfto.us.auth0.com/android/com.example.mobile/callback
```

Confirm these match your **production bundle ID / application ID** in `auth0.properties` / `Info.plist`. If the app uses a different scheme (e.g. `com.minutriporcion.app`), update Auth0 callbacks or you'll see callback mismatch errors.

---

## Production backend

### Environment

| Variable | Prod value |
|----------|------------|
| `AUTH_ISSUER` | `https://dev-imd1udg26uvzvfto.us.auth0.com/` |
| `AUTH_AUDIENCE` | `https://api.nutriconsultas.minutriporcion.com` |
| `AUTH0_MGMT_*` | Not configured (admin email lookup unavailable; manual `sub` linkage only) |

### Anonymous requests (no Bearer)

All return **401** as expected:

```
GET /rest/mobile/patient/visits      → 401
GET /rest/mobile/patient/diet-plans  → 401
GET /rest/mobile/patient/messages    → 401
GET /rest/mobile/patient/progress    → 401
GET /actuator/health                 → 200
```

### HTTP codes after login

| Code | Meaning |
|------|---------|
| **401** | Missing/invalid JWT, wrong issuer, or wrong `aud` |
| **403** | JWT valid but no linked `Paciente.patientAuthSub` — localized `ApiResponse.message` (`error.patient.not.linked`, #111) |
| **200** | Auth + linkage OK; endpoint implemented |
| **201** | POST `/messages` success (#97) |
| **400** | Validation failure (localized message, #111) |
| **404** | Resource not found (localized message, #111) |
| **429** | Write rate limit exceeded (#113); `Retry-After: 60` |

---

## Test patient linked for E2E

A production patient record is linked for live testing:

| Field | Value |
|-------|-------|
| Paciente ID | `1` |
| Name | E2E Test Patient |
| Email | `e2e-patient@example.com` (Google account used for linkage; do not commit real addresses) |
| Auth0 `sub` (must match JWT) | `google-oauth2\|<google-user-id>` |

**Important:** Linkage uses the **patient's mobile Auth0 `sub`**, not the nutritionist's web-admin `sub`. The first linkage attempt used the nutritionist sub by mistake; it was corrected on 2026-06-14.

### Who can test?

Log in on the mobile app with the **Google account linked in Admin → Pacientes → Afiliación** for paciente ID `1`. After login, API calls should pass linkage and return data (1 visit and 1 diet assignment exist in prod for this patient).

To link another tester, ask the nutritionist to use **Admin → Pacientes → Afiliación → Vincular** with the patient's Auth0 `sub`, or contact the backend team.

---

## Blockers

### A1 — Apple Sign-In (Auth0) — resolved 2026-06-14

| Item | Detail |
|------|--------|
| Connection | `apple` (`con_DsJCAz8WVvd2Atp3`) |
| Native app | ✅ `CmZdUaMZ3Oqs4JSbUBdYZYmKoqDUiLBk` enabled |
| Credentials | **Auth0 development keys** (Client ID / signing key left blank) — sufficient for simulator E2E |
| Production | Replace with Apple Developer **Services ID**, **Team ID**, **Key ID**, and **.p8 signing key** in Auth0 → Authentication → Social → apple → Settings |
| App-side | No change needed — `connection=apple` is correct |

### B3 — Progress endpoint — resolved 2026-06-14

| Item | Detail |
|------|--------|
| Backend PR | [#148](https://github.com/diego-torres/nutriconsultas/pull/148) **merged** (`GET /rest/mobile/patient/progress`) |
| Prod | Deploy with latest `main` (includes #98, #111, #113 via #151) |
| Symptom if stale | Linked JWT → **404** on `/progress` until redeploy |

---

## Recommended test matrix (update as you go)

Run with `MOCK_AUTH=false` against **https://minutriporcion.com**.

### Welcome — social login

| Step | iOS | Android | Expected |
|------|-----|---------|----------|
| Tap **Google** | [ ] | [ ] | Google account picker → return to app |
| Tap **Apple** | [ ] | n/a | Should open Apple sign-in (Auth0 dev keys) |
| Callback returns to app | [ ] | [ ] | No URL mismatch |
| Lands on home tabs | [ ] | [ ] | |

### Post-login API (Google account above)

| Endpoint | Expected | Actual | Notes |
|----------|----------|--------|-------|
| `GET /rest/mobile/patient/visits` | 200 + paged data | [ ] | |
| `GET /rest/mobile/patient/diet-plans` | 200 + paged data | [ ] | |
| `GET /rest/mobile/patient/messages` | 200 (may be empty) | [ ] | |
| `POST /rest/mobile/patient/messages` | 201 + envelope | [ ] | Rate limit: 11th POST in 1 min → **429** |
| `GET /rest/mobile/patient/progress` | 200 + snapshot | [ ] | |
| `GET /rest/mobile/patient/progress/measurements` | 200 + ASC series (`truncated` when >365) | [ ] | After #153 deploy |

### Token sanity check

After login, decode the access token (or use app logs):

- `iss` = `https://dev-imd1udg26uvzvfto.us.auth0.com/`
- `aud` includes `https://api.nutriconsultas.minutriporcion.com`
- `sub` = `google-oauth2|<google-user-id>` (must match the linked test patient's JWT)

Optional backend script (requires access token from the app):

```bash
./scripts/mobile-auth0-e2e-setup.sh verify-token '<access_token>'
```

---

## Mobile app checklist (unchanged)

| Item | Status |
|------|--------|
| Native `AUTH_CLIENT` (`CmZdUaMZ…`) | ✅ |
| `AUTH0_AUDIENCE` on `login()` | ✅ |
| Google → `connection=google-oauth2` | ✅ |
| Apple → `connection=apple` (iOS) | ✅ wired + Auth0 connection enabled |
| `MOCK_AUTH=false` for live test | Required |

---

- Backend contacts & references

- Mobile API registry: [`ISSUE.md`](../../ISSUE.md)
- Agent workflow: [`AGENT-WORKFLOW.md`](../../AGENT-WORKFLOW.md)
- Contract docs index: [`docs/mobile-api/README.md`](README.md)
- Alignment spec: [`ALIGNMENT-SPEC.md`](ALIGNMENT-SPEC.md)
- OpenAPI: [`docs/api/openapi-mobile.yaml`](../api/openapi-mobile.yaml)
- Liquibase: [`docs/db/LIQUIBASE.md`](../db/LIQUIBASE.md)
- Subscription (parallel): [`ISSUE-SUBSCRIPTION.md`](../../ISSUE-SUBSCRIPTION.md)
- E2E setup script: [`scripts/mobile-auth0-e2e-setup.sh`](../../scripts/mobile-auth0-e2e-setup.sh)
- Related mobile issues: [#22](https://github.com/Escanor4323/nutriconsultas-mobile/issues/22), PR [#61](https://github.com/Escanor4323/nutriconsultas-mobile/pull/61)

---

*Updated 2026-06-17: mobile cross-cutting done (#111–#116); **NEXT:** #132 invitation onboarding.*
