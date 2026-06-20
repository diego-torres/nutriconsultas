package com.nutriconsultas.profile;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.thymeleaf.context.Context;

/**
 * Resolves nutritionist branding fields for PDF report templates.
 *
 * <p>
 * Display name priority: profile {@code displayName} → OIDC {@code name} claim → omit.
 */
public final class NutritionistBrandingHelper {

	/** Maximum logo width/height in PDF report headers (~1.5 × 1.5 in box). */
	public static final String PDF_LOGO_MAX_SIZE = "1.5in";

	/** Header table cell width reserved for the logo column. */
	public static final String PDF_LOGO_CELL_WIDTH = "1.6in";

	/** Minimal 1×1 PNG data URI for template validation and tests. */
	public static final String MOCK_LOGO_DATA_URI = "data:image/png;base64,"
			+ "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==";

	private NutritionistBrandingHelper() {
	}

	/**
	 * Inline CSS for PDF logo images: fits within {@link #PDF_LOGO_MAX_SIZE} while
	 * preserving aspect ratio (Flying Saucer compatible).
	 * @return CSS declaration string for {@code <img>} style attribute
	 */
	public static String getPdfLogoImgStyle() {
		return "max-width: " + PDF_LOGO_MAX_SIZE + "; max-height: " + PDF_LOGO_MAX_SIZE
				+ "; width: auto; height: auto;";
	}

	/**
	 * Adds nutritionist profile, logo, and resolved display name to a Thymeleaf context.
	 * @param context the Thymeleaf context
	 * @param profile the nutritionist profile (may be null)
	 * @param logoBase64 the logo as a Base64 data URI (may be null)
	 * @param oauthDisplayName fallback display name from OAuth (may be null)
	 */
	public static void addBrandingVariables(final Context context, final NutritionistProfile profile,
			final String logoBase64, final String oauthDisplayName) {
		context.setVariable("nutritionistProfile", profile);
		context.setVariable("logoBase64", logoBase64);
		context.setVariable("nutritionistDisplayName", resolveDisplayName(profile, oauthDisplayName));
	}

	/**
	 * Adds branding variables using the current security context for OAuth name fallback.
	 * @param context the Thymeleaf context
	 * @param profile the nutritionist profile (may be null)
	 * @param logoBase64 the logo as a Base64 data URI (may be null)
	 */
	public static void addBrandingVariables(final Context context, final NutritionistProfile profile,
			final String logoBase64) {
		addBrandingVariables(context, profile, logoBase64, resolveOAuthDisplayNameFromSecurityContext());
	}

	/**
	 * Resolves the nutritionist display name for PDF headers.
	 * @param profile the nutritionist profile (may be null)
	 * @param oauthDisplayName fallback from OIDC {@code name} claim (may be null)
	 * @return resolved name, or {@code null} when no source is available
	 */
	public static String resolveDisplayName(final NutritionistProfile profile, final String oauthDisplayName) {
		if (profile != null && profile.getDisplayName() != null && !profile.getDisplayName().isBlank()) {
			return profile.getDisplayName().trim();
		}
		if (oauthDisplayName != null && !oauthDisplayName.isBlank()) {
			return oauthDisplayName.trim();
		}
		return null;
	}

	/**
	 * Extracts a display name from an OIDC user ({@code name} claim, then full name).
	 * @param principal the authenticated OIDC user (may be null)
	 * @return display name, or {@code null} when unavailable
	 */
	public static String resolveOAuthDisplayName(final OidcUser principal) {
		if (principal == null) {
			return null;
		}
		final Object nameClaim = principal.getClaims().get("name");
		if (nameClaim instanceof String name && !name.isBlank()) {
			return name.trim();
		}
		final String fullName = principal.getFullName();
		if (fullName != null && !fullName.isBlank()) {
			return fullName.trim();
		}
		return null;
	}

	/**
	 * Reads the OIDC display name from the current Spring Security context.
	 * @return display name, or {@code null} when not authenticated as OIDC user
	 */
	public static String resolveOAuthDisplayNameFromSecurityContext() {
		final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication != null && authentication.getPrincipal() instanceof OidcUser oidcUser) {
			return resolveOAuthDisplayName(oidcUser);
		}
		return null;
	}

}
