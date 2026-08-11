package com.nutriconsultas.device;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

@Configuration
@EnableConfigurationProperties(PatientPushProperties.class)
@Slf4j
public class PatientPushConfig {

	private final PatientPushProperties properties;

	public PatientPushConfig(final PatientPushProperties properties) {
		this.properties = properties;
	}

	@PostConstruct
	public void logPushConfiguration() {
		if (!properties.isEnabled()) {
			if (log.isDebugEnabled()) {
				log.debug("Patient push disabled (PUSH_ENABLED=false)");
			}
			return;
		}
		if (properties.isEnabledButMisconfigured()) {
			log.warn("Patient push is enabled (PUSH_ENABLED=true) but neither APNs nor FCM is configured. "
					+ "Push sends will no-op until credentials are set.");
			return;
		}
		if (log.isInfoEnabled()) {
			log.info("Patient push enabled (apnsConfigured={}, fcmConfigured={}, apnsProduction={})",
					properties.isApnsConfigured(), properties.isFcmConfigured(), properties.getApns().isProduction());
		}
	}

}
