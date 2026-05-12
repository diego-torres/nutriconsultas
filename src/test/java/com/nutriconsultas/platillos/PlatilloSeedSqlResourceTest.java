package com.nutriconsultas.platillos;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

/**
 * Guards issue #84: platillo auxiliary seed SQL ships on the classpath (packaged JAR),
 * analogous to {@code alimentos.sql}, so EC2 deployments are not cwd-dependent.
 */
public class PlatilloSeedSqlResourceTest {

	@Test
	public void seedPlatillosSqlMustBePackagedUnderResources() {
		final ClassPathResource resource = new ClassPathResource("seed_platillos.sql");
		assertThat(resource.exists()).as("seed_platillos.sql must be under src/main/resources").isTrue();
	}

}
