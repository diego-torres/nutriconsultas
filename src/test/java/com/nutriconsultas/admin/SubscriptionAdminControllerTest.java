package com.nutriconsultas.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.web.server.ResponseStatusException;

import com.nutriconsultas.platform.PlatformAdminAuthorization;
import com.nutriconsultas.subscription.ClinicRepository;
import com.nutriconsultas.subscription.InvitationStatus;
import com.nutriconsultas.subscription.NutritionistInvitation;
import com.nutriconsultas.subscription.NutritionistInvitationRepository;
import com.nutriconsultas.subscription.PlanTier;
import com.nutriconsultas.subscription.Subscription;
import com.nutriconsultas.subscription.SubscriptionRepository;
import com.nutriconsultas.subscription.SubscriptionStatus;
import com.nutriconsultas.subscription.invitation.NutritionistInvitationService;
import com.nutriconsultas.subscription.lifecycle.SubscriptionLifecycleService;

@ExtendWith(MockitoExtension.class)
class SubscriptionAdminControllerTest {

	@InjectMocks
	private SubscriptionAdminController controller;

	@Mock
	private PlatformAdminAuthorization platformAdminAuthorization;

	@Mock
	private SubscriptionRepository subscriptionRepository;

	@Mock
	private ClinicRepository clinicRepository;

	@Mock
	private SubscriptionLifecycleService lifecycleService;

	@Mock
	private NutritionistInvitationRepository invitationRepository;

	@Mock
	private NutritionistInvitationService invitationService;

	@Test
	void list_whenNotPlatformAdmin_throwsForbidden() {
		final OidcUser principal = principal("auth0|user");
		org.mockito.Mockito.doThrow(new ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN))
			.when(platformAdminAuthorization)
			.requirePlatformAdmin(principal, "subscriptions.list");

		assertThatThrownBy(() -> controller.list(principal, new ExtendedModelMap()))
			.isInstanceOf(ResponseStatusException.class);
	}

	@Test
	void list_whenPlatformAdmin_returnsView() {
		final OidcUser principal = principal("auth0|admin");
		final Model model = new ExtendedModelMap();

		final String view = controller.list(principal, model);

		verify(platformAdminAuthorization).requirePlatformAdmin(principal, "subscriptions.list");
		assertThat(view).isEqualTo("sbadmin/platform/subscriptions/list");
		assertThat(model.getAttribute("activeMenu")).isEqualTo("subscriptions");
	}

	@Test
	void revokeAccess_whenPlatformAdmin_delegatesToInvitationService() {
		final OidcUser principal = principal("auth0|admin");
		final Subscription subscription = new Subscription();
		subscription.setId(9L);
		final NutritionistInvitation invitation = new NutritionistInvitation();
		invitation.setId(3L);
		invitation.setStatus(InvitationStatus.REDEEMED);
		invitation.setSubscription(subscription);
		subscription.setStatus(SubscriptionStatus.ACTIVE);
		when(subscriptionRepository.findById(9L)).thenReturn(java.util.Optional.of(subscription));
		when(invitationRepository.findBySubscriptionId(9L)).thenReturn(java.util.Optional.of(invitation));
		final org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap redirectAttributes = new org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap();

		final String view = controller.revokeAccess(principal, 9L, "ADMIN", redirectAttributes);

		verify(platformAdminAuthorization).requirePlatformAdmin(principal, "subscriptions.revoke");
		verify(invitationService).revokeNutritionistAccess(principal, 3L, "ADMIN");
		assertThat(view).isEqualTo("redirect:/admin/platform/subscriptions");
	}

	private static OidcUser principal(final String subject) {
		final OidcIdToken token = OidcIdToken.withTokenValue("token").claim("sub", subject).build();
		return new DefaultOidcUser(java.util.List.of(), token);
	}

}
