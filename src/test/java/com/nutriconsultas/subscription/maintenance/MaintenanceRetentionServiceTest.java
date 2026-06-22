package com.nutriconsultas.subscription.maintenance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.nutriconsultas.platform.PlatformAdminAuditService;
import com.nutriconsultas.subscription.maintenance.MaintenanceBackupSerializer.NutritionistTenantSnapshot;
import com.nutriconsultas.subscription.maintenance.RevokedNutritionistEligibilityService.EligibleRevokedNutritionist;

@ExtendWith(MockitoExtension.class)
class MaintenanceRetentionServiceTest {

	@Mock
	private MaintenanceRunRepository maintenanceRunRepository;

	@Mock
	private RevokedNutritionistEligibilityService eligibilityService;

	@Mock
	private NutritionistTenantPurgeService tenantPurgeService;

	@Mock
	private MaintenanceBackupSerializer backupSerializer;

	@Mock
	private MaintenanceBackupStorageService backupStorageService;

	@Mock
	private PlatformAdminAuditService platformAdminAuditService;

	private MaintenanceRetentionService maintenanceRetentionService;

	private MaintenanceRetentionProperties properties;

	@BeforeEach
	void setUp() {
		properties = new MaintenanceRetentionProperties();
		properties.setS3Prefix("maintenance/revoked-nutritionist-backups");
		maintenanceRetentionService = new MaintenanceRetentionService(maintenanceRunRepository, eligibilityService,
				tenantPurgeService, backupSerializer, backupStorageService, properties, platformAdminAuditService);
	}

	@Test
	void executeCleanup_whenNoEligible_completesWithoutUploadOrPurge() {
		when(eligibilityService.findEligible()).thenReturn(List.of());
		when(maintenanceRunRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		final MaintenanceRun run = maintenanceRetentionService.executeCleanup("auth0|admin");

		assertThat(run.getStatus()).isEqualTo(MaintenanceRunStatus.COMPLETED);
		assertThat(run.getEligibleCount()).isZero();
		assertThat(run.getPurgedUserCount()).isZero();
		verify(backupStorageService, never()).uploadBackup(anyString(), any());
		verify(tenantPurgeService, never()).purgeTenant(any());
	}

	@Test
	void executeCleanup_whenBackupFails_doesNotPurge() {
		final EligibleRevokedNutritionist eligible = new EligibleRevokedNutritionist("auth0|user", 5L,
				Instant.now().minusSeconds(8640000L));
		when(eligibilityService.findEligible()).thenReturn(List.of(eligible));
		when(maintenanceRunRepository.save(any())).thenAnswer(invocation -> {
			final MaintenanceRun run = invocation.getArgument(0);
			if (run.getRunId() == null) {
				run.setRunId("run-test-1");
			}
			return run;
		});
		when(tenantPurgeService.buildSnapshot(eligible))
			.thenReturn(NutritionistTenantSnapshot.empty("auth0|user", 5L, eligible.revokedAt()));
		when(backupSerializer.toGzippedJson(anyString(), any(), any())).thenReturn(new byte[] { 1, 2, 3 });
		doThrow(new MaintenanceBackupException("upload failed", new RuntimeException("s3"))).when(backupStorageService)
			.uploadBackup(anyString(), any());

		assertThatThrownBy(() -> maintenanceRetentionService.executeCleanup("auth0|admin"))
			.isInstanceOf(MaintenanceBackupException.class);
		verify(tenantPurgeService, never()).purgeTenant(any());
	}

	@Test
	void executeCleanup_happyPath_uploadsBackupThenPurges() {
		final EligibleRevokedNutritionist eligible = new EligibleRevokedNutritionist("auth0|user", 5L,
				Instant.now().minusSeconds(8640000L));
		when(eligibilityService.findEligible()).thenReturn(List.of(eligible));
		when(maintenanceRunRepository.save(any())).thenAnswer(invocation -> {
			final MaintenanceRun run = invocation.getArgument(0);
			if (run.getRunId() == null) {
				run.setRunId("run-test-2");
			}
			return run;
		});
		when(tenantPurgeService.buildSnapshot(eligible))
			.thenReturn(NutritionistTenantSnapshot.empty("auth0|user", 5L, eligible.revokedAt()));
		when(backupSerializer.toGzippedJson(anyString(), any(), any())).thenReturn(new byte[] { 9, 8, 7 });

		final MaintenanceRun run = maintenanceRetentionService.executeCleanup("auth0|admin");

		assertThat(run.getStatus()).isEqualTo(MaintenanceRunStatus.COMPLETED);
		assertThat(run.getEligibleCount()).isEqualTo(1);
		assertThat(run.getPurgedUserCount()).isEqualTo(1);
		assertThat(run.getS3BackupKey()).contains("run-test-2");
		verify(backupStorageService)
			.uploadBackup(eq("maintenance/revoked-nutritionist-backups/run-test-2/backup.json.gz"), any());
		verify(tenantPurgeService).purgeTenant(eligible);
		verify(platformAdminAuditService).recordAction(eq("auth0|admin"), anyString());
	}

	@Test
	void deleteBackup_removesS3ObjectAndClearsKey() {
		final MaintenanceRun run = new MaintenanceRun();
		run.setRunId("run-delete");
		final String backupKey = "maintenance/revoked-nutritionist-backups/run-delete/backup.json.gz";
		run.setS3BackupKey(backupKey);
		when(maintenanceRunRepository.findById("run-delete")).thenReturn(Optional.of(run));
		when(maintenanceRunRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		maintenanceRetentionService.deleteBackup("run-delete", "auth0|admin");

		verify(backupStorageService).deleteBackup(backupKey);
		assertThat(run.getS3BackupKey()).isNull();
		verify(platformAdminAuditService).recordAction(eq("auth0|admin"),
				eq("action=retention.backup.delete,runId=run-delete"));
	}

}
