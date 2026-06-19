package com.nutriconsultas.subscription.invitation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

import com.nutriconsultas.auth0.Auth0RoleSyncClient;
import com.nutriconsultas.platform.PlatformAdminService;
import com.nutriconsultas.subscription.InvitationStatus;
import com.nutriconsultas.subscription.NutritionistInvitation;
import com.nutriconsultas.subscription.NutritionistInvitationRepository;
import com.nutriconsultas.subscription.PlanTier;
import com.nutriconsultas.subscription.Subscription;
import com.nutriconsultas.subscription.SubscriptionAuditEvent;
import com.nutriconsultas.subscription.SubscriptionAuditEventRepository;
import com.nutriconsultas.subscription.SubscriptionAuditEventType;
import com.nutriconsultas.subscription.SubscriptionRepository;
import com.nutriconsultas.subscription.SubscriptionStatus;
import com.nutriconsultas.subscription.payment.BillingInterval;
import com.nutriconsultas.subscription.payment.CheckoutSession;
import com.nutriconsultas.subscription.payment.PaymentCheckoutService;
import com.nutriconsultas.subscription.payment.PaymentProviderException;
import com.nutriconsultas.util.InvitationTokenHasher;

@ExtendWith(MockitoExtension.class)
class NutritionistInvitationServiceTest {

	private static final String ADMIN_USER_ID = "auth0|admin";

	private static final String INVITEE_USER_ID = "auth0|invitee";

	private static final String INVITEE_EMAIL = "nutri@example.com";

	@Mock
	private PlatformAdminService platformAdminService;

	@Mock
	private NutritionistInvitationRepository invitationRepository;

	@Mock
	private NutritionistInvitationProperties invitationProperties;

	@Mock
	private InvitationEmailSender invitationEmailSender;

	@Mock
	private SubscriptionProvisioningService provisioningService;

	@Mock
	private PaymentCheckoutService paymentCheckoutService;

	@Mock
	private SubscriptionAuditEventRepository auditEventRepository;

	@Mock
	private SubscriptionRepository subscriptionRepository;

	@Mock
	private Auth0RoleSyncClient auth0RoleSyncClient;

	@InjectMocks
	private NutritionistInvitationServiceImpl invitationService;

	private OidcUser adminPrincipal;

	private OidcUser inviteePrincipal;

	private String rawToken;

	@BeforeEach
	void setUp() {
		adminPrincipal = oidcUser(ADMIN_USER_ID, "admin@example.com");
		inviteePrincipal = oidcUser(INVITEE_USER_ID, INVITEE_EMAIL);
		rawToken = InvitationTokenHasher.generateToken();
	}

	@Test
	void createInvitationStoresHashAndSendsEmail() {
		when(platformAdminService.resolveActorUserId(adminPrincipal)).thenReturn(ADMIN_USER_ID);
		when(invitationProperties.getExpiryDays()).thenReturn(7);
		when(invitationProperties.buildRedeemUrl(any()))
			.thenAnswer(invocation -> "https://app.test/redeem?token=" + invocation.getArgument(0));
		when(invitationRepository.findByEmailIgnoreCaseAndStatus(INVITEE_EMAIL, InvitationStatus.PENDING))
			.thenReturn(Optional.empty());
		when(invitationRepository.findFirstByEmailIgnoreCaseAndStatusOrderByRedeemedAtDesc(INVITEE_EMAIL,
				InvitationStatus.REDEEMED))
			.thenReturn(Optional.empty());
		when(invitationRepository.save(any(NutritionistInvitation.class))).thenAnswer(invocation -> {
			final NutritionistInvitation invitation = invocation.getArgument(0);
			invitation.setId(1L);
			return invitation;
		});

		final CreatedNutritionistInvitation created = invitationService.createInvitation(adminPrincipal, INVITEE_EMAIL,
				PlanTier.PROFESIONAL, false);

		verify(platformAdminService).requirePlatformAdmin(adminPrincipal);
		final ArgumentCaptor<NutritionistInvitation> captor = ArgumentCaptor.forClass(NutritionistInvitation.class);
		verify(invitationRepository).save(captor.capture());
		assertThat(captor.getValue().getTokenHash()).hasSize(64);
		assertThat(captor.getValue().getEmail()).isEqualTo(INVITEE_EMAIL);
		verify(invitationEmailSender).sendNutritionistInvitation(eq(INVITEE_EMAIL), eq(PlanTier.PROFESIONAL), any());
		assertThat(created.invitationId()).isEqualTo(1L);
		assertThat(created.inviteUrl()).contains("token=");
	}

