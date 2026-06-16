package com.nutriconsultas.platillos;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.nutriconsultas.alimentos.Alimento;
import com.nutriconsultas.alimentos.AlimentosRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Materializes {@link Platillo} rows from Liquibase-populated auxiliary tables
 * ({@code seed_platillo}, {@code seed_platillo_ingrediente} — issue #46).
 */
@Component
@Order(2)
@ConditionalOnProperty(name = "nutriconsultas.seed.platillos.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class PlatilloSeedInitializer implements CommandLineRunner {

	private final DataSource dataSource;

	private final JdbcTemplate jdbc;

	private final PlatilloRepository platilloRepository;

	private final AlimentosRepository alimentosRepository;

	private final PlatilloService platilloService;

	@Override
	@Transactional
	public void run(final String... args) {
		final long alimentoCount = alimentosRepository.count();
		if (alimentoCount == 0) {
			log.warn("No alimentos found in database. Platillo seeding requires alimentos to exist first. "
					+ "Skipping platillo seed.");
			return;
		}
		log.info("Found {} alimentos in database. Proceeding with platillo seed.", alimentoCount);

		final long platilloCount = platilloRepository.count();
		if (platilloCount > 0) {
			log.info("Database already contains {} platillos. Skipping seed.", platilloCount);
			return;
		}

		try {
			if (!checkTableExists("seed_platillo")) {
				log.warn("Auxiliary table 'seed_platillo' does not exist. Skipping seed.");
				return;
			}
		}
		catch (SQLException e) {
			log.error("Failed to verify auxiliary table existence", e);
			return;
		}

		log.info("No platillos found in database. Starting seed from auxiliary tables...");

		final List<Map<String, Object>> platillos = jdbc
			.queryForList("SELECT name, description, ingestas_sugeridas FROM seed_platillo ORDER BY name");

		log.info("Found {} platillos in auxiliary table", platillos.size());

		final Integer totalIngredientes = jdbc.queryForObject("SELECT COUNT(*) FROM seed_platillo_ingrediente",
				Integer.class);
		log.info("Found {} total ingredients in auxiliary table", totalIngredientes != null ? totalIngredientes : 0);

		int successCount = 0;
		int ingredientCount = 0;
		for (final Map<String, Object> row : platillos) {
			final String name = (String) row.get("name");
			final String description = (String) row.get("description");
			final String ingestasSugeridas = (String) row.get("ingestas_sugeridas");

			Platillo platillo = new Platillo();
			platillo.setName(name);
			platillo.setDescription(description);
			platillo.setIngestasSugeridas(ingestasSugeridas);
			platillo = platilloRepository.save(platillo);

			final Long platilloId = platillo.getId();
			if (platilloId == null) {
				log.warn("Failed to save platillo '{}'. Skipping.", name);
				continue;
			}

			final List<Map<String, Object>> ingredientes = jdbc
				.queryForList("SELECT alimento_nombre, alimento_id, peso_neto FROM seed_platillo_ingrediente "
						+ "WHERE platillo_name = ? ORDER BY orden, id", name);

			if (ingredientes.isEmpty()) {
				final Integer countForName = jdbc.queryForObject(
						"SELECT COUNT(*) FROM seed_platillo_ingrediente WHERE platillo_name = ?", Integer.class, name);
				log.warn("No ingredients found for platillo '{}' (id: {}). Count query returned: {}", name, platilloId,
						countForName);
				if (name.length() > 50) {
					final String namePrefix = name.substring(0, 50);
					final Integer countSimilar = jdbc.queryForObject(
							"SELECT COUNT(*) FROM seed_platillo_ingrediente WHERE platillo_name LIKE ?", Integer.class,
							namePrefix + "%");
					log.debug("Found {} ingredients for names starting with '{}'", countSimilar, namePrefix);
				}
			}

			log.debug("Adding {} ingredients to platillo '{}' (id: {})", ingredientes.size(), name, platilloId);

			final int addedCount = addIngredientsToPlatillo(platilloId, name, ingredientes);
			ingredientCount += addedCount;

			Integer dbIngredientCount = jdbc.queryForObject("SELECT COUNT(*) FROM ingrediente WHERE platillo_id = ?",
					Integer.class, platilloId);
			if (dbIngredientCount == null) {
				dbIngredientCount = 0;
			}

			if (dbIngredientCount != addedCount) {
				log.warn("Platillo '{}' (id: {}) has {} ingredients in DB but {} were added. Expected: {}", name,
						platilloId, dbIngredientCount, addedCount, ingredientes.size());
			}
			else if (addedCount > 0) {
				log.debug("Platillo '{}' (id: {}) successfully created with {} ingredients", name, platilloId,
						dbIngredientCount);
				successCount++;
			}
			else {
				log.warn("Platillo '{}' (id: {}) created but no ingredients were added", name, platilloId);
			}
		}

		log.info("Finished seeding: {} platillos created successfully with {} total ingredients", successCount,
				ingredientCount);

		log.info("Finished seeding {} platillos", platillos.size());
	}

	private Alimento resolveAlimentoForIngredientRow(final String platilloName, final Map<String, Object> ing) {
		final Object nombreRaw = ing.get("alimento_nombre");
		if (nombreRaw instanceof String nombreAlimentoStr) {
			final String trimmed = nombreAlimentoStr.trim();
			if (!trimmed.isEmpty()) {
				return alimentosRepository.findFirstByNombreAlimentoIgnoreCaseOrderByIdAsc(trimmed).orElse(null);
			}
		}
		final Object alimentoIdObj = ing.get("alimento_id");
		if (!(alimentoIdObj instanceof Number)) {
			log.warn("Seed row missing alimento_nombre/alimento_id for platillo '{}'. Cannot resolve alimento.",
					platilloName);
			return null;
		}
		final Long alimentoId = ((Number) alimentoIdObj).longValue();
		return alimentosRepository.findById(alimentoId).orElse(null);
	}

	private int addIngredientsToPlatillo(final Long platilloId, final String platilloName,
			final List<Map<String, Object>> ingredientes) {
		int addedCount = 0;
		for (final Map<String, Object> ing : ingredientes) {
			final Alimento alimento = resolveAlimentoForIngredientRow(platilloName, ing);
			if (alimento == null) {
				if (ing.get("alimento_nombre") instanceof String s) {
					final String nombreTrim = s.trim();
					if (!nombreTrim.isEmpty()) {
						log.warn("No alimento matched SMAE nombre '{}' for platillo '{}'. Skipping ingredient.",
								nombreTrim, platilloName);
					}
					else if (ing.get("alimento_id") instanceof Number n) {
						log.warn("Alimento with id {} not found. Skipping ingredient for platillo '{}'.", n.longValue(),
								platilloName);
					}
				}
				else if (ing.get("alimento_id") instanceof Number n) {
					log.warn("Alimento with id {} not found. Skipping ingredient for platillo '{}'.", n.longValue(),
							platilloName);
				}
				continue;
			}
			final Long alimentoId = alimento.getId();
			if (alimentoId == null) {
				log.warn("Resolved alimento has null id for platillo '{}'. Skipping ingredient.", platilloName);
				continue;
			}
			final Object pesoObj = ing.get("peso_neto");
			if (!(pesoObj instanceof Number)) {
				log.warn("peso_neto is missing or invalid. Skipping ingredient for platillo '{}'.", platilloName);
				continue;
			}
			final Integer peso = ((Number) pesoObj).intValue();

			final String cantidad = alimento.getFractionalCantSugerida();
			if (cantidad == null || cantidad.isEmpty()) {
				log.warn("Alimento id {} has no fractionalCantSugerida. Skipping ingredient for platillo '{}'.",
						alimentoId, platilloName);
				continue;
			}

			if (platilloId == null) {
				log.warn("platilloId is null. Skipping ingredient for platillo '{}'.", platilloName);
				continue;
			}

			try {
				final Ingrediente addedIngrediente = platilloService.addIngrediente(platilloId, alimentoId, cantidad,
						peso);
				if (addedIngrediente == null) {
					log.error("Failed to add ingrediente (alimentoId: {}, peso: {}) to platillo '{}' (id: {})",
							alimentoId, peso, platilloName, platilloId);
				}
				else {
					addedCount++;
				}
			}
			catch (final Exception e) {
				log.error("Exception while adding ingrediente (alimentoId: {}, peso: {}) to platillo '{}' (id: {}): {}",
						alimentoId, peso, platilloName, platilloId, e.getMessage(), e);
			}
		}
		return addedCount;
	}

	private boolean checkTableExists(final String tableName) throws SQLException {
		try (Connection connection = dataSource.getConnection()) {
			final DatabaseMetaData metaData = connection.getMetaData();
			final String catalog = connection.getCatalog();
			try (ResultSet tables = metaData.getTables(catalog, null, "%", new String[] { "TABLE" })) {
				while (tables.next()) {
					final String foundName = tables.getString("TABLE_NAME");
					if (foundName != null && foundName.equalsIgnoreCase(tableName)) {
						return true;
					}
				}
				return false;
			}
		}
	}

}
