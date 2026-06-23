#!/usr/bin/env python3
"""Build issue #198 Mexican platillos and high-kcal diet templates in local PostgreSQL.

Uses existing alimento rows only. Run after restoring prod/local catalog:
  python3 scripts/build-issue-198-catalog.py

Requires: podman exec access to nutriconsultas-db container.
"""

from __future__ import annotations

import json
import subprocess
import sys
from dataclasses import dataclass
from typing import Any

CONTAINER = "nutriconsultas-db"
DB_USER = "nutriconsultas"
DB_NAME = "nutriconsultas"

SYSTEM_TEMPLATE_USER = "system:template-dietas"
SYSTEM_PLATILLO_USER = "system:catalog-platillos"

PLATILLO_IDS = {"molletes": 109, "tacos_pollo": 110, "entomatadas": 111}
DIETA_ID_START = 26
TEMPLATE_COUNT = 30
KCAL_MIN = 2500
KCAL_MAX = 3500

INGESTA_NAMES = ("Desayuno", "Colación", "Comida", "Cena")


@dataclass(frozen=True)
class IngredientSpec:
    alimento_id: int
    cantidad: str
    peso: int


@dataclass(frozen=True)
class PlatilloSpec:
    platillo_id: int
    name: str
    ingestas: str
    description: str
    ingredients: tuple[IngredientSpec, ...]


PLATILLOS: tuple[PlatilloSpec, ...] = (
    PlatilloSpec(
        109,
        "Molletes",
        "Desayuno",
        "Pan integral, frijoles, pico de gallo, queso panela y fresas (menú GAZPRO 1800 kcal).",
        (
            IngredientSpec(652, "2", 50),
            IngredientSpec(941, "1/3", 57),
            IngredientSpec(97, "1/2", 54),
            IngredientSpec(17, "1/4", 29),
            IngredientSpec(1316, "1", 40),
            IngredientSpec(197, "1", 166),
        ),
    ),
    PlatilloSpec(
        110,
        "Tacos de pollo",
        "Comida",
        "Pechuga de pollo asada, tortillas, verduras y fruta (menú GAZPRO 1800 kcal).",
        (
            IngredientSpec(1139, "3 1/3", 100),
            IngredientSpec(717, "4", 120),
            IngredientSpec(1697, "2", 10),
            IngredientSpec(57, "1", 124),
            IngredientSpec(97, "1/2", 54),
            IngredientSpec(17, "1/4", 29),
            IngredientSpec(95, "1", 120),
            IngredientSpec(122, "1/2", 52),
            IngredientSpec(317, "1", 124),
        ),
    ),
    PlatilloSpec(
        111,
        "Entomatadas",
        "Cena",
        "Tortillas con frijol, salsa de jitomate, queso panela, nopales y manzana.",
        (
            IngredientSpec(1316, "1", 40),
            IngredientSpec(717, "4", 120),
            IngredientSpec(940, "1/3", 73),
            IngredientSpec(97, "1", 108),
            IngredientSpec(114, "1", 149),
            IngredientSpec(240, "1", 106),
        ),
    ),
)

# Existing catalog platillos used to round out high-kcal menus (id, default portions in builder).
SUPPORT_PLATILLOS = {
    "avena_fruta": 94,
    "arroz_pollo_frijoles": 93,
    "huevos_tortilla": 98,
    "yoghur_fruta": 100,
    "frijoles_tortilla": 97,
}


def psql_json(sql: str) -> list[dict[str, Any]]:
    wrapped = f"SELECT COALESCE(json_agg(t), '[]'::json) FROM ({sql}) t"
    cmd = [
        "podman",
        "exec",
        CONTAINER,
        "psql",
        "-U",
        DB_USER,
        "-d",
        DB_NAME,
        "-t",
        "-A",
        "-c",
        wrapped,
    ]
    result = subprocess.run(cmd, check=True, capture_output=True, text=True)
    raw = result.stdout.strip()
    return json.loads(raw) if raw else []


