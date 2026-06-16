package com.nutriconsultas.alimentos;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

/**
 * Ensures catalog seed SQL ships inside the application JAR for Liquibase (#46).
 */
public class AlimentosSeedSqlResourceTest {

	@Test
	public void testAlimentosSqlIsOnClasspath() {
		final ClassPathResource legacy = new ClassPathResource("alimentos.sql");
		final ClassPathResource liquibaseSeed = new ClassPathResource("db/changelog/data/alimentos-seed.sql");
		assertThat(legacy.exists()).as("alimentos.sql must remain packaged for reference tooling").isTrue();
		assertThat(liquibaseSeed.exists()).as("Liquibase alimentos seed must be on classpath").isTrue();
	}

}
