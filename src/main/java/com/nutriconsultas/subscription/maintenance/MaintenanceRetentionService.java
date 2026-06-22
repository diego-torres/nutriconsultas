package com.nutriconsultas.subscription.maintenance;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.nutriconsultas.platform.PlatformAdminAuditService;
import com.nutriconsultas.subscription.maintenance.MaintenanceBackupSerializer.NutritionistTenantSnapshot;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class MaintenanceRetentionService {

	private final MaintenanceRunRepository maintenanceRunRepository;

	private final RevokedNutritionistEligibilityService eligibilityService;

	private final NutritionistTenantPurgeService tenantPurgeService;

	private final MaintenanceBackupSerializer backupSerializer;

	private final MaintenanceBackupStorageService backupStorageService;

	private final MaintenanceRetentionProperties properties;

	private final PlatformAdminAuditService platformAdminAuditService;

	public MaintenanceRetentionService(final MaintenanceRunRepository maintenanceRunRepository,
			final RevokedNutritionistEligibilityService eligibilityService,
			final NutritionistTenantPurgeService tenantPurgeService, final MaintenanceBackupSerializer backupSerializer,
			final MaintenanceBackupStorageService backupStorageService, final MaintenanceRetentionProperties properties,
			final PlatformAdminAuditService platformAdminAuditService) {
		this.maintenanceRunRepository = maintenanceRunRepository;
		this.eligibilityService = eligibilityService;
		this.tenantPurgeService = tenantPurgeService;
		this.backupSerializer = backupSerializer;
		this.backupStorageService = backupStorageService;
		this.properties = properties;
		this.platformAdminAuditService = platformAdminAuditService;
	}

	@Transactional(readOnly = true)
	public Optional<MaintenanceRun> findLatestRun() {
		return maintenanceRunRepository.findFirstByOrderByStartedAtDesc();
	}

	@Transactional(readOnly = true)
	public Page<MaintenanceRun> findRecentRuns(final int page, final int size) {
		return maintenanceRunRepository.findAllByOrderByStartedAtDesc(PageRequest.of(page, size));
	}

	@Transactional(readOnly = true)
	public Optional<MaintenanceRun> findRun(final String runId) {
		return maintenanceRunRepository.findById(runId);
	}

	@Transactional
	public MaintenanceRun executeCleanup(@Nullable final String actorUserId) {
		final MaintenanceRun run = new MaintenanceRun();
		run.setActorUserId(actorUserId);
		run.setStatus(MaintenanceRunStatus.RUNNING);
		maintenanceRunRepository.save(run);

		final List<RevokedNutritionistEligibilityService.EligibleRevokedNutritionist> eligible = eligibilityService
			.findEligible();
		run.setEligibleCount(eligible.size());

		if (eligible.isEmpty()) {
			return completeRun(run, 0, null);
		}

		try {
			final List<NutritionistTenantSnapshot> snapshots = new ArrayList<>();
			for (final RevokedNutritionistEligibilityService.EligibleRevokedNutritionist candidate : eligible) {
				snapshots.add(tenantPurgeService.buildSnapshot(candidate));
			}
			final Instant exportedAt = Instant.now();
			final byte[] backup = backupSerializer.toGzippedJson(run.getRunId(), exportedAt, snapshots);
			final String s3Key = buildBackupKey(run.getRunId());
			backupStorageService.uploadBackup(s3Key, backup);
			run.setS3BackupKey(s3Key);

			int purgedCount = 0;
			for (final RevokedNutritionistEligibilityService.EligibleRevokedNutritionist candidate : eligible) {
				tenantPurgeService.purgeTenant(candidate);
				purgedCount++;
			}
			run.setPurgedUserCount(purgedCount);
			if (StringUtils.hasText(actorUserId)) {
				platformAdminAuditService.recordAction(actorUserId, "action=retention.purge,runId=" + run.getRunId()
						+ ",eligibleCount=" + eligible.size() + ",purgedUserCount=" + purgedCount + ",s3Key=" + s3Key);
			}
			if (log.isInfoEnabled()) {
				log.info("Maintenance retention cleanup completed: runId={}, eligibleCount={}, purgedUserCount={}",
						run.getRunId(), eligible.size(), purgedCount);
			}
			return completeRun(run, purgedCount, null);
		}
		catch (RuntimeException ex) {
			run.setStatus(MaintenanceRunStatus.FAILED);
			run.setErrorSummary(truncate(ex.getMessage()));
			run.setCompletedAt(Instant.now());
			maintenanceRunRepository.save(run);
			if (log.isWarnEnabled()) {
				log.warn("Maintenance retention cleanup failed: runId={}", run.getRunId());
			}
			if (log.isDebugEnabled()) {
				log.debug("Maintenance retention cleanup failure", ex);
			}
			throw ex;
		}
	}

	@Transactional
	public void deleteBackup(final String runId, final String actorUserId) {
		final MaintenanceRun run = maintenanceRunRepository.findById(runId)
			.orElseThrow(() -> new IllegalArgumentException("Maintenance run not found"));
		if (!StringUtils.hasText(run.getS3BackupKey())) {
			throw new IllegalArgumentException("Run has no backup to delete");
		}
		backupStorageService.deleteBackup(run.getS3BackupKey());
		run.setS3BackupKey(null);
		maintenanceRunRepository.save(run);
		platformAdminAuditService.recordAction(actorUserId, "action=retention.backup.delete,runId=" + runId);
	}

	public Optional<String> resolveBackupDownloadUrl(final String runId) {
		return maintenanceRunRepository.findById(runId)
			.flatMap(run -> StringUtils.hasText(run.getS3BackupKey())
					? backupStorageService.createPresignedDownloadUrl(run.getS3BackupKey()) : Optional.empty());
	}

	private MaintenanceRun completeRun(final MaintenanceRun run, final int purgedCount, final String errorSummary) {
		run.setPurgedUserCount(purgedCount);
		run.setStatus(MaintenanceRunStatus.COMPLETED);
		run.setErrorSummary(errorSummary);
		run.setCompletedAt(Instant.now());
		return maintenanceRunRepository.save(run);
	}

	private String buildBackupKey(final String runId) {
		return properties.getS3Prefix() + "/" + runId + "/backup.json.gz";
	}

	private static String truncate(@Nullable final String message) {
		if (!StringUtils.hasText(message)) {
			return "Unknown error";
		}
		if (message.length() <= 500) {
			return message;
		}
		return message.substring(0, 500);
	}

}