	@Test
	void createInvitationRejectsDuplicatePendingEmail() {
		final NutritionistInvitation existing = pendingInvitation();
		when(invitationRepository.findByEmailIgnoreCaseAndStatus(INVITEE_EMAIL, InvitationStatus.PENDING))
			.thenReturn(Optional.of(existing));

		assertThatThrownBy(
				() -> invitationService.createInvitation(adminPrincipal, INVITEE_EMAIL, PlanTier.BASICO, false))
			.isInstanceOf(PendingNutritionistInvitationException.class)
			.extracting(ex -> ((PendingNutritionistInvitationException) ex).getExistingInvitationId())
			.isEqualTo(1L);
	}

	@Test
	void createInvitationRejectsEmailWithActiveRedeemedAccess() {
		final NutritionistInvitation redeemed = pendingInvitation();
		redeemed.setStatus(InvitationStatus.REDEEMED);
		final Subscription subscription = new Subscription();
		subscription.setStatus(SubscriptionStatus.TRIAL);
		redeemed.setSubscription(subscription);
		when(invitationRepository.findByEmailIgnoreCaseAndStatus(INVITEE_EMAIL, InvitationStatus.PENDING))
			.thenReturn(Optional.empty());
		when(invitationRepository.findFirstByEmailIgnoreCaseAndStatusOrderByRedeemedAtDesc(INVITEE_EMAIL,
				InvitationStatus.REDEEMED))
			.thenReturn(Optional.of(redeemed));

		assertThatThrownBy(
				() -> invitationService.createInvitation(adminPrincipal, INVITEE_EMAIL, PlanTier.BASICO, false))
			.isInstanceOf(ActiveNutritionistUserException.class)
			.extracting(ex -> ((ActiveNutritionistUserException) ex).getRedeemedInvitationId())
			.isEqualTo(1L);
	}

	@Test
	void createInvitationAllowsEmailWithCancelledSubscription() {
		final NutritionistInvitation redeemed = pendingInvitation();
		redeemed.setStatus(InvitationStatus.REDEEMED);
		final Subscription subscription = new Subscription();
		subscription.setStatus(SubscriptionStatus.CANCELLED);
		redeemed.setSubscription(subscription);
		when(platformAdminService.resolveActorUserId(adminPrincipal)).thenReturn(ADMIN_USER_ID);
		when(invitationProperties.getExpiryDays()).thenReturn(7);
		when(invitationProperties.buildRedeemUrl(any()))
			.thenAnswer(invocation -> "https://app.test/redeem?token=" + invocation.getArgument(0));
		when(invitationRepository.findByEmailIgnoreCaseAndStatus(INVITEE_EMAIL, InvitationStatus.PENDING))
			.thenReturn(Optional.empty());
		when(invitationRepository.findFirstByEmailIgnoreCaseAndStatusOrderByRedeemedAtDesc(INVITEE_EMAIL,
				InvitationStatus.REDEEMED))
			.thenReturn(Optional.of(redeemed));
		when(invitationRepository.save(any(NutritionistInvitation.class))).thenAnswer(invocation -> {
			final NutritionistInvitation invitation = invocation.getArgument(0);
			invitation.setId(2L);
			return invitation;
		});

		final CreatedNutritionistInvitation created = invitationService.createInvitation(adminPrincipal, INVITEE_EMAIL,
				PlanTier.BASICO, false);

		assertThat(created.invitationId()).isEqualTo(2L);
	}

	@Test
	void cancelInvitationMarksPendingAsCancelled() {
		final NutritionistInvitation invitation = pendingInvitation();
		when(platformAdminService.resolveActorUserId(adminPrincipal)).thenReturn(ADMIN_USER_ID);
		when(invitationRepository.findById(1L)).thenReturn(Optional.of(invitation));

		invitationService.cancelInvitation(adminPrincipal, 1L);

		assertThat(invitation.getStatus()).isEqualTo(InvitationStatus.CANCELLED);
		verify(invitationRepository).save(invitation);
	}

