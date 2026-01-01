package com.nutriconsultas.alimentos;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import lombok.extern.slf4j.Slf4j;

@Component
@Order(1)
@Slf4j
public class AlimentosInitializer implements CommandLineRunner {

	private final AlimentosRepository alimentosRepository;

	private final DataSource dataSource;

	public AlimentosInitializer(AlimentosRepository alimentosRepository, DataSource dataSource) {
		this.alimentosRepository = alimentosRepository;
		this.dataSource = dataSource;
	}

	@Override
	public void run(final String... args) {
		final long count = alimentosRepository.count();
		log.info("Checking alimentos table. Current count: {}", count);

		if (count == 0) {
			log.info("Alimentos table is empty. Initializing from alimentos.sql...");
			try {
				initializeAlimentos();
				final long newCount = alimentosRepository.count();
				log.info("Successfully initialized alimentos table from alimentos.sql. New count: {}", newCount);
			}
			catch (final Exception e) {
				log.error("Failed to initialize alimentos table from alimentos.sql", e);
			}
		}
		else {
			log.info("Alimentos table already contains {} records. Skipping initialization.", count);
		}
	}

	private void initializeAlimentos() throws IOException, SQLException {
		final Connection connection = dataSource.getConnection();
		if (connection == null) {
			throw new SQLException("Failed to obtain database connection");
		}
		try (connection) {
			final boolean isH2 = isH2Database(connection);
			final Resource resource = getAlimentosSqlResource(isH2);
			final EncodedResource encodedResource = new EncodedResource(resource, StandardCharsets.UTF_8);

			log.debug("Executing alimentos.sql script from: {}", resource.getDescription());
			ScriptUtils.executeSqlScript(connection, encodedResource);
		}
	}

	private boolean isH2Database(final Connection connection) throws SQLException {
		final DatabaseMetaData metaData = connection.getMetaData();
		final String databaseProductName = metaData.getDatabaseProductName();
		return "H2".equalsIgnoreCase(databaseProductName);
	}

	@SuppressWarnings("null")
	private @NonNull Resource getAlimentosSqlResource(final boolean isH2) throws IOException {
		final Resource originalResource = getAlimentosSqlResource();
		if (!isH2) {
			return originalResource;
		}
		// For H2, remove START TRANSACTION and COMMIT statements
		log.debug("H2 database detected, removing transaction statements from SQL script");
		try (InputStream inputStream = originalResource.getInputStream()) {
			final java.nio.charset.Charset utf8 = java.nio.charset.StandardCharsets.UTF_8;
			final String sqlContent = StreamUtils.copyToString(inputStream, utf8);
			// Remove START TRANSACTION and COMMIT statements (case-insensitive)
			String cleanedSql = sqlContent.replaceAll("(?i)^\\s*START\\s+TRANSACTION\\s*;?\\s*$", "");
			cleanedSql = cleanedSql.replaceAll("(?i)^\\s*COMMIT\\s*;?\\s*$", "");
			return new InputStreamResource(new java.io.ByteArrayInputStream(cleanedSql.getBytes(utf8)),
					originalResource.getDescription());
		}
	}

	private @NonNull Resource getAlimentosSqlResource() throws IOException {
		// Try to load from classpath first (if moved to resources)
		final ClassPathResource classPathResource = new ClassPathResource("alimentos.sql");
		if (classPathResource.exists()) {
			log.debug("Loading alimentos.sql from classpath");
			return classPathResource;
		}

		// Fallback to root directory
		final File sqlFile = new File("alimentos.sql");
		if (!sqlFile.exists()) {
			throw new IOException("alimentos.sql file not found in classpath or root directory");
		}
		log.debug("Loading alimentos.sql from root directory: {}", sqlFile.getAbsolutePath());
		return new FileSystemResource(sqlFile);
	}

}
