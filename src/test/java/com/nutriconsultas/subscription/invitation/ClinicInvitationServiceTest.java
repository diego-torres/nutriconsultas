package com.nutriconsultas.subscription.invitation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.server.ResponseStatusException;

import com.nutriconsultas.subscription.Clinic;
import com.nutriconsultas.subscription.ClinicInvitation;
import com.nutriconsultas.subscription.ClinicInvitationRepository;
import com.nutriconsultas.subscription.ClinicMemberRepository;
import com.nutriconsultas.subscription.ClinicRepository;
import com.nutriconsultas.subscription.InvitationStatus;
import com.nutriconsultas.subscription.NutritionistInvitationRepository;
import com.nutriconsultas.subscription.PlanTier;
import com.nutriconsultas.subscription.Subscription;
import com.nutriconsultas.subscription.SubscriptionAuditEventRepository;
import com.nutriconsultas.subscription.SubscriptionEntitlementService;
import com.nutriconsultas.subscription.SubscriptionStatus;
import com.nutriconsultas.util.InvitationTokenHasher;

@ExtendWith(MockitoExtension.class)
class ClinicInvitationServiceTest {

	private static final String DIRECTOR_ID = "auth0|director-1";

	private static final String INVITEE_ID = "auth0|invitee";

	private static final String INVITEE_EMAIL = "nutri@example.com";

	@Mock
	private SubscriptionEntitlementService subscriptionEntitlementService;

	@Mock
	private ClinicRepository clinicRepository;

	@Mock
	private ClinicInvitationRepository clinicInvitationRepository;

	@Mock
	private ClinicMemberRepository clinicMemberRepository;

	@Mock
	private NutritionistInvitationRepository nutritionistInvitationRepository;

	@Mock
	private NutritionistInvitationProperties invitationProperties;

	@Mock
	private InvitationEmailSender invitationEmailSender;

	@Mock
	private SubscriptionProvisioningService provisioningService;

	@Mock
	private SubscriptionAuditEventRepository auditEventRepository;

	@InjectMocks
	private ClinicInvitationService clinicInvitationService;

	private OidcUser directorPrincipal;

	private OidcUser inviteePrincipal;

	private Clinic clinic;

	private String rawToken;

	@BeforeEach
	void setUp() {
		directorPrincipal = oidcUser(DIRECTOR_ID, "director@example.com");
		inviteePrincipal = oidcUser(INVITEE_ID, INVITEE_EMAIL);
		rawToken = InvitationTokenHasher.generateToken();
		final Subscription subscription = new Subscription();
		subscription.setId(10L);
		subscription.setPlanTier(PlanTier.CONSULTORIO);
		subscription.setStatus(SubscriptionStatus.ACTIVE);
		clinic = new Clinic();
		clinic.setId(1L);
		clinic.setName("Consultorio Norte");
		clinic.setDirectorUserId(DIRECTOR_ID);
		clinic.setSubscription(subscription);
	}

	@Test
	void assertCanInviteNutritionistDelegatesToEntitlementService() {
		clinicInvitationService.assertCanInviteNutritionist(DIRECTOR_ID);

		verify(subscriptionEntitlementService).assertCanInviteNutritionist(DIRECTOR_ID);
	}

	@Test
	void createInvitationStoresHashAndSendsEmail() {
		doNothing().when(subscriptionEntitlementService).assertCanInviteNutritionist(DIRECTOR_ID);
		when(clinicRepository.findByDirectorUserIdWithSubscription(DIRECTOR_ID)).thenReturn(Optional.of(clinic));
		when(clinicInvitationRepository.findByEmailIgnoreCaseAndStatus(INVITEE_EMAIL, InvitationStatus.PENDING))
			.thenReturn(Optional.empty());
		when(nutritionistInvitationRepository.findByEmailIgnoreCaseAndStatus(INVITEE_EMAIL, InvitationStatus.PENDING))
			.thenReturn(Optional.empty());
		when(nutritionistInvitationRepository.findFirstByEmailIgnoreCaseAndStatusOrderByRedeemedAtDesc(INVITEE_EMAIL,
				InvitationStatus.REDEEMED))
			.thenReturn(Optional.empty());
		when(clinicInvitationRepository.findFirstByEmailIgnoreCaseAndStatusOrderByRedeemedAtDesc(INVITEE_EMAIL,
				InvitationStatus.REDEEMED))
			.thenReturn(Optional.empty());
		when(invitationProperties.getExpiryDays()).thenReturn(7);
		when(invitationProperties.buildClinicRedeemUrl(any()))
			.thenAnswer(invocation -> "https://app.test/clinic/redeem?token=" + invocation.getArgument(0));
		when(clinicInvitationRepository.save(any(ClinicInvitation.class))).thenAnswer(invocation -> {
			final ClinicInvitation invitation = invocation.getArgument(0);
			invitation.setId(5L);
			return invitation;
		});

		final CreatedClinicInvitation created = clinicInvitationService.createInvitation(directorPrincipal,
				INVITEE_EMAIL);

		verify(subscriptionEntitlementService).assertCanInviteNutritionist(DIRECTOR_ID);
		final ArgumentCaptor<ClinicInvitation> captor = ArgumentCaptor.forClass(ClinicInvitation.class);
		verify(clinicInvitationRepository).save(captor.capture());
		assertThat(captor.getValue().getTokenHash()).hasSize(64);
		assertThat(captor.getValue().getEmail()).isEqualTo(INVITEE_EMAIL);
		verify(invitationEmailSender).sendClinicInvitation(INVITEE_EMAIL, clinic.getName(), created.inviteUrl());
	}