def psql_exec(sql: str) -> None:
    cmd = ["podman", "exec", "-i", CONTAINER, "psql", "-U", DB_USER, "-d", DB_NAME, "-v", "ON_ERROR_STOP=1"]
    subprocess.run(cmd, input=sql, check=True, text=True, capture_output=True)


def parse_fraction(value: str) -> float:
    value = value.strip()
    if " " in value:
        whole, frac = value.split(" ", 1)
        num, den = frac.split("/")
        return int(whole) + int(num) / int(den)
    if "/" in value:
        num, den = value.split("/")
        return int(num) / int(den)
    return float(value)


def scale_nutrients(alimento: dict[str, Any], cantidad: str, peso: int) -> dict[str, Any]:
    cant_sugerida = float(alimento["cant_sugerida"])
    parsed = parse_fraction(cantidad)
    if abs(parsed - cant_sugerida) > 1e-6:
        factor = parsed / cant_sugerida
    else:
        factor = peso / float(alimento["peso_neto"])

    def scale_num(key: str, integer: bool = False) -> Any:
        val = alimento.get(key)
        if val is None:
            return None
        scaled = float(val) * factor
        return int(round(scaled)) if integer else scaled

    return {
        "energia": scale_num("energia", integer=True),
        "hidratos_de_carbono": scale_num("hidratos_de_carbono"),
        "lipidos": scale_num("lipidos"),
        "proteina": scale_num("proteina"),
        "acido_ascorbico": scale_num("acido_ascorbico"),
        "acido_folico": scale_num("acido_folico"),
        "azucar_por_equivalente": scale_num("azucar_por_equivalente"),
        "calcio": scale_num("calcio"),
        "carga_glicemica": scale_num("carga_glicemica"),
        "colesterol": scale_num("colesterol"),
        "etanol": scale_num("etanol"),
        "fibra": scale_num("fibra"),
        "fosforo": scale_num("fosforo"),
        "hierro": scale_num("hierro"),
        "hierro_no_hem": scale_num("hierro_no_hem"),
        "indice_glicemico": scale_num("indice_glicemico"),
        "peso_bruto_redondeado": scale_num("peso_bruto_redondeado", integer=True),
        "peso_neto": scale_num("peso_neto", integer=True),
        "potasio": scale_num("potasio"),
        "selenio": scale_num("selenio"),
        "sodio": scale_num("sodio"),
        "vita": scale_num("vita"),
        "cant_sugerida": parsed,
        "unidad": alimento["unidad"],
    }


def sum_nutrients(rows: list[dict[str, Any]]) -> dict[str, Any]:
    totals: dict[str, float] = {
        "energia": 0.0,
        "hidratos_de_carbono": 0.0,
        "lipidos": 0.0,
        "proteina": 0.0,
    }
    for row in rows:
        for key in totals:
            val = row.get(key)
            if val is not None:
                totals[key] += float(val)
    totals["energia"] = int(round(totals["energia"]))
    return totals


def load_alimentos() -> dict[int, dict[str, Any]]:
    rows = psql_json("SELECT * FROM alimento")
    return {int(row["id"]): row for row in rows}


def load_platillo(platillo_id: int) -> dict[str, Any]:
    rows = psql_json(f"SELECT energia, proteina, lipidos, hidratos_de_carbono FROM platillo WHERE id = {platillo_id}")
    if not rows:
        raise RuntimeError(f"Platillo {platillo_id} not found")
    return rows[0]


def platillo_exists(name: str) -> bool:
    rows = psql_json(f"SELECT id FROM platillo WHERE name = '{name.replace(chr(39), chr(39)+chr(39))}'")
    return len(rows) > 0


def delete_platillo_if_exists(platillo_id: int) -> None:
    psql_exec(
        f"""
        DELETE FROM ingrediente WHERE platillo_id = {platillo_id};
        DELETE FROM platillo WHERE id = {platillo_id};
        """
    )


