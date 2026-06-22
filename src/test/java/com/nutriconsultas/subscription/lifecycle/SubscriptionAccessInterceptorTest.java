package com.nutriconsultas.subscription.lifecycle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Locale;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.nutriconsultas.platform.PlatformAdminService;
import com.nutriconsultas.subscription.Subscription;
import com.nutriconsultas.subscription.SubscriptionErrorResponses;
import com.nutriconsultas.subscription.SubscriptionProperties;
import com.nutriconsultas.subscription.SubscriptionStatus;

@ExtendWith(MockitoExtension.class)
class SubscriptionAccessInterceptorTest {

	private static final String USER_ID = "auth0|uninvited-user";

	@Mock
	private SubscriptionAccessService subscriptionAccessService;

	@Mock
	private PlatformAdminService platformAdminService;

	@Mock
	private MessageSource messageSource;

	private SubscriptionAccessInterceptor interceptor;

	@BeforeEach
	void setUp() {
		final SubscriptionProperties properties = new SubscriptionProperties();
		properties.setEnforceNutritionistAccess(true);
		interceptor = new SubscriptionAccessInterceptor(subscriptionAccessService, platformAdminService, properties,
				messageSource);
	}

	@Test
	void allowsPublicAdminPathsWithoutSubscription() throws Exception {
		final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/admin/subscription/access-denied");
		final MockHttpServletResponse response = new MockHttpServletResponse();
		authenticateAs(USER_ID);

		assertThat(interceptor.preHandle(request, response, new Object())).isTrue();
		assertThat(response.getStatus()).isEqualTo(200);
	}

	@Test
	void redirectsUninvitedUserToAccessDeniedPage() throws Exception {
		final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/admin");
		final MockHttpServletResponse response = new MockHttpServletResponse();
		authenticateAs(USER_ID);
		when(platformAdminService.isPlatformAdmin(any(OidcUser.class))).thenReturn(false);
		when(subscriptionAccessService.isAdminAccessBlocked(anyString())).thenReturn(true);
		when(subscriptionAccessService.findSubscriptionForUser(anyString())).thenReturn(Optional.empty());

		assertThat(interceptor.preHandle(request, response, new Object())).isFalse();
		assertThat(response.getRedirectedUrl()).isEqualTo("/admin/subscription/access-denied");
	}

	@Test
	void redirectsPendingPaymentUserToBillingPage() throws Exception {
		final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/admin");
		final MockHttpServletResponse response = new MockHttpServletResponse();
		authenticateAs(USER_ID);
		final Subscription pending = new Subscription();
		pending.setStatus(SubscriptionStatus.PENDING_PAYMENT);
		when(platformAdminService.isPlatformAdmin(any(OidcUser.class))).thenReturn(false);
		when(subscriptionAccessService.isAdminAccessBlocked(anyString())).thenReturn(true);
		when(subscriptionAccessService.findSubscriptionForUser(anyString())).thenReturn(Optional.of(pending));

		assertThat(interceptor.preHandle(request, response, new Object())).isFalse();
		assertThat(response.getRedirectedUrl()).isEqualTo("/admin/subscription/billing");
	}

	@Test
	void returnsForbiddenJsonForBlockedRestRequests() throws Exception {
		final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/rest/pacientes");
		final MockHttpServletResponse response = new MockHttpServletResponse();
		authenticateAs(USER_ID);
		when(platformAdminService.isPlatformAdmin(any(OidcUser.class))).thenReturn(false);
		when(subscriptionAccessService.isAdminAccessBlocked(USER_ID)).thenReturn(true);
		when(messageSource.getMessage(eq(SubscriptionErrorResponses.KEY_INVITATION_REQUIRED), eq(null),
				eq(Locale.forLanguageTag("es-MX"))))
			.thenReturn("El acceso requiere invitación.");

		assertThat(interceptor.preHandle(request, response, new Object())).isFalse();
		assertThat(response.getStatus()).isEqualTo(403);
		assertThat(response.getContentAsString()).contains("invitation_required");
		verify(messageSource).getMessage(eq(SubscriptionErrorResponses.KEY_INVITATION_REQUIRED), eq(null),
				eq(Locale.forLanguageTag("es-MX")));
	}

	private static void authenticateAs(final String userId) {
		final OidcIdToken idToken = OidcIdToken.withTokenValue("token").subject(userId).build();
		final OidcUser principal = new DefaultOidcUser(null, idToken);
		SecurityContextHolder.getContext()
			.setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
	}

}
