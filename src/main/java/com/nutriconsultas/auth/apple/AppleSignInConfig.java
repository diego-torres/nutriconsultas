package com.nutriconsultas.auth.apple;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

@Configuration
@EnableConfigurationProperties(AppleSignInProperties.class)
@Slf4j
public class AppleSignInConfig {

	private final AppleSignInProperties properties;

	public AppleSignInConfig(final AppleSignInProperties properties) {
		this.properties = properties;
	}

	@PostConstruct
	public void validateConfiguration() {
		if (!properties.isWebhookEnabled()) {
			if (log.isDebugEnabled()) {
				log.debug("Apple Sign-In webhook disabled (APPLE_SIGNIN_WEBHOOK_ENABLED=false)");
			}
			return;
		}
		if (!properties.isWebhookConfigured()) {
			throw new IllegalStateException(
					"Apple Sign-In webhook is enabled but APPLE_SIGNIN_EXPECTED_AUDIENCE is not configured");
		}
		if (log.isInfoEnabled()) {
			log.info("Apple Sign-In webhook enabled (issuer={}, destructiveAuto={})", properties.getExpectedIssuer(),
					properties.isAutoProcessDestructiveEvents());
		}
	}

}
