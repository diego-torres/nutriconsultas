package com.nutriconsultas.platillos;

import static org.assertj.core.api.Assertions.assertThatCode;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

/**
 * Validates that {@code seed_platillos.sql} is syntactically compatible with H2 running
 * in PostgreSQL compatibility mode (typical CI profile).
 */
public class PlatilloSeedSqlH2ParsesTest {

	@Test
	public void executesWithoutSqlErrors() throws Exception {
		final DriverManagerDataSource dataSource = new DriverManagerDataSource();
		dataSource.setDriverClassName("org.h2.Driver");
		dataSource.setUrl("jdbc:h2:mem:platilloSeedParse;MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
		dataSource.setUsername("sa");
		dataSource.setPassword("");

		final EncodedResource encoded = new EncodedResource(new ClassPathResource("seed_platillos.sql"),
				StandardCharsets.UTF_8);
		try (Connection connection = dataSource.getConnection()) {
			assertThatCode(() -> ScriptUtils.executeSqlScript(connection, encoded)).doesNotThrowAnyException();
		}
	}

}
