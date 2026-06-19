# Liquibase database change management (#46)

Nutriconsultas uses [Liquibase](https://www.liquibase.org/) for schema and catalog seed data. Hibernate `ddl-auto` is disabled in all environments (`none`).

## Changelog layout

| Path | Purpose |
|------|---------|
| `src/main/resources/db/changelog/db.changelog-master.yaml` | Root changelog |
| `changes/001-baseline-schema.{postgresql,h2}.sql` | Full schema baseline (post-#156 Phase C) |
| `changes/002-seed-data.yaml` | Catalog seed: alimentos, platillos, template dietas |
| `changes/003-subscription-schema.yaml` | Subscription, clinic, invitation tables (#180) |
| `changes/004-payment-webhook-idempotency.yaml` | Payment webhook idempotency (#189) |
| `changes/005-nutritionist-invitation-payment-exempt.yaml` | `payment_exempt` on `nutritionist_invitation` (#184) |
| `changes/006-subscription-notification-log.yaml` | Lifecycle notification log (#185, PR #215) |
| `changes/007-patient-invitation-onboarding.yaml` | `PacienteStatus` columns + `patient_invitation` table (#132, PR #214) |
| `changes/008-platillo-ingesta-source-platillo-id.yaml` | `source_platillo_id` on `platillo_ingesta` + catalog backfill (#250) |
| `data/alimentos-seed.sql` | SMAE alimentos catalog (from `alimentos.sql`) |
| `data/platillos-seed.sql` | Catalog `platillo` + `ingrediente` rows |
| `data/dieta-templates-seed.sql` | 20 template `dieta` rows (`system:template-dietas`) and child ingestas |

Legacy source files `alimentos.sql` and `seed_platillos.sql` remain on the classpath for reference; Liquibase loads the copies under `db/changelog/data/`.

## Configuration

```properties
spring.jpa.hibernate.ddl-auto=none
spring.liquibase.change-log=classpath:db/changelog/db.changelog-master.yaml
spring.session.jdbc.initialize-schema=never
```

## Existing PostgreSQL databases (brownfield)

Baseline changesets use `preConditions onFail: MARK_RAN` when core tables already exist (e.g. production DB created with Hibernate `ddl-auto=update`). On first deploy:

1. Liquibase creates `databasechangelog` / `databasechangeloglock`.
2. Baseline changesets are **marked ran** without re-creating tables.
3. Seed changesets run only when target tables are empty (e.g. `platillo` count 0, no `system:template-dietas` rows).

If production still has pre-Phase-C `paciente` columns, apply [`postgresql-paciente-phase-c-split.sql`](../db/manual/postgresql-paciente-phase-c-split.sql) **before** deploying Liquibase-enabled builds.

## Regenerating catalog seed SQL

From a dev database with a fully materialized catalog (after Liquibase + prior seeders):

```bash
podman exec nutriconsultas-db pg_dump -U nutriconsultas -d nutriconsultas \
  --data-only --column-inserts --no-owner --no-privileges \
  --table=platillo --table=ingrediente | grep '^INSERT INTO' \
  | sed 's/INSERT INTO public\./INSERT INTO /' \
  > src/main/resources/db/changelog/data/platillos-seed.sql

podman exec nutriconsultas-db pg_dump -U nutriconsultas -d nutriconsultas \
  --data-only --column-inserts --no-owner --no-privileges \
  --table=dieta --table=ingesta --table=platillo_ingesta --table=ingrediente_platillo_ingesta \
  | grep '^INSERT INTO' | sed 's/INSERT INTO public\./INSERT INTO /' \
  > src/main/resources/db/changelog/data/dieta-templates-seed.sql
```

Update `catalog-seed-sequences.{postgresql,h2}.sql` if max IDs change.

## Regenerating baselines

**PostgreSQL** (from running dev DB with current Hibernate schema):

```bash
podman exec nutriconsultas-db pg_dump -U nutriconsultas -d nutriconsultas \
  --schema-only --no-owner --no-privileges \
  | grep -v '^\\' | grep -v '^--' | grep -v '^SET ' | grep -v '^SELECT pg_catalog' \
  | sed 's/public\.//g' \
  > src/main/resources/db/changelog/changes/001-baseline-schema.postgresql.sql
```

**H2** (for tests):

```bash
mvn -Djacoco.skip=true -Dtest=H2SchemaExportTest -Dspring.profiles.active=h2-schema-export \
  -Djunit.jupiter.conditions.deactivate=org.junit.jupiter.api.condition.DisabledCondition test
# Then clean MEMORY TABLE / CREATE USER lines from the exported file (see git history).
```

## Adding new schema changes

1. Add a new YAML/SQL file under `db/changelog/changes/` (incremental changeset — **do not** edit `001-baseline-schema.*` on deployed environments).
2. Include it from `db.changelog-master.yaml`.
3. Never rely on `ddl-auto=update` for new columns or tables.

## Entity, schema, and catalog data changes (agent checklist)

Use this whenever JPA entities, relationships, columns, indexes, or **catalog seed** data change.

### Ground rules

| Rule | Why |
|------|-----|
| `spring.jpa.hibernate.ddl-auto=none` always | Hibernate must not mutate production or dev PostgreSQL |
| One forward changeset per logical change | Brownfield DBs replay only new changesets from `databasechangelog` |
| Never edit merged baseline/seed SQL in place | Existing deployments already marked those changesets ran; add `003-…`, `004-…` instead |
| PostgreSQL + H2 | CI uses H2 (`db.changelog-test-master.yaml` = baseline only). Full seed runs via `LiquibaseMigrationTest` override or `@SpringBootTest` with master changelog |
| Local dev: **Java 21** | Liquibase fails on Java 24 (`Unknown change type 'sqlFile'`). Use `JAVA_HOME` for JBR 21 before `./dev-start.sh` |

### Schema change (new table, column, FK, index)

1. Change the `@Entity` / mapping (column names follow Hibernate snake_case defaults unless `@Column(name=…)`).
2. Add `db/changelog/changes/00N-<slug>.yaml` (or `.sql`) with an **incremental** changeset:
   - `ALTER TABLE … ADD COLUMN …`, `CREATE TABLE …`, etc.
   - Use `preConditions` + `onFail: MARK_RAN` when the change may already exist on brownfield DBs (e.g. column added manually during #156 Phase C).
3. Include the file from `db.changelog-master.yaml` **after** baseline and seed includes.
4. If tests need the new column/table on H2 slices that only run the test master changelog, either:
   - add the same DDL to a new H2-safe incremental changeset included in test master, or
   - rely on `@SpringBootTest` / `LiquibaseMigrationTest` with the full master changelog.
5. Run `mvn verify` and boot locally against PostgreSQL (`./dev-start.sh`) to confirm Liquibase applies cleanly.

**Do not** regenerate `001-baseline-schema.postgresql.sql` for routine feature work — reserve baseline regeneration for major schema rebases (see below).

### Catalog / reference data (alimentos, platillos, template dietas)

Catalog rows are **not** created by Java `CommandLineRunner` seeders. They load from Liquibase only (`002-seed-data.yaml` and successors).

| Change type | Action |
|-------------|--------|
| New catalog rows on empty DB | Add a new changeset with `preConditions` (`COUNT(*) = 0` or row-specific check) + `sqlFile`, or append to seed SQL only if no production DB has run the old changeset yet |
| Replace entire catalog export | Regenerate `platillos-seed.sql` / `dieta-templates-seed.sql` via `pg_dump` (see above); update `catalog-seed-sequences.*.sql` max IDs |
| App / tenant data (patients, dietas per user) | **No Liquibase seed** — created at runtime; migrations only for schema |

Template dietas use owner `system:template-dietas`. Do not seed patient PHI via Liquibase.

### Brownfield (existing PostgreSQL)

1. Baseline `001-*` changesets **MARK_RAN** when `paciente` already exists — they do not recreate tables.
2. Seed changesets **MARK_RAN** when target tables already have rows (`platillo` count > 0, etc.).
3. New incremental changesets always run on deploy — write idempotent SQL or guard with `preConditions`.
4. Pre-Phase-C `paciente` layout: run [`postgresql-paciente-phase-c-split.sql`](../manual/postgresql-paciente-phase-c-split.sql) before first Liquibase deploy.

### PR / review checklist

- [ ] Entity mapping and Liquibase DDL agree (name, type, nullability, FK)
- [ ] New changeset included in `db.changelog-master.yaml`
- [ ] No `ddl-auto=update` and no new Java startup seeders for catalog data
- [ ] `LiquibaseMigrationTest` or integration test covers migration if seed/schema affects H2 CI path
- [ ] `docs/db/LIQUIBASE.md` updated if regeneration procedure changes

See also [`AGENT-WORKFLOW.md`](../../AGENT-WORKFLOW.md) Phase 2–7 Liquibase gates.
