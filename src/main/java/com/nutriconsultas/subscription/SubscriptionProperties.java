package com.nutriconsultas.subscription;

import java.util.EnumSet;
import java.util.Set;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Subscription enforcement configuration. Grace denied entitlements default to blocking
 * PDF export and user administration while allowing read-only access per plan doc.
 */
@ConfigurationProperties(prefix = "nutriconsultas.subscription")
public class SubscriptionProperties {

	private Set<Entitlement> graceDeniedEntitlements = EnumSet.of(Entitlement.PDF_EXPORT,
			Entitlement.USER_ADMINISTRATION);

	public Set<Entitlement> getGraceDeniedEntitlements() {
		return graceDeniedEntitlements;
	}

	public void setGraceDeniedEntitlements(final Set<Entitlement> graceDeniedEntitlements) {
		if (graceDeniedEntitlements == null || graceDeniedEntitlements.isEmpty()) {
			this.graceDeniedEntitlements = EnumSet.noneOf(Entitlement.class);
			return;
		}
		this.graceDeniedEntitlements = EnumSet.copyOf(graceDeniedEntitlements);
	}

}
