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
import org.springframework.web.server.ResponseStatusException;

import com.nutriconsultas.platform.PlatformAdminAuthorization;
import com.nutriconsultas.subscription.NutritionistInvitationRepository;
import com.nutriconsultas.subscription.invitation.NutritionistInvitationService;

@ExtendWith(MockitoExtension.class)
class NutritionistInvitationAdminControllerTest {

	@Mock
	private PlatformAdminAuthorization platformAdminAuthorization;

	@Mock
	private NutritionistInvitationService invitationService;

	@Mock
	private NutritionistInvitationRepository invitationRepository;

	@InjectMocks
	private NutritionistInvitationAdminController controller;

	@Test
	void list_whenNotPlatformAdmin_throwsForbidden() {
		final OidcUser principal = principal("auth0|user");
		org.mockito.Mockito.doThrow(new ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN))
			.when(platformAdminAuthorization)
			.requirePlatformAdmin(principal, "invitations.list");

		assertThatThrownBy(() -> controller.list(principal, new ExtendedModelMap()))
			.isInstanceOf(ResponseStatusException.class);
	}

	@Test
	void list_whenPlatformAdmin_returnsView() {
		final OidcUser principal = principal("auth0|admin");
		when(invitationRepository.findAllByOrderByCreatedAtDesc()).thenReturn(java.util.List.of());

		final String view = controller.list(principal, new ExtendedModelMap());

		verify(platformAdminAuthorization).requirePlatformAdmin(principal, "invitations.list");
		assertThat(view).isEqualTo("sbadmin/platform/invitations/list");
	}

	private static OidcUser principal(final String subject) {
		final OidcIdToken idToken = OidcIdToken.withTokenValue("token").claim("sub", subject).build();
		return new DefaultOidcUser(null, idToken);
	}

}
