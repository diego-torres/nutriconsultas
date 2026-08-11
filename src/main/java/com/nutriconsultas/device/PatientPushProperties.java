package com.nutriconsultas.device;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

/**
 * APNs + FCM HTTP v1 push configuration (#575). Secrets must never be logged.
 */
@ConfigurationProperties(prefix = "nutriconsultas.push")
public class PatientPushProperties {

	private boolean enabled;

	private Apns apns = new Apns();

	private Fcm fcm = new Fcm();

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(final boolean enabled) {
		this.enabled = enabled;
	}

	public Apns getApns() {
		return apns;
	}

	public void setApns(final Apns apns) {
		this.apns = apns != null ? apns : new Apns();
	}

	public Fcm getFcm() {
		return fcm;
	}

	public void setFcm(final Fcm fcm) {
		this.fcm = fcm != null ? fcm : new Fcm();
	}

	public boolean isApnsConfigured() {
		return StringUtils.hasText(apns.getKeyId()) && StringUtils.hasText(apns.getTeamId())
				&& StringUtils.hasText(apns.getBundleId()) && StringUtils.hasText(apns.getP8Key());
	}

	public boolean isFcmConfigured() {
		return StringUtils.hasText(fcm.getProjectId()) && StringUtils.hasText(fcm.getServiceAccountJson());
	}

	/**
	 * {@code true} when the feature flag is on and at least one platform is configured.
	 */
	public boolean isOperational() {
		return enabled && (isApnsConfigured() || isFcmConfigured());
	}

	public boolean isEnabledButMisconfigured() {
		return enabled && !isApnsConfigured() && !isFcmConfigured();
	}

	public static class Apns {

		private String keyId = "";

		private String teamId = "";

		private String bundleId = "";

		private String p8Key = "";

		private boolean production;

		private int connectTimeoutMs = 5_000;

		private int readTimeoutMs = 15_000;

		public String getKeyId() {
			return keyId;
		}

		public void setKeyId(final String keyId) {
			this.keyId = trimToEmpty(keyId);
		}

		public String getTeamId() {
			return teamId;
		}

		public void setTeamId(final String teamId) {
			this.teamId = trimToEmpty(teamId);
		}

		public String getBundleId() {
			return bundleId;
		}

		public void setBundleId(final String bundleId) {
			this.bundleId = trimToEmpty(bundleId);
		}

		public String getP8Key() {
			return p8Key;
		}

		public void setP8Key(final String p8Key) {
			this.p8Key = p8Key != null ? p8Key.trim() : "";
		}

		public boolean isProduction() {
			return production;
		}

		public void setProduction(final boolean production) {
			this.production = production;
		}

		public int getConnectTimeoutMs() {
			return connectTimeoutMs;
		}

		public void setConnectTimeoutMs(final int connectTimeoutMs) {
			this.connectTimeoutMs = Math.max(connectTimeoutMs, 1_000);
		}

		public int getReadTimeoutMs() {
			return readTimeoutMs;
		}

		public void setReadTimeoutMs(final int readTimeoutMs) {
			this.readTimeoutMs = Math.max(readTimeoutMs, 1_000);
		}

		public String resolveHost() {
			return production ? "api.push.apple.com" : "api.sandbox.push.apple.com";
		}

	}

	public static class Fcm {

		private String projectId = "";

		private String serviceAccountJson = "";

		private int connectTimeoutMs = 5_000;

		private int readTimeoutMs = 15_000;

		public String getProjectId() {
			return projectId;
		}

		public void setProjectId(final String projectId) {
			this.projectId = trimToEmpty(projectId);
		}

		public String getServiceAccountJson() {
			return serviceAccountJson;
		}

		public void setServiceAccountJson(final String serviceAccountJson) {
			this.serviceAccountJson = serviceAccountJson != null ? serviceAccountJson.trim() : "";
		}

		public int getConnectTimeoutMs() {
			return connectTimeoutMs;
		}

		public void setConnectTimeoutMs(final int connectTimeoutMs) {
			this.connectTimeoutMs = Math.max(connectTimeoutMs, 1_000);
		}

		public int getReadTimeoutMs() {
			return readTimeoutMs;
		}

		public void setReadTimeoutMs(final int readTimeoutMs) {
			this.readTimeoutMs = Math.max(readTimeoutMs, 1_000);
		}

	}

	private static String trimToEmpty(final String value) {
		return StringUtils.hasText(value) ? value.trim() : "";
	}

}
