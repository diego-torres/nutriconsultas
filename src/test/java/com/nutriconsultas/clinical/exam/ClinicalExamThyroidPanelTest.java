package com.nutriconsultas.clinical.exam;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.Column;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
class ClinicalExamThyroidPanelTest {

	@Test
	void thyroidConvenienceMethods_createPanelOnSetAndReadBack() {
		final ClinicalExam exam = new ClinicalExam();

		exam.setTsh(2.5);
		exam.setT4Libre(1.2);
		exam.setT3Libre(3.1);
		exam.setAntiTpo(15.0);

		assertThat(exam.getThyroidPanel()).isNotNull();
		assertThat(exam.getTsh()).isEqualTo(2.5);
		assertThat(exam.getT4Libre()).isEqualTo(1.2);
		assertThat(exam.getT3Libre()).isEqualTo(3.1);
		assertThat(exam.getAntiTpo()).isEqualTo(15.0);
	}

	@Test
	void thyroidLibreFields_mapToSnakeCaseLiquibaseColumns() throws NoSuchFieldException {
		final Column t4 = ThyroidPanel.class.getDeclaredField("t4Libre").getAnnotation(Column.class);
		final Column t3 = ThyroidPanel.class.getDeclaredField("t3Libre").getAnnotation(Column.class);

		assertThat(t4).isNotNull();
		assertThat(t3).isNotNull();
		assertThat(t4.name()).isEqualTo("t4_libre");
		assertThat(t3.name()).isEqualTo("t3_libre");
	}

	@Test
	void thyroidConvenienceMethods_returnNullWhenUnset() {
		final ClinicalExam exam = new ClinicalExam();

		assertThat(exam.getTsh()).isNull();
		assertThat(exam.getT4Libre()).isNull();
		assertThat(exam.getT3Libre()).isNull();
		assertThat(exam.getAntiTpo()).isNull();
	}

}
