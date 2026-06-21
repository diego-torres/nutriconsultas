# Issue Registry — `[Public Booking]` track

Living index of GitHub issues for **public appointment scheduling** — shareable links, nutritionist availability, and patient self-booking. Update when status changes (commit on the PR that closes work).

**Repo:** [diego-torres/nutriconsultas](https://github.com/diego-torres/nutriconsultas)  
**Last updated:** 2026-06-21 — Epic ~~#245~~ **done** (v1 shipped). All child issues **#246–#248**, **#297**, **#300**, **#302** complete.

> **Scope.** Public routes (`/consultas/{id}/agendar-cita` or equivalent), availability configuration, and calendar blocks. Nutritionist admin UI pieces may live in `/admin/**` but this track owns the **public booking product**. Mobile API: [`ISSUE.md`](ISSUE.md). Subscription: [`ISSUE-SUBSCRIPTION.md`](ISSUE-SUBSCRIPTION.md). Nutritionist web (non-booking): [`ISSUE-NUTRITIONIST-WEB.md`](ISSUE-NUTRITIONIST-WEB.md).

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

## Epic — Public appointment scheduling link

Shareable URL for patients to book into a nutritionist's real availability:

`https://minutriporcion.com/consultas/{nutritionist-public-id}/agendar-cita`

| Requirement | Issues |
|-------------|--------|
| Epic umbrella | #245 |
| Profile: working hours / availability | #246 |
| Calendar: unavailable days & absence windows | #247 |
| Public page: slot picker + booking | #248 |
| Minimum 2-day booking advance (public only) | #248 |
| Profile: display + copy public booking link | #297 |
| Public booking: create patient on first book | #300 |
| Public booking: patient confirmation email | #302 |

**Suggested order:** #246 → #247 → #248 → #297 → #300 → #302.

| # | Title | URL | State | Depends on | Notes |
|---|-------|-----|-------|-----------|-------|
| **245** | Epic — public appointment scheduling link per nutritionist | https://github.com/diego-torres/nutriconsultas/issues/245 | **done** | — | v1 shipped; Liquibase 014–016; deferred: verification, SMTP (#209) |
| **246** | Nutritionist profile — configure working hours and availability | https://github.com/diego-torres/nutriconsultas/issues/246 | **done** | **245** | Branch `issue-246-working-hours`; `GET/PUT /rest/profile/availability`; Liquibase `014` |
| **247** | Calendar — unavailable days and absence windows | https://github.com/diego-torres/nutriconsultas/issues/247 | **done** | **246** | PR [#295](https://github.com/diego-torres/nutriconsultas/pull/295); Liquibase `015`, blocks API, slot query |
| **248** | Public page — slot picker and appointment booking | https://github.com/diego-torres/nutriconsultas/issues/248 | **done** | **246**, ~~**247**~~, ~~**243**~~ | PR [#298](https://github.com/diego-torres/nutriconsultas/pull/298); Liquibase `016`, public REST + `agendar-cita`; 2-day min advance |
| **297** | Nutritionist profile — display and copy public booking link | https://github.com/diego-torres/nutriconsultas/issues/297 | **done** | ~~**248**~~ | PR [#299](https://github.com/diego-torres/nutriconsultas/pull/299); profile form copy-to-clipboard + `swal` |
| **300** | Public booking — create patient record on first book (minimal profile) | https://github.com/diego-torres/nutriconsultas/issues/300 | **done** | ~~**248**~~ | PR [#301](https://github.com/diego-torres/nutriconsultas/pull/301); `ONBOARDING` + placeholder demographics |
| **302** | Public booking — patient confirmation email | https://github.com/diego-torres/nutriconsultas/issues/302 | **done** | ~~**248**~~, ~~**300**~~ | PR [#303](https://github.com/diego-torres/nutriconsultas/pull/303); Thymeleaf email on book (log until SMTP #209) |

---

## Cross-track links

| Track | Interaction |
|-------|-------------|
| ~~#243~~ reCAPTCHA | **done** — `RecaptchaVerificationService` + production keys; reused on public booking (~~#248~~) |
| #244 Solicitar acceso | Orthogonal — nutritionist onboarding vs patient booking |
| `CalendarEvent` | Existing appointments; clarify vs availability blocks |
| #236 Nutritionist profile | Same profile area for hours (#246) and logo |

---

**How to update this file**

Same convention as [`ISSUE.md`](ISSUE.md): mark `in-progress` when PR opens, `done` when merged. Add sprint pointer to [`AGENT-WORKFLOW.md`](AGENT-WORKFLOW.md) when this track becomes active.
