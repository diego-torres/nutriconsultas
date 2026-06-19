package com.nutriconsultas.subscription.invitation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import com.nutriconsultas.subscription.InvitationStatus;
import com.nutriconsultas.subscription.NutritionistInvitation;
import com.nutriconsultas.subscription.NutritionistInvitationRepository;
import com.nutriconsultas.subscription.PlanTier;
import com.nutriconsultas.subscription.Subscription;
import com.nutriconsultas.subscription.SubscriptionAuditEventRepository;
import com.nutriconsultas.subscription.SubscriptionRepository;
import com.nutriconsultas.subscription.SubscriptionStatus;
import com.nutriconsultas.subscription.payment.PaymentProperties;

@ExtendWith(MockitoExtension.class)
class NutritionistInvitationDevCheckoutServiceTest {

	@InjectMocks
	private NutritionistInvitationDevCheckoutService service;

	@Mock
	private PaymentProperties paymentProperties;

	@Mock
	private NutritionistInvitationRepository invitationRepository;

	@Mock
	private SubscriptionRepository subscriptionRepository;

	@Mock
	private SubscriptionAuditEventRepository auditEventRepository;

	@Mock
	private SubscriptionProvisioningService provisioningService;

	@Test
	void completeStubCheckout_activatesSubscriptionAndProvisionsAccess() {
		when(paymentProperties.isStubSimulateCheckout()).thenReturn(true);
		when(paymentProperties.isMercadoPagoConfigured()).thenReturn(false);
		final Subscription subscription = new Subscription();
		subscription.setId(2L);
		subscription.setPlanTier(PlanTier.BASICO);
		subscription.setStatus(SubscriptionStatus.PENDING_PAYMENT);
		final NutritionistInvitation invitation = new NutritionistInvitation();
		invitation.setId(5L);
		invitation.setStatus(InvitationStatus.REDEEMED);
		invitation.setRedeemedByUserId("auth0|invitee");
		invitation.setSubscription(subscription);
		when(invitationRepository.findById(5L)).thenReturn(Optional.of(invitation));

		service.completeStubCheckout(principal("auth0|invitee"), 5L);

		assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
		assertThat(subscription.getExternalSubscriptionId()).isEqualTo("stub-sub-5");
		verify(subscriptionRepository).save(subscription);
		verify(provisioningService).activatePaidAccess(invitation, subscription);
		verify(auditEventRepository).save(any());
	}

	private static OidcUser principal(final String subject) {
		final OidcIdToken token = OidcIdToken.withTokenValue("token").claim("sub", subject).build();
		return new DefaultOidcUser(java.util.List.of(), token);
	}

}
