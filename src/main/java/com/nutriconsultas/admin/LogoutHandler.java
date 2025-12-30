package com.nutriconsultas.admin;

import java.io.IOException;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponentsBuilder;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Needed to perform SSO logout with Auth0. By default, Spring will clear the
 * SecurityContext and the session. This controller will also log users out of Auth0 by
 * calling the Auth0 logout endpoint.
 */
@Controller
public class LogoutHandler extends SecurityContextLogoutHandler {

	private final ClientRegistrationRepository clientRegistrationRepository;

	/**
	 * Create a new instance with a {@code ClientRegistrationRepository}, so that we can
	 * look up information about the configured provider to call the Auth0 logout
	 * endpoint. Called by the Spring framework.
	 * @param clientRegistrationRepository the {@code ClientRegistrationRepository} for
	 * this application.
	 */
	public LogoutHandler(final ClientRegistrationRepository clientRegistrationRepository) {
		this.clientRegistrationRepository = clientRegistrationRepository;
	}

	/**
	 * Delegates to {@linkplain SecurityContextLogoutHandler} to log the user out of the
	 * application, and then logs the user out of Auth0.
	 * @param httpServletRequest the request.
	 * @param httpServletResponse the response.
	 * @param authentication the current authentication.
	 */
	@Override
	public void logout(final HttpServletRequest httpServletRequest, final HttpServletResponse httpServletResponse,
			final Authentication authentication) {

		// Invalidate the session and clear the security context
		super.logout(httpServletRequest, httpServletResponse, authentication);

		// Build the URL to log the user out of Auth0 and redirect them to the home
		// page.
		// URL will look like
		// https://YOUR-DOMAIN/v2/logout?clientId=YOUR-CLIENT-ID&returnTo=http://localhost:3000
		final String issuer = (String) getClientRegistration().getProviderDetails()
			.getConfigurationMetadata()
			.get("issuer");
		final String clientId = getClientRegistration().getClientId();
		final String returnTo = ServletUriComponentsBuilder.fromCurrentContextPath().build().toString();

		final String logoutUrl = UriComponentsBuilder
			.fromUriString(issuer + "v2/logout?client_id={clientId}&returnTo={returnTo}")
			.encode()
			.buildAndExpand(clientId, returnTo)
			.toUriString();

		try {
			httpServletResponse.sendRedirect(logoutUrl);
		} catch (final IOException ioe) {
			// Handle or log error redirecting to logout URL
			// Logging is handled by the framework
		}
	}

	/**
	 * Gets the Spring ClientRegistration, which we use to get the registered client ID
	 * and issuer for building the {@code returnTo} query parameter when calling the Auth0
	 * logout API.
	 * @return the {@code ClientRegistration} for this application.
	 */
	private ClientRegistration getClientRegistration() {
		return this.clientRegistrationRepository.findByRegistrationId("auth0");
	}

}