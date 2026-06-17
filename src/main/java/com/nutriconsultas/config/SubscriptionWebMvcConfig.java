package com.nutriconsultas.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.nutriconsultas.subscription.lifecycle.SubscriptionAccessInterceptor;

@Configuration
public class SubscriptionWebMvcConfig implements WebMvcConfigurer {

	private final SubscriptionAccessInterceptor subscriptionAccessInterceptor;

	public SubscriptionWebMvcConfig(final SubscriptionAccessInterceptor subscriptionAccessInterceptor) {
		this.subscriptionAccessInterceptor = subscriptionAccessInterceptor;
	}

	@Override
	public void addInterceptors(final InterceptorRegistry registry) {
		registry.addInterceptor(subscriptionAccessInterceptor).addPathPatterns("/admin/**");
	}

}
