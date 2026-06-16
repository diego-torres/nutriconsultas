package com.nutriconsultas.db;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/**
 * Verifies Liquibase baseline and catalog seed on the H2 test profile (#46).
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
		"spring.liquibase.change-log=classpath:db/changelog/db.changelog-master.yaml" })
public class LiquibaseMigrationTest {

	@Autowired
	private JdbcTemplate jdbc;

	@Test
	public void testBaselineCreatesPacienteTable() {
		final Long count = jdbc.queryForObject("SELECT COUNT(*) FROM paciente", Long.class);
		assertThat(count).isNotNull().isEqualTo(0L);
	}

	@Test
	public void testAlimentosSeedLoaded() {
		final Long count = jdbc.queryForObject("SELECT COUNT(*) FROM alimento", Long.class);
		assertThat(count).isNotNull().isGreaterThan(0L);
	}

	@Test
	public void testPlatilloAuxSeedLoaded() {
		final Long count = jdbc.queryForObject("SELECT COUNT(*) FROM seed_platillo", Long.class);
		assertThat(count).isNotNull().isGreaterThan(0L);
	}

}
