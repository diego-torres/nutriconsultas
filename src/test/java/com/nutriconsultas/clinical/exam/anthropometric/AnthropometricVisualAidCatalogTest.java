package com.nutriconsultas.clinical.exam.anthropometric;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AnthropometricVisualAidCatalogTest {

	@Test
	void isSkinfoldFieldRecognizesSkinfoldKeys() {
		assertThat(AnthropometricVisualAidCatalog.isSkinfoldField("skinfolds.tricepsSkinfold")).isTrue();
		assertThat(AnthropometricVisualAidCatalog.isSkinfoldField("bodyMass.weight")).isFalse();
		assertThat(AnthropometricVisualAidCatalog.isSkinfoldField(null)).isFalse();
	}

	@Test
	void imageFileNameReturnsSubescapularAsset() {
		assertThat(AnthropometricVisualAidCatalog.imageFileName("skinfolds.subscapularSkinfold"))
				.contains("subescapular.png");
		assertThat(AnthropometricVisualAidCatalog.imageFileName("skinfolds.tricepsSkinfold"))
				.contains("triceps.png");
		assertThat(AnthropometricVisualAidCatalog.imageFileName("skinfolds.bicepsSkinfold"))
				.contains("biceps.png");
		assertThat(AnthropometricVisualAidCatalog.imageFileName("skinfolds.iliacCrestSkinfold"))
				.contains("cresta-iliaca.png");
		assertThat(AnthropometricVisualAidCatalog.imageFileName("skinfolds.supraespinalSkinfold"))
				.contains("supraespinal.png");
		assertThat(AnthropometricVisualAidCatalog.imageFileName("skinfolds.abdominalSkinfold"))
				.contains("abdominal.png");
		assertThat(AnthropometricVisualAidCatalog.imageFileName("skinfolds.frontalThighSkinfold"))
				.contains("muslo-frontal.png");
		assertThat(AnthropometricVisualAidCatalog.imageFileName("skinfolds.medialCalfSkinfold"))
				.contains("pantorrilla-medial.png");
		assertThat(AnthropometricVisualAidCatalog.imageFileName("skinfolds.medialAxillarySkinfold"))
				.contains("axilar-medial.png");
		assertThat(AnthropometricVisualAidCatalog.imageFileName("skinfolds.pectoralSkinfold"))
				.contains("pectoral.png");
	}

	@Test
	void imagePathBuildsStaticUrl() {
		assertThat(AnthropometricVisualAidCatalog.imagePath("skinfolds.subscapularSkinfold"))
			.contains("/sbadmin/img/anthropometry/pliegues/subescapular.png");
	}

}
