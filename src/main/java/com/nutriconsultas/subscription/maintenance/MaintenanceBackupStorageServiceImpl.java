package com.nutriconsultas.subscription.maintenance;

import java.time.Duration;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

@Service
@Slf4j
public class MaintenanceBackupStorageServiceImpl implements MaintenanceBackupStorageService {

	private final MaintenanceRetentionProperties properties;

	private final String awsRegion;

	private final String bucketName;

	private final String accessKey;

	private final String secretKey;

	public MaintenanceBackupStorageServiceImpl(final MaintenanceRetentionProperties properties,
			@Value("${amazon.s3.region}") final String awsRegion, @Value("${amazon.s3.bucket}") final String bucketName,
			@Value("${amazon.s3.key}") final String accessKey, @Value("${amazon.s3.secret}") final String secretKey) {
		this.properties = properties;
		this.awsRegion = awsRegion;
		this.bucketName = bucketName;
		this.accessKey = accessKey;
		this.secretKey = secretKey;
	}

	@Override
	public void uploadBackup(final String s3Key, final byte[] gzippedPayload) {
		try (S3Client client = createClient()) {
			final PutObjectRequest request = PutObjectRequest.builder()
				.bucket(bucketName)
				.key(s3Key)
				.contentType("application/gzip")
				.build();
			client.putObject(request, RequestBody.fromBytes(gzippedPayload));
		}
		catch (S3Exception ex) {
			if (log.isWarnEnabled()) {
				log.warn("Maintenance backup upload failed for key={}", s3Key);
			}
			if (log.isDebugEnabled()) {
				log.debug("Maintenance backup upload failure", ex);
			}
			throw new MaintenanceBackupException("Failed to upload maintenance backup", ex);
		}
	}

	@Override
	public Optional<String> createPresignedDownloadUrl(final String s3Key) {
		if (!backupExists(s3Key)) {
			return Optional.empty();
		}
		try (S3Presigner presigner = createPresigner()) {
			final GetObjectRequest getRequest = GetObjectRequest.builder().bucket(bucketName).key(s3Key).build();
			final GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
				.getObjectRequest(getRequest)
				.signatureDuration(Duration.ofMinutes(properties.getPresignedUrlMinutes()))
				.build();
			return Optional.of(presigner.presignGetObject(presignRequest).url().toString());
		}
		catch (RuntimeException ex) {
			if (log.isWarnEnabled()) {
				log.warn("Failed to create presigned URL for maintenance backup key={}", s3Key);
			}
			if (log.isDebugEnabled()) {
				log.debug("Presigned URL failure", ex);
			}
			return Optional.empty();
		}
	}

	@Override
	public void deleteBackup(final String s3Key) {
		try (S3Client client = createClient()) {
			client.deleteObject(DeleteObjectRequest.builder().bucket(bucketName).key(s3Key).build());
		}
		catch (S3Exception ex) {
			if (log.isWarnEnabled()) {
				log.warn("Maintenance backup delete failed for key={}", s3Key);
			}
			if (log.isDebugEnabled()) {
				log.debug("Maintenance backup delete failure", ex);
			}
			throw new MaintenanceBackupException("Failed to delete maintenance backup", ex);
		}
	}

	@Override
	public boolean backupExists(final String s3Key) {
		try (S3Client client = createClient()) {
			client.headObject(HeadObjectRequest.builder().bucket(bucketName).key(s3Key).build());
			return true;
		}
		catch (NoSuchKeyException ex) {
			return false;
		}
		catch (S3Exception ex) {
			if (log.isDebugEnabled()) {
				log.debug("Maintenance backup head failure for key={}", s3Key, ex);
			}
			return false;
		}
	}

	private S3Client createClient() {
		return S3Client.builder().region(Region.of(awsRegion)).credentialsProvider(credentialsProvider()).build();
	}

	private S3Presigner createPresigner() {
		return S3Presigner.builder().region(Region.of(awsRegion)).credentialsProvider(credentialsProvider()).build();
	}

	private AwsCredentialsProvider credentialsProvider() {
		return () -> AwsBasicCredentials.create(accessKey, secretKey);
	}

}
