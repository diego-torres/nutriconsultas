-- Issue #156 Phase C: split medical history and energy preferences into satellite tables.
-- Apply on PostgreSQL when Hibernate ddl-auto is not used (production / external schema management).
-- Prerequisites: backup database; run during maintenance window.
-- Dev: Hibernate ddl-auto=update creates satellite tables automatically; run data migration only.

BEGIN;

CREATE TABLE IF NOT EXISTS paciente_medical_history (
    id BIGSERIAL PRIMARY KEY,
    paciente_id BIGINT NOT NULL UNIQUE REFERENCES paciente (id) ON DELETE CASCADE,
    antecedentes_prenatales TEXT,
    antecedentes_natales TEXT,
    antecedentes_patologicos_personales TEXT,
    antecedentes_patologicos_familiares TEXT,
    complicaciones TEXT,
    tipo_sanguineo VARCHAR(4),
    historial_alimenticio TEXT,
    desarrollo_psicomotor TEXT,
    alergias TEXT,
    hipertension BOOLEAN DEFAULT FALSE,
    diabetes BOOLEAN DEFAULT FALSE,
    hipotiroidismo BOOLEAN DEFAULT FALSE,
    obesidad BOOLEAN DEFAULT FALSE,
    anemia BOOLEAN DEFAULT FALSE,
    bulimia BOOLEAN DEFAULT FALSE,
    anorexia BOOLEAN DEFAULT FALSE,
    enfermedades_hepaticas BOOLEAN DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS paciente_energy_preferences (
    id BIGSERIAL PRIMARY KEY,
    paciente_id BIGINT NOT NULL UNIQUE REFERENCES paciente (id) ON DELETE CASCADE,
    activity_factor_scale VARCHAR(30),
    preferred_bmr_formula VARCHAR(30),
    physical_activity_level VARCHAR(30),
    activity_factor DOUBLE PRECISION,
    custom_factor_sedentary DOUBLE PRECISION,
    custom_factor_light DOUBLE PRECISION,
    custom_factor_moderate DOUBLE PRECISION,
    custom_factor_intense DOUBLE PRECISION,
    custom_factor_very_intense DOUBLE PRECISION,
    physiological_stress_active BOOLEAN DEFAULT FALSE,
    physiological_stress_type VARCHAR(40),
    stress_formula_table VARCHAR(30),
    stress_increment_mode VARCHAR(30),
    stress_factor_value DOUBLE PRECISION,
    stress_valid_from DATE,
    stress_valid_until DATE,
    stress_fever_temperature DOUBLE PRECISION,
    tef_method VARCHAR(30),
    tef_base VARCHAR(30),
    tef_fixed_percent DOUBLE PRECISION,
    tef_macro_protein_percent DOUBLE PRECISION,
    tef_macro_carbs_percent DOUBLE PRECISION,
    tef_macro_fat_percent DOUBLE PRECISION
);

INSERT INTO paciente_medical_history (
    paciente_id,
    antecedentes_prenatales,
    antecedentes_natales,
    antecedentes_patologicos_personales,
    antecedentes_patologicos_familiares,
    complicaciones,
    tipo_sanguineo,
    historial_alimenticio,
    desarrollo_psicomotor,
    alergias,
    hipertension,
    diabetes,
    hipotiroidismo,
    obesidad,
    anemia,
    bulimia,
    anorexia,
    enfermedades_hepaticas
)
SELECT
    id,
    antecedentes_prenatales,
    antecedentes_natales,
    antecedentes_patologicos_personales,
    antecedentes_patologicos_familiares,
    complicaciones,
    tipo_sanguineo,
    historial_alimenticio,
    desarrollo_psicomotor,
    alergias,
    COALESCE(hipertension, FALSE),
    COALESCE(diabetes, FALSE),
    COALESCE(hipotiroidismo, FALSE),
    COALESCE(obesidad, FALSE),
    COALESCE(anemia, FALSE),
    COALESCE(bulimia, FALSE),
    COALESCE(anorexia, FALSE),
    COALESCE(enfermedades_hepaticas, FALSE)
FROM paciente
WHERE NOT EXISTS (
    SELECT 1 FROM paciente_medical_history pmh WHERE pmh.paciente_id = paciente.id
);

INSERT INTO paciente_energy_preferences (
    paciente_id,
    activity_factor_scale,
    preferred_bmr_formula,
    physical_activity_level,
    activity_factor,
    custom_factor_sedentary,
    custom_factor_light,
    custom_factor_moderate,
    custom_factor_intense,
    custom_factor_very_intense,
    physiological_stress_active,
    physiological_stress_type,
    stress_formula_table,
    stress_increment_mode,
    stress_factor_value,
    stress_valid_from,
    stress_valid_until,
    stress_fever_temperature,
    tef_method,
    tef_base,
    tef_fixed_percent,
    tef_macro_protein_percent,
    tef_macro_carbs_percent,
    tef_macro_fat_percent
)
SELECT
    id,
    activity_factor_scale,
    preferred_bmr_formula,
    physical_activity_level,
    activity_factor,
    custom_factor_sedentary,
    custom_factor_light,
    custom_factor_moderate,
    custom_factor_intense,
    custom_factor_very_intense,
    COALESCE(physiological_stress_active, FALSE),
    physiological_stress_type,
    stress_formula_table,
    stress_increment_mode,
    stress_factor_value,
    stress_valid_from,
    stress_valid_until,
    stress_fever_temperature,
    tef_method,
    tef_base,
    tef_fixed_percent,
    tef_macro_protein_percent,
    tef_macro_carbs_percent,
    tef_macro_fat_percent
FROM paciente
WHERE NOT EXISTS (
    SELECT 1 FROM paciente_energy_preferences pep WHERE pep.paciente_id = paciente.id
);

ALTER TABLE paciente DROP COLUMN IF EXISTS antecedentes_prenatales;
ALTER TABLE paciente DROP COLUMN IF EXISTS antecedentes_natales;
ALTER TABLE paciente DROP COLUMN IF EXISTS antecedentes_patologicos_personales;
ALTER TABLE paciente DROP COLUMN IF EXISTS antecedentes_patologicos_familiares;
ALTER TABLE paciente DROP COLUMN IF EXISTS complicaciones;
ALTER TABLE paciente DROP COLUMN IF EXISTS tipo_sanguineo;
ALTER TABLE paciente DROP COLUMN IF EXISTS historial_alimenticio;
ALTER TABLE paciente DROP COLUMN IF EXISTS desarrollo_psicomotor;
ALTER TABLE paciente DROP COLUMN IF EXISTS alergias;
ALTER TABLE paciente DROP COLUMN IF EXISTS hipertension;
ALTER TABLE paciente DROP COLUMN IF EXISTS diabetes;
ALTER TABLE paciente DROP COLUMN IF EXISTS hipotiroidismo;
ALTER TABLE paciente DROP COLUMN IF EXISTS obesidad;
ALTER TABLE paciente DROP COLUMN IF EXISTS anemia;
ALTER TABLE paciente DROP COLUMN IF EXISTS bulimia;
ALTER TABLE paciente DROP COLUMN IF EXISTS anorexia;
ALTER TABLE paciente DROP COLUMN IF EXISTS enfermedades_hepaticas;

ALTER TABLE paciente DROP COLUMN IF EXISTS activity_factor_scale;
ALTER TABLE paciente DROP COLUMN IF EXISTS preferred_bmr_formula;
ALTER TABLE paciente DROP COLUMN IF EXISTS physical_activity_level;
ALTER TABLE paciente DROP COLUMN IF EXISTS activity_factor;
ALTER TABLE paciente DROP COLUMN IF EXISTS custom_factor_sedentary;
ALTER TABLE paciente DROP COLUMN IF EXISTS custom_factor_light;
ALTER TABLE paciente DROP COLUMN IF EXISTS custom_factor_moderate;
ALTER TABLE paciente DROP COLUMN IF EXISTS custom_factor_intense;
ALTER TABLE paciente DROP COLUMN IF EXISTS custom_factor_very_intense;
ALTER TABLE paciente DROP COLUMN IF EXISTS physiological_stress_active;
ALTER TABLE paciente DROP COLUMN IF EXISTS physiological_stress_type;
ALTER TABLE paciente DROP COLUMN IF EXISTS stress_formula_table;
ALTER TABLE paciente DROP COLUMN IF EXISTS stress_increment_mode;
ALTER TABLE paciente DROP COLUMN IF EXISTS stress_factor_value;
ALTER TABLE paciente DROP COLUMN IF EXISTS stress_valid_from;
ALTER TABLE paciente DROP COLUMN IF EXISTS stress_valid_until;
ALTER TABLE paciente DROP COLUMN IF EXISTS stress_fever_temperature;
ALTER TABLE paciente DROP COLUMN IF EXISTS tef_method;
ALTER TABLE paciente DROP COLUMN IF EXISTS tef_base;
ALTER TABLE paciente DROP COLUMN IF EXISTS tef_fixed_percent;
ALTER TABLE paciente DROP COLUMN IF EXISTS tef_macro_protein_percent;
ALTER TABLE paciente DROP COLUMN IF EXISTS tef_macro_carbs_percent;
ALTER TABLE paciente DROP COLUMN IF EXISTS tef_macro_fat_percent;

COMMIT;
