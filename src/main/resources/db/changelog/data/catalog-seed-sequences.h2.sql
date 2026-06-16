-- Reset identity columns after explicit-ID catalog seed inserts (H2 PostgreSQL mode).
ALTER TABLE platillo ALTER COLUMN id RESTART WITH 101;
ALTER TABLE ingrediente ALTER COLUMN id RESTART WITH 399;
ALTER TABLE dieta ALTER COLUMN id RESTART WITH 21;
ALTER TABLE ingesta ALTER COLUMN id RESTART WITH 81;
ALTER TABLE platillo_ingesta ALTER COLUMN id RESTART WITH 81;
ALTER TABLE ingrediente_platillo_ingesta ALTER COLUMN id RESTART WITH 308;
