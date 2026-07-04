package com.nutriconsultas.ai;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class AiRequestScopeGuardTest {

	private AiRequestScopeGuard guard;

	@BeforeEach
	void setUp() {
		final AiProperties properties = new AiProperties();
		guard = new AiRequestScopeGuard(properties);
	}

	@Test
	void allowsNormalSevenDayMenuRequest() {
		assertThat(guard.evaluate("Genera un menú de 7 días bajo en sodio")).isEmpty();
	}

	@Test
	void allowsSingleDishRequest() {
		assertThat(guard.evaluate("Crea un borrador de platillo con avena")).isEmpty();
	}

	@Test
	void allowsFourteenDayPlanRequest() {
		assertThat(guard.evaluate("Genera un plan alimenticio de 14 días")).isEmpty();
	}

	@Test
	void refusesFifteenDayPlan() {
		final AiRequestScopeViolation violation = guard.evaluate("Genera un plan de 15 días").orElseThrow();

		assertThat(violation.kind()).isEqualTo(AiRequestScopeKind.DIET_PLAN_DAYS);
		assertThat(violation.requestedAmount()).isEqualTo(15);
		assertThat(violation.refusalMessage()).contains("15").contains("borrador de ejemplo");
	}

	@Test
	void refusesEightDayMenu() {
		final AiRequestScopeViolation violation = guard.evaluate("Genera un menú de 8 días").orElseThrow();

		assertThat(violation.kind()).isEqualTo(AiRequestScopeKind.MENU_DAYS);
		assertThat(violation.requestedAmount()).isEqualTo(8);
	}

	@Test
	void refusesMultipleDishes() {
		final AiRequestScopeViolation violation = guard.evaluate("Genera 5 platillos con pollo").orElseThrow();

		assertThat(violation.kind()).isEqualTo(AiRequestScopeKind.DISH_COUNT);
		assertThat(violation.requestedAmount()).isEqualTo(5);
	}

	@Test
	void refusalMessageForUnitsMatchesRefusalFor() {
		final AiProperties properties = new AiProperties();
		final AiRequestScopeGuard scopeGuard = new AiRequestScopeGuard(properties);

		assertThat(scopeGuard.refusalMessageForUnits(new AiRequestScopeRequestedUnits(null, 5, null, null)))
			.get()
			.isEqualTo(AiRequestScopeGuard.refusalFor(AiRequestScopeKind.DISH_COUNT, 5, "platillos"));
	}

	@ParameterizedTest
	@ValueSource(strings = { "Genera 100 platillos para la semana", "Crea cien recetas de desayuno",
			"Genera 1000 planes nutricionales", "Genera un plan para todos mis pacientes" })
	void refusesBulkPhrasing(final String message) {
		assertThat(guard.evaluate(message)).isPresent();
	}

	@Test
	void refusesYearLongScope() {
		final AiRequestScopeViolation violation = guard
			.evaluate("Genera comidas para cada día del año")
			.orElseThrow();

		assertThat(violation.kind()).isEqualTo(AiRequestScopeKind.DIET_PLAN_DAYS);
		assertThat(violation.requestedAmount()).isEqualTo(365);
	}

	@Test
	void refusesMultiplePatientsByCount() {
		final AiRequestScopeViolation violation = guard.evaluate("Genera planes para 20 pacientes").orElseThrow();

		assertThat(violation.kind()).isEqualTo(AiRequestScopeKind.MULTI_PATIENT);
		assertThat(violation.requestedAmount()).isEqualTo(20);
	}

}
