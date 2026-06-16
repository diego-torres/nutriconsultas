# Paciente schema after #156 Phase C

Handoff reference for [#46 Liquibase](https://github.com/diego-torres/nutriconsultas/issues/46) baseline.

## Entity-relationship (post Phase C)

```
paciente (core + body snapshot embeddable columns)
├── 1:1 paciente_medical_history   (LAZY, FK paciente_id)
├── 1:1 paciente_energy_preferences (LAZY, FK paciente_id)
└── 1:N body_metric_record, calendar_event, … (unchanged)

paciente_medical_history
└── TEXT antecedentes*, alergias, historial, desarrollo, tipo_sanguineo, 8 pathology flags

paciente_energy_preferences
└── GET/TDEE prefs: activity factors, BMR formula, TEF, physiological stress
```

Body snapshot (`peso`, `estatura`, `imc`, `bmr`, `get_kcal`, `nivel_peso`, cached TDEE fields) remains **embedded** on `paciente` (Phase B). Phase D may later move snapshot to `BodyMetricRecord` only.

## Manual migration (pre-Liquibase)

| Script | Purpose |
|--------|---------|
| `postgresql-paciente-phase-c-split.sql` | Create satellites, copy data, drop wide columns from `paciente` |
| `postgresql-paciente-phase-c-rollback.sql` | Restore wide `paciente` columns from satellites |

Dev/test: Hibernate `ddl-auto=update` creates satellite tables; production applies forward script before deploying Phase C code.