def insert_platillo(spec: PlatilloSpec, alimentos: dict[int, dict[str, Any]], ingrediente_id_start: int) -> int:
    delete_platillo_if_exists(spec.platillo_id)
    ingredient_rows: list[dict[str, Any]] = []
    ing_id = ingrediente_id_start
    for ing in spec.ingredients:
        alimento = alimentos[ing.alimento_id]
        nutrients = scale_nutrients(alimento, ing.cantidad, ing.peso)
        nutrients["alimento_id"] = ing.alimento_id
        nutrients["platillo_id"] = spec.platillo_id
        nutrients["id"] = ing_id
        nutrients["description"] = None
        ingredient_rows.append(nutrients)
        ing_id += 1

    totals = sum_nutrients(ingredient_rows)
    desc = spec.description.replace("'", "''")
    name = spec.name.replace("'", "''")
    psql_exec(
        f"""
        INSERT INTO platillo (
          id, energia, hidratos_de_carbono, lipidos, proteina,
          description, ingestas_sugeridas, name, user_id
        ) VALUES (
          {spec.platillo_id}, {totals['energia']}, {totals['hidratos_de_carbono']},
          {totals['lipidos']}, {totals['proteina']},
          '{desc}', '{spec.ingestas}', '{name}', '{SYSTEM_PLATILLO_USER}'
        );
        """
    )

    for row in ingredient_rows:
        psql_exec(
            f"""
            INSERT INTO ingrediente (
              id, energia, hidratos_de_carbono, lipidos, proteina,
              acido_ascorbico, acido_folico, azucar_por_equivalente, calcio, carga_glicemica,
              colesterol, etanol, fibra, fosforo, hierro, hierro_no_hem, indice_glicemico,
              peso_bruto_redondeado, peso_neto, potasio, selenio, sodio, vita,
              cant_sugerida, unidad, alimento_id, platillo_id
            ) VALUES (
              {row['id']}, {row['energia']}, {row['hidratos_de_carbono']}, {row['lipidos']}, {row['proteina']},
              {sql_num(row.get('acido_ascorbico'))}, {sql_num(row.get('acido_folico'))},
              {sql_num(row.get('azucar_por_equivalente'))}, {sql_num(row.get('calcio'))},
              {sql_num(row.get('carga_glicemica'))}, {sql_num(row.get('colesterol'))},
              {sql_num(row.get('etanol'))}, {sql_num(row.get('fibra'))}, {sql_num(row.get('fosforo'))},
              {sql_num(row.get('hierro'))}, {sql_num(row.get('hierro_no_hem'))},
              {sql_num(row.get('indice_glicemico'))}, {sql_num(row.get('peso_bruto_redondeado'))},
              {sql_num(row.get('peso_neto'))}, {sql_num(row.get('potasio'))},
              {sql_num(row.get('selenio'))}, {sql_num(row.get('sodio'))}, {sql_num(row.get('vita'))},
              {row['cant_sugerida']}, '{row['unidad']}', {row['alimento_id']}, {row['platillo_id']}
            );
            """
        )
    return ing_id


def sql_num(value: Any) -> str:
    if value is None:
        return "NULL"
    return str(value)


def delete_high_kcal_templates() -> None:
    psql_exec(
        f"""
        DELETE FROM ingrediente_platillo_ingesta WHERE platillo_id IN (
          SELECT pi.id FROM platillo_ingesta pi
          JOIN ingesta i ON pi.ingesta_id = i.id
          JOIN dieta d ON i.dieta_id = d.id
          WHERE d.user_id = '{SYSTEM_TEMPLATE_USER}'
            AND d.id >= {DIETA_ID_START}
        );
        DELETE FROM platillo_ingesta WHERE ingesta_id IN (
          SELECT i.id FROM ingesta i
          JOIN dieta d ON i.dieta_id = d.id
          WHERE d.user_id = '{SYSTEM_TEMPLATE_USER}'
            AND d.id >= {DIETA_ID_START}
        );
        DELETE FROM alimento_ingesta WHERE ingesta_id IN (
          SELECT i.id FROM ingesta i
          JOIN dieta d ON i.dieta_id = d.id
          WHERE d.user_id = '{SYSTEM_TEMPLATE_USER}'
            AND d.id >= {DIETA_ID_START}
        );
        DELETE FROM ingesta WHERE dieta_id IN (
          SELECT id FROM dieta
          WHERE user_id = '{SYSTEM_TEMPLATE_USER}' AND id >= {DIETA_ID_START}
        );
        DELETE FROM dieta
        WHERE user_id = '{SYSTEM_TEMPLATE_USER}' AND id >= {DIETA_ID_START};
        """
    )


