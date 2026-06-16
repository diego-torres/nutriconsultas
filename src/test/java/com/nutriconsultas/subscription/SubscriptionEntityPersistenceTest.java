package com.nutriconsultas.subscription;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class SubscriptionEntityPersistenceTest {

	private static final String TOKEN_HASH = "a".repeat(64);

	@Autowired
	private SubscriptionRepository subscriptionRepository;

	@Autowired
	private ClinicRepository clinicRepository;

	@Autowired
	private ClinicMemberRepository clinicMemberRepository;

	@Autowired
	private NutritionistInvitationRepository nutritionistInvitationRepository;

	@Autowired
	private ClinicInvitationRepository clinicInvitationRepository;

	@Autowired
	private SubscriptionAuditEventRepository subscriptionAuditEventRepository;

	@Autowired
	private EntityManager entityManager;

	@Test
	void subscriptionRoundTripsWithDefaults() {
		final Subscription subscription = new Subscription();
		subscription.setPlanTier(PlanTier.PROFESIONAL);
		subscription.setStatus(SubscriptionStatus.ACTIVE);
		subscription.setPeriodStart(Instant.now());
		subscription.setPeriodEnd(Instant.now().plus(30, ChronoUnit.DAYS));
		subscription.setPaymentExempt(false);
		subscription.setGracePeriodDays(7);
		subscription.setExternalSubscriptionId("ext-sub-1");
		subscription.setExternalCustomerId("ext-cust-1");

		final Subscription saved = subscriptionRepository.saveAndFlush(subscription);
		entityManager.clear();

		final Subscription loaded = subscriptionRepository.findById(saved.getId()).orElseThrow();
		assertThat(loaded.getPlanTier()).isEqualTo(PlanTier.PROFESIONAL);
		assertThat(loaded.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
		assertThat(loaded.getGracePeriodDays()).isEqualTo(7);
		assertThat(loaded.getExternalSubscriptionId()).isEqualTo("ext-sub-1");
		assertThat(loaded.getCreatedAt()).isNotNull();
		assertThat(loaded.getUpdatedAt()).isNotNull();
	}

	@Test
	void clinicAndMemberPersistWithSubscriptionLink() {
		final Subscription subscription = subscriptionRepository.saveAndFlush(activeSubscription(PlanTier.CONSULTORIO));

		final Clinic clinic = new Clinic();
		clinic.setName("Clínica Norte");
		clinic.setDirectorUserId("auth0|director-1");
		clinic.setSubscription(subscription);
		final Clinic savedClinic = clinicRepository.saveAndFlush(clinic);

		final ClinicMember director = new ClinicMember();
		director.setClinic(savedClinic);
		director.setUserId("auth0|director-1");
		director.setRole(ClinicMemberRole.DIRECTOR);
		director.setMembershipStatus(MembershipStatus.ACTIVE);
		clinicMemberRepository.saveAndFlush(director);

		entityManager.clear();

		final Clinic loadedClinic = clinicRepository.findByDirectorUserId("auth0|director-1").orElseThrow();
		assertThat(loadedClinic.getSubscription().getPlanTier()).isEqualTo(PlanTier.CONSULTORIO);
		assertThat(clinicMemberRepository.findByClinicIdAndUserId(loadedClinic.getId(), "auth0|director-1"))
			.map(ClinicMember::getRole)
			.contains(ClinicMemberRole.DIRECTOR);
	}

	@Test
	void invitationsStoreTokenHashOnly() {
		final Subscription subscription = subscriptionRepository.saveAndFlush(activeSubscription(PlanTier.BASICO));
		final Clinic clinic = new Clinic();
		clinic.setName("Solo Practice");
		clinic.setDirectorUserId("auth0|solo-1");
		clinic.setSubscription(subscription);
		final Clinic savedClinic = clinicRepository.saveAndFlush(clinic);

		final NutritionistInvitation adminInvite = new NutritionistInvitation();
		adminInvite.setEmail("nutri@example.com");
		adminInvite.setTokenHash(TOKEN_HASH);
		adminInvite.setPlanTier(PlanTier.BASICO);
		adminInvite.setStatus(InvitationStatus.PENDING);
		adminInvite.setExpiresAt(Instant.now().plus(7, ChronoUnit.DAYS));
		adminInvite.setCreatedByUserId("auth0|admin-1");
		nutritionistInvitationRepository.saveAndFlush(adminInvite);

		final ClinicInvitation clinicInvite = new ClinicInvitation();
		clinicInvite.setClinic(savedClinic);
		clinicInvite.setEmail("member@example.com");
		clinicInvite.setTokenHash("b".repeat(64));
		clinicInvite.setInvitedByUserId("auth0|director-1");
		clinicInvite.setStatus(InvitationStatus.PENDING);
		clinicInvite.setExpiresAt(Instant.now().plus(7, ChronoUnit.DAYS));
		clinicInvitationRepository.saveAndFlush(clinicInvite);

		entityManager.clear();

		assertThat(nutritionistInvitationRepository.findByTokenHash(TOKEN_HASH)).isPresent();
		assertThat(clinicInvitationRepository.findByTokenHash("b".repeat(64))).isPresent();
	}

	@Test
	void auditEventPersistsStateTransition() {
		final Subscription subscription = subscriptionRepository.saveAndFlush(activeSubscription(PlanTier.PLUS));

		final SubscriptionAuditEvent event = new SubscriptionAuditEvent();
		event.setSubscription(subscription);
		event.setEventType(SubscriptionAuditEventType.STATE_TRANSITION);
		event.setActorUserId("auth0|admin-1");
		event.setPreviousStatus(SubscriptionStatus.ACTIVE);
		event.setNewStatus(SubscriptionStatus.GRACE);
		event.setReasonCode("PERIOD_EXPIRED");
		event.setDetails("Grace period started");
		subscriptionAuditEventRepository.saveAndFlush(event);

		entityManager.clear();

		final SubscriptionAuditEvent loaded = subscriptionAuditEventRepository.findAll().get(0);
		assertThat(loaded.getEventType()).isEqualTo(SubscriptionAuditEventType.STATE_TRANSITION);
		assertThat(loaded.getPreviousStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
		assertThat(loaded.getNewStatus()).isEqualTo(SubscriptionStatus.GRACE);
		assertThat(loaded.getSubscription().getId()).isEqualTo(subscription.getId());
	}

	private static Subscription activeSubscription(final PlanTier planTier) {
		final Subscription subscription = new Subscription();
		subscription.setPlanTier(planTier);
		subscription.setStatus(SubscriptionStatus.ACTIVE);
		subscription.setPeriodStart(Instant.now());
		subscription.setPeriodEnd(Instant.now().plus(30, ChronoUnit.DAYS));
		subscription.setPaymentExempt(false);
		subscription.setGracePeriodDays(7);
		return subscription;
	}

}
