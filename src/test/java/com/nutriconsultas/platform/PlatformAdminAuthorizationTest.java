package com.nutriconsultas.platform;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class PlatformAdminAuthorizationTest {

	@InjectMocks
	private PlatformAdminAuthorization platformAdminAuthorization;

	@Mock
	private PlatformAdminService platformAdminService;

	@Mock
	private PlatformAdminAuditService platformAdminAuditService;

	@Mock
	private OidcUser principal;

	@Test
	void requirePlatformAdminWithoutAction_delegatesToService() {
		platformAdminAuthorization.requirePlatformAdmin(principal);

		verify(platformAdminService).requirePlatformAdmin(principal);
		verify(platformAdminAuditService, never()).recordAction(org.mockito.ArgumentMatchers.anyString(),
				org.mockito.ArgumentMatchers.anyString());
	}

	@Test
	void requirePlatformAdminWithAction_auditsAfterAllowlistCheck() {
		when(platformAdminService.resolveActorUserId(principal)).thenReturn("auth0|admin-one");

		platformAdminAuthorization.requirePlatformAdmin(principal, "platform.index");

		verify(platformAdminService).requirePlatformAdmin(principal);
		verify(platformAdminAuditService).recordAction("auth0|admin-one", "platform.index");
	}

	@Test
	void requirePlatformAdminWithAction_doesNotAuditWhenForbidden() {
		org.mockito.Mockito.doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN))
			.when(platformAdminService)
			.requirePlatformAdmin(principal);

		assertThatThrownBy(() -> platformAdminAuthorization.requirePlatformAdmin(principal, "platform.index"))
			.isInstanceOf(ResponseStatusException.class);

		verify(platformAdminAuditService, never()).recordAction(org.mockito.ArgumentMatchers.anyString(),
				org.mockito.ArgumentMatchers.anyString());
	}

}
