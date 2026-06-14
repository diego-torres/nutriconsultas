package com.nutriconsultas.platform;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

class PlatformAdminServiceTest {

	@Test
	void isPlatformAdmin_returnsTrueForConfiguredUserId() {
		final PlatformAdminProperties properties = new PlatformAdminProperties();
		properties.setAdminUserIds(java.util.List.of("auth0|admin-one", "auth0|admin-two"));
		final PlatformAdminService service = new PlatformAdminService(properties);

		assertThat(service.isPlatformAdminByUserId("auth0|admin-one")).isTrue();
		assertThat(service.isPlatformAdminByUserId("auth0|admin-two")).isTrue();
		assertThat(service.isPlatformAdminByUserId("auth0|regular-user")).isFalse();
		assertThat(service.isPlatformAdminByUserId(null)).isFalse();
		assertThat(service.isPlatformAdminByUserId("")).isFalse();
	}

	@Test
	void isPlatformAdmin_returnsTrueForConfiguredEmail() {
		final PlatformAdminProperties properties = new PlatformAdminProperties();
		properties.setAdminEmails(java.util.List.of("Admin@Example.com"));
		final PlatformAdminService service = new PlatformAdminService(properties);
		final OidcUser principal = mock(OidcUser.class);
		when(principal.getSubject()).thenReturn("auth0|regular-user");
		when(principal.getEmail()).thenReturn("admin@example.com");

		assertThat(service.isPlatformAdmin(principal)).isTrue();
	}

	@Test
	void isPlatformAdmin_returnsFalseWhenPrincipalIsNull() {
		final PlatformAdminService service = new PlatformAdminService(new PlatformAdminProperties());

		assertThat(service.isPlatformAdmin(null)).isFalse();
	}

}
