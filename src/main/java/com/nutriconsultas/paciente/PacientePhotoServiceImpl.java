package com.nutriconsultas.paciente;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Service
@Slf4j
public class PacientePhotoServiceImpl implements PacientePhotoService {

	@Autowired
	private PacienteRepository pacienteRepository;

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
	public void savePhotoForNutritionist(@NonNull final Long pacienteId, @NonNull final String userId,
			@NonNull final byte[] bytes, @NonNull final String fileExtension) {
		final Paciente paciente = requireOwnedPaciente(pacienteId, userId);
		uploadPhoto(paciente, bytes, fileExtension);
	}

	@Override
	@Transactional
	public void savePhotoForPatient(@NonNull final Long pacienteId, @NonNull final byte[] bytes,
			@NonNull final String fileExtension) {
		final Paciente paciente = pacienteRepository.findById(pacienteId)
			.orElseThrow(() -> new IllegalArgumentException("Paciente no encontrado"));
		uploadPhoto(paciente, bytes, fileExtension);
	}

	@Override
	@Transactional
	public void deletePhotoForNutritionist(@NonNull final Long pacienteId, @NonNull final String userId) {
		final Paciente paciente = requireOwnedPaciente(pacienteId, userId);
		clearPhoto(paciente);
	}

	@Override
	@Transactional
	public void deletePhotoForPatient(@NonNull final Long pacienteId) {
		final Paciente paciente = pacienteRepository.findById(pacienteId)
			.orElseThrow(() -> new IllegalArgumentException("Paciente no encontrado"));
		clearPhoto(paciente);
	}

	@Override
	@Transactional(readOnly = true)
	@Nullable
	public byte[] getPhotoBytes(@NonNull final Long pacienteId) {
		final String extension = getPhotoExtension(pacienteId);
		if (extension == null) {
			return null;
		}
		final String key = PacientePictureSupport.buildPhotoKey(pacienteId, extension);
		final S3Client s3Client = getClient();
		try {
			return s3Client.getObject(GetObjectRequest.builder().bucket(bucketName).key(key).build()).readAllBytes();
		}
		catch (final NoSuchKeyException e) {
			log.warn("Patient photo missing in S3 for patient id {}", pacienteId);
			return null;
		}
		catch (final Exception e) {
			log.error("Error retrieving patient photo from S3 for patient id {}", pacienteId, e);
			return null;
		}
	}

	@Override
	@Transactional(readOnly = true)
	@Nullable
	public String getPhotoExtension(@NonNull final Long pacienteId) {
		return pacienteRepository.findById(pacienteId)
			.map(Paciente::getPhotoExtension)
			.filter(ext -> ext != null && !ext.isBlank())
			.orElse(null);
	}

	@Override
	public void deletePhotoFromStorage(@NonNull final Long pacienteId, @Nullable final String extension) {
		if (extension == null || extension.isBlank()) {
			return;
		}
		final String key = PacientePictureSupport.buildPhotoKey(pacienteId, extension);
		final S3Client s3Client = getClient();
		try {
			if (keyExists(s3Client, key)) {
				s3Client.deleteObject(builder -> builder.bucket(bucketName).key(key));
			}
		}
		catch (final S3Exception e) {
			log.error("Error deleting patient photo from S3 for patient id {}", pacienteId, e);
		}
	}

	private Paciente requireOwnedPaciente(final Long pacienteId, final String userId) {
		return pacienteRepository.findByIdAndUserId(pacienteId, userId)
			.orElseThrow(() -> new IllegalArgumentException("Paciente no encontrado"));
	}

	private void uploadPhoto(final Paciente paciente, final byte[] bytes, final String fileExtension) {
		final String extension = PacientePictureSupport.normalizeExtension(fileExtension);
		final Long pacienteId = paciente.getId();
		log.info("Uploading profile photo for patient id {}", pacienteId);
		final String key = PacientePictureSupport.buildPhotoKey(pacienteId, extension);
		final S3Client s3Client = getClient();
		try {
			if (paciente.getPhotoExtension() != null && !paciente.getPhotoExtension().equals(extension)) {
				deletePhotoFromStorage(pacienteId, paciente.getPhotoExtension());
			}
			else if (keyExists(s3Client, key)) {
				s3Client.deleteObject(builder -> builder.bucket(bucketName).key(key));
			}
			s3Client.putObject(PutObjectRequest.builder().bucket(bucketName).key(key).build(),
					RequestBody.fromBytes(bytes));
			paciente.setPhotoExtension(extension);
			pacienteRepository.save(paciente);
			log.info("Profile photo uploaded for patient id {}", pacienteId);
		}
		catch (final S3Exception e) {
			log.error("Error uploading patient photo to S3 for patient id {}", pacienteId, e);
			throw new IllegalStateException("Error al guardar la foto del paciente", e);
		}
	}

	private void clearPhoto(final Paciente paciente) {
		final Long pacienteId = paciente.getId();
		deletePhotoFromStorage(pacienteId, paciente.getPhotoExtension());
		paciente.setPhotoExtension(null);
		pacienteRepository.save(paciente);
		log.info("Removed custom profile photo for patient id {}", pacienteId);
	}

	private boolean keyExists(final S3Client s3Client, final String key) {
		try {
			s3Client.headObject(HeadObjectRequest.builder().bucket(bucketName).key(key).build());
			return true;
		}
		catch (final NoSuchKeyException e) {
			return false;
		}
	}

	private S3Client getClient() {
		final AwsCredentialsProvider credentialsProvider = () -> AwsBasicCredentials.create(accessKey, secretKey);
		return S3Client.builder().region(Region.of(awsRegion)).credentialsProvider(credentialsProvider).build();
	}

}
