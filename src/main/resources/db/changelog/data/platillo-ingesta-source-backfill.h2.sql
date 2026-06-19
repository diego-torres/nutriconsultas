UPDATE platillo_ingesta pi
SET source_platillo_id = (
    SELECT p.id
    FROM platillo p
    WHERE p.name = pi.name
      AND p.energia = pi.energia
      AND p.proteina = pi.proteina
      AND p.lipidos = pi.lipidos
      AND p.hidratos_de_carbono = pi.hidratos_de_carbono
    FETCH FIRST 1 ROW ONLY
)
WHERE pi.source_platillo_id IS NULL;
