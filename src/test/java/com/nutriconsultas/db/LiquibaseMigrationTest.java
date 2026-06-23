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
@TestPropertySource(properties = { "spring.liquibase.change-log=classpath:db/changelog/db.changelog-master.yaml" })
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
	public void testPlatillosSeedLoaded() {
		final Long count = jdbc.queryForObject("SELECT COUNT(*) FROM platillo", Long.class);
		assertThat(count).isNotNull().isEqualTo(103L);
	}

	@Test
	public void testDietaTemplatesSeedLoaded() {
		final Long count = jdbc.queryForObject("SELECT COUNT(*) FROM dieta WHERE user_id = 'system:template-dietas'",
				Long.class);
		assertThat(count).isNotNull().isEqualTo(50L);
	}

	@Test
	public void testHighKcalDietaTemplatesSeedLoaded() {
		final Long count = jdbc.queryForObject(
				"SELECT COUNT(*) FROM dieta WHERE user_id = 'system:template-dietas' AND energia >= 2500", Long.class);
		assertThat(count).isNotNull().isEqualTo(30L);
		final Long minKcal = jdbc.queryForObject(
				"SELECT MIN(energia) FROM dieta WHERE user_id = 'system:template-dietas' AND energia >= 2500",
				Long.class);
		final Long maxKcal = jdbc.queryForObject(
				"SELECT MAX(energia) FROM dieta WHERE user_id = 'system:template-dietas' AND energia >= 2500",
				Long.class);
		assertThat(minKcal).isGreaterThanOrEqualTo(2500L);
		assertThat(maxKcal).isLessThanOrEqualTo(3500L);
	}

	@Test
	public void testMexicanPlatillosSeedLoaded() {
		final Long count = jdbc.queryForObject(
				"SELECT COUNT(*) FROM platillo WHERE name IN ('Molletes', 'Tacos de pollo', 'Entomatadas')",
				Long.class);
		assertThat(count).isNotNull().isEqualTo(3L);
	}

	@Test
	public void testSubscriptionSchemaTablesExist() {
		assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM subscription", Long.class)).isEqualTo(0L);
		assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM clinic", Long.class)).isEqualTo(0L);
		assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM clinic_member", Long.class)).isEqualTo(0L);
		assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM nutritionist_invitation", Long.class)).isEqualTo(0L);
		assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM clinic_invitation", Long.class)).isEqualTo(0L);
		assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM subscription_audit_event", Long.class)).isEqualTo(0L);
	}

	@Test
	public void testPatientInvitationOnboardingSchema() {
		assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM patient_invitation", Long.class)).isEqualTo(0L);
		assertThat(jdbc.queryForObject(
				"SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'PACIENTE' AND COLUMN_NAME = 'STATUS'",
				Long.class))
			.isEqualTo(1L);
	}

	@Test
	public void testPlatilloIngestaSourcePlatilloIdBackfilled() {
		assertThat(
				jdbc.queryForObject(
						"SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS "
								+ "WHERE TABLE_NAME = 'PLATILLO_INGESTA' AND COLUMN_NAME = 'SOURCE_PLATILLO_ID'",
						Long.class))
			.isEqualTo(1L);
		assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM platillo_ingesta WHERE source_platillo_id IS NOT NULL",
				Long.class))
			.isGreaterThan(0L);
		assertThat(jdbc.queryForObject("SELECT source_platillo_id FROM platillo_ingesta WHERE id = 32", Long.class))
			.isEqualTo(97L);
	}

}
