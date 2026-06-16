package com.nutriconsultas.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.server.ResponseStatusException;

import com.nutriconsultas.platform.PlatformAdminAuthorization;

import org.springframework.security.oauth2.core.oidc.user.OidcUser;

@ExtendWith(MockitoExtension.class)
class PlatformAdminControllerTest {

	@InjectMocks
	private PlatformAdminController controller;

	@Mock
	private PlatformAdminAuthorization platformAdminAuthorization;

	@Mock
	private OidcUser principal;

	@Test
	void index_whenNotPlatformAdmin_throwsForbidden() {
		doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN)).when(platformAdminAuthorization)
			.requirePlatformAdmin(principal, "platform.index");

		assertThatThrownBy(() -> controller.index(principal, new ExtendedModelMap()))
			.isInstanceOf(ResponseStatusException.class)
			.extracting(ex -> ((ResponseStatusException) ex).getStatusCode().value())
			.isEqualTo(403);
	}

	@Test
	void index_whenPlatformAdmin_returnsPlatformView() {
		final ExtendedModelMap model = new ExtendedModelMap();

		final String view = controller.index(principal, model);

		verify(platformAdminAuthorization).requirePlatformAdmin(principal, "platform.index");
		assertThat(view).isEqualTo("sbadmin/platform/index");
		assertThat(model.get("activeMenu")).isEqualTo("platform");
	}

}
