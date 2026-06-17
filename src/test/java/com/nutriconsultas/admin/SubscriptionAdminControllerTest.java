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
import com.nutriconsultas.subscription.SubscriptionRepository;
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

	private static OidcUser principal(final String subject) {
		final OidcIdToken token = OidcIdToken.withTokenValue("token").claim("sub", subject).build();
		return new DefaultOidcUser(java.util.List.of(), token);
	}

}
