package com.nutriconsultas.clinic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import com.nutriconsultas.clinic.CreateClinicInvitationForm;
import com.nutriconsultas.subscription.ClinicMemberRole;
import com.nutriconsultas.subscription.MembershipStatus;
import com.nutriconsultas.subscription.PlanTier;
import com.nutriconsultas.subscription.SubscriptionStatus;
import com.nutriconsultas.subscription.clinic.ClinicMemberView;
import com.nutriconsultas.subscription.clinic.ClinicRosterOverview;
import com.nutriconsultas.subscription.clinic.ClinicService;
import com.nutriconsultas.subscription.invitation.ClinicInvitationService;
import com.nutriconsultas.subscription.invitation.CreatedClinicInvitation;

@ExtendWith(MockitoExtension.class)
class ClinicDirectorControllerTest {

	private static final String DIRECTOR_ID = "auth0|director-1";

	@InjectMocks
	private ClinicDirectorController controller;

	@Mock
	private ClinicService clinicService;

	@Mock
	private ClinicInvitationService clinicInvitationService;

	@Mock
	private OidcUser principal;

	@Test
	void roster_delegatesToClinicService() {
		when(principal.getSubject()).thenReturn(DIRECTOR_ID);
		final ClinicRosterOverview roster = new ClinicRosterOverview(1L, "Consultorio", PlanTier.CONSULTORIO,
				SubscriptionStatus.ACTIVE, 20, 1L, 0L, List.of(new ClinicMemberView(1L, DIRECTOR_ID, "Director",
						ClinicMemberRole.DIRECTOR, MembershipStatus.ACTIVE, Instant.now(), true)),
				List.of());
		when(clinicService.getDirectorRoster(DIRECTOR_ID)).thenReturn(roster);
		final ExtendedModelMap model = new ExtendedModelMap();

		final String view = controller.roster(principal, model);

		assertThat(view).isEqualTo("sbadmin/clinic/members");
		assertThat(model.get("roster")).isEqualTo(roster);
		assertThat(model.get("activeMenu")).isEqualTo("clinic");
		assertThat(model.get("inviteForm")).isNotNull();
	}

	@Test
	void createInvitation_redirectsWithFlashMessage() {
		when(clinicInvitationService.createInvitation(principal, "nutri@example.com"))
			.thenReturn(new CreatedClinicInvitation(1L, "https://app.test/clinic/redeem?token=abc"));
		final CreateClinicInvitationForm form = new CreateClinicInvitationForm();
		form.setEmail("nutri@example.com");
		final RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

		final String view = controller.createInvitation(principal, form,
				new org.springframework.validation.BeanPropertyBindingResult(form, "inviteForm"),
				new ExtendedModelMap(), redirectAttributes);

		assertThat(view).isEqualTo("redirect:/admin/clinic");
		assertThat(redirectAttributes.getFlashAttributes().get("inviteUrl"))
			.isEqualTo("https://app.test/clinic/redeem?token=abc");
	}

	@Test
	void suspendMember_redirectsWithFlashMessage() {
		when(principal.getSubject()).thenReturn(DIRECTOR_ID);
		final RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

		final String view = controller.suspendMember(principal, 5L, redirectAttributes);

		verify(clinicService).suspendMember(DIRECTOR_ID, 5L);
		assertThat(view).isEqualTo("redirect:/admin/clinic");
		assertThat(redirectAttributes.getFlashAttributes().get("successMessage"))
			.isEqualTo("Acceso del nutriólogo suspendido.");
	}

	@Test
	void reactivateMember_redirectsWithFlashMessage() {
		when(principal.getSubject()).thenReturn(DIRECTOR_ID);
		final RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

		final String view = controller.reactivateMember(principal, 5L, redirectAttributes);

		verify(clinicService).reactivateMember(DIRECTOR_ID, 5L);
		assertThat(view).isEqualTo("redirect:/admin/clinic");
		assertThat(redirectAttributes.getFlashAttributes().get("successMessage"))
			.isEqualTo("Acceso del nutriólogo reactivado.");
	}

}
