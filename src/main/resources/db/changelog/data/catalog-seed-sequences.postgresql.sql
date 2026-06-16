-- Reset identity sequences after explicit-ID catalog seed inserts.
SELECT setval(pg_get_serial_sequence('platillo', 'id'), COALESCE((SELECT MAX(id) FROM platillo), 1));
SELECT setval(pg_get_serial_sequence('ingrediente', 'id'), COALESCE((SELECT MAX(id) FROM ingrediente), 1));
SELECT setval(pg_get_serial_sequence('dieta', 'id'), COALESCE((SELECT MAX(id) FROM dieta), 1));
SELECT setval(pg_get_serial_sequence('ingesta', 'id'), COALESCE((SELECT MAX(id) FROM ingesta), 1));
SELECT setval(pg_get_serial_sequence('platillo_ingesta', 'id'), COALESCE((SELECT MAX(id) FROM platillo_ingesta), 1));
SELECT setval(pg_get_serial_sequence('ingrediente_platillo_ingesta', 'id'),
    COALESCE((SELECT MAX(id) FROM ingrediente_platillo_ingesta), 1));
