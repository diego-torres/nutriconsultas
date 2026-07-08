package com.nutriconsultas.platillos;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PlatilloImageSupportTest {

	@Test
	void hasAssignedPictureFalseWhenMissingOrBlank() {
		final Platillo withoutImage = new Platillo();
		withoutImage.setImageUrl(null);

		final Platillo blankImage = new Platillo();
		blankImage.setImageUrl("   ");

		assertThat(PlatilloImageSupport.hasAssignedPicture(withoutImage)).isFalse();
		assertThat(PlatilloImageSupport.hasAssignedPicture(blankImage)).isFalse();
	}

	@Test
	void resolveGridImageUrlBuildsAdminPicturePath() {
		final Platillo platillo = new Platillo();
		platillo.setId(12L);
		platillo.setImageUrl("platillo/12/picture.jpg");

		assertThat(PlatilloImageSupport.resolveGridImageUrl(platillo))
			.isEqualTo("/admin/platillos/platillo/12/picture.jpg");
	}

	@Test
	void buildGridImageColumnShowsMissingBadgeWhenNoPicture() {
		final Platillo platillo = new Platillo();
		platillo.setId(3L);

		final String column = PlatilloImageSupport.buildGridImageColumn(platillo);

		assertThat(column).contains("Sin imagen");
		assertThat(column).contains("platillo-grid-thumb-missing");
	}

	@Test
	void buildGridImageColumnShowsThumbnailWhenPictureExists() {
		final Platillo platillo = new Platillo();
		platillo.setId(4L);
		platillo.setImageUrl("platillo/4/picture.png");

		final String column = PlatilloImageSupport.buildGridImageColumn(platillo);

		assertThat(column).contains("/admin/platillos/platillo/4/picture.png");
		assertThat(column).doesNotContain("Sin imagen");
	}

}
