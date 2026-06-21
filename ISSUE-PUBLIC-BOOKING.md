# Issue Registry — `[Public Booking]` track

Living index of GitHub issues for **public appointment scheduling** — shareable links, nutritionist availability, and patient self-booking. Update when status changes (commit on the PR that closes work).

**Repo:** [diego-torres/nutriconsultas](https://github.com/diego-torres/nutriconsultas)  
**Last updated:** 2026-06-21 — ~~#246~~ **done** (branch `issue-246-working-hours`). **NEXT:** #247. Epic **#245** with child issues **#246–#248** registered.

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

**Suggested order:** #246 → #247 → #248.

| # | Title | URL | State | Depends on | Notes |
|---|-------|-----|-------|-----------|-------|
| **245** | Epic — public appointment scheduling link per nutritionist | https://github.com/diego-torres/nutriconsultas/issues/245 | open | — | New track; Liquibase for availability + public slug |
| **246** | Nutritionist profile — configure working hours and availability | https://github.com/diego-torres/nutriconsultas/issues/246 | **done** | **245** | Branch `issue-246-working-hours`; `GET/PUT /rest/profile/availability`; Liquibase `014` |
| **247** | Calendar — unavailable days and absence windows | https://github.com/diego-torres/nutriconsultas/issues/247 | **NEXT** | **246** | Blocks subtract from public slots |
| 248 | Public page — slot picker and appointment booking | https://github.com/diego-torres/nutriconsultas/issues/248 | open | **246**, **247**, ~~**243**~~ | reCAPTCHA via `@PublicRecaptchaForm`; opaque public id |

---

## Cross-track links

| Track | Interaction |
|-------|-------------|
| ~~#243~~ reCAPTCHA | **done** — `RecaptchaVerificationService` + production keys; reuse on public booking (#248) |
| #244 Solicitar acceso | Orthogonal — nutritionist onboarding vs patient booking |
| `CalendarEvent` | Existing appointments; clarify vs availability blocks |
| #236 Nutritionist profile | Same profile area for hours (#246) and logo |

---

**How to update this file**

Same convention as [`ISSUE.md`](ISSUE.md): mark `in-progress` when PR opens, `done` when merged. Add sprint pointer to [`AGENT-WORKFLOW.md`](AGENT-WORKFLOW.md) when this track becomes active.
