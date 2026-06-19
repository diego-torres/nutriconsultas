package com.nutriconsultas.subscription;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class PlanTierTest {

	@Test
	void basicoLimitsAndEntitlements() {
		assertThat(PlanTier.BASICO.getMaxPatients()).isEqualTo(10);
		assertThat(PlanTier.BASICO.getMaxNutritionists()).isEqualTo(1);
		assertThat(PlanTier.BASICO.getRoleSlug()).isEqualTo("nutriologo-basico");
		assertThat(PlanTier.BASICO.hasEntitlement(Entitlement.PATIENT_MANAGEMENT)).isTrue();
		assertThat(PlanTier.BASICO.hasEntitlement(Entitlement.REPORTS_BASIC)).isTrue();
		assertThat(PlanTier.BASICO.hasEntitlement(Entitlement.PDF_EXPORT)).isFalse();
		assertThat(PlanTier.BASICO.hasEntitlement(Entitlement.USER_ADMINISTRATION)).isFalse();
	}

	@Test
	void profesionalIncludesBrandedReportsAndPdf() {
		assertThat(PlanTier.PROFESIONAL.getMaxPatients()).isEqualTo(50);
		assertThat(PlanTier.PROFESIONAL.hasEntitlement(Entitlement.REPORTS_ADVANCED)).isTrue();
		assertThat(PlanTier.PROFESIONAL.hasEntitlement(Entitlement.REPORTS_FULL)).isTrue();
		assertThat(PlanTier.PROFESIONAL.hasEntitlement(Entitlement.PDF_EXPORT)).isTrue();
		assertThat(PlanTier.PROFESIONAL.hasEntitlement(Entitlement.REPORTS_BRANDED)).isTrue();
		assertThat(PlanTier.PROFESIONAL.hasEntitlement(Entitlement.PRIORITY_SUPPORT)).isFalse();
	}

	@Test
	void plusHasUnlimitedPatientsAndPrioritySupport() {
		assertThat(PlanTier.PLUS.getMaxPatients()).isNull();
		assertThat(PlanTier.PLUS.hasEntitlement(Entitlement.PRIORITY_SUPPORT)).isTrue();
		assertThat(PlanTier.PLUS.hasEntitlement(Entitlement.USER_ADMINISTRATION)).isFalse();
	}

	@Test
	void consultorioHasClinicAdministrationAndTwentySeats() {
		assertThat(PlanTier.CONSULTORIO.getMaxPatients()).isNull();
		assertThat(PlanTier.CONSULTORIO.getMaxNutritionists()).isEqualTo(20);
		assertThat(PlanTier.CONSULTORIO.getRoleSlug()).isEqualTo("director-consultorio");
		assertThat(PlanTier.CONSULTORIO.hasEntitlement(Entitlement.USER_ADMINISTRATION)).isTrue();
	}

	@Test
	void fromRoleSlugResolvesKnownTiers() {
		assertThat(PlanTier.fromRoleSlug("nutriologo-profesional")).isEqualTo(PlanTier.PROFESIONAL);
		assertThat(PlanTier.fromRoleSlug("director-consultorio")).isEqualTo(PlanTier.CONSULTORIO);
	}

	@Test
	void fromRoleSlugRejectsUnknownSlug() {
		assertThatThrownBy(() -> PlanTier.fromRoleSlug("unknown-role")).isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("unknown-role");
	}

	@Test
	void displayNameReturnsSpanishMarketingLabel() {
		assertThat(PlanTier.BASICO.getDisplayName()).isEqualTo("Básico");
		assertThat(PlanTier.PROFESIONAL.getDisplayName()).isEqualTo("Profesional");
		assertThat(PlanTier.PLUS.getDisplayName()).isEqualTo("Plus");
		assertThat(PlanTier.CONSULTORIO.getDisplayName()).isEqualTo("Consultorio");
	}

}
