package com.nutriconsultas.platillos;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PlatilloPictureFilterTest {

	@Test
	void fromRequestValueDefaultsToTodas() {
		assertThat(PlatilloPictureFilter.fromRequestValue(null)).isEqualTo(PlatilloPictureFilter.TODAS);
		assertThat(PlatilloPictureFilter.fromRequestValue("")).isEqualTo(PlatilloPictureFilter.TODAS);
		assertThat(PlatilloPictureFilter.fromRequestValue("unknown")).isEqualTo(PlatilloPictureFilter.TODAS);
	}

	@Test
	void fromRequestValueParsesSinImagenVariants() {
		assertThat(PlatilloPictureFilter.fromRequestValue("sin-imagen")).isEqualTo(PlatilloPictureFilter.SIN_IMAGEN);
		assertThat(PlatilloPictureFilter.fromRequestValue("sin_imagen")).isEqualTo(PlatilloPictureFilter.SIN_IMAGEN);
		assertThat(PlatilloPictureFilter.fromRequestValue("no-picture")).isEqualTo(PlatilloPictureFilter.SIN_IMAGEN);
	}

}
