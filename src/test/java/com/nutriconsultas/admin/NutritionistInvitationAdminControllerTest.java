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

import com.nutriconsultas.admin.CreateNutritionistInvitationForm;
import com.nutriconsultas.platform.PlatformAdminAuthorization;
import com.nutriconsultas.subscription.PlanTier;
import com.nutriconsultas.subscription.invitation.NutritionistInvitationService;
import com.nutriconsultas.subscription.invitation.ActiveNutritionistUserException;
import com.nutriconsultas.subscription.invitation.PendingNutritionistInvitationException;

@ExtendWith(MockitoExtension.class)
class NutritionistInvitationAdminControllerTest {

	@Mock
	private PlatformAdminAuthorization platformAdminAuthorization;

	@Mock
	private NutritionistInvitationService invitationService;

	@InjectMocks
	private NutritionistInvitationAdminController controller;

	@Test
	void list_whenNotPlatformAdmin_throwsForbidden() {
		final OidcUser principal = principal("auth0|user");
		org.mockito.Mockito.doThrow(new ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN))
			.when(platformAdminAuthorization)
			.requirePlatformAdmin(principal, "invitations.list");

		assertThatThrownBy(() -> controller.list(principal, new ExtendedModelMap(), null))
			.isInstanceOf(ResponseStatusException.class);
	}

	@Test
	void list_whenPlatformAdmin_returnsView() {
		final OidcUser principal = principal("auth0|admin");

		final String view = controller.list(principal, new ExtendedModelMap(), null);

		verify(platformAdminAuthorization).requirePlatformAdmin(principal, "invitations.list");
		assertThat(view).isEqualTo("sbadmin/platform/invitations/list");
	}

	@Test
	void create_whenDuplicatePending_returnsFormWithError() {
		final OidcUser principal = principal("auth0|admin");
		final CreateNutritionistInvitationForm form = new CreateNutritionistInvitationForm();
		form.setEmail("nutri@example.com");
		form.setPlanTier(PlanTier.BASICO);
		org.mockito.Mockito.when(invitationService.createInvitation(principal, form.getEmail(), form.getPlanTier(),
				form.isPaymentExempt()))
			.thenThrow(new PendingNutritionistInvitationException(42L));
		final ExtendedModelMap model = new ExtendedModelMap();
		final org.springframework.validation.BeanPropertyBindingResult bindingResult = new org.springframework.validation.BeanPropertyBindingResult(
				form, "form");

		final String view = controller.create(principal, form, bindingResult, model,
				new org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap());

		assertThat(view).isEqualTo("sbadmin/platform/invitations/form");
		assertThat(model.get("errorMessage")).isNotNull();
		assertThat(model.get("conflictingInvitationId")).isEqualTo(42L);
	}

	@Test
	void create_whenActiveNutritionist_returnsFormWithError() {
		final OidcUser principal = principal("auth0|admin");
		final CreateNutritionistInvitationForm form = new CreateNutritionistInvitationForm();
		form.setEmail("nutri@example.com");
		form.setPlanTier(PlanTier.BASICO);
		org.mockito.Mockito.when(invitationService.createInvitation(principal, form.getEmail(), form.getPlanTier(),
				form.isPaymentExempt()))
			.thenThrow(new ActiveNutritionistUserException(99L));
		final ExtendedModelMap model = new ExtendedModelMap();
		final org.springframework.validation.BeanPropertyBindingResult bindingResult = new org.springframework.validation.BeanPropertyBindingResult(
				form, "form");

		final String view = controller.create(principal, form, bindingResult, model,
				new org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap());

		assertThat(view).isEqualTo("sbadmin/platform/invitations/form");
		assertThat(model.get("errorMessage")).asString()
			.contains("acceso activo");
	}

	@Test
	void cancel_whenPlatformAdmin_redirectsToList() {
		final OidcUser principal = principal("auth0|admin");
		final org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap redirectAttributes = new org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap();

		final String view = controller.cancel(principal, 7L, redirectAttributes);

		verify(invitationService).cancelInvitation(principal, 7L);
		assertThat(view).isEqualTo("redirect:/admin/platform/invitations");
	}

	@Test
	void regenerateLink_whenPlatformAdmin_redirectsWithInviteUrl() {
		final OidcUser principal = principal("auth0|admin");
		final org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap redirectAttributes = new org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap();
		when(invitationService.regenerateInvitationLink(principal, 3L)).thenReturn("https://app.test/redeem?token=abc");

		final String view = controller.regenerateLink(principal, 3L, redirectAttributes);

		verify(invitationService).regenerateInvitationLink(principal, 3L);
		assertThat(view).isEqualTo("redirect:/admin/platform/invitations?highlight=3");
		assertThat(redirectAttributes.getFlashAttributes().get("inviteUrl")).isEqualTo("https://app.test/redeem?token=abc");
	}

	private static OidcUser principal(final String subject) {
		final OidcIdToken idToken = OidcIdToken.withTokenValue("token").claim("sub", subject).build();
		return new DefaultOidcUser(null, idToken);
	}

}
