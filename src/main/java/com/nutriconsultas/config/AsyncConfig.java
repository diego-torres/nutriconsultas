package com.nutriconsultas.config;

import java.util.concurrent.Executor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Async execution for best-effort side effects such as patient push (#576).
 */
@Configuration
@EnableAsync
public class AsyncConfig {

	@Bean(name = "patientPushExecutor")
	public Executor patientPushExecutor() {
		final ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(2);
		executor.setMaxPoolSize(8);
		executor.setQueueCapacity(200);
		executor.setThreadNamePrefix("patient-push-");
		executor.initialize();
		return executor;
	}

}
