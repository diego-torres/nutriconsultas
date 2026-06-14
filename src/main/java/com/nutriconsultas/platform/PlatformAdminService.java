package com.nutriconsultas.platform;

import java.util.Locale;

import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PlatformAdminService {

	private final PlatformAdminProperties platformAdminProperties;

	public PlatformAdminService(final PlatformAdminProperties platformAdminProperties) {
		this.platformAdminProperties = platformAdminProperties;
	}

	public boolean isPlatformAdmin(final OidcUser principal) {
		if (principal == null) {
			return false;
		}
		return isPlatformAdminByUserId(principal.getSubject()) || isPlatformAdminByEmail(principal.getEmail());
	}

	public boolean isPlatformAdminByUserId(final String userId) {
		return StringUtils.hasText(userId) && platformAdminProperties.getAdminUserIds().contains(userId);
	}

	private boolean isPlatformAdminByEmail(final String email) {
		if (!StringUtils.hasText(email)) {
			return false;
		}
		final String normalized = email.trim().toLowerCase(Locale.ROOT);
		return platformAdminProperties.getAdminEmails().contains(normalized);
	}

}
