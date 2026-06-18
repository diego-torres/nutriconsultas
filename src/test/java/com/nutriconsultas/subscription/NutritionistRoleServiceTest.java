package com.nutriconsultas.subscription;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.server.ResponseStatusException;

import com.nutriconsultas.auth0.Auth0RoleSyncClient;
import com.nutriconsultas.paciente.PacienteRepository;
import com.nutriconsultas.platform.PlatformAdminService;

@ExtendWith(MockitoExtension.class)
class NutritionistRoleServiceTest {

	private static final String TARGET_USER_ID = "auth0|target";

	private static final String ADMIN_USER_ID = "auth0|admin-one";

	@InjectMocks
	private NutritionistRoleServiceImpl service;

	@Mock
	private PlatformAdminService platformAdminService;

	@Mock
	private ClinicRepository clinicRepository;

	@Mock
	private ClinicMemberRepository clinicMemberRepository;

	@Mock
	private SubscriptionRepository subscriptionRepository;

	@Mock
	private NutritionistInvitationRepository invitationRepository;

	@Mock
	private PacienteRepository pacienteRepository;

	@Mock
	private Auth0RoleSyncClient auth0RoleSyncClient;

	@Mock
	private SubscriptionAuditEventRepository subscriptionAuditEventRepository;

	@Mock
	private OidcUser adminPrincipal;

