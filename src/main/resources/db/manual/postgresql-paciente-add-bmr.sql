-- Issue #57: add basal metabolic rate (BMR) stored in kcal/day on paciente.
-- Use when Hibernate ddl-auto is not applied (e.g. external schema management).
-- PostgreSQL 9.1+

ALTER TABLE paciente ADD COLUMN IF NOT EXISTS bmr DOUBLE PRECISION;

COMMENT ON COLUMN paciente.bmr IS 'Basal metabolic rate (kcal/day)';
