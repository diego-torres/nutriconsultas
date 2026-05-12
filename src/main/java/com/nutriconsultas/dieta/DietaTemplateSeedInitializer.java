package com.nutriconsultas.dieta;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.nutriconsultas.platillos.Ingrediente;
import com.nutriconsultas.platillos.Platillo;
import com.nutriconsultas.platillos.PlatilloRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Seeds reusable template {@link Dieta} rows composed from catalog {@link Platillo}
 * records (issue #85).
 *
 * <p>
 * Runs after {@link com.nutriconsultas.alimentos.AlimentosInitializer} (order 1) and
 * {@link com.nutriconsultas.platillos.PlatilloSeedInitializer} (order 2). Template diets
 * use a dedicated {@value #TEMPLATE_DIETA_USER_ID} owner id so they are shared;
 * nutritionists can duplicate them into their account via the existing duplicate API.
 */
@Component
@Order(3)
@RequiredArgsConstructor
@Slf4j
public class DietaTemplateSeedInitializer implements CommandLineRunner {

	/**
	 * Synthetic OAuth-style subject reserved for catalog templates (not a real Auth0
	 * user).
	 */
	public static final String TEMPLATE_DIETA_USER_ID = "system:template-dietas";

	private static final int MIN_DIETAS = 20;

	private static final List<TemplateDay> TEMPLATE_DAYS = List.of(
			new TemplateDay("Plantilla: Menú equilibrado 01", "Huevos revueltos con tortilla", "Yoghur con fruta",
					"Arroz con pollo y frijoles", "Ensalada con aguacate"),
			new TemplateDay("Plantilla: Menú equilibrado 02", "Avena con leche y fruta", "Yoghur con fruta",
					"Espagueti con brócoli", "Salmón con arroz integral"),
			new TemplateDay("Plantilla: Menú equilibrado 03", "Arroz con huevo, jitomate y aceite", "Yoghur con fruta",
					"Frijoles con tortilla", "Arroz con huevo, lechuga y aceite de oliva"),
			new TemplateDay("Plantilla: Menú equilibrado 04", "Arroz con huevo, jitomate y aceite de oliva",
					"Yoghur con fruta", "Arroz con huevo, pepino y aguacate", "Salmón con arroz integral"),
			new TemplateDay("Plantilla: Menú proteico 01", "Huevos revueltos con tortilla", "Yoghur con fruta",
					"Arroz con pollo cocido sin piel, jitomate y aceite", "Ensalada con aguacate"),
			new TemplateDay("Plantilla: Menú proteico 02", "Avena con leche y fruta", "Yoghur con fruta",
					"Arroz con pollo cocido sin piel, lechuga y aguacate", "Espagueti con brócoli"),
			new TemplateDay("Plantilla: Menú vegetal 01", "Avena con leche y fruta", "Yoghur con fruta",
					"Arroz con huevo, brócoli y aceite de oliva", "Ensalada con aguacate"),
			new TemplateDay("Plantilla: Menú vegetal 02", "Arroz con huevo, lechuga y aguacate", "Yoghur con fruta",
					"Espagueti con brócoli", "Frijoles con tortilla"),
			new TemplateDay("Plantilla: Pollo cocido variado 01",
					"Arroz con pollo cocido sin piel, jitomate y aguacate",
					"Arroz con pollo cocido sin piel, espárragos y aceite",
					"Arroz con pollo cocido sin piel, champiñones y aguacate", "Ensalada con aguacate"),
			new TemplateDay("Plantilla: Pollo cocido variado 02", "Arroz con pollo cocido sin piel, lechuga y aceite",
					"Arroz con pollo cocido sin piel, brócoli y aceite de oliva",
					"Arroz con pollo cocido sin piel, pepino y aguacate", "Yoghur con fruta"),
			new TemplateDay("Plantilla: Pollo crudo catálogo 01", "Arroz con pollo crudo, jitomate y aceite",
					"Arroz con pollo crudo, jitomate y aceite de oliva", "Arroz con pollo crudo, jitomate y aguacate",
					"Avena con leche y fruta"),
			new TemplateDay("Plantilla: Pollo crudo catálogo 02", "Arroz con pollo crudo, lechuga y aceite",
					"Arroz con pollo crudo, lechuga y aceite de oliva", "Arroz con pollo crudo, lechuga y aguacate",
					"Ensalada con aguacate"),
			new TemplateDay("Plantilla: Pollo crudo verduras 01", "Arroz con pollo crudo, brócoli y aceite",
					"Arroz con pollo crudo, brócoli y aceite de oliva", "Arroz con pollo crudo, brócoli y aguacate",
					"Yoghur con fruta"),
			new TemplateDay("Plantilla: Pollo crudo verduras 02", "Arroz con pollo crudo, pepino y aceite",
					"Arroz con pollo crudo, pepino y aceite de oliva", "Arroz con pollo crudo, pepino y aguacate",
					"Huevos revueltos con tortilla"),
			new TemplateDay("Plantilla: Calabacita y ejotes 01", "Arroz con pollo crudo, calabacita y aceite",
					"Arroz con pollo crudo, calabacita y aceite de oliva",
					"Arroz con pollo crudo, calabacita y aguacate", "Espagueti con brócoli"),
			new TemplateDay("Plantilla: Calabacita y ejotes 02", "Arroz con pollo crudo, ejotes y aceite",
					"Arroz con pollo crudo, ejotes y aceite de oliva", "Arroz con pollo crudo, ejotes y aguacate",
					"Ensalada con aguacate"),
			new TemplateDay("Plantilla: Espárragos y champiñones 01", "Arroz con pollo crudo, espárragos y aceite",
					"Arroz con pollo crudo, espárragos y aceite de oliva",
					"Arroz con pollo cocido sin piel, champiñones y aceite", "Salmón con arroz integral"),
			new TemplateDay("Plantilla: Huevo cocido combinados 01", "Arroz con huevo cocido, jitomate y aceite",
					"Arroz con huevo cocido, jitomate y aceite de oliva", "Arroz con huevo cocido, jitomate y aguacate",
					"Yoghur con fruta"),
			new TemplateDay("Plantilla: Huevo cocido combinados 02", "Arroz con huevo cocido, lechuga y aceite",
					"Arroz con huevo cocido, lechuga y aceite de oliva", "Arroz con huevo cocido, lechuga y aguacate",
					"Ensalada con aguacate"),
			new TemplateDay("Plantilla: Huevo cocido verde 01", "Arroz con huevo cocido, brócoli y aceite",
					"Arroz con huevo cocido, brócoli y aceite de oliva", "Arroz con huevo cocido, brócoli y aguacate",
					"Frijoles con tortilla"));

	private final DietaRepository dietaRepository;

	private final PlatilloRepository platilloRepository;

	static {
		if (TEMPLATE_DAYS.size() < MIN_DIETAS) {
			throw new IllegalStateException(
					"Template diet seed definitions must define at least " + MIN_DIETAS + " dietas");
		}
	}

	@Override
	@Transactional
	public void run(@NonNull final String... args) {
		if (platilloRepository.count() == 0L) {
			log.warn("No platillos in database; skipping template dieta seed (requires platillo catalog).");
			return;
		}
		if (!dietaRepository.findByUserId(TEMPLATE_DIETA_USER_ID).isEmpty()) {
			log.info("Template dietas already present for owner {}; skipping seed.", TEMPLATE_DIETA_USER_ID);
			return;
		}

		int created = 0;
		for (final TemplateDay day : TEMPLATE_DAYS) {
			final Dieta dieta = buildTemplateDieta(day);
			dietaRepository.save(dieta);
			created++;
		}
		log.info("Seeded {} template dietas under owner {}", created, TEMPLATE_DIETA_USER_ID);
	}

	private Dieta buildTemplateDieta(final TemplateDay day) {
		final Dieta dieta = new Dieta();
		dieta.setNombre(day.nombre());
		dieta.setUserId(TEMPLATE_DIETA_USER_ID);

		final List<Ingesta> ingestas = new ArrayList<>();
		ingestas.add(mealIngesta(dieta, "Desayuno", day.desayuno()));
		ingestas.add(mealIngesta(dieta, "Colación", day.colacion()));
		ingestas.add(mealIngesta(dieta, "Comida", day.comida()));
		ingestas.add(mealIngesta(dieta, "Cena", day.cena()));
		dieta.setIngestas(ingestas);
		return dieta;
	}

	private Ingesta mealIngesta(final Dieta dieta, final String nombre, final String platilloName) {
		final Ingesta ingesta = new Ingesta(nombre);
		ingesta.setDieta(dieta);
		final Platillo platillo = platilloRepository.findFirstByNameIgnoreCaseOrderByIdAsc(platilloName).orElse(null);
		if (platillo == null) {
			log.error("Template dieta seed: platillo not found by name (check seed_platillos.sql): {}", platilloName);
			throw new IllegalStateException("Template dieta seed: missing platillo '" + platilloName + "'");
		}
		if (platillo.getIngredientes() == null || platillo.getIngredientes().isEmpty()) {
			log.error("Template dieta seed: platillo has no ingredientes ({}).", platilloName);
			throw new IllegalStateException("Template dieta seed: platillo has no ingredientes: " + platilloName);
		}
		ingesta.getPlatillos().add(buildPlatilloIngesta(platillo, ingesta, 1));
		return ingesta;
	}

	private static PlatilloIngesta buildPlatilloIngesta(final Platillo platillo, final Ingesta ingesta,
			final int portions) {
		final PlatilloIngesta platilloIngesta = PlatilloIngestaMapping.mapPlatilloIngesta(platillo);
		platilloIngesta.setIngesta(ingesta);
		platilloIngesta.setPortions(portions);
		for (final Ingrediente ingrediente : platillo.getIngredientes()) {
			final IngredientePlatilloIngesta mapped = PlatilloIngestaMapping
				.mapFromIngredienteToIngredientePlatilloIngesta(ingrediente);
			mapped.setPlatillo(platilloIngesta);
			platilloIngesta.getIngredientes().add(mapped);
		}
		return platilloIngesta;
	}

	private record TemplateDay(String nombre, String desayuno, String colacion, String comida, String cena) {

		private TemplateDay {
			Objects.requireNonNull(nombre);
			Objects.requireNonNull(desayuno);
			Objects.requireNonNull(colacion);
			Objects.requireNonNull(comida);
			Objects.requireNonNull(cena);
		}

	}

}
