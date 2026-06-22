package com.nutriconsultas.subscription.clinic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.nutriconsultas.auth0.Auth0UserLookup;
import com.nutriconsultas.profile.NutritionistProfile;
import com.nutriconsultas.profile.NutritionistProfileRepository;
import com.nutriconsultas.subscription.ClinicInvitation;
import com.nutriconsultas.subscription.ClinicInvitationRepository;
import com.nutriconsultas.subscription.NutritionistInvitation;
import com.nutriconsultas.subscription.NutritionistInvitationRepository;

@ExtendWith(MockitoExtension.class)
class ClinicMemberLabelResolverTest {

	private static final String USER_ID = "auth0|nutri-1";

	@InjectMocks
	private ClinicMemberLabelResolver resolver;

	@Mock
	private NutritionistProfileRepository nutritionistProfileRepository;

	@Mock
	private NutritionistInvitationRepository nutritionistInvitationRepository;

	@Mock
	private ClinicInvitationRepository clinicInvitationRepository;

	@Mock
	private Auth0UserLookup auth0UserLookup;

	@Test
	void resolveLabel_prefersProfileDisplayName() {
		final NutritionistProfile profile = new NutritionistProfile();
		profile.setDisplayName("Lic. Ana Pérez");
		when(nutritionistProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(profile));

		assertThat(resolver.resolveLabel(USER_ID)).isEqualTo("Lic. Ana Pérez");
	}

	@Test
	void resolveLabel_fallsBackToNutritionistInvitationEmail() {
		when(nutritionistProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
		final NutritionistInvitation invitation = new NutritionistInvitation();
		invitation.setEmail("nutriologo@example.com");
		when(nutritionistInvitationRepository.findFirstByRedeemedByUserIdOrderByRedeemedAtDesc(USER_ID))
			.thenReturn(Optional.of(invitation));

		assertThat(resolver.resolveLabel(USER_ID)).isEqualTo("nutriologo@example.com");
	}

	@Test
	void resolveLabel_fallsBackToClinicInvitationEmail() {
		when(nutritionistProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
		when(nutritionistInvitationRepository.findFirstByRedeemedByUserIdOrderByRedeemedAtDesc(USER_ID))
			.thenReturn(Optional.empty());
		final ClinicInvitation invitation = new ClinicInvitation();
		invitation.setEmail("clinic.invite@example.com");
		when(clinicInvitationRepository.findFirstByRedeemedByUserIdOrderByRedeemedAtDesc(USER_ID))
			.thenReturn(Optional.of(invitation));

		assertThat(resolver.resolveLabel(USER_ID)).isEqualTo("clinic.invite@example.com");
	}

	@Test
	void resolveLabel_fallsBackToAuth0Email() {
		when(nutritionistProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
		when(nutritionistInvitationRepository.findFirstByRedeemedByUserIdOrderByRedeemedAtDesc(USER_ID))
			.thenReturn(Optional.empty());
		when(clinicInvitationRepository.findFirstByRedeemedByUserIdOrderByRedeemedAtDesc(USER_ID))
			.thenReturn(Optional.empty());
		when(auth0UserLookup.findEmailByUserId(USER_ID)).thenReturn(Optional.of("auth0.user@example.com"));

		assertThat(resolver.resolveLabel(USER_ID)).isEqualTo("auth0.user@example.com");
	}

	@Test
	void resolveLabel_whenNoEmailAvailable_returnsPlaceholder() {
		when(nutritionistProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
		when(nutritionistInvitationRepository.findFirstByRedeemedByUserIdOrderByRedeemedAtDesc(USER_ID))
			.thenReturn(Optional.empty());
		when(clinicInvitationRepository.findFirstByRedeemedByUserIdOrderByRedeemedAtDesc(USER_ID))
			.thenReturn(Optional.empty());
		when(auth0UserLookup.findEmailByUserId(USER_ID)).thenReturn(Optional.empty());

		assertThat(resolver.resolveLabel(USER_ID)).isEqualTo("Correo no disponible");
	}

}
