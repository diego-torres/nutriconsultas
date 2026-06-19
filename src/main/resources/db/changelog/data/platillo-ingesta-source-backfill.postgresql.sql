UPDATE platillo_ingesta pi
SET source_platillo_id = p.id
FROM platillo p
WHERE pi.source_platillo_id IS NULL
  AND pi.name = p.name
  AND pi.energia = p.energia
  AND pi.proteina = p.proteina
  AND pi.lipidos = p.lipidos
  AND pi.hidratos_de_carbono = p.hidratos_de_carbono;
