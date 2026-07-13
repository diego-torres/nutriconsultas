package com.nutriconsultas.paciente;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class PacientePictureSupportTest {

	@Test
	void hasCustomPhoto_whenExtensionPresent() {
		final Paciente paciente = new Paciente();
		paciente.setId(5L);
		paciente.setPhotoExtension("png");

		assertThat(PacientePictureSupport.hasCustomPhoto(paciente)).isTrue();
	}

	@Test
	void resolveDisplayUrlForAdmin_usesPictureEndpointWhenCustomPhoto() {
		final Paciente paciente = new Paciente();
		paciente.setId(5L);
		paciente.setPhotoExtension("jpg");

		assertThat(PacientePictureSupport.resolveDisplayUrlForAdmin(paciente)).isEqualTo("/admin/pacientes/5/picture");
	}

	@Test
	void resolveDisplayUrlForMobile_usesMobileEndpointWhenCustomPhoto() {
		final Paciente paciente = new Paciente();
		paciente.setId(5L);
		paciente.setPhotoExtension("webp");

		assertThat(PacientePictureSupport.resolveDisplayUrlForMobile(paciente))
			.isEqualTo("/rest/mobile/patient/profile/photo");
	}

	@Test
	void normalizeExtension_rejectsUnsupportedFormat() {
		assertThatThrownBy(() -> PacientePictureSupport.normalizeExtension("bmp"))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Formato de imagen no permitido");
	}

	@Test
	void buildPhotoKey_usesNormalizedExtension() {
		assertThat(PacientePictureSupport.buildPhotoKey(9L, "JPEG")).isEqualTo("patients/9/photo.jpeg");
	}

	@Test
	void resolveMediaType_mapsKnownExtensions() {
		assertThat(PacientePictureSupport.resolveMediaType("jpg")).isEqualTo(MediaType.IMAGE_JPEG);
		assertThat(PacientePictureSupport.resolveMediaType("png")).isEqualTo(MediaType.IMAGE_PNG);
	}

}
