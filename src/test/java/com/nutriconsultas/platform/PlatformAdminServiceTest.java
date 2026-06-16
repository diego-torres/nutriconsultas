package com.nutriconsultas.platform;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

	@Test
	void requirePlatformAdmin_throwsForbiddenForNonAdmin() {
		final PlatformAdminService service = new PlatformAdminService(new PlatformAdminProperties());
		final OidcUser principal = mock(OidcUser.class);
		when(principal.getSubject()).thenReturn("auth0|regular-user");
		when(principal.getEmail()).thenReturn("user@example.com");

		assertThatThrownBy(() -> service.requirePlatformAdmin(principal))
			.isInstanceOf(org.springframework.web.server.ResponseStatusException.class)
			.extracting(ex -> ((org.springframework.web.server.ResponseStatusException) ex).getStatusCode().value())
			.isEqualTo(403);
	}

	@Test
	void requirePlatformAdmin_allowsConfiguredAdmin() {
		final PlatformAdminProperties properties = new PlatformAdminProperties();
		properties.setAdminUserIds(java.util.List.of("auth0|admin-one"));
		final PlatformAdminService service = new PlatformAdminService(properties);
		final OidcUser principal = mock(OidcUser.class);
		when(principal.getSubject()).thenReturn("auth0|admin-one");

		service.requirePlatformAdmin(principal);
	}

	@Test
	void resolveActorUserId_returnsSubjectOrNull() {
		final PlatformAdminService service = new PlatformAdminService(new PlatformAdminProperties());
		final OidcUser principal = mock(OidcUser.class);
		when(principal.getSubject()).thenReturn("auth0|admin-one");

		assertThat(service.resolveActorUserId(principal)).isEqualTo("auth0|admin-one");
		assertThat(service.resolveActorUserId(null)).isNull();
	}

}
