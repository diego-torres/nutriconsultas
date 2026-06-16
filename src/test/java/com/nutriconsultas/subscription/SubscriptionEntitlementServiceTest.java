package com.nutriconsultas.subscription;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.EnumSet;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SubscriptionEntitlementServiceTest {

	private static final String DIRECTOR_ID = "auth0|director-1";

	private static final String NUTRITIONIST_ID = "auth0|nutri-1";

	private static final String SOLO_ID = "auth0|solo-1";

	@Mock
	private ClinicMemberRepository clinicMemberRepository;

	@Mock
	private ClinicRepository clinicRepository;

	private SubscriptionProperties subscriptionProperties;

	private SubscriptionEntitlementServiceImpl service;

	@BeforeEach
	void setUp() {
		subscriptionProperties = new SubscriptionProperties();
		service = new SubscriptionEntitlementServiceImpl(clinicMemberRepository, clinicRepository,
				subscriptionProperties);
	}

	@Test
	void getEffectivePlanTierReturnsTierForActiveClinicMember() {
		when(clinicMemberRepository.findByUserIdWithClinicAndSubscription(NUTRITIONIST_ID))
			.thenReturn(Optional.of(activeMember(NUTRITIONIST_ID, PlanTier.CONSULTORIO)));

		assertThat(service.getEffectivePlanTier(NUTRITIONIST_ID)).contains(PlanTier.CONSULTORIO);
	}

	@Test
	void getEffectivePlanTierFallsBackToDirectorClinic() {
		when(clinicMemberRepository.findByUserIdWithClinicAndSubscription(SOLO_ID)).thenReturn(Optional.empty());
		when(clinicRepository.findByDirectorUserIdWithSubscription(SOLO_ID))
			.thenReturn(Optional.of(clinicWithSubscription(SOLO_ID, PlanTier.BASICO)));

		assertThat(service.getEffectivePlanTier(SOLO_ID)).contains(PlanTier.BASICO);
	}

	@Test
	void getEffectivePlanTierEmptyWhenMemberSuspended() {
		final ClinicMember suspended = activeMember(NUTRITIONIST_ID, PlanTier.PROFESIONAL);
		suspended.setMembershipStatus(MembershipStatus.SUSPENDED);
		when(clinicMemberRepository.findByUserIdWithClinicAndSubscription(NUTRITIONIST_ID))
			.thenReturn(Optional.of(suspended));

		assertThat(service.getEffectivePlanTier(NUTRITIONIST_ID)).isEmpty();
	}

	@Test
	void hasEntitlementTrueForActiveSubscriptionEntitlement() {
		when(clinicMemberRepository.findByUserIdWithClinicAndSubscription(SOLO_ID))
			.thenReturn(Optional.of(activeMember(SOLO_ID, PlanTier.PROFESIONAL)));

		assertThat(service.hasEntitlement(SOLO_ID, Entitlement.PDF_EXPORT)).isTrue();
		assertThat(service.hasEntitlement(SOLO_ID, Entitlement.REPORTS_BRANDED)).isTrue();
		assertThat(service.hasEntitlement(SOLO_ID, Entitlement.PRIORITY_SUPPORT)).isFalse();
	}

	@Test
	void directorMemberGetsUserAdministrationOnConsultorioPlan() {
		final ClinicMember director = activeMember(DIRECTOR_ID, PlanTier.CONSULTORIO);
		director.setRole(ClinicMemberRole.DIRECTOR);
		when(clinicMemberRepository.findByUserIdWithClinicAndSubscription(DIRECTOR_ID))
			.thenReturn(Optional.of(director));

		assertThat(service.hasEntitlement(DIRECTOR_ID, Entitlement.USER_ADMINISTRATION)).isTrue();
	}

	@Test
	void clinicMemberInheritsClinicSubscriptionTier() {
		when(clinicMemberRepository.findByUserIdWithClinicAndSubscription(NUTRITIONIST_ID))
			.thenReturn(Optional.of(activeMember(NUTRITIONIST_ID, PlanTier.CONSULTORIO)));

		assertThat(service.hasEntitlement(NUTRITIONIST_ID, Entitlement.USER_ADMINISTRATION)).isFalse();
		assertThat(service.hasEntitlement(NUTRITIONIST_ID, Entitlement.PDF_EXPORT)).isTrue();
	}

	@Test
	void suspendedMemberHasNoEntitlements() {
		final ClinicMember suspended = activeMember(NUTRITIONIST_ID, PlanTier.CONSULTORIO);
		suspended.setMembershipStatus(MembershipStatus.SUSPENDED);
		when(clinicMemberRepository.findByUserIdWithClinicAndSubscription(NUTRITIONIST_ID))
			.thenReturn(Optional.of(suspended));

		assertThat(service.hasEntitlement(NUTRITIONIST_ID, Entitlement.PATIENT_MANAGEMENT)).isFalse();
		assertThat(service.hasEntitlement(NUTRITIONIST_ID, Entitlement.PDF_EXPORT)).isFalse();
	}

	@Test
	void graceStateDeniesConfiguredWriteEntitlements() {
		final ClinicMember member = activeMember(SOLO_ID, PlanTier.PLUS);
		member.getClinic().getSubscription().setStatus(SubscriptionStatus.GRACE);
		when(clinicMemberRepository.findByUserIdWithClinicAndSubscription(SOLO_ID)).thenReturn(Optional.of(member));

		assertThat(service.hasEntitlement(SOLO_ID, Entitlement.PDF_EXPORT)).isFalse();
		assertThat(service.hasEntitlement(SOLO_ID, Entitlement.USER_ADMINISTRATION)).isFalse();
		assertThat(service.hasEntitlement(SOLO_ID, Entitlement.REPORTS_BASIC)).isTrue();
		assertThat(service.hasEntitlement(SOLO_ID, Entitlement.PRIORITY_SUPPORT)).isTrue();
	}

	@Test
	void graceDeniedEntitlementsAreConfigurable() {
		subscriptionProperties.setGraceDeniedEntitlements(EnumSet.of(Entitlement.REPORTS_FULL));
		service = new SubscriptionEntitlementServiceImpl(clinicMemberRepository, clinicRepository,
				subscriptionProperties);

		final ClinicMember member = activeMember(SOLO_ID, PlanTier.PROFESIONAL);
		member.getClinic().getSubscription().setStatus(SubscriptionStatus.GRACE);
		when(clinicMemberRepository.findByUserIdWithClinicAndSubscription(SOLO_ID)).thenReturn(Optional.of(member));

		assertThat(service.hasEntitlement(SOLO_ID, Entitlement.REPORTS_FULL)).isFalse();
		assertThat(service.hasEntitlement(SOLO_ID, Entitlement.PDF_EXPORT)).isTrue();
	}

	@Test
	void pendingPaymentSubscriptionHasNoEntitlements() {
		final ClinicMember member = activeMember(SOLO_ID, PlanTier.BASICO);
		member.getClinic().getSubscription().setStatus(SubscriptionStatus.PENDING_PAYMENT);
		when(clinicMemberRepository.findByUserIdWithClinicAndSubscription(SOLO_ID)).thenReturn(Optional.of(member));

		assertThat(service.hasEntitlement(SOLO_ID, Entitlement.PATIENT_MANAGEMENT)).isFalse();
	}

	@Test
	void trialSubscriptionGrantsFullPlanEntitlements() {
		final ClinicMember member = activeMember(SOLO_ID, PlanTier.PROFESIONAL);
		member.getClinic().getSubscription().setStatus(SubscriptionStatus.TRIAL);
		when(clinicMemberRepository.findByUserIdWithClinicAndSubscription(SOLO_ID)).thenReturn(Optional.of(member));

		assertThat(service.hasEntitlement(SOLO_ID, Entitlement.PDF_EXPORT)).isTrue();
	}

	@Test
	void hasEntitlementFalseWhenUserUnknown() {
		when(clinicMemberRepository.findByUserIdWithClinicAndSubscription("auth0|unknown"))
			.thenReturn(Optional.empty());
		when(clinicRepository.findByDirectorUserIdWithSubscription("auth0|unknown")).thenReturn(Optional.empty());

		assertThat(service.hasEntitlement("auth0|unknown", Entitlement.CALENDAR)).isFalse();
	}

	private static ClinicMember activeMember(final String userId, final PlanTier planTier) {
		final Clinic clinic = clinicWithSubscription(DIRECTOR_ID, planTier);
		final ClinicMember member = new ClinicMember();
		member.setClinic(clinic);
		member.setUserId(userId);
		member.setRole(ClinicMemberRole.NUTRITIONIST);
		member.setMembershipStatus(MembershipStatus.ACTIVE);
		return member;
	}

	private static Clinic clinicWithSubscription(final String directorUserId, final PlanTier planTier) {
		final Subscription subscription = new Subscription();
		subscription.setId(1L);
		subscription.setPlanTier(planTier);
		subscription.setStatus(SubscriptionStatus.ACTIVE);

		final Clinic clinic = new Clinic();
		clinic.setId(1L);
		clinic.setName("Test Clinic");
		clinic.setDirectorUserId(directorUserId);
		clinic.setSubscription(subscription);
		return clinic;
	}

}