	@Test
	void assignRole_whenNotPlatformAdmin_throwsForbidden() {
		doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN)).when(platformAdminService)
			.requirePlatformAdmin(adminPrincipal);

		assertThatThrownBy(() -> service.assignRole(adminPrincipal, TARGET_USER_ID, PlanTier.BASICO))
			.isInstanceOf(ResponseStatusException.class)
			.extracting(ex -> ((ResponseStatusException) ex).getStatusCode().value())
			.isEqualTo(403);

		verify(subscriptionRepository, never()).save(any());
		verify(auth0RoleSyncClient, never()).syncPlanRole(any(), any());
	}

	@Test
	void assignRole_whenSubscriptionNotFound_throwsNotFound() {
		when(clinicRepository.findByDirectorUserIdWithSubscription(TARGET_USER_ID)).thenReturn(Optional.empty());
		when(clinicMemberRepository.findByUserIdWithClinicAndSubscription(TARGET_USER_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.assignRole(adminPrincipal, TARGET_USER_ID, PlanTier.BASICO))
			.isInstanceOf(ResponseStatusException.class)
			.extracting(ex -> ((ResponseStatusException) ex).getStatusCode().value())
			.isEqualTo(404);
	}

	@Test
	void assignRole_whenDirector_updatesDbSyncsAuth0AndAudits() {
		final Subscription subscription = activeSubscription(PlanTier.BASICO);
		final Clinic clinic = clinicWithSubscription(subscription);
		when(clinicRepository.findByDirectorUserIdWithSubscription(TARGET_USER_ID)).thenReturn(Optional.of(clinic));
		when(clinicRepository.findBySubscriptionId(1L)).thenReturn(Optional.of(clinic));
		when(invitationRepository.findBySubscriptionId(1L)).thenReturn(Optional.empty());
		when(platformAdminService.resolveActorUserId(adminPrincipal)).thenReturn(ADMIN_USER_ID);
		when(auth0RoleSyncClient.isConfigured()).thenReturn(true);

		service.assignRole(adminPrincipal, TARGET_USER_ID, PlanTier.PROFESIONAL);

		assertThat(subscription.getPlanTier()).isEqualTo(PlanTier.PROFESIONAL);
		verify(subscriptionRepository).save(subscription);
		verify(auth0RoleSyncClient).syncPlanRole(TARGET_USER_ID, PlanTier.PROFESIONAL);
		final ArgumentCaptor<SubscriptionAuditEvent> auditCaptor = ArgumentCaptor
			.forClass(SubscriptionAuditEvent.class);
		verify(subscriptionAuditEventRepository).save(auditCaptor.capture());
		final SubscriptionAuditEvent audit = auditCaptor.getValue();
		assertThat(audit.getEventType()).isEqualTo(SubscriptionAuditEventType.ROLE_ASSIGNED);
		assertThat(audit.getActorUserId()).isEqualTo(ADMIN_USER_ID);
		assertThat(audit.getDetails()).contains("targetUserId=" + TARGET_USER_ID);
		assertThat(audit.getDetails()).contains("previousTier=BASICO");
		assertThat(audit.getDetails()).contains("newTier=PROFESIONAL");
	}

	@Test
	void assignRole_whenClinicMember_resolvesSubscriptionViaMember() {
		final Subscription subscription = activeSubscription(PlanTier.PLUS);
		final Clinic clinic = clinicWithSubscription(subscription);
		final ClinicMember member = new ClinicMember();
		member.setClinic(clinic);
		when(clinicRepository.findByDirectorUserIdWithSubscription(TARGET_USER_ID)).thenReturn(Optional.empty());
		when(clinicMemberRepository.findByUserIdWithClinicAndSubscription(TARGET_USER_ID))
			.thenReturn(Optional.of(member));
		when(clinicRepository.findBySubscriptionId(1L)).thenReturn(Optional.of(clinic));
		when(invitationRepository.findBySubscriptionId(1L)).thenReturn(Optional.empty());
		when(platformAdminService.resolveActorUserId(adminPrincipal)).thenReturn(ADMIN_USER_ID);
		when(auth0RoleSyncClient.isConfigured()).thenReturn(true);

		service.assignRole(adminPrincipal, TARGET_USER_ID, PlanTier.CONSULTORIO);

		verify(auth0RoleSyncClient).syncPlanRole(TARGET_USER_ID, PlanTier.CONSULTORIO);
		assertThat(subscription.getPlanTier()).isEqualTo(PlanTier.CONSULTORIO);
	}

	@Test
	void changeSubscriptionPlanTier_whenDowngradeExceedsPatientCap_throwsConflict() {
		final Subscription subscription = activeSubscription(PlanTier.PROFESIONAL);
		final Clinic clinic = clinicWithSubscription(subscription);
		clinic.setId(5L);
		when(subscriptionRepository.findById(1L)).thenReturn(Optional.of(subscription));
		when(clinicRepository.findBySubscriptionId(1L)).thenReturn(Optional.of(clinic));
		when(clinicMemberRepository.findUserIdsByClinicIdAndMembershipStatus(5L, MembershipStatus.ACTIVE))
			.thenReturn(List.of(TARGET_USER_ID));
		when(pacienteRepository.countByUserIdIn(List.of(TARGET_USER_ID))).thenReturn(25L);

		assertThatThrownBy(() -> service.changeSubscriptionPlanTier(adminPrincipal, 1L, PlanTier.BASICO))
			.isInstanceOf(ResponseStatusException.class)
			.extracting(ex -> ((ResponseStatusException) ex).getStatusCode().value())
			.isEqualTo(409);

		verify(subscriptionRepository, never()).save(any());
	}

	@Test
	void changeSubscriptionPlanTier_whenActive_upgradesAndAuditsPlatformAdminAction() {
		final Subscription subscription = activeSubscription(PlanTier.BASICO);
		final Clinic clinic = clinicWithSubscription(subscription);
		clinic.setId(5L);
		clinic.setDirectorUserId(TARGET_USER_ID);
		final NutritionistInvitation invitation = new NutritionistInvitation();
		invitation.setPlanTier(PlanTier.BASICO);
		invitation.setRedeemedByUserId(TARGET_USER_ID);
		when(subscriptionRepository.findById(1L)).thenReturn(Optional.of(subscription));
		when(clinicRepository.findBySubscriptionId(1L)).thenReturn(Optional.of(clinic));
		when(invitationRepository.findBySubscriptionId(1L)).thenReturn(Optional.of(invitation));
		when(clinicMemberRepository.findUserIdsByClinicIdAndMembershipStatus(5L, MembershipStatus.ACTIVE))
			.thenReturn(List.of(TARGET_USER_ID));
		when(pacienteRepository.countByUserIdIn(List.of(TARGET_USER_ID))).thenReturn(3L);
		when(clinicMemberRepository.countByClinicIdAndMembershipStatus(5L, MembershipStatus.ACTIVE)).thenReturn(1L);
		when(platformAdminService.resolveActorUserId(adminPrincipal)).thenReturn(ADMIN_USER_ID);
		when(auth0RoleSyncClient.isConfigured()).thenReturn(true);

		final PlanTierChangeResult result = service.changeSubscriptionPlanTier(adminPrincipal, 1L,
				PlanTier.PROFESIONAL);

		assertThat(result.previousTier()).isEqualTo(PlanTier.BASICO);
		assertThat(result.newTier()).isEqualTo(PlanTier.PROFESIONAL);
		assertThat(result.auth0SyncSucceeded()).isTrue();
		assertThat(subscription.getPlanTier()).isEqualTo(PlanTier.PROFESIONAL);
		assertThat(invitation.getPlanTier()).isEqualTo(PlanTier.PROFESIONAL);
		verify(auth0RoleSyncClient).syncPlanRole(TARGET_USER_ID, PlanTier.PROFESIONAL);
		final ArgumentCaptor<SubscriptionAuditEvent> auditCaptor = ArgumentCaptor
			.forClass(SubscriptionAuditEvent.class);
		verify(subscriptionAuditEventRepository).save(auditCaptor.capture());
		assertThat(auditCaptor.getValue().getEventType()).isEqualTo(SubscriptionAuditEventType.PLATFORM_ADMIN_ACTION);
		assertThat(auditCaptor.getValue().getDetails()).contains("action=plan.tier.change");
		assertThat(auditCaptor.getValue().getDetails()).contains("previousTier=BASICO");
		assertThat(auditCaptor.getValue().getDetails()).contains("newTier=PROFESIONAL");
	}

	@Test
	void changeSubscriptionPlanTier_whenAuth0SyncFails_stillPersistsTierAndReportsFailure() {
		final Subscription subscription = activeSubscription(PlanTier.BASICO);
		final Clinic clinic = clinicWithSubscription(subscription);
		clinic.setId(5L);
		clinic.setDirectorUserId(TARGET_USER_ID);
		when(subscriptionRepository.findById(1L)).thenReturn(Optional.of(subscription));
		when(clinicRepository.findBySubscriptionId(1L)).thenReturn(Optional.of(clinic));
		when(invitationRepository.findBySubscriptionId(1L)).thenReturn(Optional.empty());
		when(clinicMemberRepository.countByClinicIdAndMembershipStatus(5L, MembershipStatus.ACTIVE)).thenReturn(1L);
		when(platformAdminService.resolveActorUserId(adminPrincipal)).thenReturn(ADMIN_USER_ID);
		when(auth0RoleSyncClient.isConfigured()).thenReturn(true);
		doThrow(new RuntimeException("Auth0 down")).when(auth0RoleSyncClient)
			.syncPlanRole(eq(TARGET_USER_ID), eq(PlanTier.PLUS));

		final PlanTierChangeResult result = service.changeSubscriptionPlanTier(adminPrincipal, 1L, PlanTier.PLUS);

		assertThat(result.auth0SyncSucceeded()).isFalse();
		assertThat(subscription.getPlanTier()).isEqualTo(PlanTier.PLUS);
		final ArgumentCaptor<SubscriptionAuditEvent> auditCaptor = ArgumentCaptor
			.forClass(SubscriptionAuditEvent.class);
		verify(subscriptionAuditEventRepository).save(auditCaptor.capture());
		assertThat(auditCaptor.getValue().getDetails()).contains("auth0Synced=false");
	}

	@Test
	void changeSubscriptionPlanTier_whenCancelled_throwsConflict() {
		final Subscription subscription = activeSubscription(PlanTier.BASICO);
		subscription.setStatus(SubscriptionStatus.CANCELLED);
		when(subscriptionRepository.findById(1L)).thenReturn(Optional.of(subscription));

		assertThatThrownBy(() -> service.changeSubscriptionPlanTier(adminPrincipal, 1L, PlanTier.PLUS))
			.isInstanceOf(ResponseStatusException.class)
			.extracting(ex -> ((ResponseStatusException) ex).getStatusCode().value())
			.isEqualTo(409);
	}

	private static Subscription activeSubscription(final PlanTier planTier) {
		final Subscription subscription = new Subscription();
		subscription.setId(1L);
		subscription.setPlanTier(planTier);
		subscription.setStatus(SubscriptionStatus.ACTIVE);
		return subscription;
	}

	private static Clinic clinicWithSubscription(final Subscription subscription) {
		final Clinic clinic = new Clinic();
		clinic.setSubscription(subscription);
		return clinic;
	}

}
