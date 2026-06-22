package com.nutriconsultas.subscription.maintenance;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "maintenance_run")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MaintenanceRun {

	@Id
	@Column(name = "run_id", nullable = false, length = 36)
	private String runId;

	@Column(name = "started_at", nullable = false)
	private Instant startedAt;

	@Column(name = "completed_at")
	private Instant completedAt;

	@Column(name = "actor_user_id", length = 255)
	private String actorUserId;

	@Column(name = "eligible_count", nullable = false)
	private int eligibleCount;

	@Column(name = "purged_user_count", nullable = false)
	private int purgedUserCount;

	@Column(name = "s3_backup_key", length = 512)
	private String s3BackupKey;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private MaintenanceRunStatus status = MaintenanceRunStatus.RUNNING;

	@Column(name = "error_summary", length = 500)
	private String errorSummary;

	@PrePersist
	void onCreate() {
		if (runId == null) {
			runId = UUID.randomUUID().toString();
		}
		if (startedAt == null) {
			startedAt = Instant.now();
		}
	}

}
