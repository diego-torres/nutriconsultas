package com.nutriconsultas.profile;

import java.util.Base64;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nutriconsultas.util.LogRedaction;

import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

/**
 * Implementation of {@link NutritionistProfileService}.
 *
 * <p>
 * Profile is lazily created on first access. Logo images are stored in and retrieved from
 * S3 using the same credentials pattern as {@code PlatilloServiceImpl}.
 */
@Service
@Slf4j
public class NutritionistProfileServiceImpl implements NutritionistProfileService {

	private static final String LOGO_KEY_PREFIX = "profile/";

	private static final String LOGO_FILE_NAME = "/logo.";

	@Autowired
	private NutritionistProfileRepository repository;

	@Value("${amazon.s3.region}")
	private String awsRegion;

	@Value("${amazon.s3.bucket}")
	private String bucketName;

	@Value("${amazon.s3.key}")
	private String accessKey;

	@Value("${amazon.s3.secret}")
	private String secretKey;

	@Override
	@Transactional
	@NonNull
	public NutritionistProfile getOrCreateProfile(@NonNull final String userId) {
		log.info("Retrieving profile for user id: {}", LogRedaction.redactUserId(userId));
		return repository.findByUserId(userId).orElseGet(() -> {
			log.info("No profile found, creating empty profile for user id: {}", LogRedaction.redactUserId(userId));
			final NutritionistProfile emptyProfile = new NutritionistProfile();
			emptyProfile.setUserId(userId);
			return repository.save(emptyProfile);
		});
	}

	@Override
	@Transactional
	@NonNull
	public NutritionistProfile saveProfile(@NonNull final NutritionistProfile profile, @NonNull final String userId) {
		log.info("Saving profile for user id: {}", LogRedaction.redactUserId(userId));
		final NutritionistProfile existing = getOrCreateProfile(userId);
		existing.setDisplayName(profile.getDisplayName());
		existing.setCedulaProfesional(profile.getCedulaProfesional());
		final NutritionistProfile saved = repository.save(existing);
		log.info("Profile saved for user id: {}", LogRedaction.redactUserId(userId));
		return saved;
	}

	@Override
	public void saveLogo(@NonNull final String userId, @NonNull final byte[] bytes,
			@NonNull final String fileExtension) {
		log.info("Uploading logo for user id: {}", LogRedaction.redactUserId(userId));
		final NutritionistProfile profile = getOrCreateProfile(userId);
		final String key = buildLogoKey(userId, fileExtension);
		final S3Client s3Client = getClient();
		try {
			if (keyExists(key)) {
				log.debug("Deleting existing logo with key: {}", key);
				s3Client.deleteObject(builder -> builder.bucket(bucketName).key(key));
			}
			s3Client.putObject(PutObjectRequest.builder().bucket(bucketName).key(key).build(),
					RequestBody.fromBytes(bytes));
			profile.setLogoExtension(fileExtension);
			repository.save(profile);
			log.info("Logo uploaded successfully for user id: {}", LogRedaction.redactUserId(userId));
		}
		catch (final S3Exception e) {
			log.error("Error uploading logo to S3 for user id: {}", LogRedaction.redactUserId(userId), e);
		}
	}

	@Override
	@Nullable
	public byte[] getLogo(@NonNull final String userId) {
		log.debug("Retrieving logo for user id: {}", LogRedaction.redactUserId(userId));
		final NutritionistProfile profile = repository.findByUserId(userId).orElse(null);
		if (profile == null || profile.getLogoExtension() == null) {
			log.debug("No logo found for user id: {}", LogRedaction.redactUserId(userId));
			return null;
		}
		final String key = buildLogoKey(userId, profile.getLogoExtension());
		final S3Client s3Client = getClient();
		try {
			return s3Client.getObject(builder -> builder.bucket(bucketName).key(key)).readAllBytes();
		}
		catch (final Exception e) {
			log.error("Error retrieving logo from S3 for user id: {}", LogRedaction.redactUserId(userId), e);
			return null;
		}
	}

	@Override
	@Nullable
	public String getLogoAsBase64DataUri(@NonNull final String userId) {
		final NutritionistProfile profile = repository.findByUserId(userId).orElse(null);
		if (profile == null || profile.getLogoExtension() == null) {
			return null;
		}
		final byte[] logoBytes = getLogo(userId);
		if (logoBytes == null) {
			return null;
		}
		final String mimeType = resolveMimeType(profile.getLogoExtension());
		final String base64 = Base64.getEncoder().encodeToString(logoBytes);
		return "data:" + mimeType + ";base64," + base64;
	}

	private String buildLogoKey(final String userId, final String extension) {
		return LOGO_KEY_PREFIX + userId + LOGO_FILE_NAME + extension;
	}

	private String resolveMimeType(final String extension) {
		if ("jpg".equalsIgnoreCase(extension) || "jpeg".equalsIgnoreCase(extension)) {
			return "image/jpeg";
		}
		if ("gif".equalsIgnoreCase(extension)) {
			return "image/gif";
		}
		return "image/png";
	}

	private boolean keyExists(final String key) {
		final S3Client s3Client = getClient();
		try {
			final HeadObjectRequest headObjectRequest = HeadObjectRequest.builder().bucket(bucketName).key(key).build();
			final HeadObjectResponse headObjectResponse = s3Client.headObject(headObjectRequest);
			return headObjectResponse.contentType().length() > 0;
		}
		catch (final AwsServiceException | SdkClientException e) {
			return false;
		}
	}

	private S3Client getClient() {
		return S3Client.builder().region(Region.of(awsRegion)).credentialsProvider(new AwsCredentialsProvider() {
			@Override
			public software.amazon.awssdk.auth.credentials.AwsCredentials resolveCredentials() {
				return AwsBasicCredentials.create(accessKey, secretKey);
			}
		}).build();
	}

}
