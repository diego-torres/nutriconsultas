package com.nutriconsultas.profile;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

/**
 * Unit tests for {@link NutritionistBrandingHelper}.
 */
public class NutritionistBrandingHelperTest {

	@Test
	public void resolveDisplayNamePrefersProfileDisplayName() {
		final NutritionistProfile profile = new NutritionistProfile();
		profile.setDisplayName("Lic. María García");

		assertThat(NutritionistBrandingHelper.resolveDisplayName(profile, "Auth0 Name")).isEqualTo("Lic. María García");
	}

	@Test
	public void resolveDisplayNameFallsBackToOAuthName() {
		final NutritionistProfile profile = new NutritionistProfile();

		assertThat(NutritionistBrandingHelper.resolveDisplayName(profile, "Auth0 Name")).isEqualTo("Auth0 Name");
	}

	@Test
	public void resolveDisplayNameReturnsNullWhenNoSourceAvailable() {
		assertThat(NutritionistBrandingHelper.resolveDisplayName(null, null)).isNull();
		assertThat(NutritionistBrandingHelper.resolveDisplayName(new NutritionistProfile(), "  ")).isNull();
	}

	@Test
	public void resolveOAuthDisplayNameUsesNameClaim() {
		final OidcUser principal = createOidcUser("María García López", "auth0|123");

		assertThat(NutritionistBrandingHelper.resolveOAuthDisplayName(principal)).isEqualTo("María García López");
	}

	@Test
	public void resolveOAuthDisplayNameReturnsNullForNullPrincipal() {
		assertThat(NutritionistBrandingHelper.resolveOAuthDisplayName(null)).isNull();
	}

	@Test
	public void pdfLogoMaxSizeIsOneAndHalfInches() {
		assertThat(NutritionistBrandingHelper.PDF_LOGO_MAX_SIZE).isEqualTo("1.5in");
		assertThat(NutritionistBrandingHelper.PDF_LOGO_CELL_WIDTH).isEqualTo("1.6in");
		assertThat(PdfLogoDimensions.MAX_SIZE_PT).isEqualTo(108.0);
	}

	private OidcUser createOidcUser(final String name, final String subject) {
		final Map<String, Object> claims = new HashMap<>();
		claims.put("sub", subject);
		claims.put("name", name);
		final OidcIdToken idToken = new OidcIdToken("token", null, null, claims);
		return new DefaultOidcUser(null, idToken);
	}

}
