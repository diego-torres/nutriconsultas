package com.nutriconsultas.subscription;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Subscription enforcement configuration. Grace denied entitlements default to blocking
 * new patients, PDF export and user administration while allowing read-only access per
 * plan doc.
 */
@ConfigurationProperties(prefix = "nutriconsultas.subscription")
public class SubscriptionProperties {

	private int defaultGracePeriodDays = 7;

	private List<Integer> expiryReminderDays = List.of(7, 3, 1);

	private boolean lifecycleJobEnabled = true;

	private Set<Entitlement> graceDeniedEntitlements = EnumSet.of(Entitlement.CREATE_PATIENT, Entitlement.PDF_EXPORT,
			Entitlement.USER_ADMINISTRATION);

	private boolean enforceNutritionistAccess = true;

	public int getDefaultGracePeriodDays() {
		return defaultGracePeriodDays;
	}

	public void setDefaultGracePeriodDays(final int defaultGracePeriodDays) {
		this.defaultGracePeriodDays = defaultGracePeriodDays;
	}

	public List<Integer> getExpiryReminderDays() {
		return expiryReminderDays;
	}

	public void setExpiryReminderDays(final List<Integer> expiryReminderDays) {
		if (expiryReminderDays == null || expiryReminderDays.isEmpty()) {
			this.expiryReminderDays = List.of();
			return;
		}
		this.expiryReminderDays = List.copyOf(expiryReminderDays);
	}

	public boolean isLifecycleJobEnabled() {
		return lifecycleJobEnabled;
	}

	public void setLifecycleJobEnabled(final boolean lifecycleJobEnabled) {
		this.lifecycleJobEnabled = lifecycleJobEnabled;
	}

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

	public boolean isEnforceNutritionistAccess() {
		return enforceNutritionistAccess;
	}

	public void setEnforceNutritionistAccess(final boolean enforceNutritionistAccess) {
		this.enforceNutritionistAccess = enforceNutritionistAccess;
	}

}
