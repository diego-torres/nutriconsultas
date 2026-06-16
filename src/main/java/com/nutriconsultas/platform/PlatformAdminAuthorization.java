package com.nutriconsultas.platform;

import org.springframework.lang.NonNull;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Component;

/**
 * Central authorization helper for platform admin surfaces. Delegates allowlist checks to
 * {@link PlatformAdminService} and records audit events (user IDs only).
 */
@Component
public class PlatformAdminAuthorization {

	private final PlatformAdminService platformAdminService;

	private final PlatformAdminAuditService platformAdminAuditService;

	public PlatformAdminAuthorization(final PlatformAdminService platformAdminService,
			final PlatformAdminAuditService platformAdminAuditService) {
		this.platformAdminService = platformAdminService;
		this.platformAdminAuditService = platformAdminAuditService;
	}

	public void requirePlatformAdmin(final OidcUser principal) {
		platformAdminService.requirePlatformAdmin(principal);
	}

	public void requirePlatformAdmin(final OidcUser principal, @NonNull final String action) {
		platformAdminService.requirePlatformAdmin(principal);
		platformAdminAuditService.recordAction(platformAdminService.resolveActorUserId(principal), action);
	}

}
