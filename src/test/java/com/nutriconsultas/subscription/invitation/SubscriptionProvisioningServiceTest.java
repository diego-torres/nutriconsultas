package com.nutriconsultas.subscription.invitation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.nutriconsultas.auth0.Auth0RoleSyncClient;
import com.nutriconsultas.subscription.ClinicMemberRepository;
import com.nutriconsultas.subscription.ClinicMemberRole;
import com.nutriconsultas.subscription.ClinicRepository;
import com.nutriconsultas.subscription.InvitationStatus;
import com.nutriconsultas.subscription.NutritionistInvitation;
import com.nutriconsultas.subscription.PlanTier;
import com.nutriconsultas.subscription.Subscription;
import com.nutriconsultas.subscription.SubscriptionAuditEventRepository;
import com.nutriconsultas.subscription.SubscriptionRepository;
import com.nutriconsultas.subscription.SubscriptionStatus;

@ExtendWith(MockitoExtension.class)
class SubscriptionProvisioningServiceTest {

	@Mock
	private ClinicRepository clinicRepository;

	@Mock
	private ClinicMemberRepository clinicMemberRepository;

	@Mock
	private SubscriptionRepository subscriptionRepository;

	@Mock
	private Auth0RoleSyncClient auth0RoleSyncClient;

	@Mock
	private SubscriptionAuditEventRepository auditEventRepository;

	@InjectMocks
	private SubscriptionProvisioningService provisioningService;

	@Test
	void activateTrialAccessCreatesClinicAndSyncsRole() {
		final NutritionistInvitation invitation = invitation(PlanTier.BASICO, true);
		when(clinicMemberRepository.findByUserIdWithClinicAndSubscription("auth0|user-1")).thenReturn(Optional.empty());
		when(clinicRepository.findByDirectorUserId("auth0|user-1")).thenReturn(Optional.empty());
		when(subscriptionRepository.save(org.mockito.ArgumentMatchers.any(Subscription.class)))
			.thenAnswer(invocation -> {
				final Subscription subscription = invocation.getArgument(0);
				subscription.setId(5L);
				return subscription;
			});
		when(clinicRepository.save(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> {
			final com.nutriconsultas.subscription.Clinic clinic = invocation.getArgument(0);
			clinic.setId(9L);
			return clinic;
		});
		when(auth0RoleSyncClient.isConfigured()).thenReturn(true);

		provisioningService.activateTrialAccess(invitation, "auth0|user-1");

		assertThat(invitation.getSubscription()).isNotNull();
		assertThat(invitation.getSubscription().getStatus()).isEqualTo(SubscriptionStatus.TRIAL);
		verify(auth0RoleSyncClient).syncPlanRole("auth0|user-1", PlanTier.BASICO);
		verify(clinicMemberRepository)
			.save(org.mockito.ArgumentMatchers.argThat(member -> member.getRole() == ClinicMemberRole.NUTRITIONIST
					&& member.getUserId().equals("auth0|user-1")));
	}

	@Test
	void activateTrialAccessContinuesWhenAuth0SyncFails() {
		final NutritionistInvitation invitation = invitation(PlanTier.BASICO, true);
		when(clinicMemberRepository.findByUserIdWithClinicAndSubscription("auth0|user-1")).thenReturn(Optional.empty());
		when(clinicRepository.findByDirectorUserId("auth0|user-1")).thenReturn(Optional.empty());
		when(subscriptionRepository.save(org.mockito.ArgumentMatchers.any(Subscription.class)))
			.thenAnswer(invocation -> {
				final Subscription subscription = invocation.getArgument(0);
				subscription.setId(5L);
				return subscription;
			});
		when(clinicRepository.save(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> {
			final com.nutriconsultas.subscription.Clinic clinic = invocation.getArgument(0);
			clinic.setId(9L);
			return clinic;
		});
		when(auth0RoleSyncClient.isConfigured()).thenReturn(true);
		org.mockito.Mockito.doThrow(new IllegalStateException("Auth0 role sync failed"))
			.when(auth0RoleSyncClient)
			.syncPlanRole("auth0|user-1", PlanTier.BASICO);

		provisioningService.activateTrialAccess(invitation, "auth0|user-1");

		assertThat(invitation.getSubscription().getStatus()).isEqualTo(SubscriptionStatus.TRIAL);
		verify(clinicMemberRepository).save(org.mockito.ArgumentMatchers.any());
	}

	private static NutritionistInvitation invitation(final PlanTier planTier, final boolean paymentExempt) {
		final NutritionistInvitation invitation = new NutritionistInvitation();
		invitation.setId(1L);
		invitation.setEmail("nutri@example.com");
		invitation.setPlanTier(planTier);
		invitation.setStatus(InvitationStatus.REDEEMED);
		invitation.setExpiresAt(Instant.now().plus(1, ChronoUnit.DAYS));
		invitation.setCreatedByUserId("auth0|admin");
		invitation.setPaymentExempt(paymentExempt);
		invitation.setRedeemedByUserId("auth0|user-1");
		return invitation;
	}

}
