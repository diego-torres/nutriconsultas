package com.nutriconsultas.alimentos;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

/**
 * Ensures the alimentos seed SQL ships inside the application JAR (classpath) so
 * empty-database initialization works on AWS and other deployments where the process
 * working directory does not contain alimentos.sql.
 */
public class AlimentosSeedSqlResourceTest {

	@Test
	public void testAlimentosSqlIsOnClasspath() {
		final ClassPathResource resource = new ClassPathResource("alimentos.sql");
		assertThat(resource.exists()).as("alimentos.sql must be packaged under src/main/resources").isTrue();
	}

}
