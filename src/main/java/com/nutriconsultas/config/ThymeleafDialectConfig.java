package com.nutriconsultas.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ThymeleafDialectConfig {

	@Bean
	public ImcGaugeDialect imcGaugeDialect() {
		return new ImcGaugeDialect();
	}

	@Bean
	public AnthropometricVisualAidDialect anthropometricVisualAidDialect() {
		return new AnthropometricVisualAidDialect();
	}

}
