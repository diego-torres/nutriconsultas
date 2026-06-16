package com.nutriconsultas.subscription;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import com.nutriconsultas.platform.PlatformAdminService;

@ExtendWith(MockitoExtension.class)
class NutritionistRoleServiceTest {

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
	private Auth0RoleSyncClient auth0RoleSyncClient;

	@Mock
	private SubscriptionAuditEventRepository subscriptionAuditEventRepository;

	@Mock
	private OidcUser adminPrincipal;

	@Test
	void assignRole_whenNotPlatformAdmin_throwsForbidden() {
		doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN)).when(platformAdminService)
			.requirePlatformAdmin(adminPrincipal);

		assertThatThrownBy(() -> service.assignRole(adminPrincipal, "auth0|target", PlanTier.BASICO))
			.isInstanceOf(ResponseStatusException.class)
			.extracting(ex -> ((ResponseStatusException) ex).getStatusCode().value())
			.isEqualTo(403);

		verify(subscriptionRepository, never()).save(any());
		verify(auth0RoleSyncClient, never()).syncPlanRole(any(), any());
	}

	@Test
	void assignRole_whenSubscriptionNotFound_throwsNotFound() {
		when(clinicRepository.findByDirectorUserIdWithSubscription("auth0|missing")).thenReturn(Optional.empty());
		when(clinicMemberRepository.findByUserIdWithClinicAndSubscription("auth0|missing"))
			.thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.assignRole(adminPrincipal, "auth0|missing", PlanTier.BASICO))
			.isInstanceOf(ResponseStatusException.class)
			.extracting(ex -> ((ResponseStatusException) ex).getStatusCode().value())
			.isEqualTo(404);
	}

	@Test
	void assignRole_whenDirector_updatesDbSyncsAuth0AndAudits() {
		final Subscription subscription = new Subscription();
		subscription.setId(1L);
		subscription.setPlanTier(PlanTier.BASICO);
		final Clinic clinic = new Clinic();
		clinic.setSubscription(subscription);
		when(clinicRepository.findByDirectorUserIdWithSubscription("auth0|target")).thenReturn(Optional.of(clinic));
		when(platformAdminService.resolveActorUserId(adminPrincipal)).thenReturn("auth0|admin-one");

		service.assignRole(adminPrincipal, "auth0|target", PlanTier.PROFESIONAL);

		assertThat(subscription.getPlanTier()).isEqualTo(PlanTier.PROFESIONAL);
		verify(subscriptionRepository).save(subscription);
		verify(auth0RoleSyncClient).syncPlanRole("auth0|target", PlanTier.PROFESIONAL);
		final ArgumentCaptor<SubscriptionAuditEvent> auditCaptor = ArgumentCaptor
			.forClass(SubscriptionAuditEvent.class);
		verify(subscriptionAuditEventRepository).save(auditCaptor.capture());
		final SubscriptionAuditEvent audit = auditCaptor.getValue();
		assertThat(audit.getEventType()).isEqualTo(SubscriptionAuditEventType.ROLE_ASSIGNED);
		assertThat(audit.getActorUserId()).isEqualTo("auth0|admin-one");
		assertThat(audit.getDetails()).contains("targetUserId=auth0|target");
		assertThat(audit.getDetails()).contains("previousTier=BASICO");
		assertThat(audit.getDetails()).contains("newTier=PROFESIONAL");
	}

	@Test
	void assignRole_whenClinicMember_resolvesSubscriptionViaMember() {
		final Subscription subscription = new Subscription();
		subscription.setPlanTier(PlanTier.PLUS);
		final Clinic clinic = new Clinic();
		clinic.setSubscription(subscription);
		final ClinicMember member = new ClinicMember();
		member.setClinic(clinic);
		when(clinicRepository.findByDirectorUserIdWithSubscription("auth0|member")).thenReturn(Optional.empty());
		when(clinicMemberRepository.findByUserIdWithClinicAndSubscription("auth0|member"))
			.thenReturn(Optional.of(member));
		when(platformAdminService.resolveActorUserId(adminPrincipal)).thenReturn("auth0|admin-one");

		service.assignRole(adminPrincipal, "auth0|member", PlanTier.CONSULTORIO);

		verify(auth0RoleSyncClient).syncPlanRole("auth0|member", PlanTier.CONSULTORIO);
		assertThat(subscription.getPlanTier()).isEqualTo(PlanTier.CONSULTORIO);
	}

}
