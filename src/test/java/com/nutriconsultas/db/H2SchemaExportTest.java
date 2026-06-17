package com.nutriconsultas.db;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Generates {@code src/main/resources/db/changelog/changes/001-baseline-schema.h2.sql}
 * from Hibernate {@code ddl-auto=create} on H2. Run manually when entities change:
 * {@code mvn -Dtest=H2SchemaExportTest -Dspring.profiles.active=h2-schema-export
 * -Djunit.jupiter.conditions.deactivate=org.junit.jupiter.api.condition.DisabledCondition test}
 */
@SpringBootTest
@ActiveProfiles("h2-schema-export")
@Disabled("Manual maintenance — regenerates H2 Liquibase baseline")
public class H2SchemaExportTest {

	private static final Path OUTPUT = Path.of("src/main/resources/db/changelog/changes/001-baseline-schema.h2.sql");

	@Autowired
	private DataSource dataSource;

	@Test
	public void exportH2Schema() throws SQLException {
		try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
			final String outputPath = OUTPUT.toAbsolutePath().toString().replace('\\', '/');
			statement.execute("SCRIPT TO '" + outputPath + "'");
		}
	}

}