def fetch_platillo_ingesta_row(platillo_id: int, portions: int, ingesta_id: int, pi_id: int) -> None:
    plat = psql_json(f"SELECT * FROM platillo WHERE id = {platillo_id}")[0]
    name = plat["name"].replace("'", "''")
    desc = (plat.get("description") or "").replace("'", "''")
    energia = int(round(float(plat["energia"]) * portions))
    hc = float(plat["hidratos_de_carbono"]) * portions
    lip = float(plat["lipidos"]) * portions
    prot = float(plat["proteina"]) * portions
    psql_exec(
        f"""
        INSERT INTO platillo_ingesta (
          id, energia, hidratos_de_carbono, lipidos, proteina, name, portions,
          recommendations, ingesta_id, source_platillo_id
        ) VALUES (
          {pi_id}, {energia}, {hc}, {lip}, {prot}, '{name}', {portions},
          '{desc}', {ingesta_id}, {platillo_id}
        );
        """
    )
    ingredients = psql_json(f"SELECT * FROM ingrediente WHERE platillo_id = {platillo_id} ORDER BY id")
    ipi_id = pi_id * 100
    for ing in ingredients:
        factor = portions
        psql_exec(
            f"""
            INSERT INTO ingrediente_platillo_ingesta (
              id, energia, hidratos_de_carbono, lipidos, proteina,
              cant_sugerida, unidad, alimento_id, platillo_id
            ) VALUES (
              {ipi_id}, {int(round(float(ing['energia']) * factor))},
              {float(ing['hidratos_de_carbono']) * factor},
              {float(ing['lipidos']) * factor},
              {float(ing['proteina']) * factor},
              {float(ing['cant_sugerida'])}, '{ing['unidad']}',
              {ing['alimento_id']}, {pi_id}
            );
            """
        )
        ipi_id += 1


def diet_kcal(plan: list[tuple[int, int]]) -> int:
    total = 0
    for platillo_id, portions in plan:
        plat = load_platillo(platillo_id)
        total += int(round(float(plat["energia"]) * portions))
    return total


def build_plan(target: int, profile_idx: int) -> tuple[tuple[int, ...], list[int]]:
    """Return platillo ids and portions for Desayuno, Colación, Comida, Cena."""
    molletes = PLATILLO_IDS["molletes"]
    tacos = PLATILLO_IDS["tacos_pollo"]
    entomatadas = PLATILLO_IDS["entomatadas"]
    avena = SUPPORT_PLATILLOS["avena_fruta"]
    arroz = SUPPORT_PLATILLOS["arroz_pollo_frijoles"]
    yoghurt = SUPPORT_PLATILLOS["yoghur_fruta"]
    huevos = SUPPORT_PLATILLOS["huevos_tortilla"]

    if profile_idx % 3 == 0:
        meal_ids = (molletes, yoghurt, tacos, entomatadas)
    elif profile_idx % 3 == 1:
        meal_ids = (huevos, yoghurt, arroz, entomatadas)
    else:
        meal_ids = (molletes, avena, arroz, tacos)

    portions = [2 if target >= 2800 else 1] * 4
    if target >= 3200:
        portions[2] = 3
    if target >= 3400:
        portions[0] = 3

    plan = list(zip(meal_ids, portions))
    kcal = diet_kcal(plan)
    guard = 0
    meal_idx = 0
    while kcal < target * 0.97 and guard < 60:
        portions[meal_idx % 4] += 1
        plan = list(zip(meal_ids, portions))
        kcal = diet_kcal(plan)
        meal_idx += 1
        guard += 1

    guard = 0
    while kcal > min(target * 1.05, KCAL_MAX + 75) and guard < 60:
        idx = max(range(4), key=lambda i: portions[i])
        if portions[idx] > 1:
            portions[idx] -= 1
            plan = list(zip(meal_ids, portions))
            kcal = diet_kcal(plan)
        else:
            break
        guard += 1
    return meal_ids, portions


