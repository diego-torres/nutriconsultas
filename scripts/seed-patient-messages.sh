#!/usr/bin/env bash
# Seeds sample patient messages in the local PostgreSQL database for manual UI testing.
# Requires the nutriconsultas-db container (see dev-start.sh).

set -euo pipefail

CONTAINER_NAME="${CONTAINER_NAME:-nutriconsultas-db}"
POSTGRES_USER="${POSTGRES_USER:-nutriconsultas}"
POSTGRES_DB="${POSTGRES_DB:-nutriconsultas}"

if ! command -v podman >/dev/null 2>&1; then
  echo "podman is required to run this script." >&2
  exit 1
fi

if ! podman ps --format '{{.Names}}' | grep -qx "${CONTAINER_NAME}"; then
  echo "Database container '${CONTAINER_NAME}' is not running. Start it with ./dev-start.sh first." >&2
  exit 1
fi

echo "Seeding patient messages for the first two patients in the database..."

podman exec -i "${CONTAINER_NAME}" psql -U "${POSTGRES_USER}" -d "${POSTGRES_DB}" <<'SQL'
WITH target_patients AS (
  SELECT id, user_id, name
  FROM paciente
  ORDER BY id
  LIMIT 2
),
inserted AS (
  INSERT INTO patient_message (
    paciente_id,
    nutritionist_user_id,
    sender_role,
    body,
    sent_at,
    read_by_patient,
    read_by_nutritionist
  )
  SELECT
    p.id,
    p.user_id,
    v.sender_role,
    v.body,
    NOW() - (v.minutes_ago || ' minutes')::interval,
    v.read_by_patient,
    v.read_by_nutritionist
  FROM target_patients p
  CROSS JOIN (
    VALUES
      ('PATIENT', 'Hola doctor, ¿puedo sustituir el pollo del almuerzo?', 180, true, false),
      ('NUTRITIONIST', 'Sí, puedes usar pescado blanco o tofu en la misma porción.', 150, false, true),
      ('PATIENT', 'Perfecto, gracias. ¿Y el snack de la tarde?', 45, true, false)
  ) AS v(sender_role, body, minutes_ago, read_by_patient, read_by_nutritionist)
  WHERE NOT EXISTS (
    SELECT 1
    FROM patient_message m
    WHERE m.paciente_id = p.id
      AND m.body = v.body
  )
  RETURNING paciente_id
)
SELECT COUNT(*) AS inserted_messages FROM inserted;
SQL

echo "Done. Open /admin and /admin/pacientes/{id} to test the chat widget."
