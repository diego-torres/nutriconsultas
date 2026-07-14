package com.nutriconsultas.controller;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

class AppVersionModelAdviceTest {

	@Test
	void appVersion_returnsConfiguredValue() {
		final AppVersionModelAdvice advice = new AppVersionModelAdvice("2.0-SNAPSHOT");

		assertThat(advice.appVersion()).isEqualTo("2.0-SNAPSHOT");
	}

	@Test
	void appVersion_canBeAddedAsModelAttribute() {
		final AppVersionModelAdvice advice = new AppVersionModelAdvice("2.1.0");
		final Model model = new ExtendedModelMap();

		model.addAttribute("appVersion", advice.appVersion());

		assertThat(model.getAttribute("appVersion")).isEqualTo("2.1.0");
	}

}
