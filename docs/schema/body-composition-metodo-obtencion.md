# DDL: método de obtención de composición corporal (#161)

Hibernate `ddl-auto=update` aplica estos cambios en desarrollo. Para producción con Liquibase (#46), usar como referencia tras #156.

```sql
-- body_composition (entidad BodyComposition)
ALTER TABLE body_composition
  ADD COLUMN IF NOT EXISTS metodo_obtencion VARCHAR(30);

-- body_metric_record (timeline agregado)
ALTER TABLE body_metric_record
  ADD COLUMN IF NOT EXISTS metodo_obtencion_composicion VARCHAR(30);
```

Valores permitidos (`MetodoObtencionComposicionCorporal`):

- `MANUAL`
- `BIOIMPEDANCIA`
- `PLIEGUES`
- `DEURENBERG`
- `DEXA`
- `OTRO`

Registros existentes quedan con `NULL` (sin método registrado).
