# Liquibase database change management (#46)

Nutriconsultas uses [Liquibase](https://www.liquibase.org/) for schema and catalog seed data. Hibernate `ddl-auto` is disabled in all environments (`none`).

## Changelog layout

| Path | Purpose |
|------|---------|
| `src/main/resources/db/changelog/db.changelog-master.yaml` | Root changelog |
| `changes/001-baseline-schema.{postgresql,h2}.sql` | Full schema baseline (post-#156 Phase C) |
| `changes/002-seed-data.yaml` | Catalog seed: alimentos, platillos, template dietas |
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

1. Add a new YAML/SQL file under `db/changelog/changes/`.
2. Include it from `db.changelog-master.yaml`.
3. Never rely on `ddl-auto=update` for new columns or tables.
