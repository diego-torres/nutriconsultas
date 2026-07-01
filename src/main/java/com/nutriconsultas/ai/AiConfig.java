package com.nutriconsultas.ai;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

@Configuration
@EnableConfigurationProperties(AiProperties.class)
@Slf4j
public class AiConfig {

	private final AiProperties properties;

	public AiConfig(final AiProperties properties) {
		this.properties = properties;
	}

	@PostConstruct
	public void logAiConfiguration() {
		if (!properties.isEnabled()) {
			if (log.isDebugEnabled()) {
				log.debug("AI assistant disabled (AI_ENABLED=false)");
			}
			return;
		}
		if (properties.isEnabledButMisconfigured()) {
			log.warn("AI assistant is enabled (AI_ENABLED=true) but OpenAI is not fully configured "
					+ "(OPENAI_API_KEY or OPENAI_MODEL missing). AI endpoints will respond with 503 until fixed.");
			return;
		}
		if (log.isInfoEnabled()) {
			log.info("AI assistant enabled (model={}, openai.store={}, maxToolCalls={})",
					properties.getOpenai().getModel(), properties.getOpenai().isStore(), properties.getMaxToolCalls());
		}
	}

}