	@Test
	void revokeNutritionistAccessCancelsSubscriptionAndRevokesAuth0() {
		final NutritionistInvitation invitation = redeemedInvitationWithSubscription(SubscriptionStatus.ACTIVE);
		invitation.getSubscription().setExternalSubscriptionId("mp-sub-1");
		when(platformAdminService.resolveActorUserId(adminPrincipal)).thenReturn(ADMIN_USER_ID);
		when(invitationRepository.findById(1L)).thenReturn(Optional.of(invitation));
		when(auth0RoleSyncClient.isConfigured()).thenReturn(true);

		invitationService.revokeNutritionistAccess(adminPrincipal, 1L, "ADMIN_REVOKE");

		assertThat(invitation.getSubscription().getStatus()).isEqualTo(SubscriptionStatus.CANCELLED);
		verify(paymentCheckoutService).cancelSubscription("mp-sub-1");
		verify(auth0RoleSyncClient).revokePlanRoles(INVITEE_USER_ID);
		verify(subscriptionRepository).save(invitation.getSubscription());
		final ArgumentCaptor<SubscriptionAuditEvent> auditCaptor = ArgumentCaptor
			.forClass(SubscriptionAuditEvent.class);
		verify(auditEventRepository).save(auditCaptor.capture());
		assertThat(auditCaptor.getValue().getEventType()).isEqualTo(SubscriptionAuditEventType.PLATFORM_ADMIN_ACTION);
		assertThat(auditCaptor.getValue().getDetails()).contains("action=access.revoke");
		assertThat(auditCaptor.getValue().getDetails()).contains("reason=ADMIN_REVOKE");
	}

	@Test
	void revokeNutritionistAccessAllowsReInviteAfterCancelled() {
		final NutritionistInvitation redeemed = redeemedInvitationWithSubscription(SubscriptionStatus.CANCELLED);
		when(platformAdminService.resolveActorUserId(adminPrincipal)).thenReturn(ADMIN_USER_ID);
		when(invitationProperties.getExpiryDays()).thenReturn(7);
		when(invitationProperties.buildRedeemUrl(any()))
			.thenAnswer(invocation -> "https://app.test/redeem?token=" + invocation.getArgument(0));
		when(invitationRepository.findByEmailIgnoreCaseAndStatus(INVITEE_EMAIL, InvitationStatus.PENDING))
			.thenReturn(Optional.empty());
		when(invitationRepository.findFirstByEmailIgnoreCaseAndStatusOrderByRedeemedAtDesc(INVITEE_EMAIL,
				InvitationStatus.REDEEMED))
			.thenReturn(Optional.of(redeemed));
		when(invitationRepository.save(any(NutritionistInvitation.class))).thenAnswer(invocation -> {
			final NutritionistInvitation invitation = invocation.getArgument(0);
			invitation.setId(2L);
			return invitation;
		});

		final CreatedNutritionistInvitation created = invitationService.createInvitation(adminPrincipal, INVITEE_EMAIL,
				PlanTier.BASICO, false);

		assertThat(created.invitationId()).isEqualTo(2L);
	}

	@Test
	void revokeNutritionistAccessRejectsPendingInvitation() {
		when(invitationRepository.findById(1L)).thenReturn(Optional.of(pendingInvitation()));

		assertThatThrownBy(() -> invitationService.revokeNutritionistAccess(adminPrincipal, 1L, null))
			.isInstanceOf(ResponseStatusException.class);
	}

	@Test
	void regenerateInvitationLinkRotatesTokenForPendingInvitation() {
		final NutritionistInvitation invitation = pendingInvitation();
		when(platformAdminService.resolveActorUserId(adminPrincipal)).thenReturn(ADMIN_USER_ID);
		when(invitationRepository.findById(1L)).thenReturn(Optional.of(invitation));
		when(invitationProperties.buildRedeemUrl(org.mockito.ArgumentMatchers.anyString()))
			.thenAnswer(invocation -> "https://app.test/redeem?token=" + invocation.getArgument(0));
		final String previousHash = invitation.getTokenHash();

		final String inviteUrl = invitationService.regenerateInvitationLink(adminPrincipal, 1L);

		assertThat(invitation.getTokenHash()).isNotEqualTo(previousHash);
		assertThat(inviteUrl).contains("token=");
		verify(invitationRepository).save(invitation);
	}

