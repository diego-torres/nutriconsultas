-- Resync identity sequences after catalog seed rows inserted with explicit IDs.
-- Uses GREATEST(MAX(id), last_value) so brownfield/production sequences never regress.
SELECT setval(pg_get_serial_sequence('dieta', 'id'),
    GREATEST(COALESCE((SELECT MAX(id) FROM dieta), 1), COALESCE((SELECT last_value FROM dieta_id_seq), 1)));
SELECT setval(pg_get_serial_sequence('ingesta', 'id'),
    GREATEST(COALESCE((SELECT MAX(id) FROM ingesta), 1), COALESCE((SELECT last_value FROM ingesta_id_seq), 1)));
SELECT setval(pg_get_serial_sequence('platillo_ingesta', 'id'),
    GREATEST(COALESCE((SELECT MAX(id) FROM platillo_ingesta), 1),
        COALESCE((SELECT last_value FROM platillo_ingesta_id_seq), 1)));
SELECT setval(pg_get_serial_sequence('alimento_ingesta', 'id'),
    GREATEST(COALESCE((SELECT MAX(id) FROM alimento_ingesta), 1),
        COALESCE((SELECT last_value FROM alimento_ingesta_id_seq), 1)));
SELECT setval(pg_get_serial_sequence('ingrediente_platillo_ingesta', 'id'),
    GREATEST(COALESCE((SELECT MAX(id) FROM ingrediente_platillo_ingesta), 1),
        COALESCE((SELECT last_value FROM ingrediente_platillo_ingesta_id_seq), 1)));
SELECT setval(pg_get_serial_sequence('platillo', 'id'),
    GREATEST(COALESCE((SELECT MAX(id) FROM platillo), 1), COALESCE((SELECT last_value FROM platillo_id_seq), 1)));
SELECT setval(pg_get_serial_sequence('ingrediente', 'id'),
    GREATEST(COALESCE((SELECT MAX(id) FROM ingrediente), 1),
        COALESCE((SELECT last_value FROM ingrediente_id_seq), 1)));
SELECT setval(pg_get_serial_sequence('paciente_dieta', 'id'),
    GREATEST(COALESCE((SELECT MAX(id) FROM paciente_dieta), 1),
        COALESCE((SELECT last_value FROM paciente_dieta_id_seq), 1)));
SELECT setval(pg_get_serial_sequence('paciente_dieta_weekday', 'id'),
    GREATEST(COALESCE((SELECT MAX(id) FROM paciente_dieta_weekday), 1),
        COALESCE((SELECT last_value FROM paciente_dieta_weekday_id_seq), 1)));
