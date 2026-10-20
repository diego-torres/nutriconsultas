# Issue Registry — `[Guest Patients]` track

Living index of GitHub issues for **guest patients**: people who use the Minutriporcion **patient app without a nutritionist**, with a **30-day** record TTL, private foods/dishes, weight/BMI, and the ability to **request** an appointment.

**Repo (backend):** [diego-torres/nutriconsultas](https://github.com/diego-torres/nutriconsultas)  
**Repo (mobile):** [Escanor4323/nutriconsultas-mobile](https://github.com/Escanor4323/nutriconsultas-mobile)  
**Plan:** [`docs/paciente/GUEST-PATIENTS-PLAN.md`](docs/paciente/GUEST-PATIENTS-PLAN.md)  
**Workflow:** [`GUEST-PATIENTS-WORKFLOW.md`](GUEST-PATIENTS-WORKFLOW.md)  
**Last updated:** 2026-08-23 — epic and children **filed** (not implemented). **NEXT:** [#613](https://github.com/diego-torres/nutriconsultas/issues/613) (docs on `main`).

> **Scope.** New patient lifecycle `GUEST` (`Paciente.userId` null). **Not** App Store Review demo [#607](https://github.com/diego-torres/nutriconsultas/issues/607). **Not** public booking #300 (anonymous web creates `ONBOARDING` **under** a nutritionist). **Not** revoked-nutritionist retention [#220](https://github.com/diego-torres/nutriconsultas/issues/220) (separate maintenance action). Linked-patient mobile API: [`ISSUE.md`](ISSUE.md).

---

## Legend

| State | Meaning |
|-------|---------|
| `NEXT` | Active — pick this up now (if unblocked) |
| `open` | Not started |
| `in-progress` | Branch / PR open |
| `done` | Merged to `main` |
| `deferred` | Paused |

---

## Epic mapping (requirements → issues)

| Requirement | Backend | Mobile |
|-------------|---------|--------|
| Epic umbrella | [#612](https://github.com/diego-torres/nutriconsultas/issues/612) | [#152](https://github.com/Escanor4323/nutriconsultas-mobile/issues/152) |
| Plan / registries | [#613](https://github.com/diego-torres/nutriconsultas/issues/613) | (mobile `ISSUE.md` section) |
| Schema (`GUEST`, nullable `userId`, TTL) | [#614](https://github.com/diego-torres/nutriconsultas/issues/614) | — |
| Auth without invite + bootstrap + access rules | [#615](https://github.com/diego-torres/nutriconsultas/issues/615) | [#154](https://github.com/Escanor4323/nutriconsultas-mobile/issues/154), [#153](https://github.com/Escanor4323/nutriconsultas-mobile/issues/153) |
| Limited app (no diet/visits/messages) | (#615 rules) | [#155](https://github.com/Escanor4323/nutriconsultas-mobile/issues/155) |
| Nutritionist directory | [#616](https://github.com/diego-torres/nutriconsultas/issues/616) | [#156](https://github.com/Escanor4323/nutriconsultas-mobile/issues/156) |
| Request appointment (no #300 patient insert) | [#617](https://github.com/diego-torres/nutriconsultas/issues/617) | [#157](https://github.com/Escanor4323/nutriconsultas-mobile/issues/157) |
| Nutritionist accept / adopt (cap #190) | [#618](https://github.com/diego-torres/nutriconsultas/issues/618) | — |
| Weight / BMI | [#619](https://github.com/diego-torres/nutriconsultas/issues/619) | [#158](https://github.com/Escanor4323/nutriconsultas-mobile/issues/158) |
| Catalogs + nutrient calc (read-only catalog) | [#620](https://github.com/diego-torres/nutriconsultas/issues/620) | [#159](https://github.com/Escanor4323/nutriconsultas-mobile/issues/159) |
| Patient platillos **not** in nutritionist catalog | [#621](https://github.com/diego-torres/nutriconsultas/issues/621) | [#159](https://github.com/Escanor4323/nutriconsultas-mobile/issues/159) |
| Patient foods; **no** catalog `alimento` nutrient writes | [#622](https://github.com/diego-torres/nutriconsultas/issues/622) | [#160](https://github.com/Escanor4323/nutriconsultas-mobile/issues/160) |
| Maintenance purge expired guests + foods/dishes | [#623](https://github.com/diego-torres/nutriconsultas/issues/623) | [#161](https://github.com/Escanor4323/nutriconsultas-mobile/issues/161) |
| Tests / OpenAPI / contract | [#624](https://github.com/diego-torres/nutriconsultas/issues/624) | [#162](https://github.com/Escanor4323/nutriconsultas-mobile/issues/162) |

**Suggested order:** #613 → #614 → #615 → (#619 ∥ #620 ∥ #622 → #621) → #616 → #617 → #618 → #623 → #624. Mobile: #153 with #615, then #154 → #155, then feature screens as APIs land.

---

## Backend issues

| # | Title | URL | State | Depends on | Notes |
|---|-------|-----|-------|------------|-------|
| **612** | Epic — guest patients | https://github.com/diego-torres/nutriconsultas/issues/612 | **open** | — | Children #613–#624 |
| **613** | Plan, registry, and workflow docs | https://github.com/diego-torres/nutriconsultas/issues/613 | **NEXT** | 612 | Land this file + plan + pointers |
| **614** | Schema — GUEST, nullable `userId`, `guest_expires_at` | https://github.com/diego-torres/nutriconsultas/issues/614 | open | 613 | Liquibase incremental only |
| **615** | Auth — guest without invite, bootstrap, access rules | https://github.com/diego-torres/nutriconsultas/issues/615 | open | **614** | Auth0 Post-Login guest intent; keep invite-only default |
| **616** | Nutritionist directory API | https://github.com/diego-torres/nutriconsultas/issues/616 | open | **615** | Public booking + active subscription; no email/phone |
| **617** | Appointment request API | https://github.com/diego-torres/nutriconsultas/issues/617 | open | **616** | Reuse slots; **do not** run #300 create-patient |
| **618** | Nutritionist web — accept/adopt | https://github.com/diego-torres/nutriconsultas/issues/618 | open | **617** | Cap #190 on **accept**; sweetalert |
| **619** | Weight and BMI self-report | https://github.com/diego-torres/nutriconsultas/issues/619 | open | **615** | POST; GUEST only in v1 |
| **620** | Read-only catalogs + nutrient calculation | https://github.com/diego-torres/nutriconsultas/issues/620 | open | **615** | `system:catalog-platillos`; 403 on catalog writes |
| **621** | Patient-owned platillos (isolated) | https://github.com/diego-torres/nutriconsultas/issues/621 | open | 614, 620 | `platillo.paciente_id`; never nutritionist lists |
| **622** | Patient-owned foods (no catalog nutrient writes) | https://github.com/diego-torres/nutriconsultas/issues/622 | open | **614**, 615 | Prefer table `paciente_alimento` |
| **623** | Platform maintenance purge | https://github.com/diego-torres/nutriconsultas/issues/623 | open | 614, 621, 622 | Separate from #220 |
| **624** | Tests, OpenAPI, contract appendix | https://github.com/diego-torres/nutriconsultas/issues/624 | open | 615–623 | Close-out; tests also ship per feature PR |

---

## Mobile issues (Escanor4323/nutriconsultas-mobile)

| # | Title | URL | State | Backend | Notes |
|---|-------|-----|-------|---------|-------|
| **152** | Epic — guest mode | https://github.com/Escanor4323/nutriconsultas-mobile/issues/152 | **open** | **612** | Children #153–#162 |
| **153** | API client / models | https://github.com/Escanor4323/nutriconsultas-mobile/issues/153 | open | 615+ | |
| **154** | Sign in without invitation | https://github.com/Escanor4323/nutriconsultas-mobile/issues/154 | open | **615** | Auth0 guest intent |
| **155** | Limited home / shell | https://github.com/Escanor4323/nutriconsultas-mobile/issues/155 | open | 615 | es-MX copy |
| **156** | Nutritionist directory | https://github.com/Escanor4323/nutriconsultas-mobile/issues/156 | open | **616** | |
| **157** | Request appointment | https://github.com/Escanor4323/nutriconsultas-mobile/issues/157 | open | **617** | |
| **158** | Weight and BMI | https://github.com/Escanor4323/nutriconsultas-mobile/issues/158 | open | **619** | |
| **159** | Dish builder + calc | https://github.com/Escanor4323/nutriconsultas-mobile/issues/159 | open | **620**, **621** | No catalog nutrient edit UI |
| **160** | Custom foods | https://github.com/Escanor4323/nutriconsultas-mobile/issues/160 | open | **622** | Own dishes only |
| **161** | Expiry / purged UX | https://github.com/Escanor4323/nutriconsultas-mobile/issues/161 | open | **623** | 30 days |
| **162** | Tests | https://github.com/Escanor4323/nutriconsultas-mobile/issues/162 | open | 624 | Widget / mock e2e |

---

## Cross-track links

| Track | Interaction |
|-------|-------------|
| Public booking #245–#302 | Reuse **availability**; do **not** reuse #300 insert-under-nutritionist |
| Subscription #190 | Guest **excluded** from cap until #618 adopt |
| Retention #220 | Same **page**, different **action** (#623) |
| Invitation #132–#141 / Auth0 #140 | Guest intent bypass only; default remains invite-only |
| App Review #607 | Orthogonal seeded `ACTIVE` patient |
| Platillo catalog `system:catalog-platillos` | Read-only for guests; patient dishes use `paciente_id` |

---

## Definition of done (track-level)

- [ ] Guest can use the app without a nutritionist (limited)
- [ ] Directory + appointment request; adopt checks cap
- [ ] Weight/BMI self-report
- [ ] Private platillos/foods + catalog calc; catalog nutrients immutable
- [ ] Nutritionist catalogs never list patient dishes/foods
- [ ] 30-day TTL; maintenance purge of guest + foods + dishes
- [ ] This registry marked `done` for children; epics #612 / #152 closed

---

**How to update this file**

Same convention as [`ISSUE.md`](ISSUE.md): `in-progress` when a PR opens, `done` when merged. Keep **NEXT** on the unblocked issue agents should pick up.
