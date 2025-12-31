package com.nutriconsultas.platillos;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.nutriconsultas.alimentos.Alimento;
import com.nutriconsultas.alimentos.AlimentosRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Order(2)
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
		// Verify alimentos exist before proceeding
		final long alimentoCount = alimentosRepository.count();
		if (alimentoCount == 0) {
			log.warn("No alimentos found in database. Platillo seeding requires alimentos to exist first. "
					+ "Skipping platillo seed.");
			return;
		}
		log.info("Found {} alimentos in database. Proceeding with platillo seed.", alimentoCount);

		// Check if there are any platillos in the database
		final long platilloCount = platilloRepository.count();
		if (platilloCount > 0) {
			log.info("Database already contains {} platillos. Skipping seed.", platilloCount);
			return;
		}

		// Initialize auxiliary tables if they don't exist
		initializeAuxiliaryTables();

		// Verify auxiliary table exists
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

		// Read platillos from auxiliary table
		final List<Map<String, Object>> platillos = jdbc
			.queryForList("SELECT name, description, ingestas_sugeridas FROM public.seed_platillo ORDER BY name");

		log.info("Found {} platillos in auxiliary table", platillos.size());

		// Verify ingredients exist in auxiliary table
		final Integer totalIngredientes = jdbc.queryForObject("SELECT COUNT(*) FROM public.seed_platillo_ingrediente",
				Integer.class);
		log.info("Found {} total ingredients in auxiliary table", totalIngredientes != null ? totalIngredientes : 0);

		// Create platillos with ingredients
		int successCount = 0;
		int ingredientCount = 0;
		for (final Map<String, Object> row : platillos) {
			final String name = (String) row.get("name");
			final String description = (String) row.get("description");
			final String ingestasSugeridas = (String) row.get("ingestas_sugeridas");

			// Create platillo
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

			// Read ingredients from auxiliary table
			final List<Map<String, Object>> ingredientes = jdbc
				.queryForList("SELECT alimento_id, peso_neto FROM public.seed_platillo_ingrediente "
						+ "WHERE platillo_name = ? ORDER BY orden, id", name);

			// Diagnostic: if no ingredients found, check if any ingredients exist for
			// similar names
			if (ingredientes.isEmpty()) {
				final Integer countForName = jdbc.queryForObject(
						"SELECT COUNT(*) FROM public.seed_platillo_ingrediente WHERE platillo_name = ?", Integer.class,
						name);
				log.warn("No ingredients found for platillo '{}' (id: {}). Count query returned: {}", name, platilloId,
						countForName);
				// Check for similar names (first 50 chars)
				if (name.length() > 50) {
					final String namePrefix = name.substring(0, 50);
					final Integer countSimilar = jdbc.queryForObject(
							"SELECT COUNT(*) FROM public.seed_platillo_ingrediente " + "WHERE platillo_name LIKE ?",
							Integer.class, namePrefix + "%");
					log.debug("Found {} ingredients for names starting with '{}'", countSimilar, namePrefix);
				}
			}

			log.debug("Adding {} ingredients to platillo '{}' (id: {})", ingredientes.size(), name, platilloId);

			// Add ingredients using service (calculates nutrients automatically)
			final int addedCount = addIngredientsToPlatillo(platilloId, name, ingredientes);
			ingredientCount += addedCount;

			// Verify ingredients were added by checking the database directly
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

	private int addIngredientsToPlatillo(final Long platilloId, final String platilloName,
			final List<Map<String, Object>> ingredientes) {
		int addedCount = 0;
		for (final Map<String, Object> ing : ingredientes) {
			final Object alimentoIdObj = ing.get("alimento_id");
			if (alimentoIdObj == null) {
				log.warn("alimento_id is null. Skipping ingredient for platillo '{}'.", platilloName);
				continue;
			}
			final Long alimentoId = ((Number) alimentoIdObj).longValue();
			final Object pesoObj = ing.get("peso_neto");
			if (pesoObj == null) {
				log.warn("peso_neto is null. Skipping ingredient for platillo '{}'.", platilloName);
				continue;
			}
			final Integer peso = ((Number) pesoObj).intValue();

			final Alimento alimento = alimentosRepository.findById(alimentoId).orElse(null);
			if (alimento == null) {
				log.warn("Alimento with id {} not found. Skipping ingredient for platillo '{}'.", alimentoId,
						platilloName);
				continue;
			}
			final String cantidad = alimento.getFractionalCantSugerida();
			if (cantidad == null) {
				log.warn("Alimento {} has no fractionalCantSugerida. Skipping ingredient for platillo '{}'.",
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
				log.error("Exception while adding ingrediente (alimentoId: {}, peso: {}) to platillo '{}' "
						+ "(id: {}): {}", alimentoId, peso, platilloName, platilloId, e.getMessage(), e);
			}
		}
		return addedCount;
	}

	private void initializeAuxiliaryTables() {
		try {
			final boolean tableExists = checkTableExists("seed_platillo_ingrediente");
			log.debug("Checking seed_platillo_ingrediente table. Exists: {}", tableExists);

			if (!tableExists) {
				log.info("Auxiliary tables do not exist. Initializing from seed_platillos.sql...");
				try {
					executeSeedScript();
					final boolean tablesCreated = checkTableExists("seed_platillo_ingrediente");
					if (tablesCreated) {
						log.info("Successfully initialized auxiliary tables from seed_platillos.sql");
						// Verify data was inserted
						try {
							final Integer platilloCount = jdbc
								.queryForObject("SELECT COUNT(*) FROM public.seed_platillo", Integer.class);
							final String countQuery = "SELECT COUNT(*) FROM public.seed_platillo_ingrediente";
							final Integer ingredienteCount = jdbc.queryForObject(countQuery, Integer.class);
							log.info("Auxiliary tables populated: {} platillos, {} ingredients",
									platilloCount != null ? platilloCount : 0,
									ingredienteCount != null ? ingredienteCount : 0);
							if (platilloCount == null || platilloCount == 0) {
								log.warn("Auxiliary table seed_platillo is empty after initialization!");
							}
							if (ingredienteCount == null || ingredienteCount == 0) {
								log.warn("Auxiliary table seed_platillo_ingrediente is empty after initialization!");
							}
						}
						catch (final Exception e) {
							log.warn("Could not verify auxiliary table data: {}", e.getMessage());
						}
					}
					else {
						log.warn("Auxiliary tables were not created. Script may have failed.");
					}
				}
				catch (final Exception e) {
					log.error("Error during auxiliary table initialization: {}", e.getMessage());
					log.debug("Full error details:", e);
				}
			}
			else {
				log.debug("Auxiliary tables already exist. Checking if they need to be populated...");
				// Check if tables are empty and need to be populated
				try {
					Integer platilloCount = jdbc.queryForObject("SELECT COUNT(*) FROM public.seed_platillo",
							Integer.class);
					Integer ingredienteCount = jdbc
						.queryForObject("SELECT COUNT(*) FROM public.seed_platillo_ingrediente", Integer.class);
					log.info("Auxiliary tables contain: {} platillos, {} ingredients",
							platilloCount != null ? platilloCount : 0, ingredienteCount != null ? ingredienteCount : 0);

					// If tables exist but are empty, run the script to populate them
					if ((platilloCount == null || platilloCount == 0)
							|| (ingredienteCount == null || ingredienteCount == 0)) {
						log.info("Auxiliary tables exist but are empty. Populating from seed_platillos.sql...");
						try {
							// Clear existing data to ensure clean state
							jdbc.update("DELETE FROM public.seed_platillo_ingrediente");
							jdbc.update("DELETE FROM public.seed_platillo");
							log.debug("Cleared existing auxiliary table data");
							executeSeedScript();
							// Re-check counts after script execution
							platilloCount = jdbc.queryForObject("SELECT COUNT(*) FROM public.seed_platillo",
									Integer.class);
							final String countQuery = "SELECT COUNT(*) FROM public.seed_platillo_ingrediente";
							ingredienteCount = jdbc.queryForObject(countQuery, Integer.class);
							log.info("After population: {} platillos, {} ingredients",
									platilloCount != null ? platilloCount : 0,
									ingredienteCount != null ? ingredienteCount : 0);
							if (ingredienteCount == null || ingredienteCount == 0) {
								log.error("CRITICAL: Ingredients table is still empty after script execution!");
							}
						}
						catch (final Exception e) {
							log.error("Error populating auxiliary tables: {}", e.getMessage());
							log.debug("Full error details:", e);
						}
					}
				}
				catch (final Exception e) {
					log.debug("Could not check auxiliary table counts: {}", e.getMessage());
				}
			}
		}
		catch (final SQLException e) {
			log.error("Failed to check if auxiliary tables exist", e);
		}
	}

	private boolean checkTableExists(final String tableName) throws SQLException {
		try (Connection connection = dataSource.getConnection()) {
			final DatabaseMetaData metaData = connection.getMetaData();
			try (ResultSet tables = metaData.getTables(null, "public", tableName, null)) {
				return tables.next();
			}
		}
	}

	private void executeSeedScript() throws IOException, SQLException {
		final Resource resource = getSeedPlatillosSqlResource();
		final EncodedResource encodedResource = new EncodedResource(resource, StandardCharsets.UTF_8);

		try (Connection connection = dataSource.getConnection()) {
			if (connection == null) {
				throw new SQLException("Failed to obtain database connection");
			}
			log.debug("Executing seed_platillos.sql script from: {}", resource.getDescription());
			try {
				ScriptUtils.executeSqlScript(connection, encodedResource);
			}
			catch (final Exception e) {
				log.warn("Error executing seed_platillos.sql (some statements may have succeeded): {}", e.getMessage());
				if (e.getMessage() != null && e.getMessage().contains("transaction is aborted")) {
					try {
						if (!connection.getAutoCommit()) {
							connection.rollback();
						}
					}
					catch (final SQLException rollbackException) {
						log.debug("Error during rollback (may be expected): {}", rollbackException.getMessage());
					}
				}
				throw e;
			}
		}
	}

	private @NonNull Resource getSeedPlatillosSqlResource() throws IOException {
		// Try to load from classpath first
		final ClassPathResource classPathResource = new ClassPathResource("seed_platillos.sql");
		if (classPathResource.exists()) {
			log.debug("Loading seed_platillos.sql from classpath");
			return classPathResource;
		}

		// Fallback to root directory
		final File sqlFile = new File("seed_platillos.sql");
		if (!sqlFile.exists()) {
			throw new IOException("seed_platillos.sql file not found in classpath or root directory");
		}
		log.debug("Loading seed_platillos.sql from root directory: {}", sqlFile.getAbsolutePath());
		return new FileSystemResource(sqlFile);
	}

}
