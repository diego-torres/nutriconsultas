package com.nutriconsultas.dieta;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;

/**
 * Resolves patient-facing platillo picture URLs and S3 object locations (#598).
 */
public final class PlatilloIngestaPictureSupport {

	public static final String PLACEHOLDER_IMAGE_PATH = "/sbadmin/img/plato-vacio.jpg";

	private static final Pattern S3_KEY_PATTERN = Pattern.compile("^/?platillo/(\\d+)/([^/]+)$");

	private static final Pattern ADMIN_PATH_PATTERN = Pattern.compile("^/admin/platillos/platillo/(\\d+)/([^/]+)$");

	private PlatilloIngestaPictureSupport() {
	}

	public static boolean hasCustomPicture(@Nullable final PlatilloIngesta platillo) {
		boolean custom = false;
		if (platillo != null) {
			final String stored = platillo.getStoredImageUrl();
			custom = stored != null && !stored.isBlank() && !PLACEHOLDER_IMAGE_PATH.equals(stored.trim());
		}
		return custom;
	}

	public static String resolveDisplayUrlForMobile(@Nullable final Long assignmentId,
			@Nullable final PlatilloIngesta platillo) {
		String imageUrl = PLACEHOLDER_IMAGE_PATH;
		if (hasCustomPicture(platillo) && assignmentId != null && platillo.getId() != null) {
			imageUrl = mobilePicturePath(assignmentId, platillo.getId());
		}
		return imageUrl;
	}

	public static String mobilePicturePath(final Long assignmentId, final Long platilloIngestaId) {
		return "/rest/mobile/patient/diet-plans/" + assignmentId + "/platillos/" + platilloIngestaId + "/image";
	}

	@Nullable
	public static PictureObject resolvePictureObject(@Nullable final PlatilloIngesta platillo) {
		PictureObject picture = null;
		if (hasCustomPicture(platillo)) {
			picture = parseStoredPicture(platillo, platillo.getStoredImageUrl().trim());
		}
		return picture;
	}

	private static PictureObject parseStoredPicture(final PlatilloIngesta platillo, final String stored) {
		PictureObject picture = null;
		if (!stored.contains("..")) {
			picture = firstNonNullPicture(parseS3Key(stored), parseAdminPath(stored),
					parseSourcePlatillo(platillo, stored));
		}
		return picture;
	}

	private static PictureObject firstNonNullPicture(final PictureObject s3Key, final PictureObject adminPath,
			final PictureObject sourcePlatillo) {
		PictureObject picture = s3Key;
		if (picture == null) {
			picture = adminPath;
		}
		if (picture == null) {
			picture = sourcePlatillo;
		}
		return picture;
	}

	public static MediaType resolveMediaType(@Nullable final String fileName) {
		final String extension = extractExtension(fileName);
		return switch (extension) {
			case "jpg", "jpeg" -> MediaType.IMAGE_JPEG;
			case "gif" -> MediaType.IMAGE_GIF;
			case "webp" -> MediaType.parseMediaType("image/webp");
			default -> MediaType.IMAGE_PNG;
		};
	}

	private static PictureObject parseS3Key(final String stored) {
		PictureObject picture = null;
		final Matcher matcher = S3_KEY_PATTERN.matcher(stored);
		if (matcher.matches() && isSafeFileName(matcher.group(2))) {
			picture = new PictureObject(Long.parseLong(matcher.group(1)), matcher.group(2));
		}
		return picture;
	}

	private static PictureObject parseAdminPath(final String stored) {
		PictureObject picture = null;
		final Matcher matcher = ADMIN_PATH_PATTERN.matcher(stored);
		if (matcher.matches() && isSafeFileName(matcher.group(2))) {
			picture = new PictureObject(Long.parseLong(matcher.group(1)), matcher.group(2));
		}
		return picture;
	}

	private static PictureObject parseSourcePlatillo(final PlatilloIngesta platillo, final String stored) {
		PictureObject picture = null;
		if (platillo.getSourcePlatilloId() != null) {
			final String fileName = extractFileName(stored);
			if (isSafeFileName(fileName)) {
				picture = new PictureObject(platillo.getSourcePlatilloId(), fileName);
			}
		}
		return picture;
	}

	private static String extractFileName(final String imageUrl) {
		final int slashIndex = imageUrl.lastIndexOf('/');
		String fileName = imageUrl;
		if (slashIndex >= 0 && slashIndex < imageUrl.length() - 1) {
			fileName = imageUrl.substring(slashIndex + 1);
		}
		return fileName;
	}

	private static boolean isSafeFileName(final String fileName) {
		return fileName != null && !fileName.isBlank() && !fileName.contains("..") && !fileName.contains("/")
				&& !fileName.contains("\\");
	}

	private static String extractExtension(final String fileName) {
		String extension = "png";
		if (fileName != null) {
			final int dotIndex = fileName.lastIndexOf('.');
			if (dotIndex > 0 && dotIndex < fileName.length() - 1) {
				extension = fileName.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
			}
		}
		return extension;
	}

	/**
	 * Catalog platillo S3 object referenced by a diet-plan platillo picture.
	 */
	public record PictureObject(Long catalogPlatilloId, String fileName) {
	}

}
