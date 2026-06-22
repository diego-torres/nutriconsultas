package com.nutriconsultas.subscription.maintenance;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

@Component
@ConfigurationProperties(prefix = "nutriconsultas.subscription.maintenance")
@Data
public class MaintenanceRetentionProperties {

	private int retentionDays = 90;

	private String s3Prefix = "maintenance/revoked-nutritionist-backups";

	private int presignedUrlMinutes = 15;

}
