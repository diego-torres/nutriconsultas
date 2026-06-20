package com.nutriconsultas.profile;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;

import javax.imageio.ImageIO;

/**
 * Computes PDF header logo display size for Flying Saucer.
 *
 * <p>
 * Flying Saucer maps image pixels to points 1:1 and ignores {@code max-width} /
 * {@code max-height}. Explicit {@code width}/{@code height} in points is required.
 */
public final class PdfLogoDimensions {

	/** 1.5 in at 72 pt/in — standard PDF header logo box. */
	public static final double MAX_SIZE_PT = 108.0;

	private PdfLogoDimensions() {
	}

	/**
	 * Display size in PDF points, preserving aspect ratio within {@link #MAX_SIZE_PT}.
	 *
	 * @param widthPt rendered width in points
	 * @param heightPt rendered height in points
	 */
	public record DisplaySize(double widthPt, double heightPt) {
	}

	/**
	 * Parses a {@code data:*;base64,...} URI and computes display dimensions.
	 * @param logoDataUri logo data URI (may be null)
	 * @return display size, or {@code null} when the URI is missing or unreadable
	 */
	public static DisplaySize computeFromDataUri(final String logoDataUri) {
		if (logoDataUri == null || logoDataUri.isBlank()) {
			return null;
		}
		final int commaIndex = logoDataUri.indexOf(',');
		if (commaIndex < 0) {
			return null;
		}
		try {
			final byte[] imageBytes = Base64.getDecoder().decode(logoDataUri.substring(commaIndex + 1));
			return computeFromImageBytes(imageBytes);
		}
		catch (final IllegalArgumentException e) {
			return null;
		}
	}

	/**
	 * Computes display dimensions from raw image bytes.
	 * @param imageBytes encoded image (PNG/JPEG/GIF)
	 * @return display size fitting inside {@link #MAX_SIZE_PT}, or max box when
	 * unreadable
	 */
	public static DisplaySize computeFromImageBytes(final byte[] imageBytes) {
		if (imageBytes == null || imageBytes.length == 0) {
			return new DisplaySize(MAX_SIZE_PT, MAX_SIZE_PT);
		}
		try {
			final BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageBytes));
			if (image == null) {
				return new DisplaySize(MAX_SIZE_PT, MAX_SIZE_PT);
			}
			return computeFromPixelSize(image.getWidth(), image.getHeight());
		}
		catch (final IOException e) {
			return new DisplaySize(MAX_SIZE_PT, MAX_SIZE_PT);
		}
	}

	/**
	 * Scales pixel dimensions to fit a {@link #MAX_SIZE_PT} square box (1 px → 1 pt in
	 * FS).
	 * @param widthPx intrinsic image width in pixels
	 * @param heightPx intrinsic image height in pixels
	 * @return display size in points
	 */
	public static DisplaySize computeFromPixelSize(final int widthPx, final int heightPx) {
		if (widthPx <= 0 || heightPx <= 0) {
			return new DisplaySize(MAX_SIZE_PT, MAX_SIZE_PT);
		}
		final double scale = Math.min(MAX_SIZE_PT / widthPx, MAX_SIZE_PT / heightPx);
		final double widthPt = roundPt(widthPx * scale);
		final double heightPt = roundPt(heightPx * scale);
		return new DisplaySize(widthPt, heightPt);
	}

	/**
	 * Inline CSS for Flying Saucer {@code <img>} tags.
	 * @param size display size in points (must not be null)
	 * @return CSS declaration string
	 */
	public static String toInlineStyle(final DisplaySize size) {
		return "width: " + size.widthPt() + "pt; height: " + size.heightPt() + "pt;";
	}

	private static double roundPt(final double value) {
		return Math.round(value * 100.0) / 100.0;
	}

}
