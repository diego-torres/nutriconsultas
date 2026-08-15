package com.nutriconsultas.dieta;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class PlatilloIngestaPictureSupportTest {

	@Test
	void hasCustomPicture_falseWhenMissingBlankOrPlaceholder() {
		final PlatilloIngesta missing = new PlatilloIngesta();
		final PlatilloIngesta blank = new PlatilloIngesta();
		blank.setImageUrl("   ");
		final PlatilloIngesta placeholder = new PlatilloIngesta();
		placeholder.setImageUrl(PlatilloIngestaPictureSupport.PLACEHOLDER_IMAGE_PATH);

		assertThat(PlatilloIngestaPictureSupport.hasCustomPicture(null)).isFalse();
		assertThat(PlatilloIngestaPictureSupport.hasCustomPicture(missing)).isFalse();
		assertThat(PlatilloIngestaPictureSupport.hasCustomPicture(blank)).isFalse();
		assertThat(PlatilloIngestaPictureSupport.hasCustomPicture(placeholder)).isFalse();
	}

	@Test
	void resolveDisplayUrlForMobile_usesResolverWhenCustomPicture() {
		final PlatilloIngesta platillo = new PlatilloIngesta();
		platillo.setId(30L);
		platillo.setImageUrl("platillo/12/picture.jpg");

		assertThat(PlatilloIngestaPictureSupport.resolveDisplayUrlForMobile(7L, platillo))
			.isEqualTo("/rest/mobile/patient/diet-plans/7/platillos/30/image");
	}

	@Test
	void resolveDisplayUrlForMobile_usesPlaceholderWhenNoCustomPicture() {
		final PlatilloIngesta platillo = new PlatilloIngesta();
		platillo.setId(30L);

		assertThat(PlatilloIngestaPictureSupport.resolveDisplayUrlForMobile(7L, platillo))
			.isEqualTo(PlatilloIngestaPictureSupport.PLACEHOLDER_IMAGE_PATH);
	}

	@Test
	void resolvePictureObject_parsesS3Key() {
		final PlatilloIngesta platillo = new PlatilloIngesta();
		platillo.setImageUrl("platillo/12/picture.jpg");

		final PlatilloIngestaPictureSupport.PictureObject picture = PlatilloIngestaPictureSupport
			.resolvePictureObject(platillo);

		assertThat(picture.catalogPlatilloId()).isEqualTo(12L);
		assertThat(picture.fileName()).isEqualTo("picture.jpg");
	}

	@Test
	void resolvePictureObject_parsesAdminPath() {
		final PlatilloIngesta platillo = new PlatilloIngesta();
		platillo.setImageUrl("/admin/platillos/platillo/12/picture.png");

		final PlatilloIngestaPictureSupport.PictureObject picture = PlatilloIngestaPictureSupport
			.resolvePictureObject(platillo);

		assertThat(picture.catalogPlatilloId()).isEqualTo(12L);
		assertThat(picture.fileName()).isEqualTo("picture.png");
	}

	@Test
	void resolvePictureObject_fallsBackToSourcePlatilloId() {
		final PlatilloIngesta platillo = new PlatilloIngesta();
		platillo.setSourcePlatilloId(99L);
		platillo.setImageUrl("/uploads/platillo.webp");

		final PlatilloIngestaPictureSupport.PictureObject picture = PlatilloIngestaPictureSupport
			.resolvePictureObject(platillo);

		assertThat(picture.catalogPlatilloId()).isEqualTo(99L);
		assertThat(picture.fileName()).isEqualTo("platillo.webp");
	}

	@Test
	void resolvePictureObject_rejectsPathTraversal() {
		final PlatilloIngesta platillo = new PlatilloIngesta();
		platillo.setSourcePlatilloId(99L);
		platillo.setImageUrl("../secret.jpg");

		assertThat(PlatilloIngestaPictureSupport.resolvePictureObject(platillo)).isNull();
	}

	@Test
	void resolveMediaType_mapsKnownExtensions() {
		assertThat(PlatilloIngestaPictureSupport.resolveMediaType("picture.jpg")).isEqualTo(MediaType.IMAGE_JPEG);
		assertThat(PlatilloIngestaPictureSupport.resolveMediaType("picture.png")).isEqualTo(MediaType.IMAGE_PNG);
		assertThat(PlatilloIngestaPictureSupport.resolveMediaType("picture.webp"))
			.isEqualTo(MediaType.parseMediaType("image/webp"));
	}

}
