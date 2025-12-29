package com.nutriconsultas.alimentos;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

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
	public void run(String... args) {
		long count = alimentosRepository.count();
		log.info("Checking alimentos table. Current count: {}", count);

		if (count == 0) {
			log.info("Alimentos table is empty. Initializing from alimentos.sql...");
			try {
				initializeAlimentos();
				long newCount = alimentosRepository.count();
				log.info("Successfully initialized alimentos table from alimentos.sql. New count: {}", newCount);
			}
			catch (Exception e) {
				log.error("Failed to initialize alimentos table from alimentos.sql", e);
			}
		}
		else {
			log.info("Alimentos table already contains {} records. Skipping initialization.", count);
		}
	}

	private void initializeAlimentos() throws IOException, SQLException {
		Resource resource = getAlimentosSqlResource();
		EncodedResource encodedResource = new EncodedResource(resource, StandardCharsets.UTF_8);

		Connection connection = dataSource.getConnection();
		if (connection == null) {
			throw new SQLException("Failed to obtain database connection");
		}
		try (connection) {
			log.debug("Executing alimentos.sql script from: {}", resource.getDescription());
			ScriptUtils.executeSqlScript(connection, encodedResource);
		}
	}

	private @NonNull Resource getAlimentosSqlResource() throws IOException {
		// Try to load from classpath first (if moved to resources)
		ClassPathResource classPathResource = new ClassPathResource("alimentos.sql");
		if (classPathResource.exists()) {
			log.debug("Loading alimentos.sql from classpath");
			return classPathResource;
		}

		// Fallback to root directory
		File sqlFile = new File("alimentos.sql");
		if (!sqlFile.exists()) {
			throw new IOException("alimentos.sql file not found in classpath or root directory");
		}
		log.debug("Loading alimentos.sql from root directory: {}", sqlFile.getAbsolutePath());
		return new FileSystemResource(sqlFile);
	}

}
