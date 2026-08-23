# App Store Review demo patient (#607–#610)

Production **ops fixture** so Apple App Review can finish a patient session under Guideline 2.1 (App Completeness). No new mobile endpoints, no invitation `maxUses`/`ttl` change, no Liquibase patient PHI.

**Epic:** [#607](https://github.com/diego-torres/nutriconsultas/issues/607) · **Mobile:** [nutriconsultas-mobile#149](https://github.com/Escanor4323/nutriconsultas-mobile/issues/149)

| Child | Role |
|-------|------|
| [#608](https://github.com/diego-torres/nutriconsultas/issues/608) | Auth0 email/password user + `Paciente` `ACTIVE` + `patientAuthSub` link |
| [#610](https://github.com/diego-torres/nutriconsultas/issues/610) | Seed visits, diet, progress, messages via **nutritionist web** |
| [#609](https://github.com/diego-torres/nutriconsultas/issues/609) | Spare `PENDING` invitation (SIWA / paste-code fallback) + 14-day re-issue |

**Secrets:** email/password and invitation human codes live in **1Password** and App Store Connect Review Notes only. Never commit them. Never log them.

---

## Why email/password is the primary path

Invitations are **single-use** and expire in **14 days** (`PATIENT_INVITATION_EXPIRY_DAYS`, default 14; `PatientInvitation.maxUses = 1`). App Review must reach Home on first launch.

Sign in with Apple creates a **new** Auth0 `sub`. It **cannot** attach to an already-linked demo patient (anti-hijack **409**). Mobile copy tells reviewers not to use SIWA; the spare invitation (#609) is only if they already did.

---

## Production inventory (fill IDs when created)

| Item | Value | Notes |
|------|--------|--------|
| Auth0 tenant | `minutriporcion-prod.us.auth0.com` | Confirmed 2026-08-23 |
| Database connection | `Username-Password-Authentication` | Native app must have this connection **enabled** (Dashboard → Authentication → Database → Applications) |
| Native app | `minutriporcion-native` | Client id `sVD9GF40Mt0rqCukRHN79ZRZInLSGe19` |
| API audience | `https://api.nutriconsultas.minutriporcion.com` | Resource `minutriporcion-mobile-api` |
| Web app | https://minutriporcion.com | Nutritionist session |
| Review email | `app-review+ios@minutriporcion.com` | Synthetic; not a real patient |
| Auth0 `user_id` / JWT `sub` | `auth0\|6a8b3c7c6adac7dd61cf238c` | Created 2026-08-23; `email_verified=true`; `app_metadata.invited=true` |
| Password | **1Password** | Item suggestion: `Minutriporcion / App Store Review / iOS` |
| Primary `Paciente` id | **35** | `Alex Demo Review`, `status=ACTIVE`, email matches Auth0, `patientAuthSub` linked 2026-08-23 |
| Nutritionist | Diego A. Torres | Production nutritionist with an **active subscription** (create-patient succeeded) |
| Spare `Paciente` id | **36** | `Alex Demo SIWA Fallback` — `INVITED`; `patientAuthSub` unset |
| Spare invitation human code | **1Password + ASC notes** | Issued 2026-08-23; do **not** redeem with the email/password user |
| Spare invitation expires | 2026-09-06 | Re-issue before day 14 / before each App Store submit |

`app_metadata.invited=true` is **required**. The Auth0 Post-Login Action [`patient-invitation-gate.js`](../auth0/actions/patient-invitation-gate.js) denies **first** login (`logins_count === 1`) unless `app_metadata.invited === true` **or** a valid `invitation_token` is passed. A pre-linked review user does not send an invitation token, so a missing `invited` flag looks like a broken app.

---

## Do not

- Add a reviewer bypass, multi-use invitation, or public signup
- Liquibase-seed patient rows (`docs/db/LIQUIBASE.md`)
- Run `scripts/seed-patient-messages.sh` against production (local Docker only)
- Use real patient PHI
- Redeem the spare invitation with the email/password Auth0 user
- Change global invitation TTL or `maxUses`

---

## #608 — Auth0 user + ACTIVE patient + link

### A. Recreate the Auth0 user (if deleted)

Auth0 CLI `users create` may fail with “no active database connections” even when the connection exists. Use the Management API:

```bash
auth0 tenants use minutriporcion-prod.us.auth0.com

# Generate a password; store it in 1Password immediately. Do not put it in a ticket or git.
PASS='…'   # from 1Password generator (mixed case + number + symbol)

auth0 api post users --data "$(python3 -c "
import json, os
print(json.dumps({
  'email': 'app-review+ios@minutriporcion.com',
  'connection': 'Username-Password-Authentication',
  'password': os.environ['PASS'],
  'email_verified': True,
  'verify_email': False,
  'name': 'App Review Demo iOS',
  'app_metadata': {
    'invited': True,
    'app_review_fixture': True,
    'label': 'ios-app-store-review'
  }
}))
")"
```

Confirm Dashboard → Users → `app-review+ios@minutriporcion.com`:

- Connection `Username-Password-Authentication`
- Email verified
- `app_metadata.invited` is `true`
- Copy `user_id` (`auth0|…`) for linkage if email lookup is unavailable

Reset the CLI tenant when finished (`auth0 tenants use` back to the non-prod tenant you normally use).

### B. Create the primary `Paciente` (web)

1. Sign in to https://minutriporcion.com as a **nutritionist with an active subscription**.
2. **Pacientes → Nuevo** (`/admin/pacientes/nuevo`).
3. Use obviously fake data, for example:
   - Nombre: `Alex Demo Review`
   - Fecha Nac.: a plausible adult DOB (`dd/mm/aaaa`)
   - Correo: **same** as Auth0 (`app-review+ios@minutriporcion.com`) — needed for “Vincular por correo”
   - Género: any
4. Save. Web create defaults `Paciente.status` to **`ACTIVE`** (do **not** onboard this person through an invitation).
5. Open the patient → **Afiliación** (`/admin/pacientes/{id}/afiliacion`).
6. Under **App móvil del paciente**:
   - Prefer **Vincular por correo del paciente** (needs `AUTH0_MGMT_*` on the server), or
   - **Vincular por identificador** and paste `auth0|6a8b3c7c6adac7dd61cf238c`
7. Badge should read **Vinculado**. That writes `Paciente.patientAuthSub` (`POST /rest/pacientes/{id}/mobile-auth`, #109).

`MobilePatientAccessRules` allows `/rest/mobile/patient/**` only when status is `ACTIVE` (and Apple lifecycle is `NONE`). `INVITED` / `ONBOARDING` / unlinked JWT → **403** on home APIs.

### C. Confirm linkage

After the iOS app (or a password grant, if enabled) obtains a JWT for this user:

```bash
curl -sS -H "Authorization: Bearer $TOKEN" \
  https://minutriporcion.com/rest/mobile/patient/me
```

Expect HTTP **200** and `status` **`ACTIVE`**, not `ONBOARDING` and not 403 `error.patient.onboarding.required`.

If first login is denied by Auth0 with `invitation_required`, the user is missing `app_metadata.invited` — patch:

```bash
auth0 api patch 'users/auth0|6a8b3c7c6adac7dd61cf238c' --data \
  '{"app_metadata":{"invited":true,"app_review_fixture":true,"label":"ios-app-store-review"}}'
```

If the Auth0 Universal Login screen has no email/password, enable `Username-Password-Authentication` on application `minutriporcion-native`.

Unlinked SIWA (a different Apple ID) must still **403** — do not “fix” that.

---

## #610 — Seed Home tabs (web only)

Use the **same primary** App Review `Paciente`. Label everything so nobody mistakes it for a real clinic record.

| Home tab | Minimum | Where in web |
|----------|---------|----------------|
| Visitas | ≥1 scheduled/completed visit with fictional notes | **Calendario → Nuevo** (`/admin/calendario/nuevo`) — attach this patient |
| Dieta | ≥1 **ACTIVE** diet assignment (list + PDF) | Patient **Plan Alimentario** (`/admin/pacientes/{id}/dietas`) → **Asignar Nuevo Plan Alimentario** — assign a catalog/demo plan |
| Progreso | ≥1 weight/IMC snapshot | Patient **Historial** → antropometría (`/admin/pacientes/{id}/antropometricos`) |
| Mensajes | ≥1 nutritionist message | Floating **Mensajes de pacientes** widget → open this patient’s thread → send e.g. `Mensaje demo App Review` (`POST /rest/patient-messages/thread/{pacienteId}`, #114) |

Optional: 1–2 appointment questions if #587 is deployed to prod (not blocking).

**Verify** with the review JWT (all **200**; `POST` messages still **201**):

```bash
curl -sS -o /dev/null -w 'visits %{http_code}\n' -H "Authorization: Bearer $TOKEN" \
  'https://minutriporcion.com/rest/mobile/patient/visits'
curl -sS -o /dev/null -w 'diet-plans %{http_code}\n' -H "Authorization: Bearer $TOKEN" \
  'https://minutriporcion.com/rest/mobile/patient/diet-plans'
curl -sS -o /dev/null -w 'progress %{http_code}\n' -H "Authorization: Bearer $TOKEN" \
  'https://minutriporcion.com/rest/mobile/patient/progress'
curl -sS -o /dev/null -w 'messages %{http_code}\n' -H "Authorization: Bearer $TOKEN" \
  'https://minutriporcion.com/rest/mobile/patient/messages'
```

Lists must be **non-empty**. iPad screenshot pass is mobile #149; this issue is “API returns data”.

---

## #609 — Spare PENDING invitation (SIWA safety net)

Create a **second** synthetic `Paciente` (different name/email, e.g. `Alex Demo SIWA Fallback`). Do **not** link `patientAuthSub` to the email/password user.

1. **Pacientes → Nuevo** with fictional identity (or invite-only create if that is the clinic’s usual path).
2. **Afiliación → Invitación a la app móvil → Enviar invitación**.
3. Copy the **human code** (`NUTRI-XXXX-XXXX`) and optional Universal Link `/i/{token}`.
4. Store code + expiry in 1Password; paste into App Store Connect notes.
5. Confirm preview (no auth):

```bash
curl -sS -o /dev/null -w '%{http_code}\n' \
  "https://minutriporcion.com/rest/mobile/invitations/by-code/${CODE}/preview"
```

Expect **200**. 404 means expired, revoked, already redeemed, or typo.

**Calendar:** re-issue **before day 14**, and **before every App Store submit**. After a reviewer redeems it, treat it as burned and mint a new PENDING invite for the next submission.

A long-lived review invite would need a spec change (per-invite TTL or `maxUses`) — new issue, not this one.

---

## App Review notes (for mobile #149)

Primary (tell Apple to use this):

```text
Sign in with Email (do not use Sign in with Apple).
Email: app-review+ios@minutriporcion.com
Password: <from 1Password>
```

Fallback (only if they already used SIWA / are stuck on the invite screen):

```text
If you already used Sign in with Apple, paste this one-time invitation code:
<NUTRI-XXXX-XXXX>
Then complete onboarding. Do not use this code with the email/password account.
```

---

## Recreate checklist (Auth0 user deleted or unlinked)

1. Recreate Auth0 user (section A) with `email_verified` + `app_metadata.invited`.
2. If the `Paciente` still exists: Afiliación → unlink if a stale `sub` is present → link the new `sub`.
3. If the `Paciente` was deleted: repeat B + #610 + #609.
4. Update 1Password and ASC notes.
5. Fresh iOS install → email/password → Home shows visits, diet, progress, messages.

---

## Related

- Access rules: `MobilePatientAccessRules` / `PatientLinkageFilter` (`ACTIVE` vs onboarding 403)
- Linkage API: `PacienteMobileAuthRestController` `POST /rest/pacientes/{id}/mobile-auth`
- Invitation TTL: `nutriconsultas.patient.invitation.expiry-days` / `PATIENT_INVITATION_EXPIRY_DAYS`
- Gate: [`docs/auth0/PATIENT-POST-LOGIN-GATE.md`](../auth0/PATIENT-POST-LOGIN-GATE.md)
- Optional DOB for `ACTIVE` PATCH: [#605](https://github.com/diego-torres/nutriconsultas/issues/605) (mobile #151) — not this epic
