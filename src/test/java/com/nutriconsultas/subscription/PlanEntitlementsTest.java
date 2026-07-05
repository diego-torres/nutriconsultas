package com.nutriconsultas.subscription;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

class PlanEntitlementsTest {

	@Test
	void basicoLimitsMatchMarketingTable() {
		final PlanEntitlements basico = PlanEntitlements.BASICO;
		assertThat(basico.getPlanTier()).isEqualTo(PlanTier.BASICO);
		assertThat(basico.getRoleSlug()).isEqualTo("nutriologo-basico");
		assertThat(basico.getMaxPatients()).isEqualTo(10);
		assertThat(basico.getMaxNutritionists()).isEqualTo(1);
	}

	@Test
	void consultorioLimitsMatchMarketingTable() {
		final PlanEntitlements consultorio = PlanEntitlements.CONSULTORIO;
		assertThat(consultorio.getMaxPatients()).isNull();
		assertThat(consultorio.getMaxNutritionists()).isEqualTo(20);
		assertThat(consultorio.getRoleSlug()).isEqualTo("director-consultorio");
	}

	@ParameterizedTest
	@MethodSource("planTierEntitlementMatrix")
	void fullPlanTierEntitlementMatrix(final PlanTier tier, final Entitlement entitlement, final boolean expected) {
		assertThat(PlanEntitlements.forTier(tier).hasEntitlement(entitlement)).isEqualTo(expected);
	}

	@ParameterizedTest
	@EnumSource(PlanTier.class)
	void forTierReturnsImmutableEntitlementSet(final PlanTier tier) {
		final Set<Entitlement> first = PlanEntitlements.forTier(tier).getEntitlements();
		final Set<Entitlement> second = PlanEntitlements.forTier(tier).getEntitlements();
		assertThat(first).isUnmodifiable().isEqualTo(second);
	}

	@Test
	void fromRoleSlugResolvesKnownTiers() {
		assertThat(PlanEntitlements.fromRoleSlug("nutriologo-plus").getPlanTier()).isEqualTo(PlanTier.PLUS);
		assertThat(PlanEntitlements.fromRoleSlug("director-consultorio").getPlanTier()).isEqualTo(PlanTier.CONSULTORIO);
	}

	@Test
	void fromRoleSlugRejectsUnknownSlug() {
		assertThatThrownBy(() -> PlanEntitlements.fromRoleSlug("unknown-role"))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("unknown-role");
	}

	private static Stream<Arguments> planTierEntitlementMatrix() {
		return Stream.of(PlanTier.values())
			.flatMap(tier -> Stream.of(Entitlement.values())
				.map(entitlement -> Arguments.of(tier, entitlement, expectedEntitlement(tier, entitlement))));
	}

	private static boolean expectedEntitlement(final PlanTier tier, final Entitlement entitlement) {
		return expectedForTier(tier).contains(entitlement);
	}

	private static Set<Entitlement> expectedForTier(final PlanTier tier) {
		return switch (tier) {
			case BASICO -> EnumSet.of(Entitlement.PATIENT_MANAGEMENT, Entitlement.CREATE_PATIENT,
					Entitlement.DIET_PLANS, Entitlement.CALENDAR, Entitlement.REPORTS_BASIC);
			case PROFESIONAL ->
				EnumSet.of(Entitlement.PATIENT_MANAGEMENT, Entitlement.CREATE_PATIENT, Entitlement.DIET_PLANS,
						Entitlement.CALENDAR, Entitlement.REPORTS_BASIC, Entitlement.REPORTS_ADVANCED,
						Entitlement.REPORTS_FULL, Entitlement.PDF_EXPORT, Entitlement.REPORTS_BRANDED);
			case PLUS -> EnumSet.of(Entitlement.PATIENT_MANAGEMENT, Entitlement.CREATE_PATIENT, Entitlement.DIET_PLANS,
					Entitlement.CALENDAR, Entitlement.REPORTS_BASIC, Entitlement.REPORTS_ADVANCED,
					Entitlement.REPORTS_FULL, Entitlement.PDF_EXPORT, Entitlement.REPORTS_BRANDED,
					Entitlement.PRIORITY_SUPPORT, Entitlement.AI_ASSISTANT);
			case CONSULTORIO -> EnumSet.of(Entitlement.PATIENT_MANAGEMENT, Entitlement.CREATE_PATIENT,
					Entitlement.DIET_PLANS, Entitlement.CALENDAR, Entitlement.REPORTS_BASIC,
					Entitlement.REPORTS_ADVANCED, Entitlement.REPORTS_FULL, Entitlement.PDF_EXPORT,
					Entitlement.REPORTS_BRANDED, Entitlement.PRIORITY_SUPPORT, Entitlement.USER_ADMINISTRATION,
					Entitlement.AI_ASSISTANT);
		};
	}

}