	@Test
	void cancelInvitationMarksCancelled() {
		when(clinicRepository.findByDirectorUserIdWithSubscription(DIRECTOR_ID)).thenReturn(Optional.of(clinic));
		final ClinicInvitation pending = pendingInvitation();
		when(clinicInvitationRepository.findByIdAndClinicId(5L, 1L)).thenReturn(Optional.of(pending));

		clinicInvitationService.cancelInvitation(directorPrincipal, 5L);

		assertThat(pending.getStatus()).isEqualTo(InvitationStatus.CANCELLED);
		verify(clinicInvitationRepository).save(pending);
	}

	@Test
	void redeemInvitationProvisionsClinicMember() {
		final ClinicInvitation invitation = pendingInvitation();
		invitation.setTokenHash(InvitationTokenHasher.hashToken(rawToken));
		when(clinicInvitationRepository.findByTokenHash(InvitationTokenHasher.hashToken(rawToken)))
			.thenReturn(Optional.of(invitation));
		when(clinicInvitationRepository.save(any(ClinicInvitation.class)))
			.thenAnswer(invocation -> invocation.getArgument(0));

		clinicInvitationService.redeemInvitation(inviteePrincipal, rawToken);

		assertThat(invitation.getStatus()).isEqualTo(InvitationStatus.REDEEMED);
		assertThat(invitation.getRedeemedByUserId()).isEqualTo(INVITEE_ID);
		verify(provisioningService).provisionClinicInvitationMember(invitation, INVITEE_ID);
	}

	@Test
	void redeemRejectsMismatchedEmail() {
		final ClinicInvitation invitation = pendingInvitation();
		invitation.setTokenHash(InvitationTokenHasher.hashToken(rawToken));
		when(clinicInvitationRepository.findByTokenHash(InvitationTokenHasher.hashToken(rawToken)))
			.thenReturn(Optional.of(invitation));

		assertThatThrownBy(
				() -> clinicInvitationService.redeemInvitation(oidcUser(INVITEE_ID, "other@example.com"), rawToken))
			.isInstanceOf(ResponseStatusException.class)
			.hasMessageContaining("Sign in with the email");
		verify(provisioningService, never()).provisionClinicInvitationMember(any(), any());
	}

	@Test
	void createInvitationThrowsWhenPendingClinicInviteExists() {
		doNothing().when(subscriptionEntitlementService).assertCanInviteNutritionist(DIRECTOR_ID);
		when(clinicRepository.findByDirectorUserIdWithSubscription(DIRECTOR_ID)).thenReturn(Optional.of(clinic));
		final ClinicInvitation existing = pendingInvitation();
		existing.setId(99L);
		when(clinicInvitationRepository.findByEmailIgnoreCaseAndStatus(INVITEE_EMAIL, InvitationStatus.PENDING))
			.thenReturn(Optional.of(existing));

		assertThatThrownBy(() -> clinicInvitationService.createInvitation(directorPrincipal, INVITEE_EMAIL))
			.isInstanceOf(PendingClinicInvitationException.class);
	}

	private ClinicInvitation pendingInvitation() {
		final ClinicInvitation invitation = new ClinicInvitation();
		invitation.setId(5L);
		invitation.setClinic(clinic);
		invitation.setEmail(INVITEE_EMAIL);
		invitation.setInvitedByUserId(DIRECTOR_ID);
		invitation.setStatus(InvitationStatus.PENDING);
		invitation.setExpiresAt(Instant.now().plus(7, ChronoUnit.DAYS));
		return invitation;
	}

	private static OidcUser oidcUser(final String subject, final String email) {
		final OidcIdToken idToken = OidcIdToken.withTokenValue("token")
			.claim("sub", subject)
			.claim("email", email)
			.build();
		return new DefaultOidcUser(null, idToken);
	}

}
