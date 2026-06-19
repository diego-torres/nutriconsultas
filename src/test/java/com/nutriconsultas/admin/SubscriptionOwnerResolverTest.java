package com.nutriconsultas.admin;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.nutriconsultas.subscription.Clinic;
import com.nutriconsultas.subscription.ClinicRepository;
import com.nutriconsultas.subscription.InvitationStatus;
import com.nutriconsultas.subscription.NutritionistInvitation;
import com.nutriconsultas.subscription.NutritionistInvitationRepository;

@ExtendWith(MockitoExtension.class)
class SubscriptionOwnerResolverTest {

	@InjectMocks
	private SubscriptionOwnerResolver resolver;

	@Mock
	private NutritionistInvitationRepository invitationRepository;

	@Mock
	private ClinicRepository clinicRepository;

	@Test
	void resolve_whenInvitationRedeemed_returnsEmailAndUserId() {
		final NutritionistInvitation invitation = new NutritionistInvitation();
		invitation.setId(7L);
		invitation.setEmail("nutri@example.com");
		invitation.setRedeemedByUserId("auth0|redeemed-user");
		org.mockito.Mockito.when(invitationRepository.findBySubscriptionId(3L)).thenReturn(Optional.of(invitation));

		final Optional<SubscriptionOwnerView> owner = resolver.resolve(3L);

		assertThat(owner).isPresent();
		assertThat(owner.get().email()).isEqualTo("nutri@example.com");
		assertThat(owner.get().userId()).isEqualTo("auth0|redeemed-user");
		assertThat(owner.get().invitationId()).isEqualTo(7L);
	}

	@Test
	void resolve_whenInvitationPending_usesClinicDirectorAsFallbackUserId() {
		final NutritionistInvitation invitation = new NutritionistInvitation();
		invitation.setId(8L);
		invitation.setEmail("pending@example.com");
		invitation.setStatus(InvitationStatus.PENDING);
		final Clinic clinic = new Clinic();
		clinic.setDirectorUserId("auth0|director-fallback");
		org.mockito.Mockito.when(invitationRepository.findBySubscriptionId(4L)).thenReturn(Optional.of(invitation));
		org.mockito.Mockito.when(clinicRepository.findBySubscriptionId(4L)).thenReturn(Optional.of(clinic));

		final Optional<SubscriptionOwnerView> owner = resolver.resolve(4L);

		assertThat(owner).isPresent();
		assertThat(owner.get().email()).isEqualTo("pending@example.com");
		assertThat(owner.get().userId()).isEqualTo("auth0|director-fallback");
	}

	@Test
	void resolve_whenNoInvitation_usesClinicDirectorOnly() {
		final Clinic clinic = new Clinic();
		clinic.setDirectorUserId("auth0|solo-director");
		org.mockito.Mockito.when(invitationRepository.findBySubscriptionId(5L)).thenReturn(Optional.empty());
		org.mockito.Mockito.when(clinicRepository.findBySubscriptionId(5L)).thenReturn(Optional.of(clinic));

		final Optional<SubscriptionOwnerView> owner = resolver.resolve(5L);

		assertThat(owner).isPresent();
		assertThat(owner.get().email()).isNull();
		assertThat(owner.get().userId()).isEqualTo("auth0|solo-director");
		assertThat(owner.get().invitationId()).isNull();
	}

}
