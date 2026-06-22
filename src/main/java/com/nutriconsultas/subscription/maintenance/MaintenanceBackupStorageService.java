package com.nutriconsultas.subscription.maintenance;

import java.util.Optional;

public interface MaintenanceBackupStorageService {

	void uploadBackup(String s3Key, byte[] gzippedPayload);

	Optional<String> createPresignedDownloadUrl(String s3Key);

	void deleteBackup(String s3Key);

	boolean backupExists(String s3Key);

}
