# Guest patients — Agent Workflow

How AI agents (and humans) ship the **`[Guest Patients]`** track on **`diego-torres/nutriconsultas`**, with the Flutter consumer on **`Escanor4323/nutriconsultas-mobile`**. Do **not** mix guest schema, Auth0 gate changes, or catalog isolation into unrelated PRs (App Review #607, invitation onboarding, public booking #300, subscription retention #220) unless a child issue explicitly couples them.

**Registries**

| File | Purpose |
|------|---------|
| [`ISSUE-GUEST-PATIENTS.md`](ISSUE-GUEST-PATIENTS.md) | Backend epic + children, states, dependencies, mobile cross-links |
| [`docs/paciente/GUEST-PATIENTS-PLAN.md`](docs/paciente/GUEST-PATIENTS-PLAN.md) | Product decisions, data model, API surface |
| [`AGENT-WORKFLOW.md`](AGENT-WORKFLOW.md) | Patient mobile API workflow (orthogonal; linked patients) |
| Mobile [`ISSUE.md`](https://github.com/Escanor4323/nutriconsultas-mobile/blob/main/ISSUE.md) | Guest mode UI issues |

**Current next issue:** [#613](https://github.com/diego-torres/nutriconsultas/issues/613) — land plan, this workflow, and registry pointers on `main`. Epic [#612](https://github.com/diego-torres/nutriconsultas/issues/612). Mobile epic [#152](https://github.com/Escanor4323/nutriconsultas-mobile/issues/152).

---

## Recommended delivery sequence

| Wave | Backend outcome | Mobile outcome |
|------|-----------------|----------------|
| **A — Docs** | Plan, this workflow, pointers in `AGENT-WORKFLOW.md` / `AGENTS.md` / `ISSUE.md` / `README.md` | Guest section in mobile `ISSUE.md` |
| **B — Identity** | Liquibase `GUEST` + nullable `user_id` + `guest_expires_at`; Auth0 guest intent; bootstrap; `MobilePatientAccessRules` | Sign in / continue without invite; bootstrap client |
| **C — Self-serve** | Catalog read; `paciente_alimento`; private platillos; nutrient calc; weight/BMI POST | Limited home; progress write; dish builder; custom foods |
| **D — Care request** | Directory; appointment request (no #300 insert); nutritionist accept/adopt + #190 cap | Directory; request appointment |
| **E — Retention** | Maintenance purge action (not #220) | Expiry warning / deleted account UX |
| **F — Contract** | OpenAPI + remaining tests | Widget tests / mock e2e |

Each feature PR still ships its own tests (project rule). Wave F is leftover coverage and spec regen, not the first time tests appear.

---

## Product context (read before every session)

| Topic | Guidance |
|-------|----------|
| **Language** | User-facing copy **Spanish (es-MX)** — mobile, nutritionist inbox, maintenance, errors |
| **Identity** | JWT `sub` → `Paciente.patientAuthSub`. **`userId` is the nutritionist tenant** — null while `GUEST` |
| **Catalog isolation** | Patient dishes/foods **never** in nutritionist platillo/alimento lists. System platillos use `userId = system:catalog-platillos` only |
| **No catalog nutrient writes** | Guests cannot create/update shared `alimento` rows |
| **Caps** | Guests **do not** count toward #190 until **adopt** |
| **TTL** | 30 days from **creation**, not sliding; purge is **manual** on `/admin/platform/maintenance` |
| **Dialogs (admin)** | bootstrap-sweetalert only — no native `alert`/`confirm` |
| **PHI** | Never log names, emails, phones, or full `Paciente` objects |
| **Liquibase** | Incremental changeset; do not edit merged baseline |
| **Not #607** | App Review demo remains a seeded **ACTIVE** patient under a nutritionist |

---

## Cross-repo

1. Backend contract (OpenAPI + `ALIGNMENT-SPEC` guest appendix) before mobile screens that call new endpoints.
2. Cross-link GitHub issues both ways (backend child ↔ mobile child).
3. Auth0 Post-Login Action change is **ops + docs** in this repo (`docs/auth0/PATIENT-POST-LOGIN-GATE.md`); mobile must send guest intent on `authorize`.

---

## Out of scope unless a new issue says otherwise

- Auto cron purge
- Guest AI assistant
- Nutritionist reuse of patient platillos
- Changing public anonymous `/agendar-cita` (#248/#300)
- Weakening invitation-only login for users who did **not** opt into guest
