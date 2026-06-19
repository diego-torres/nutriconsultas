package com.nutriconsultas.subscription.lifecycle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.nutriconsultas.subscription.Clinic;
import com.nutriconsultas.subscription.ClinicMember;
import com.nutriconsultas.subscription.ClinicMemberRepository;
import com.nutriconsultas.subscription.ClinicMemberRole;
import com.nutriconsultas.subscription.ClinicRepository;
import com.nutriconsultas.subscription.InvitationStatus;
import com.nutriconsultas.subscription.MembershipStatus;
import com.nutriconsultas.subscription.NutritionistInvitation;
import com.nutriconsultas.subscription.NutritionistInvitationRepository;
import com.nutriconsultas.subscription.PlanTier;
import com.nutriconsultas.subscription.Subscription;
import com.nutriconsultas.subscription.SubscriptionStatus;

@ExtendWith(MockitoExtension.class)
class SubscriptionAccessServiceTest {

	private static final String USER_ID = "auth0|reinvited-user";

	@Mock
	private ClinicMemberRepository clinicMemberRepository;

	@Mock
	private ClinicRepository clinicRepository;

	@Mock
	private NutritionistInvitationRepository invitationRepository;

	@InjectMocks
	private SubscriptionAccessService subscriptionAccessService;

	@Test
	void isAdminAccessBlockedWhenClinicSubscriptionCancelled() {
		final Subscription cancelled = subscription(2L, PlanTier.PROFESIONAL, SubscriptionStatus.CANCELLED);
		stubActiveMemberWithSubscription(cancelled);
		when(invitationRepository.findFirstByRedeemedByUserIdAndStatusOrderByRedeemedAtDesc(USER_ID,
				InvitationStatus.REDEEMED))
			.thenReturn(Optional.empty());

		assertThat(subscriptionAccessService.isAdminAccessBlocked(USER_ID)).isTrue();
	}

	@Test
	void isAdminAccessNotBlockedWhenRedeemedInvitationHasActiveSubscription() {
		final Subscription cancelled = subscription(2L, PlanTier.PROFESIONAL, SubscriptionStatus.CANCELLED);
		final Subscription activeBasic = subscription(5L, PlanTier.BASICO, SubscriptionStatus.ACTIVE);
		stubActiveMemberWithSubscription(cancelled);
		when(invitationRepository.findFirstByRedeemedByUserIdAndStatusOrderByRedeemedAtDesc(USER_ID,
				InvitationStatus.REDEEMED))
			.thenReturn(Optional.of(invitation(activeBasic)));

		assertThat(subscriptionAccessService.isAdminAccessBlocked(USER_ID)).isFalse();
		assertThat(subscriptionAccessService.findGrantingSubscriptionForUser(USER_ID)).contains(activeBasic);
	}

	@Test
	void findSubscriptionForUserPrefersGrantingInvitationOverCancelledClinicLink() {
		final Subscription cancelled = subscription(2L, PlanTier.PROFESIONAL, SubscriptionStatus.CANCELLED);
		final Subscription activeBasic = subscription(5L, PlanTier.BASICO, SubscriptionStatus.ACTIVE);
		stubActiveMemberWithSubscription(cancelled);
		when(invitationRepository.findFirstByRedeemedByUserIdAndStatusOrderByRedeemedAtDesc(USER_ID,
				InvitationStatus.REDEEMED))
			.thenReturn(Optional.of(invitation(activeBasic)));

		assertThat(subscriptionAccessService.findSubscriptionForUser(USER_ID)).contains(activeBasic);
	}

	private void stubActiveMemberWithSubscription(final Subscription subscription) {
		final Clinic clinic = new Clinic();
		clinic.setId(2L);
		clinic.setSubscription(subscription);
		final ClinicMember member = new ClinicMember();
		member.setClinic(clinic);
		member.setUserId(USER_ID);
		member.setRole(ClinicMemberRole.NUTRITIONIST);
		member.setMembershipStatus(MembershipStatus.ACTIVE);
		when(clinicMemberRepository.findByUserIdWithClinicAndSubscription(USER_ID)).thenReturn(Optional.of(member));
	}

	private static Subscription subscription(final Long id, final PlanTier planTier, final SubscriptionStatus status) {
		final Subscription subscription = new Subscription();
		subscription.setId(id);
		subscription.setPlanTier(planTier);
		subscription.setStatus(status);
		return subscription;
	}

	private static NutritionistInvitation invitation(final Subscription subscription) {
		final NutritionistInvitation invitation = new NutritionistInvitation();
		invitation.setId(8L);
		invitation.setStatus(InvitationStatus.REDEEMED);
		invitation.setSubscription(subscription);
		return invitation;
	}

}
