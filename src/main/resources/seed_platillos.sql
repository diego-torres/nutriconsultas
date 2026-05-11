-- Common platillo catalog for fresh installs. Ingredient references use SMAE/alimentos.sql
-- `nombre_alimento` values so they survive database insert-order differences (#84).

DROP TABLE IF EXISTS seed_platillo_ingrediente CASCADE;
DROP TABLE IF EXISTS seed_platillo CASCADE;

CREATE TABLE seed_platillo (
  name varchar(255) PRIMARY KEY,
  description varchar(510),
  ingestas_sugeridas varchar(255)
);

CREATE TABLE seed_platillo_ingrediente (
  id BIGSERIAL PRIMARY KEY,
  platillo_name varchar(255) NOT NULL REFERENCES seed_platillo(name) ON DELETE CASCADE,
  alimento_nombre varchar(255) NOT NULL,
  alimento_id bigint,
  peso_neto integer NOT NULL,
  orden integer NOT NULL DEFAULT 1
);

INSERT INTO seed_platillo (name, description, ingestas_sugeridas) VALUES
('Huevos revueltos con tortilla', 'Huevo con tortilla de maíz y jitomate. Desayuno frecuente.', 'Desayuno'),
('Avena con leche y fruta', 'Avena cocida con leche descremada, plátano y manzana.', 'Desayuno'),
('Arroz con pollo y frijoles', 'Arroz cocido, pollo sin piel, frijoles, jitomate y aceite.', 'Comida'),
('Espagueti con brócoli', 'Pasta cocida con brócoli y aceite.', 'Comida,Cena'),
('Ensalada con aguacate', 'Lechuga, pepino, jitomate, aguacate hass y aceite de oliva.', 'Comida,Cena'),
('Frijoles con tortilla', 'Frijoles de olla enteros enlatados equivalentes con tortilla de maíz.', 'Comida'),
('Salmón con arroz integral', 'Arroz integral cocido, salmón cocido con aceite.', 'Comida,Cena'),
('Yoghur con fruta', 'Yoghur light con plátano y manzana (snack ligero).', 'Colacion');

INSERT INTO seed_platillo_ingrediente (platillo_name, alimento_nombre, peso_neto, orden) VALUES
('Huevos revueltos con tortilla', 'Huevo entero fresco', 132, 1),
('Huevos revueltos con tortilla', 'Tortilla de maíz', 60, 2),
('Huevos revueltos con tortilla', 'Aceite', 5, 3),
('Huevos revueltos con tortilla', 'Jitomate', 56, 4),

('Avena con leche y fruta', 'Avena cocida', 164, 1),
('Avena con leche y fruta', 'Leche descremada', 245, 2),
('Avena con leche y fruta', 'Plátano', 54, 3),
('Avena con leche y fruta', 'Manzana', 106, 4),

('Arroz con pollo y frijoles', 'Arroz cocido', 188, 1),
('Arroz con pollo y frijoles', 'Pollo sin piel cocido', 120, 2),
('Arroz con pollo y frijoles', 'Frijol promedio cocido', 86, 3),
('Arroz con pollo y frijoles', 'Jitomate', 56, 4),
('Arroz con pollo y frijoles', 'Aceite', 5, 5),

('Espagueti con brócoli', 'Espagueti cocido', 138, 1),
('Espagueti con brócoli', 'Brócoli cocido', 184, 2),
('Espagueti con brócoli', 'Aceite de oliva', 5, 3),

('Ensalada con aguacate', 'Lechuga', 135, 1),
('Ensalada con aguacate', 'Pepino con cáscara rebanado', 104, 2),
('Ensalada con aguacate', 'Jitomate', 56, 3),
('Ensalada con aguacate', 'Aguacate hass', 58, 4),
('Ensalada con aguacate', 'Aceite de oliva', 5, 5),

('Frijoles con tortilla', 'Frijoles enteros enlatados', 128, 1),
('Frijoles con tortilla', 'Tortilla de maíz', 90, 2),
('Frijoles con tortilla', 'Jitomate', 56, 3),

('Salmón con arroz integral', 'Arroz integral cocido', 195, 1),
('Salmón con arroz integral', 'Salmón cocido', 90, 2),
('Salmón con arroz integral', 'Aceite', 5, 3),

('Yoghur con fruta', 'Yoghur light', 180, 1),
('Yoghur con fruta', 'Plátano', 54, 2),
('Yoghur con fruta', 'Manzana', 106, 3);
