package com.nutriconsultas.reports;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class ReportLineChartRendererTest {

	@Test
	void renderReturnsEmptyStringWhenValuesEmpty() {
		assertThat(ReportLineChartRenderer.render(List.of("01/01/24"), List.of(), "#4e73df", "kg")).isEmpty();
	}

	@Test
	void renderProducesSvgWithDataPoints() {
		final String svg = ReportLineChartRenderer.render(List.of("01/01/24", "01/02/24"), List.of(70.0, 69.5),
				"#4e73df", "kg");

		assertThat(svg).contains("<svg");
		assertThat(svg).contains("polyline");
		assertThat(svg).contains("circle");
		assertThat(svg).contains("01/01/24");
		assertThat(svg).contains("01/02/24");
	}

	@Test
	void renderEscapesXmlInLabels() {
		final String svg = ReportLineChartRenderer.render(List.of("A&B"), List.of(10.0), "#4e73df", "kg");

		assertThat(svg).contains("A&amp;B");
	}

}
