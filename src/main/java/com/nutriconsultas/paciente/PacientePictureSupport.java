package com.nutriconsultas.paciente;

import java.util.Locale;
import java.util.Set;

import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

/**
 * Resolves patient profile picture URLs and validates photo extensions (#529).
 */
public final class PacientePictureSupport {

	private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "webp");

	private PacientePictureSupport() {
	}

	public static boolean hasCustomPhoto(@Nullable final Paciente paciente) {
		return paciente != null && paciente.getPhotoExtension() != null && !paciente.getPhotoExtension().isBlank();
	}

	public static String normalizeExtension(@NonNull final String extension) {
		final String normalized = extension.trim().toLowerCase(Locale.ROOT);
		if (!ALLOWED_EXTENSIONS.contains(normalized)) {
			throw new IllegalArgumentException("Formato de imagen no permitido");
		}
		return normalized;
	}

	public static String adminPicturePath(@NonNull final Long pacienteId) {
		return "/admin/pacientes/" + pacienteId + "/picture";
	}

	public static String restPicturePath(@NonNull final Long pacienteId) {
		return "/rest/pacientes/" + pacienteId + "/picture";
	}

	public static String mobilePicturePath() {
		return "/rest/mobile/patient/profile/photo";
	}

	public static String resolveDisplayUrlForAdmin(@Nullable final Paciente paciente) {
		if (hasCustomPhoto(paciente)) {
			return adminPicturePath(paciente.getId());
		}
		return PacienteAvatarCatalog.resolveImagePath(paciente);
	}

	public static String resolveDisplayUrlForRest(@Nullable final Paciente paciente) {
		if (hasCustomPhoto(paciente)) {
			return restPicturePath(paciente.getId());
		}
		return PacienteAvatarCatalog.resolveImagePath(paciente);
	}

	public static String resolveDisplayUrlForMobile(@Nullable final Paciente paciente) {
		if (hasCustomPhoto(paciente)) {
			return mobilePicturePath();
		}
		return PacienteAvatarCatalog.resolveImagePath(paciente);
	}

	public static MediaType resolveMediaType(@NonNull final String extension) {
		return switch (extension.toLowerCase(Locale.ROOT)) {
			case "jpg", "jpeg" -> MediaType.IMAGE_JPEG;
			case "gif" -> MediaType.IMAGE_GIF;
			case "webp" -> MediaType.parseMediaType("image/webp");
			default -> MediaType.IMAGE_PNG;
		};
	}

	public static String buildPhotoKey(@NonNull final Long pacienteId, @NonNull final String extension) {
		return "patients/" + pacienteId + "/photo." + normalizeExtension(extension);
	}

}
