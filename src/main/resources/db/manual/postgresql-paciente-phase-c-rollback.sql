-- Rollback for postgresql-paciente-phase-c-split.sql (Issue #156 Phase C).
-- Restores wide paciente columns from satellite tables. Satellite rows must exist.
-- WARNING: run only if forward migration was applied and rollback is required.

BEGIN;

ALTER TABLE paciente ADD COLUMN IF NOT EXISTS antecedentes_prenatales TEXT;
ALTER TABLE paciente ADD COLUMN IF NOT EXISTS antecedentes_natales TEXT;
ALTER TABLE paciente ADD COLUMN IF NOT EXISTS antecedentes_patologicos_personales TEXT;
ALTER TABLE paciente ADD COLUMN IF NOT EXISTS antecedentes_patologicos_familiares TEXT;
ALTER TABLE paciente ADD COLUMN IF NOT EXISTS complicaciones TEXT;
ALTER TABLE paciente ADD COLUMN IF NOT EXISTS tipo_sanguineo VARCHAR(4);
ALTER TABLE paciente ADD COLUMN IF NOT EXISTS historial_alimenticio TEXT;
ALTER TABLE paciente ADD COLUMN IF NOT EXISTS desarrollo_psicomotor TEXT;
ALTER TABLE paciente ADD COLUMN IF NOT EXISTS alergias TEXT;
ALTER TABLE paciente ADD COLUMN IF NOT EXISTS hipertension BOOLEAN DEFAULT FALSE;
ALTER TABLE paciente ADD COLUMN IF NOT EXISTS diabetes BOOLEAN DEFAULT FALSE;
ALTER TABLE paciente ADD COLUMN IF NOT EXISTS hipotiroidismo BOOLEAN DEFAULT FALSE;
ALTER TABLE paciente ADD COLUMN IF NOT EXISTS obesidad BOOLEAN DEFAULT FALSE;
ALTER TABLE paciente ADD COLUMN IF NOT EXISTS anemia BOOLEAN DEFAULT FALSE;
ALTER TABLE paciente ADD COLUMN IF NOT EXISTS bulimia BOOLEAN DEFAULT FALSE;
ALTER TABLE paciente ADD COLUMN IF NOT EXISTS anorexia BOOLEAN DEFAULT FALSE;
ALTER TABLE paciente ADD COLUMN IF NOT EXISTS enfermedades_hepaticas BOOLEAN DEFAULT FALSE;

ALTER TABLE paciente ADD COLUMN IF NOT EXISTS activity_factor_scale VARCHAR(30);
ALTER TABLE paciente ADD COLUMN IF NOT EXISTS preferred_bmr_formula VARCHAR(30);
ALTER TABLE paciente ADD COLUMN IF NOT EXISTS physical_activity_level VARCHAR(30);
ALTER TABLE paciente ADD COLUMN IF NOT EXISTS activity_factor DOUBLE PRECISION;
ALTER TABLE paciente ADD COLUMN IF NOT EXISTS custom_factor_sedentary DOUBLE PRECISION;
ALTER TABLE paciente ADD COLUMN IF NOT EXISTS custom_factor_light DOUBLE PRECISION;
ALTER TABLE paciente ADD COLUMN IF NOT EXISTS custom_factor_moderate DOUBLE PRECISION;
ALTER TABLE paciente ADD COLUMN IF NOT EXISTS custom_factor_intense DOUBLE PRECISION;
ALTER TABLE paciente ADD COLUMN IF NOT EXISTS custom_factor_very_intense DOUBLE PRECISION;
ALTER TABLE paciente ADD COLUMN IF NOT EXISTS physiological_stress_active BOOLEAN DEFAULT FALSE;
ALTER TABLE paciente ADD COLUMN IF NOT EXISTS physiological_stress_type VARCHAR(40);
ALTER TABLE paciente ADD COLUMN IF NOT EXISTS stress_formula_table VARCHAR(30);
ALTER TABLE paciente ADD COLUMN IF NOT EXISTS stress_increment_mode VARCHAR(30);
ALTER TABLE paciente ADD COLUMN IF NOT EXISTS stress_factor_value DOUBLE PRECISION;
ALTER TABLE paciente ADD COLUMN IF NOT EXISTS stress_valid_from DATE;
ALTER TABLE paciente ADD COLUMN IF NOT EXISTS stress_valid_until DATE;
ALTER TABLE paciente ADD COLUMN IF NOT EXISTS stress_fever_temperature DOUBLE PRECISION;
ALTER TABLE paciente ADD COLUMN IF NOT EXISTS tef_method VARCHAR(30);
ALTER TABLE paciente ADD COLUMN IF NOT EXISTS tef_base VARCHAR(30);
ALTER TABLE paciente ADD COLUMN IF NOT EXISTS tef_fixed_percent DOUBLE PRECISION;
ALTER TABLE paciente ADD COLUMN IF NOT EXISTS tef_macro_protein_percent DOUBLE PRECISION;
ALTER TABLE paciente ADD COLUMN IF NOT EXISTS tef_macro_carbs_percent DOUBLE PRECISION;
ALTER TABLE paciente ADD COLUMN IF NOT EXISTS tef_macro_fat_percent DOUBLE PRECISION;

UPDATE paciente p
SET
    antecedentes_prenatales = pmh.antecedentes_prenatales,
    antecedentes_natales = pmh.antecedentes_natales,
    antecedentes_patologicos_personales = pmh.antecedentes_patologicos_personales,
    antecedentes_patologicos_familiares = pmh.antecedentes_patologicos_familiares,
    complicaciones = pmh.complicaciones,
    tipo_sanguineo = pmh.tipo_sanguineo,
    historial_alimenticio = pmh.historial_alimenticio,
    desarrollo_psicomotor = pmh.desarrollo_psicomotor,
    alergias = pmh.alergias,
    hipertension = pmh.hipertension,
    diabetes = pmh.diabetes,
    hipotiroidismo = pmh.hipotiroidismo,
    obesidad = pmh.obesidad,
    anemia = pmh.anemia,
    bulimia = pmh.bulimia,
    anorexia = pmh.anorexia,
    enfermedades_hepaticas = pmh.enfermedades_hepaticas
FROM paciente_medical_history pmh
WHERE pmh.paciente_id = p.id;

UPDATE paciente p
SET
    activity_factor_scale = pep.activity_factor_scale,
    preferred_bmr_formula = pep.preferred_bmr_formula,
    physical_activity_level = pep.physical_activity_level,
    activity_factor = pep.activity_factor,
    custom_factor_sedentary = pep.custom_factor_sedentary,
    custom_factor_light = pep.custom_factor_light,
    custom_factor_moderate = pep.custom_factor_moderate,
    custom_factor_intense = pep.custom_factor_intense,
    custom_factor_very_intense = pep.custom_factor_very_intense,
    physiological_stress_active = pep.physiological_stress_active,
    physiological_stress_type = pep.physiological_stress_type,
    stress_formula_table = pep.stress_formula_table,
    stress_increment_mode = pep.stress_increment_mode,
    stress_factor_value = pep.stress_factor_value,
    stress_valid_from = pep.stress_valid_from,
    stress_valid_until = pep.stress_valid_until,
    stress_fever_temperature = pep.stress_fever_temperature,
    tef_method = pep.tef_method,
    tef_base = pep.tef_base,
    tef_fixed_percent = pep.tef_fixed_percent,
    tef_macro_protein_percent = pep.tef_macro_protein_percent,
    tef_macro_carbs_percent = pep.tef_macro_carbs_percent,
    tef_macro_fat_percent = pep.tef_macro_fat_percent
FROM paciente_energy_preferences pep
WHERE pep.paciente_id = p.id;

DROP TABLE IF EXISTS paciente_energy_preferences;
DROP TABLE IF EXISTS paciente_medical_history;

COMMIT;
