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
	void isCircumferenceFieldRecognizesCircumferenceKeys() {
		assertThat(AnthropometricVisualAidCatalog.isCircumferenceField("circumferences.cephalicCircumference"))
			.isTrue();
		assertThat(AnthropometricVisualAidCatalog.isCircumferenceField("skinfolds.tricepsSkinfold")).isFalse();
	}

	@Test
	void isVisualAidFieldIncludesSkinfoldsAndCircumferences() {
		assertThat(AnthropometricVisualAidCatalog.isVisualAidField("skinfolds.tricepsSkinfold")).isTrue();
		assertThat(AnthropometricVisualAidCatalog.isVisualAidField("circumferences.neckCircumference")).isTrue();
		assertThat(AnthropometricVisualAidCatalog.isVisualAidField("bodyMass.weight")).isFalse();
	}

	@Test
	void imageFileNameReturnsSkinfoldAssets() {
		assertThat(AnthropometricVisualAidCatalog.imageFileName("skinfolds.subscapularSkinfold"))
			.contains("subescapular.png");
		assertThat(AnthropometricVisualAidCatalog.imageFileName("skinfolds.pectoralSkinfold")).contains("pectoral.png");
	}

	@Test
	void imageFileNameReturnsCefalicoAsset() {
		assertThat(AnthropometricVisualAidCatalog.imageFileName("circumferences.cephalicCircumference"))
			.contains("cefalico.png");
		assertThat(AnthropometricVisualAidCatalog.imageFileName("circumferences.neckCircumference"))
			.contains("cuello.png");
		assertThat(AnthropometricVisualAidCatalog.imageFolder("circumferences.cephalicCircumference"))
			.contains("perimetros");
		assertThat(AnthropometricVisualAidCatalog.imageFileName("circumferences.midUpperArmCircumferenceRelaxed"))
			.contains("brazo-relajado.png");
		assertThat(AnthropometricVisualAidCatalog.imageFileName("circumferences.midUpperArmCircumferenceContracted"))
			.contains("brazo-contraido.png");
		assertThat(AnthropometricVisualAidCatalog.imageFileName("circumferences.forearmCircumference"))
			.contains("antebrazo.png");
		assertThat(AnthropometricVisualAidCatalog.imageFileName("circumferences.wristCircumference"))
			.contains("muneca.png");
		assertThat(AnthropometricVisualAidCatalog.imageFileName("circumferences.mesosternalCircumference"))
			.contains("mesoesternal.png");
		assertThat(AnthropometricVisualAidCatalog.imageFileName("circumferences.umbilicalCircumference"))
			.contains("umbilical.png");
		assertThat(AnthropometricVisualAidCatalog.imageFileName("circumferences.waistCircumference"))
			.contains("cintura.png");
		assertThat(AnthropometricVisualAidCatalog.imageFileName("circumferences.hipCircumference"))
			.contains("cadera.png");
		assertThat(AnthropometricVisualAidCatalog.imageFileName("circumferences.thighCircumference"))
			.contains("muslo.png");
		assertThat(AnthropometricVisualAidCatalog.imageFileName("circumferences.midThighCircumference"))
			.contains("muslo-medio.png");
		assertThat(AnthropometricVisualAidCatalog.imageFileName("circumferences.calfCircumference"))
			.contains("pantorrilla.png");
		assertThat(AnthropometricVisualAidCatalog.imageFileName("circumferences.ankleCircumference"))
			.contains("tobillo.png");
	}

	@Test
	void imagePathBuildsStaticUrl() {
		assertThat(AnthropometricVisualAidCatalog.imagePath("skinfolds.subscapularSkinfold"))
			.contains("/sbadmin/img/anthropometry/pliegues/subescapular.png");
		assertThat(AnthropometricVisualAidCatalog.imagePath("circumferences.cephalicCircumference"))
			.contains("/sbadmin/img/anthropometry/perimetros/cefalico.png");
	}

}
