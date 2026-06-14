package com.nutriconsultas.platform;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

@ConfigurationProperties(prefix = "nutriconsultas.platform")
public class PlatformAdminProperties {

	private List<String> adminUserIds = new ArrayList<>();

	private List<String> adminEmails = new ArrayList<>();

	public List<String> getAdminUserIds() {
		return adminUserIds;
	}

	public void setAdminUserIds(final List<String> adminUserIds) {
		this.adminUserIds = adminUserIds != null ? adminUserIds : new ArrayList<>();
	}

	public List<String> getAdminEmails() {
		return adminEmails;
	}

	public void setAdminEmails(final List<String> adminEmails) {
		if (adminEmails == null) {
			this.adminEmails = new ArrayList<>();
			return;
		}
		this.adminEmails = adminEmails.stream()
			.filter(StringUtils::hasText)
			.map(email -> email.trim().toLowerCase(Locale.ROOT))
			.toList();
	}

}
