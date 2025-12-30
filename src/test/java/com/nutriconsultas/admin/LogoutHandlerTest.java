package com.nutriconsultas.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.ClientRegistration.ProviderDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.extern.slf4j.Slf4j;

@SpringBootTest
@Slf4j
@ActiveProfiles("test")
@SuppressWarnings("null")
public class LogoutHandlerTest {

	@Autowired
	private LogoutHandler logoutHandler;

	@MockitoBean
	private ClientRegistrationRepository clientRegistrationRepository;

	private HttpServletRequest httpServletRequest;

	private HttpServletResponse httpServletResponse;

	private Authentication authentication;

	private ClientRegistration clientRegistration;

	private Map<String, Object> configurationMetadata;

	@BeforeEach
	public void setup() {
		log.info("setting up LogoutHandler test");

		httpServletRequest = org.mockito.Mockito.mock(HttpServletRequest.class);
		httpServletResponse = org.mockito.Mockito.mock(HttpServletResponse.class);
		authentication = org.mockito.Mockito.mock(Authentication.class);
		clientRegistration = org.mockito.Mockito.mock(ClientRegistration.class);

		configurationMetadata = new HashMap<>();
		configurationMetadata.put("issuer", "https://test.auth0.com/");

		org.mockito.Mockito.when(clientRegistrationRepository.findByRegistrationId("auth0"))
			.thenReturn(clientRegistration);
		org.mockito.Mockito.when(clientRegistration.getClientId()).thenReturn("test-client-id");
		ProviderDetails providerDetails = org.mockito.Mockito.mock(ProviderDetails.class);
		org.mockito.Mockito.when(providerDetails.getConfigurationMetadata()).thenReturn(configurationMetadata);
		org.mockito.Mockito.when(clientRegistration.getProviderDetails()).thenReturn(providerDetails);

		// Set up ServletRequestAttributes for ServletUriComponentsBuilder
		RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(httpServletRequest));

		log.info("finished setting up LogoutHandler test");
	}

	@Test
	public void testLogout() throws IOException {
		log.info("Starting testLogout");
		// Arrange
		org.mockito.Mockito.when(httpServletRequest.getContextPath()).thenReturn("");

		// Act
		logoutHandler.logout(httpServletRequest, httpServletResponse, authentication);

		// Assert
		ArgumentCaptor<String> redirectUrlCaptor = ArgumentCaptor.forClass(String.class);
		org.mockito.Mockito.verify(httpServletResponse).sendRedirect(redirectUrlCaptor.capture());
		String redirectUrl = redirectUrlCaptor.getValue();
		assertThat(redirectUrl).isNotNull();
		assertThat(redirectUrl).contains("https://test.auth0.com/v2/logout");
		assertThat(redirectUrl).contains("client_id=test-client-id");
		assertThat(redirectUrl).contains("returnTo=");
		log.info("Finishing testLogout");
	}

	@Test
	public void testLogoutWithContextPath() throws IOException {
		log.info("Starting testLogoutWithContextPath");
		// Arrange
		org.mockito.Mockito.when(httpServletRequest.getContextPath()).thenReturn("/nutriconsultas");
		RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(httpServletRequest));

		// Act
		logoutHandler.logout(httpServletRequest, httpServletResponse, authentication);

		// Assert
		ArgumentCaptor<String> redirectUrlCaptor = ArgumentCaptor.forClass(String.class);
		org.mockito.Mockito.verify(httpServletResponse).sendRedirect(redirectUrlCaptor.capture());
		String redirectUrl = redirectUrlCaptor.getValue();
		assertThat(redirectUrl).isNotNull();
		assertThat(redirectUrl).contains("returnTo=/nutriconsultas");
		log.info("Finishing testLogoutWithContextPath");
	}

	@Test
	public void testLogoutHandlesIOException() throws IOException {
		log.info("Starting testLogoutHandlesIOException");
		// Arrange
		org.mockito.Mockito.when(httpServletRequest.getContextPath()).thenReturn("");
		doThrow(new IOException("Test exception")).when(httpServletResponse).sendRedirect(anyString());

		// Act - should not throw exception
		logoutHandler.logout(httpServletRequest, httpServletResponse, authentication);

		// Assert
		org.mockito.Mockito.verify(httpServletResponse).sendRedirect(anyString());
		log.info("Finishing testLogoutHandlesIOException");
	}

	@Test
	public void testLogoutWithNullAuthentication() throws IOException {
		log.info("Starting testLogoutWithNullAuthentication");
		// Arrange
		org.mockito.Mockito.when(httpServletRequest.getContextPath()).thenReturn("");

		// Act
		logoutHandler.logout(httpServletRequest, httpServletResponse, null);

		// Assert
		org.mockito.Mockito.verify(httpServletResponse).sendRedirect(anyString());
		log.info("Finishing testLogoutWithNullAuthentication");
	}

}
