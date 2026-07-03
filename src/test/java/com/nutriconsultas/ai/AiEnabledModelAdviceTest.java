package com.nutriconsultas.ai;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiEnabledModelAdviceTest {

	@InjectMocks
	private AiEnabledModelAdvice advice;

	@Mock
	private AiProperties aiProperties;

	@Test
	void addsAiEnabledWhenFeatureFlagOn() {
		when(aiProperties.isEnabled()).thenReturn(true);
		final Model model = new ExtendedModelMap();

		advice.addAiEnabledFlag(model);

		assertThat(model.getAttribute("aiEnabled")).isEqualTo(true);
		assertThat(model.asMap()).doesNotContainKey("openaiApiKey");
		assertThat(model.asMap()).doesNotContainKey("openaiModel");
	}

	@Test
	void addsAiEnabledFalseWhenFeatureFlagOff() {
		when(aiProperties.isEnabled()).thenReturn(false);
		final Model model = new ExtendedModelMap();

		advice.addAiEnabledFlag(model);

		assertThat(model.getAttribute("aiEnabled")).isEqualTo(false);
	}

}
