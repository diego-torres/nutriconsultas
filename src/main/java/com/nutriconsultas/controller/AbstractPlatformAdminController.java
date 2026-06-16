package com.nutriconsultas.controller;

import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import com.nutriconsultas.platform.PlatformAdminAuthorization;

/**
 * Base controller for {@code /admin/platform/**} routes. Enforces the platform admin
 * allowlist on every mutating or sensitive action.
 */
public abstract class AbstractPlatformAdminController extends AbstractAuthorizedController {

	protected final PlatformAdminAuthorization platformAdminAuthorization;

	protected AbstractPlatformAdminController(final PlatformAdminAuthorization platformAdminAuthorization) {
		this.platformAdminAuthorization = platformAdminAuthorization;
	}

	protected void requirePlatformAdmin(final OidcUser principal, final String action) {
		platformAdminAuthorization.requirePlatformAdmin(principal, action);
	}

}
