# Agent Workflow

How AI agents (and humans pairing with them) ship the **patient mobile API** on **`diego-torres/nutriconsultas`** (Spring Boot · Java 21 · Maven). Follow every phase in order. Do not skip ahead.

**Registries & reference docs**

| File | Purpose |
|------|---------|
| [`ISSUE.md`](ISSUE.md) | `[Mobile API]` issues (#91–#99, #107–#116, #132–#141), integration prerequisites (#156, #46), URLs, states, dependencies, data contracts |
| [`docs/mobile-api/README.md`](docs/mobile-api/README.md) | Index of all mobile contract docs below |
| [`docs/mobile-api/ALIGNMENT-SPEC.md`](docs/mobile-api/ALIGNMENT-SPEC.md) | Canonical cross-repo contract — §F7 audience, §F8 schema/enum map, verified gaps |
| [`docs/mobile-api/mobile-api-roadmap-v2.md`](docs/mobile-api/mobile-api-roadmap-v2.md) | Per-endpoint (#91–#99) request/response JSON and field mappings |
| [`docs/mobile-api/PHI-LOGGING-AUDIT.md`](docs/mobile-api/PHI-LOGGING-AUDIT.md) | Completed PHI logging audit for `/rest/mobile/**` (#115) |
| [`docs/mobile-api/MOBILE-E2E-STATUS.md`](docs/mobile-api/MOBILE-E2E-STATUS.md) | Live E2E status, Auth0 setup, HTTP code matrix |
| [`docs/api/openapi-mobile.yaml`](docs/api/openapi-mobile.yaml) | OpenAPI 3.1 export for `/rest/mobile/patient/**` (#112); regen via `scripts/export-openapi-mobile.sh` |
| [`docs/db/LIQUIBASE.md`](docs/db/LIQUIBASE.md) | Liquibase baseline (#46), incremental changesets, brownfield `preConditions`, H2 vs PG test paths |
| [`AGENTS.md`](AGENTS.md) | Agent onboarding — mobile + subscription sprint pointers |
| [`README.md`](README.md) | Project overview and high-level **NEXT** for both tracks |

**Parallel track (subscription — do not mix into mobile PRs):**

| File | Purpose |
|------|---------|
| [`ISSUE-SUBSCRIPTION.md`](ISSUE-SUBSCRIPTION.md) | `[Subscription]` issues (#180–#211), states, dependencies |
| [`SUBSCRIPTION-ENFORCEMENT-WORKFLOW.md`](SUBSCRIPTION-ENFORCEMENT-WORKFLOW.md) | Agent workflow for subscription enforcement |
| [`docs/subscription/SUBSCRIPTION-ENFORCEMENT-PLAN.md`](docs/subscription/SUBSCRIPTION-ENFORCEMENT-PLAN.md) | Plan tiers, entitlements, lifecycle, data model |

**Parallel track (nutritionist web — do not mix into mobile/subscription PRs unless coupled):**

| File | Purpose |
|------|---------|
| [`ISSUE-NUTRITIONIST-WEB.md`](ISSUE-NUTRITIONIST-WEB.md) | `[Nutritionist Web]` issues (#221–#223 MPX; #232–#242 epics; #257–#259 platillo ownership; #271–#272 system catalog create; #280–#281 diet nutrients & ingesta platillo edit; ~~#285~~ platillo inline cantidad; ~~#241~~ ~~#242~~ patient UX; ~~#250~~), states, dependencies |
| [`docs/paciente/PATIENT-MPX-PLAN.md`](docs/paciente/PATIENT-MPX-PLAN.md) | Patient registration export/import plan |

**Parallel track (public booking — v1 complete):**

| File | Purpose |
|------|---------|
| [`ISSUE-PUBLIC-BOOKING.md`](ISSUE-PUBLIC-BOOKING.md) | `[Public Booking]` epic #245–#248 |
| [`docs/public-booking/AVAILABILITY.md`](docs/public-booking/AVAILABILITY.md) | Working hours model, timezone, REST (#246) |

**Parallel track (AI assistant — do not mix into mobile/subscription PRs unless coupled):**

| File | Purpose |
|------|---------|
| [`ISSUE-AI-ASSISTANT.md`](ISSUE-AI-ASSISTANT.md) | `[AI Assistant]` issues (#360–#442), epics, milestones |
| [`AI-ASSISTANT-WORKFLOW.md`](AI-ASSISTANT-WORKFLOW.md) | Agent workflow for AI nutrition assistant |
| [`docs/ai/AI-ASSISTANT-PLAN.md`](docs/ai/AI-ASSISTANT-PLAN.md) | Architecture, security, tools, definition of done |

**Parallel track (in-app support — do not mix into mobile/subscription PRs unless coupled):**

| File | Purpose |
|------|---------|
| [`ISSUE-SUPPORT.md`](ISSUE-SUPPORT.md) | `[Support]` issues (#540–#548), states, dependencies |
| [`SUPPORT-WORKFLOW.md`](SUPPORT-WORKFLOW.md) | Agent workflow for Soporte / Acerca de |
| [`docs/support/SUPPORT-TICKETS-PLAN.md`](docs/support/SUPPORT-TICKETS-PLAN.md) | Menu, roles, data model, version bump |

**Current next issue (public booking):** None — epic ~~#245~~ **done**; ~~#246~~, ~~#247~~, ~~#248~~, ~~#297~~, ~~#300~~, ~~#302~~ done. Deferred follow-ups need new issues. See [`ISSUE-PUBLIC-BOOKING.md`](ISSUE-PUBLIC-BOOKING.md).

**Current next issue (mobile):** [#353 — Grocery list for patient diet plan](https://github.com/diego-torres/nutriconsultas/issues/353) (`in-progress`). ~~#354~~ ~~#352~~ **done** (PR [#357](https://github.com/diego-torres/nutriconsultas/pull/357)).

**Current next issue (subscription):** Registered track **complete** (~~#244~~ ✓ on `subscription/244-contact-form-prefill`). Triage open `[Subscription]` GitHub issues. See [`ISSUE-SUBSCRIPTION.md`](ISSUE-SUBSCRIPTION.md).

**Current next issue (nutritionist web):** None — all registered epics complete: ~~#271~~ done (PR [#288](https://github.com/diego-torres/nutriconsultas/pull/288)) + ~~#272~~ done (PR [#289](https://github.com/diego-torres/nutriconsultas/pull/289)) system catalog create; ~~#241~~–~~#242~~ patient UX; ~~#232~~–~~#235~~ diet grid; ~~#236~~–~~#240~~ profile/PDF/nutrients; ~~#257~~–~~#259~~ platillo ownership; ~~#275~~, ~~#280~~–~~#281~~, ~~#285~~ diet/ingredient editing. Deferred follow-ups need new issues. See [`ISSUE-NUTRITIONIST-WEB.md`](ISSUE-NUTRITIONIST-WEB.md).

**Current next issue (AI assistant):** None — registered implementation track **complete** (#360–#450). Production rollout: [`RELEASE-CHECKLIST.md`](docs/ai/RELEASE-CHECKLIST.md) (#408). See [`ISSUE-AI-ASSISTANT.md`](ISSUE-AI-ASSISTANT.md).

**Current next issue (support):** None — `[Support]` track **complete** (#540–#548). See [`ISSUE-SUPPORT.md`](ISSUE-SUPPORT.md).

---

## Product context (read before every session)

| Topic | Guidance |
|-------|----------|
| **Audience** | **Patient-only API** — `/rest/mobile/patient/**` serves a patient their **own** visits, diet plans, progress, and messages. Nutritionist roster/consultation/meal-plan authoring stays on the existing Thymeleaf web app — do **not** add authoring endpoints under `/rest/mobile/**`. |
| **API surface** | All mobile endpoints live under `/rest/mobile/patient/` as plain JSON (not DataTables-shaped like the admin `*RestController`s). |
| **Identity (security-critical)** | JWT `sub` → **`Paciente.patientAuthSub`** (#107 ✓ PR #117). **Never `Paciente.userId`** — that is the NUTRITIONIST's Auth0 sub / tenant owner (ALIGNMENT-SPEC §F2). `PatientLinkageFilter` returns **403** if no linked `Paciente`. |
| **Ownership / IDOR** | Return only the authenticated patient's rows. On an ownership miss prefer **404** (not 403) so existence isn't leaked (esp. #92). Never return cross-tenant data. |
| **Backend state** | **Phase 0 done** (#107, #109, #110). **All endpoints #91–#99 done** on `main`. **Cross-cutting done** (#111–#116). **#156**, **#46**, **#132–#141** done. **Phase 2 invitation onboarding complete.** Next mobile work requires new issues. Requires `AUTH_AUDIENCE` env var. |
| **DTO envelope** | `ApiResponse<T>`; lists in `PagedResponse<T>` or `CursorPagedResponse<T>` (messages); ISO-8601 date strings. See #110. |
| **Schema ground truth** | ALIGNMENT-SPEC §F8 field-name map (`nombre→dietaName`, `energia→totalKcal`, `lipidos→totalGrasas`, `hidratosDeCarbono→totalCarbohidratos`, `Ingesta.nombre→tipo`); enums `EventStatus`/`PacienteDietaStatus` (no INACTIVE)/`NivelPeso`. Serialization aliases only — **no DB schema changes** for field renames. |
| **PHI & logging** | No patient names/emails/DOB in unstructured logs. `LogRedaction` + `PhiLogTurboFilter`; CI runs `scripts/audit-logging.sh` and `scripts/audit-mobile-logging.sh` (#115 done). |
| **Existing code to reuse** | Visits → `calendar/CalendarEventService`; Diet → `PacienteDietaService` + `dieta/` + PDF (#95); Progress → `BodyMetricRecordService` + anthropometrics (#98/#99); Messages → `PatientMessage` entity + mobile/admin controllers (#96/#97). |
| **Paciente / Liquibase** | ~~#156~~ Phase C done; ~~#46~~ Liquibase baseline on `main` (PR #196). **All entity/schema/catalog changes → incremental Liquibase changesets** ([`docs/db/LIQUIBASE.md`](docs/db/LIQUIBASE.md)). Do not use `ddl-auto=update`. Mobile `#98`/`#99` DTOs stay stable — map in the service layer. |
| **Liquibase / entities** | Changing `@Entity` fields, tables, FKs, or catalog seed **requires** a new changeset in `db/changelog/changes/` + `db.changelog-master.yaml` include. Never edit merged `001-baseline` or seed SQL in place on brownfield DBs. Local boot: **Java 21** (Liquibase breaks on Java 24). |

---

## Overview

```mermaid
flowchart LR
  A[1. Triage & registry] --> B[2. Branch & plan]
  B --> C[3. Delegate & implement]
  C --> D[4. Review]
  D --> E[5. Tests]
  E --> F[6. CI validation]
  F --> G[7. Fresh baseline]
  G --> H[8. Summaries]
  H --> I[9. PR & registry]
```

---

## Phase 1 — Triage & registry sync

**Goal:** Know exactly what to work on before writing code.

1. **Pull latest** on `main`:
   ```bash
   git fetch origin && git checkout main && git pull origin main
   ```
2. **Open the registries:**
   - [`ISSUE.md`](ISSUE.md) — find the row marked `NEXT` (mobile track)
   - [`ISSUE-SUBSCRIPTION.md`](ISSUE-SUBSCRIPTION.md) — if touching billing/RBAC (parallel track)
   - [`docs/mobile-api/README.md`](docs/mobile-api/README.md) — contract doc index
   - Confirm every dependency in the issue's "Depends on" column is `done`
3. **Sync with GitHub (remote):**
   ```bash
   gh issue view <number>          # title, body, labels, linked PRs
   gh issue list --label "" --search "[Mobile API] in:title" --state open
   ```
4. **Assess readiness.** Examples:
   - **#107 + #110 must be `done` before any endpoint (#91–#99).** Endpoints have no auth filter chain or DTO envelope without them.
   - **#109 (linkage) gates live E2E** — endpoints can be built and tested with a seeded `patientAuthSub` before #109, but cannot be exercised by a real Auth0 patient until it lands.
   - **#113 (rate limit)** should land with or before **#97** (write endpoint).
   - **#114 (nutritionist reply) is web-only** — do not expose it under `/rest/mobile/**`.
   - **#46 (Liquibase)** ✓ — all schema/catalog changes are incremental changesets (see [Liquibase section](#liquibase--entity-schema-and-catalog-data)).
   - **Phase 2 invitation onboarding** ✓ — #134–#141 all merged (PRs #319, #324–#333). **Phase 2 complete.** No NEXT in the mobile track — new issues required for additional work.
   - If a dependency is still `open`, complete it first or document the blocker in the plan (Phase 2) and stop.
5. **Update local registry** when remote state drifted (issue closed on GitHub but still `open` here, or vice versa). `ISSUE.md` must match GitHub before proceeding.

**Exit criteria:** One issue identified as `NEXT`, dependencies satisfied, local + remote registries aligned.

---

## Phase 2 — Branch, context, and plan

**Goal:** No implementation until the plan is written and acknowledged.

1. **Create a branch** from latest `main`:
   ```bash
   git checkout -b mobile-api/<number>-<short-slug>
   # example: mobile-api/107-jwt-resource-server
   ```
2. **Read the issue** locally (`gh issue view <number>`) and on GitHub (acceptance criteria, labels, linked PRs).
3. **Gather context** before executing anything:

   | Source | What to extract |
   |--------|-----------------|
   | Issue body | Acceptance criteria, endpoint shape, edge cases |
   | [`ISSUE.md`](ISSUE.md) Data contracts | Backend source entity/service, DTO field map, enums |
   | [`docs/mobile-api/ALIGNMENT-SPEC.md`](docs/mobile-api/ALIGNMENT-SPEC.md) §F8 | Field aliases, enum values, verified gaps |
   | [`docs/mobile-api/mobile-api-roadmap-v2.md`](docs/mobile-api/mobile-api-roadmap-v2.md) | Exact request/response JSON for the endpoint |
   | `SecurityConfig.java` | Existing web chain — the mobile chain is **separate** `@Order(1)` |
   | Existing `*Service` / entity | Query methods and fields to reuse (don't re-query the DB ad hoc) |

4. **Output a plan first** (in chat, or `.claude/issues/mobile/issue-<number>.md` for large work). The plan must include:
   - Files to create / modify (package, class names)
   - Data flow: Controller → Service → Repository, and DTO mapping (entity field → contract key)
   - Security: filter-chain matcher, principal resolution, ownership/IDOR guard
   - Test strategy (`@WebMvcTest` security + slice, `@DataJpaTest`, service unit)
   - CI impact (new deps in `pom.xml`? **Liquibase changeset**? coverage threshold?)
   - **Liquibase (if entity/schema/catalog touched):** files to add, `preConditions` for brownfield, H2 test path (`db.changelog-test-master.yaml` vs full master in `LiquibaseMigrationTest`) — checklist in [`docs/db/LIQUIBASE.md`](docs/db/LIQUIBASE.md)
   - Risks, blockers, explicit out-of-scope items

**Do not implement until the plan is acknowledged** (user says "go ahead" or equivalent).

**Exit criteria:** Branch exists, plan posted, context sources cited.

---

## Phase 3 — Delegate and implement (agentic)

**Goal:** Ship the endpoint completely by delegating focused sub-tasks to specialized agents.

For each plan step, delegate to the best-fit subagent. Run **independent** steps in parallel when safe; run **dependent** Phase 0 work in sequence (#107 → #110 → endpoints).

| Step type | Delegate to | Delivers |
|-----------|-------------|----------|
| Security filter chain / JWT | `coder` + security review | `MobileSecurityConfig`, resource-server config, principal resolver |
| Entity / migration | `coder` | JPA entity + **incremental Liquibase changeset** (`db/changelog/changes/`, include in master); catalog seed via `sqlFile` only — no Java seeders |
| DTOs + mappers | `coder` | `mobile/dto/*`, entity→DTO mapping per §F8 |
| Controller + service | `coder` | `*MobileController`, service method (reuse existing services) |
| Schema/contract check | `reviewer` / `architect` | DTO matches ALIGNMENT-SPEC §F8 and roadmap JSON |
| PHI / logging review | `reviewer` + security | No PHI at INFO; `LogRedaction` applied |

**Rules**

- One cohesive issue per PR — no drive-by refactors of the web app.
- **Mobile endpoints are plain JSON** — do not reuse the DataTables `Abstract*RestController` shape.
- **Never reuse `Paciente.userId`** for patient identity — always `patientAuthSub`.
- **No authoring endpoints** under `/rest/mobile/**` (patient app is read-only except `POST /messages`).
- Reuse existing services (`CalendarEventService`, `PacienteDietaService`, anthropometric services) — don't duplicate queries.
- After delegates finish, the **lead agent integrates** — wires the controller, registers the filter chain order, **commits Liquibase changesets with entity changes**, regenerates coverage.
- **Entity or schema change:** add Liquibase changeset in the same PR as the `@Entity` edit; run `mvn verify` and boot with Java 21 (`./dev-start.sh`).
- **Run the app locally** (`./dev-start.sh` or `mvn spring-boot:run`) and hit the endpoint with a test JWT before committing when the change touches security config, **Liquibase**, or startup.

**Exit criteria:** In-scope acceptance criteria met; endpoint returns the contract shape for the owning patient and 403/404 for others; no TODO stubs for in-scope work.

---

## Phase 4 — Review mode

**Goal:** Changes make sense together; nothing orphaned or contradictory.

1. **Self-review the full diff:**
   ```bash
   git diff main...HEAD
   ```
2. **Check cohesion:**
   - Filter chain matches only `/rest/mobile/**` and is ordered before the web chain
   - Principal resolver enforced on every endpoint; ownership/IDOR guard present
   - DTO field names + enums match §F8 exactly (no `nombre` leaking as `nombre` where the contract says `dietaName`)
   - **Liquibase:** entity columns match changeset DDL; new file included in `db.changelog-master.yaml`; no `ddl-auto=update`; no in-place edits to `001-baseline` / seed SQL on brownfield paths
   - No dead code, no duplicate DTOs, no parallel security config
3. **Adversarial / reviewer pass** (`reviewer` subagent or critical read-through):
   - Matches issue acceptance criteria verbatim
   - No PHI (names/emails/DOB) in logs or error bodies
   - Paths and response shapes match `mobile-api-roadmap-v2.md` and the mobile consumer's expectations
4. **Read validation** — read every changed file for wrong types, missing `@Valid`, missing `@PreAuthorize`/principal check, N+1 queries, unredacted logs.

**Exit criteria:** Review findings addressed or explicitly deferred with a tracked follow-up issue.

---

## Phase 5 — Testing

**Goal:** Automated proof the endpoint works and is secure; coverage threshold stays green.

1. **Audit existing coverage** for touched modules.
2. **Add tests for the current changes:**

   | Layer | Tool | Location |
   |-------|------|----------|
   | Controller + security | `@WebMvcTest` + `spring-security-test` | `src/test/java/.../mobile/` |
   | Service / mapping | JUnit + Mockito | `src/test/.../<feature>` |
   | Repository / query | `@DataJpaTest` | `src/test/.../<feature>` |
   | Liquibase migration | `LiquibaseMigrationTest` or `@SpringBootTest` + master changelog | `src/test/java/.../db/` |
   | Ownership / IDOR | security test (wrong `sub` → 403/404) | controller test |

3. **Run locally** (full CI parity — `lint.sh` alone does **not** cover everything):
   ```bash
   ./lint.sh                       # checkstyle + spotbugs + pmd + thymeleaf (runs WITHOUT -Pci)
   bash scripts/audit-logging.sh && bash scripts/audit-mobile-logging.sh   # PHI logging audit — separate CI steps, NOT inside lint.sh
   mvn -B verify                   # package + tests + jacoco:check
   ```
   > Note: `lint.sh` runs `checkstyle:check`/`pmd:check` without the `-Pci` profile, so CI (which adds `-Pci`) is stricter. Use the Phase 6 commands for exact CI parity.
4. **Security-specific:** a test proving an unlinked or cross-tenant `sub` cannot read another patient's data is **required** for every endpoint.

**Exit criteria:** `mvn verify` passes; `jacoco:check` meets threshold; new behavior has meaningful coverage including the negative auth path.

---

## Phase 6 — CI pipeline validation

**Goal:** Same checks GitHub Actions ([`.github/workflows/maven.yml`](.github/workflows/maven.yml)) will run — all green before opening a PR.

| Check | Local command | CI job |
|-------|---------------|--------|
| Checkstyle (style + format) | `mvn -B checkstyle:check -Pci` | lint |
| SpotBugs | `mvn -B spotbugs:check` | lint |
| PMD | `mvn -B pmd:check -Pci` | lint |
| Thymeleaf templates | `mvn -B test -Dtest=ThymeleafTemplateValidationTest` | lint |
| Logging security (PHI) | `bash scripts/audit-logging.sh` + `bash scripts/audit-mobile-logging.sh` | lint |
| Build + unit/integration tests | `mvn -B package` | build |
| Coverage threshold | `mvn -B jacoco:report jacoco:check -Pci` | build |

```bash
./lint.sh && bash scripts/audit-logging.sh && bash scripts/audit-mobile-logging.sh && mvn -B verify
```

**Exit criteria:** All checks green locally (JDK 21).

---

## Phase 7 — Fresh baseline check

**Goal:** Always commit on validated, up-to-date code.

Before the final commit:

1. Re-sync with remote:
   ```bash
   git fetch origin && git rebase origin/main
   ```
2. Re-run `./lint.sh && bash scripts/audit-logging.sh && bash scripts/audit-mobile-logging.sh && mvn -B verify` after rebase.
3. **Validate workflow artifacts are present and updated:**
   - [ ] [`ISSUE.md`](ISSUE.md) — issue marked `in-progress` (or `done` when closing); `NEXT` advanced when it merges
   - [ ] [`AGENT-WORKFLOW.md`](AGENT-WORKFLOW.md) — sprint pointer updated when `NEXT` advances
   - [ ] [`docs/mobile-api/README.md`](docs/mobile-api/README.md) + [`ALIGNMENT-SPEC.md`](docs/mobile-api/ALIGNMENT-SPEC.md) + [`mobile-api-roadmap-v2.md`](docs/mobile-api/mobile-api-roadmap-v2.md) — status lines match `ISSUE.md`
   - [ ] [`docs/mobile-api/MOBILE-E2E-STATUS.md`](docs/mobile-api/MOBILE-E2E-STATUS.md) — if E2E/auth behavior changed
   - [ ] [`docs/api/openapi-mobile.yaml`](docs/api/openapi-mobile.yaml) — if mobile REST contract changed (#112)
   - [ ] [`AGENTS.md`](AGENTS.md) + [`README.md`](README.md) — high-level **NEXT** pointers
   - [ ] [`ISSUE-SUBSCRIPTION.md`](ISSUE-SUBSCRIPTION.md) + [`SUBSCRIPTION-ENFORCEMENT-WORKFLOW.md`](SUBSCRIPTION-ENFORCEMENT-WORKFLOW.md) — when subscription work ships (parallel track)
   - [ ] **Liquibase changeset** committed in the same PR when any `@Entity`, catalog seed, or schema changed ([`docs/db/LIQUIBASE.md`](docs/db/LIQUIBASE.md) checklist)
   - [ ] Mobile registry's "Backend cross-reference" kept in sync when an endpoint's state changes
   - [ ] Tests + any new `pom.xml` deps committed together with feature code
4. If any Phase 1–6 step was skipped or failed, **go back and fix** — do not open a PR on a broken tree.

**Exit criteria:** Clean rebase, lint + verify green, registries consistent.

---

## Phase 8 — Summaries (required output)

Every completed issue session **must** end with two summaries in chat.

### A. Explain like I'm 5

Plain language. No jargon without an immediate plain-English translation.

> **Example (#107):** We gave the app a bouncer for the phone-app door. The bouncer checks the patient's digital ID badge (a token from Auth0), looks them up in our records by a new "this badge belongs to this patient" tag, and only lets them see their own stuff. The old web-app door is untouched.

### B. Technical summary

Precise list of what changed. For each technical term, add a one-line plain-English note in parentheses.

> **Example (#107):**
> - Added `spring-boot-starter-oauth2-resource-server`; new `MobileSecurityConfig` `@Order(1)` stateless chain on `/rest/mobile/**` (validates Auth0 JWTs by Bearer token, no session)
> - Added `Paciente.patientAuthSub` (unique, nullable) + Liquibase changeset (maps the patient's Auth0 `sub` to their record — **not** `userId`, which is the nutritionist)
> - `PatientPrincipalResolver` returns 403 when a `sub` has no linked `Paciente` (blocks unlinked tokens)
> - Web `SecurityConfig` left unchanged (existing nutritionist login still session-backed)

---

## Phase 9 — Commit, PR, and registry update

**Goal:** Trigger CI; human merges after green build.

1. **Commit** with a conventional message referencing the issue:
   ```bash
   git add -A
   git commit -m "feat(mobile-api): Auth0 JWT resource server + patientAuthSub (#107)"
   ```
2. **Push and open PR:**
   ```bash
   git push -u origin HEAD
   gh pr create --title "feat(mobile-api): Auth0 JWT resource server + patientAuthSub (#107)" --body "$(cat <<'EOF'
   ## Summary
   - Implements #107 — `MobileSecurityConfig` resource-server chain on `/rest/mobile/**`
   - Adds `Paciente.patientAuthSub` + principal resolver (403 if unlinked)

   ## Test plan
   - [ ] `./lint.sh && bash scripts/audit-logging.sh && bash scripts/audit-mobile-logging.sh && mvn -B verify` green
   - [ ] `@WebMvcTest` proves valid JWT → 200, unlinked/cross-tenant sub → 403/404
   - [ ] Web app login unaffected

   Closes #107
   EOF
   )"
   ```
3. **Update registries in the same PR:**
   - [`ISSUE.md`](ISSUE.md) — `NEXT` → `in-progress` when PR opens; → `done` when merged; advance `NEXT` to the next unblocked row
   - [`AGENT-WORKFLOW.md`](AGENT-WORKFLOW.md) — sprint pointer when `NEXT` advances
   - [`docs/mobile-api/README.md`](docs/mobile-api/README.md), [`ALIGNMENT-SPEC.md`](docs/mobile-api/ALIGNMENT-SPEC.md), [`mobile-api-roadmap-v2.md`](docs/mobile-api/mobile-api-roadmap-v2.md) — contract/status lines
   - [`AGENTS.md`](AGENTS.md), [`README.md`](README.md) — summary pointers
   - [`docs/api/openapi-mobile.yaml`](docs/api/openapi-mobile.yaml) — when endpoint shapes change
   - [`ISSUE-SUBSCRIPTION.md`](ISSUE-SUBSCRIPTION.md) — only for subscription-track PRs
   - Sync the mobile registry's "Backend cross-reference" when an endpoint changes state (cross-repo courtesy)
4. **Human gate:** User reviews the PR, waits for CI green, then merges.
5. **Post-merge (user):** Refresh local `main`:
   ```bash
   git checkout main && git pull origin main
   ```

**Exit criteria:** PR open, CI running, registries updated in the PR diff, both summaries delivered in chat.

---

## Liquibase — entity, schema, and catalog data

**Canonical detail:** [`docs/db/LIQUIBASE.md`](docs/db/LIQUIBASE.md). Apply this section whenever JPA entities or database-backed catalog data change.

### When Liquibase is required

| Change | Liquibase action |
|--------|------------------|
| New/changed `@Entity`, `@Column`, `@Table`, relationship, index | Incremental changeset (`ALTER` / `CREATE`); include in `db.changelog-master.yaml` |
| New catalog rows (alimentos, platillos, template dietas) | New or updated seed changeset + `preConditions`; no Java startup seeders |
| Patient/tenant runtime rows | Application code only — schema migration if new columns/tables |
| Renamed Java field only (same DB column) | Usually **no** migration; document `@Column(name=…)` if needed |

### Agent rules

1. **`ddl-auto=none`** — Hibernate never auto-applies entity diffs.
2. **Do not edit** `001-baseline-schema.*` or committed seed SQL for brownfield fixes — add `00N-<feature>.yaml` forward.
3. **`preConditions` + `onFail: MARK_RAN`** when production may already have the column/table (brownfield).
4. **H2 CI:** default test profile uses `db.changelog-test-master.yaml` (baseline only). Full catalog seed is validated in `LiquibaseMigrationTest`.
5. **Local boot:** Java **21** required (Java 24 breaks Liquibase service loading).
6. **Same PR:** entity/mapping changes and Liquibase changeset ship together.

### Verification before PR

```bash
export JAVA_HOME=<path-to-jdk-21>
mvn -B verify
./dev-start.sh   # confirm Liquibase runs against PostgreSQL
```

PR must include: changeset file(s), `db.changelog-master.yaml` include (if new file), tests if migration affects CI path, and `docs/db/LIQUIBASE.md` update when regeneration procedures change.

---

## Quick reference

```bash
# Start of session
git fetch origin && git checkout main && git pull origin main
gh issue view 135
cat ISSUE.md
cat docs/mobile-api/README.md

# During work
git checkout -b mobile-api/135-invitation-preview
./lint.sh && bash scripts/audit-logging.sh && bash scripts/audit-mobile-logging.sh && mvn -B verify
./dev-start.sh   # Java 21 — run locally when touching security, Liquibase, or startup

# End of session
gh pr create ...
# ISSUE.md, AGENT-WORKFLOW.md, docs/mobile-api/*, AGENTS.md updated in the PR
```

---

## Current sprint pointer

| Field | Value |
|-------|-------|
| **Next issue** | None — Phase 2 invitation onboarding **complete** (~~#141~~) |
| **Status** | **Complete** — new issues needed for next mobile sprint |
| **Just completed** | [#141 — Invitation security hardening](https://github.com/diego-torres/nutriconsultas/issues/141) — [`INVITATION-SECURITY-AUDIT.md`](docs/mobile-api/INVITATION-SECURITY-AUDIT.md) |

### Upcoming gates

| Gate | Issues | When |
|------|--------|------|
| Phase 0 foundation | ~~#107~~ ✓, ~~#109~~ ✓, ~~#110~~ ✓ | **Done** |
| Auth linkage (tenant) | #108 (tenant config; prod audience deployed #118) | Before full Auth0 tenant hardening |
| Endpoints | ~~#91–#99~~ ✓ | **Done** (PR #153) |
| Cross-cutting | ~~#111~~ ✓, ~~#112~~ ✓ (OpenAPI), ~~#115~~ ✓ (PHI audit) | **Done** |
| Hardening / additive | ~~#113~~ ✓, ~~#116~~ ✓ (`senderDisplayName`), ~~#114~~ ✓ (nutritionist reply) | **Done** |
| Schema / Liquibase | ~~**#46**~~ ✓ (PR #196) → ~~**#132**~~ ✓ (PR #214) → ~~**#133–#141**~~ ✓ (PRs #229, #319, #324–#333) | **Done** — forward changesets per [`docs/db/LIQUIBASE.md`](docs/db/LIQUIBASE.md) |

### Status snapshot (2026-06-23)

**Patient mobile API on `main`:** Phase 0 + endpoints **#91–#99** done; cross-cutting **#111–#116** done. Phase 2 invitation onboarding **#132–#141 complete** (PRs #214, #229, #319, #324–#333).

**Next (mobile):** Phase 2 invitation onboarding **complete** (~~#141~~).

**Schema track:** ~~#46~~ Liquibase baseline (PR #196). Changesets **003–007** on `main` (subscription, patient invitation). All new edits → forward changesets only.

**Subscription track (parallel):** see [`ISSUE-SUBSCRIPTION.md`](ISSUE-SUBSCRIPTION.md). ~~#180–#185~~, ~~#187~~ (PR #218), ~~#190~~ (PR #216), ~~#210~~ (PR #224), ~~#211~~ (PR [#230](https://github.com/diego-torres/nutriconsultas/pull/230)), ~~#220~~ (PR [#313](https://github.com/diego-torres/nutriconsultas/pull/313)), ~~#186~~, ~~#188~~, ~~#314~~, ~~#244~~ (`subscription/244-contact-form-prefill`). ~~#207~~ ~~#208~~ ~~#209~~ done (PR [#310](https://github.com/diego-torres/nutriconsultas/pull/310)). **Registered track complete.**

**Nutritionist web (parallel):** [`ISSUE-NUTRITIONIST-WEB.md`](ISSUE-NUTRITIONIST-WEB.md) — ~~#285~~ done (`issue-285-platillo-inline-cantidad`, 340a318); ~~#280~~ done (PR [#284](https://github.com/diego-torres/nutriconsultas/pull/284)); ~~#281~~ done (`issue-281-ingesta-platillo-ingredient-edit`); ~~#240~~ done (PR [#283](https://github.com/diego-torres/nutriconsultas/pull/283)); ~~#238~~ done (PR [#279](https://github.com/diego-torres/nutriconsultas/pull/279)); ~~#239~~ done (`issue-239-ingredient-weight-recalc`, bd07fb4); ~~#236~~ done (PR [#274](https://github.com/diego-torres/nutriconsultas/pull/274)); ~~#237~~ done (PR [#278](https://github.com/diego-torres/nutriconsultas/pull/278)); platillo ownership ~~#257–#259~~ done (#259 PR [#270](https://github.com/diego-torres/nutriconsultas/pull/270)); system catalog create ~~#271~~ (PR [#288](https://github.com/diego-torres/nutriconsultas/pull/288)) + ~~#272~~ (PR [#289](https://github.com/diego-torres/nutriconsultas/pull/289)) done; diet nutrients ~~#280–#281~~ done; patient UX ~~#241–#242~~ done. **NEXT:** None — all registered epics complete; new issues needed.

**Production (2026-06-18):** ~~#226~~ invitation base URL fix (PR #227) — `APP_BASE_URL` / host remediation on EC2.

See [`ISSUE.md`](ISSUE.md) Data contracts, [`docs/mobile-api/ALIGNMENT-SPEC.md`](docs/mobile-api/ALIGNMENT-SPEC.md) §F8, and [`docs/db/LIQUIBASE.md`](docs/db/LIQUIBASE.md) for per-endpoint and schema requirements.
