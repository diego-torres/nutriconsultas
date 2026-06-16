package com.nutriconsultas.subscription;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(SubscriptionProperties.class)
public class SubscriptionConfig {

}