def insert_dieta_templates() -> None:
    delete_high_kcal_templates()
    ingesta_id = int(psql_json("SELECT COALESCE(MAX(id),0) AS m FROM ingesta")[0]["m"]) + 1
    pi_id = int(psql_json("SELECT COALESCE(MAX(id),0) AS m FROM platillo_ingesta")[0]["m"]) + 1

    profiles = ("equilibrado", "proteico", "vegetal")
    for idx in range(TEMPLATE_COUNT):
        target = int(round(KCAL_MIN + (KCAL_MAX - KCAL_MIN) * idx / (TEMPLATE_COUNT - 1)))
        dieta_id = DIETA_ID_START + idx
        profile = profiles[idx % 3]
        nombre = f"Plantilla: Menú alto calórico mexicano {target} kcal ({profile})"
        meal_ids, portions = build_plan(target, idx)

        psql_exec(
            f"""
            INSERT INTO dieta (id, energia, hidratos_de_carbono, lipidos, proteina, nombre, user_id)
            VALUES ({dieta_id}, 0, 0, 0, 0, '{nombre}', '{SYSTEM_TEMPLATE_USER}');
            """
        )

        ingesta_ids: list[int] = []
        for ing_name in INGESTA_NAMES:
            psql_exec(
                f"""
                INSERT INTO ingesta (id, nombre, dieta_id)
                VALUES ({ingesta_id}, '{ing_name}', {dieta_id});
                """
            )
            ingesta_ids.append(ingesta_id)
            ingesta_id += 1

        for meal_idx, (plat_id, portion) in enumerate(zip(meal_ids, portions)):
            fetch_platillo_ingesta_row(plat_id, portion, ingesta_ids[meal_idx], pi_id)
            pi_id += 1

        psql_exec(
            f"""
            UPDATE dieta SET
              energia = sub.e,
              proteina = sub.p,
              lipidos = sub.l,
              hidratos_de_carbono = sub.h
            FROM (
              SELECT
                COALESCE(SUM(pi.energia),0) AS e,
                COALESCE(SUM(pi.proteina),0) AS p,
                COALESCE(SUM(pi.lipidos),0) AS l,
                COALESCE(SUM(pi.hidratos_de_carbono),0) AS h
              FROM ingesta i
              LEFT JOIN platillo_ingesta pi ON pi.ingesta_id = i.id
              WHERE i.dieta_id = {dieta_id}
            ) sub
            WHERE dieta.id = {dieta_id};
            """
        )


def main() -> int:
    alimentos = load_alimentos()
    max_ing = int(psql_json("SELECT COALESCE(MAX(id),0) AS m FROM ingrediente")[0]["m"]) + 1
    for spec in PLATILLOS:
        max_ing = insert_platillo(spec, alimentos, max_ing)
        totals = psql_json(f"SELECT energia, proteina, lipidos, hidratos_de_carbono FROM platillo WHERE id = {spec.platillo_id}")
        print(f"Platillo {spec.name}: {totals[0]}")

    insert_dieta_templates()
    summary = psql_json(
        f"""
        SELECT COUNT(*) AS cnt, MIN(energia) AS min_e, MAX(energia) AS max_e
        FROM dieta WHERE user_id = '{SYSTEM_TEMPLATE_USER}' AND energia >= {KCAL_MIN}
        """
    )[0]
    print(f"High-kcal templates: {summary}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
