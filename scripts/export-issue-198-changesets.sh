#!/usr/bin/env bash
# Export issue #198 catalog rows from local PostgreSQL into Liquibase SQL files.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
DATA_DIR="$ROOT/src/main/resources/db/changelog/data"
CONTAINER="${POSTGRES_CONTAINER:-nutriconsultas-db}"

dump_table() {
  local table="$1"
  local where="$2"
  local outfile="$3"
  podman exec "$CONTAINER" psql -U nutriconsultas -d nutriconsultas -At -c \
    "COPY (SELECT * FROM ${table} WHERE ${where} ORDER BY id) TO STDOUT WITH (FORMAT csv, HEADER false, DELIMITER E'\\t')" \
    >/dev/null 2>&1 || true
  podman exec "$CONTAINER" pg_dump -U nutriconsultas -d nutriconsultas \
    --data-only --column-inserts --no-owner --no-privileges \
    --table="$table" 2>/dev/null | grep '^INSERT INTO' | grep -E "$4" \
    | sed 's/INSERT INTO public\./INSERT INTO /' >"$outfile" || true
}

mkdir -p "$DATA_DIR"

podman exec "$CONTAINER" pg_dump -U nutriconsultas -d nutriconsultas \
  --data-only --column-inserts --no-owner --no-privileges \
  --table=platillo --table=ingrediente \
  | grep '^INSERT INTO' | sed 's/INSERT INTO public\./INSERT INTO /' \
  | awk '/INSERT INTO platillo .*VALUES \(109,|INSERT INTO platillo .*VALUES \(110,|INSERT INTO platillo .*VALUES \(111,|INSERT INTO ingrediente .*platillo_id.*\(109\)|INSERT INTO ingrediente .*platillo_id.*\(110\)|INSERT INTO ingrediente .*platillo_id.*\(111\)/' \
  >"$DATA_DIR/issue-198-mexican-platillos-seed.sql"

podman exec "$CONTAINER" pg_dump -U nutriconsultas -d nutriconsultas \
  --data-only --column-inserts --no-owner --no-privileges \
  --table=dieta --table=ingesta --table=platillo_ingesta --table=ingrediente_platillo_ingesta \
  | grep '^INSERT INTO' | sed 's/INSERT INTO public\./INSERT INTO /' \
  | awk '/INSERT INTO dieta .*VALUES \((2[6-9]|[3-5][0-9]),|INSERT INTO ingesta .*dieta_id.*\((2[6-9]|[3-5][0-9])\)|INSERT INTO platillo_ingesta|INSERT INTO ingrediente_platillo_ingesta/' \
  >"$DATA_DIR/issue-198-high-kcal-dieta-templates-seed.sql"

echo "Exported:"
wc -l "$DATA_DIR/issue-198-mexican-platillos-seed.sql" "$DATA_DIR/issue-198-high-kcal-dieta-templates-seed.sql"