	@Test
	void redeemPaymentExemptInvitationActivatesTrial() {
		final NutritionistInvitation invitation = pendingInvitation();
		when(invitationRepository.findByTokenHash(InvitationTokenHasher.hashToken(rawToken)))
			.thenReturn(Optional.of(invitation));

		final RedeemNutritionistInvitationResult result = invitationService.redeemInvitation(inviteePrincipal,
				rawToken);

		assertThat(result).isInstanceOf(RedeemNutritionistInvitationResult.Activated.class);
		assertThat(invitation.getStatus()).isEqualTo(InvitationStatus.REDEEMED);
		verify(provisioningService).activateTrialAccess(invitation, INVITEE_USER_ID);
		verify(paymentCheckoutService, never()).createCheckoutSession(any(), any(), any());
	}

	@Test
	void redeemPaidInvitationCreatesCheckoutSession() {
		final NutritionistInvitation invitation = pendingInvitation();
		invitation.setPaymentExempt(false);
		when(invitationRepository.findByTokenHash(InvitationTokenHasher.hashToken(rawToken)))
			.thenReturn(Optional.of(invitation));
		when(paymentCheckoutService.createCheckoutSession(1L, PlanTier.PROFESIONAL, BillingInterval.MONTHLY))
			.thenReturn(new CheckoutSession(10L, "https://pay.test/checkout", "mp-1", null));

		final RedeemNutritionistInvitationResult result = invitationService.redeemInvitation(inviteePrincipal,
				rawToken);

		assertThat(result).isInstanceOf(RedeemNutritionistInvitationResult.CheckoutRedirect.class);
		assertThat(((RedeemNutritionistInvitationResult.CheckoutRedirect) result).checkoutUrl())
			.isEqualTo("https://pay.test/checkout");
		verify(provisioningService).createPendingSubscription(invitation);
	}

	@Test
	void redeemPaidInvitation_whenPaymentProviderUnavailable_returnsServiceUnavailable() {
		final NutritionistInvitation invitation = pendingInvitation();
		invitation.setPaymentExempt(false);
		when(invitationRepository.findByTokenHash(InvitationTokenHasher.hashToken(rawToken)))
			.thenReturn(Optional.of(invitation));
		when(paymentCheckoutService.createCheckoutSession(1L, PlanTier.PROFESIONAL, BillingInterval.MONTHLY))
			.thenThrow(new PaymentProviderException("Payment provider is not configured"));

		assertThatThrownBy(() -> invitationService.redeemInvitation(inviteePrincipal, rawToken))
			.isInstanceOf(ResponseStatusException.class)
			.extracting(ex -> ((ResponseStatusException) ex).getStatusCode().value())
			.isEqualTo(503);
	}

	@Test
	void redeemRejectsMismatchedEmail() {
		final NutritionistInvitation invitation = pendingInvitation();
		when(invitationRepository.findByTokenHash(InvitationTokenHasher.hashToken(rawToken)))
			.thenReturn(Optional.of(invitation));

		assertThatThrownBy(
				() -> invitationService.redeemInvitation(oidcUser(INVITEE_USER_ID, "other@example.com"), rawToken))
			.isInstanceOf(ResponseStatusException.class);
	}

	private NutritionistInvitation pendingInvitation() {
		final NutritionistInvitation invitation = new NutritionistInvitation();
		invitation.setId(1L);
		invitation.setEmail(INVITEE_EMAIL);
		invitation.setTokenHash(InvitationTokenHasher.hashToken(rawToken));
		invitation.setPlanTier(PlanTier.PROFESIONAL);
		invitation.setStatus(InvitationStatus.PENDING);
		invitation.setExpiresAt(Instant.now().plus(2, ChronoUnit.DAYS));
		invitation.setCreatedByUserId(ADMIN_USER_ID);
		invitation.setPaymentExempt(true);
		return invitation;
	}

	private NutritionistInvitation redeemedInvitationWithSubscription(final SubscriptionStatus status) {
		final NutritionistInvitation invitation = pendingInvitation();
		invitation.setStatus(InvitationStatus.REDEEMED);
		invitation.setRedeemedAt(Instant.now());
		invitation.setRedeemedByUserId(INVITEE_USER_ID);
		final Subscription subscription = new Subscription();
		subscription.setId(10L);
		subscription.setStatus(status);
		subscription.setPlanTier(PlanTier.PROFESIONAL);
		invitation.setSubscription(subscription);
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
