# Support tickets — product & technical plan

In-app **Soporte** for authenticated nutritionists and platform administrators, plus an **Acerca de** version modal from the topbar user menu.

**Issue registry:** [`ISSUE-SUPPORT.md`](../../ISSUE-SUPPORT.md)  
**Workflow:** [`SUPPORT-WORKFLOW.md`](../../SUPPORT-WORKFLOW.md)  
**Epic:** [#540](https://github.com/diego-torres/nutriconsultas/issues/540)

---

## Goals

1. Replace topbar user-circle menu placeholders (**Settings**, **Activity Log**) with **Soporte** and **Acerca de**.
2. Let nutritionists open tickets and see **only their own** tickets.
3. Let platform admins triage all tickets: **user**, **subscription**, **title**; **update** / **close**; filter **activos** vs **cerrados**.
4. Show the running web app version in **Acerca de**, with a documented bump process for releases to `main`.

## Non-goals (v1)

- Replacing or merging public **ContactInquiry** (`/admin/platform/contact-inquiries`) — that remains the anonymous marketing contact inbox.
- Email / push notifications when a ticket is created or closed (defer).
- Patient mobile-app support surface.
- Rich attachments / screenshots (defer unless a child issue expands scope).

---

## User menu

| Item | Behavior |
|------|----------|
| **Perfil** | Existing `/admin/perfil` |
| **Soporte** | `/admin/soporte` (role-aware page) |
| **Acerca de** | Opens Bootstrap modal (version + product name) |
| **Salir** | Existing logout modal |

Copy and labels: **Spanish (es-MX)**. Confirmations: **bootstrap-sweetalert** (`swal`), never native `alert`/`confirm`.

Template entry point: `src/main/resources/templates/sbadmin/topbar.html`.

---

## Roles

| Actor | Capability |
|-------|------------|
| Nutritionist (authenticated `/admin` user) | Create ticket; list/view **own** tickets only |
| Platform admin (`PlatformAdminService`) | List all; filter by status; update (e.g. admin notes); close |

Reuse existing platform-admin patterns (`AbstractPlatformAdminController`, `requirePlatformAdmin`, contact-inquiry style audits where useful).

**Suggested UX routing:** single entry `/admin/soporte` that branches in the controller:

- Non-admin → nutritionist grid + create form
- Platform admin → admin inbox (user, subscription, title + actions)

Alternatively `/admin/platform/soporte` for admin only and `/admin/soporte` for users — pick one in implementation and keep the topbar link consistent.

---

## Data model

Table (name illustrative): `support_ticket`

| Column | Type / notes |
|--------|----------------|
| `id` | PK |
| `user_id` | Auth0 `sub` of creator (`NOT NULL`) |
| `title` | Short subject |
| `description` | Body |
| `status` | `OPEN` \| `CLOSED` (UI: activos / cerrados) |
| `admin_notes` | Optional; platform admin updates |
| `created_at` / `updated_at` | Timestamps |
| `closed_at` | Set when closed |

**Subscription** is resolved at read time from the creator’s clinic membership / subscription (same sources as platform subscription screens) — do not denormalize plan on the ticket row unless a later issue requires historical freeze.

Ship schema via **incremental Liquibase** only (`docs/db/LIQUIBASE.md`). Never `ddl-auto=update`.

---

## Application version (“Acerca de”)

**Source of truth:** Maven `<version>` in `pom.xml` (today `2.0-SNAPSHOT`).

**Runtime exposure (recommended):**

1. Add `app.version=${project.version}` (or equivalent) with Maven resource filtering into `application.properties`, **or** inject `@Value("${app.version}")` from a filtered property.
2. Expose to Thymeleaf via `@ControllerAdvice` / model attribute (e.g. `appVersion`) used by the about modal fragment in the admin layout.

Do not hardcode the version only in HTML.

### How to set the version for new releases on `main`

Documented for maintainers in the root [`README.md`](../../README.md) (section **Application version**):

1. Update `<version>` in `pom.xml` before or as part of the release PR to `main`.
2. Confirm the build filters `app.version` (or the chosen property) into the packaged artifact.
3. After deploy, open **Acerca de** and verify the new value.
4. Optionally create a matching Git tag (e.g. `v2.1.0`).

Child issue: [#542](https://github.com/diego-torres/nutriconsultas/issues/542).

---

## Security & privacy

- Nutritionist paths: always scope by `userId` (`findByIdAndUserId` style). Prefer **404** on ownership miss.
- Platform admin paths: `requirePlatformAdmin` before list/update/close.
- Logging: ticket **ids** only; no names/emails in unstructured logs; use / extend `LogRedaction` if entities are logged.

---

## UI surfaces

| Surface | Content |
|---------|---------|
| Nutritionist Soporte | DataTables-style or simple table of own tickets; create form (title + description) |
| Admin Soporte | Columns: user, subscription, title; filter activos/cerrados; update + close actions |
| Acerca de modal | Minutriporcion + version string |

---

## Suggested delivery order

1. [#548](https://github.com/diego-torres/nutriconsultas/issues/548) — registry/plan docs *(this document)*  
2. [#543](https://github.com/diego-torres/nutriconsultas/issues/543) — Liquibase + entity + repository  
3. [#544](https://github.com/diego-torres/nutriconsultas/issues/544) — service + access control  
4. [#541](https://github.com/diego-torres/nutriconsultas/issues/541) + [#542](https://github.com/diego-torres/nutriconsultas/issues/542) — menu + Acerca de (can parallelize with 543)  
5. [#545](https://github.com/diego-torres/nutriconsultas/issues/545) — nutritionist UI  
6. [#546](https://github.com/diego-torres/nutriconsultas/issues/546) — platform admin UI  
7. [#547](https://github.com/diego-torres/nutriconsultas/issues/547) — remaining tests / validators (also land tests with each feature PR)

---

## Definition of done (track)

- Menu: Perfil, Soporte, Acerca de, Salir only  
- Nutritionists: create + list own tickets  
- Platform admins: list with user/subscription/title; filter; update; close  
- Acerca de shows build version; README documents bump process  
- Liquibase + tests + template validators  
- Registry [`ISSUE-SUPPORT.md`](../../ISSUE-SUPPORT.md) updated per merge  
