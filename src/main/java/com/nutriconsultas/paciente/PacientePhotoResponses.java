package com.nutriconsultas.paciente;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;

/**
 * Builds HTTP responses for patient profile picture resolver endpoints (#529).
 */
public final class PacientePhotoResponses {

	private PacientePhotoResponses() {
	}

	public static ResponseEntity<byte[]> buildPictureResponse(@NonNull final Paciente paciente,
			@NonNull final PacientePhotoService pacientePhotoService) {
		if (!PacientePictureSupport.hasCustomPhoto(paciente)) {
			return ResponseEntity.status(HttpStatus.FOUND)
				.header(HttpHeaders.LOCATION, PacienteAvatarCatalog.resolveImagePath(paciente))
				.build();
		}
		final byte[] bytes = pacientePhotoService.getPhotoBytes(paciente.getId());
		if (bytes == null) {
			return ResponseEntity.status(HttpStatus.FOUND)
				.header(HttpHeaders.LOCATION, PacienteAvatarCatalog.resolveImagePath(paciente))
				.build();
		}
		return ResponseEntity.ok()
			.cacheControl(CacheControl.noStore())
			.contentType(PacientePictureSupport.resolveMediaType(paciente.getPhotoExtension()))
			.body(bytes);
	}

}
