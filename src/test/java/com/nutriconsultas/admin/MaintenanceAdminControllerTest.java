package com.nutriconsultas.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import com.nutriconsultas.platform.PlatformAdminAuthorization;
import com.nutriconsultas.subscription.maintenance.MaintenanceRetentionProperties;
import com.nutriconsultas.subscription.maintenance.MaintenanceRetentionService;
import com.nutriconsultas.subscription.maintenance.MaintenanceRun;
import com.nutriconsultas.subscription.maintenance.MaintenanceRunStatus;

@ExtendWith(MockitoExtension.class)
class MaintenanceAdminControllerTest {

	@Mock
	private PlatformAdminAuthorization platformAdminAuthorization;

	@Mock
	private MaintenanceRetentionService maintenanceRetentionService;

	@Mock
	private MaintenanceRetentionProperties maintenanceRetentionProperties;

	@InjectMocks
	private MaintenanceAdminController controller;

	@Test
	void list_whenNotPlatformAdmin_throwsForbidden() {
		final OidcUser principal = principal("auth0|user");
		org.mockito.Mockito.doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN))
			.when(platformAdminAuthorization)
			.requirePlatformAdmin(principal, "maintenance.list");

		assertThatThrownBy(() -> controller.list(principal, 0, new ExtendedModelMap()))
			.isInstanceOf(ResponseStatusException.class);
	}

	@Test
	void list_whenPlatformAdmin_returnsView() {
		final OidcUser principal = principal("auth0|admin");
		when(maintenanceRetentionService.findLatestRun()).thenReturn(Optional.empty());
		when(maintenanceRetentionService.findRecentRuns(0, 10)).thenReturn(Page.empty());
		when(maintenanceRetentionProperties.getRetentionDays()).thenReturn(90);

		final ExtendedModelMap model = new ExtendedModelMap();
		final String view = controller.list(principal, 0, model);

		verify(platformAdminAuthorization).requirePlatformAdmin(principal, "maintenance.list");
		assertThat(view).isEqualTo("sbadmin/platform/maintenance/list");
		assertThat(model.get("activeMenu")).isEqualTo("maintenance");
	}

	@Test
	void execute_whenPlatformAdmin_delegatesToService() {
		final OidcUser principal = principal("auth0|admin");
		final MaintenanceRun run = new MaintenanceRun();
		run.setEligibleCount(2);
		run.setPurgedUserCount(2);
		run.setStatus(MaintenanceRunStatus.COMPLETED);
		when(maintenanceRetentionService.executeCleanup("auth0|admin")).thenReturn(run);

		final String view = controller.execute(principal, new RedirectAttributesModelMap());

		verify(maintenanceRetentionService).executeCleanup("auth0|admin");
		assertThat(view).isEqualTo("redirect:/admin/platform/maintenance");
	}

	private static OidcUser principal(final String subject) {
		final OidcIdToken token = new OidcIdToken("token", Instant.now(), Instant.now().plusSeconds(3600),
				java.util.Map.of("sub", subject));
		return new DefaultOidcUser(java.util.List.of(), token, "sub");
	}

}
