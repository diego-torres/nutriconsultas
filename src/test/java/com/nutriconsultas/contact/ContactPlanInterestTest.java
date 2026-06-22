package com.nutriconsultas.contact;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.nutriconsultas.subscription.PlanTier;

class ContactPlanInterestTest {

	@Test
	void resolveFromParam_returnsEmptyForBlank() {
		assertThat(ContactPlanInterest.resolveFromParam(null)).isEmpty();
		assertThat(ContactPlanInterest.resolveFromParam("")).isEmpty();
		assertThat(ContactPlanInterest.resolveFromParam("   ")).isEmpty();
	}

	@Test
	void resolveFromParam_returnsTierForKnownSlug() {
		assertThat(ContactPlanInterest.resolveFromParam("nutriologo-profesional")).contains(PlanTier.PROFESIONAL);
	}

	@Test
	void resolveFromParam_ignoresUnknownSlug() {
		assertThat(ContactPlanInterest.resolveFromParam("unknown-plan")).isEmpty();
	}

	@Test
	void normalizeForStorage_persistsKnownSlug() {
		assertThat(ContactPlanInterest.normalizeForStorage("nutriologo-plus")).isEqualTo("nutriologo-plus");
	}

	@Test
	void normalizeForStorage_returnsNullForInvalidSlug() {
		assertThat(ContactPlanInterest.normalizeForStorage("invalid")).isNull();
	}

	@Test
	void defaultSubjectForPlan_includesDisplayName() {
		assertThat(ContactPlanInterest.defaultSubjectForPlan(PlanTier.BASICO))
			.isEqualTo("Solicitud de acceso — Plan Básico");
	}

}
