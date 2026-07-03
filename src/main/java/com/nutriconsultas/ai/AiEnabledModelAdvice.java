package com.nutriconsultas.ai;

import org.springframework.stereotype.Component;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Exposes the AI feature flag to admin templates (#388). Never adds API keys or OpenAI
 * configuration — only {@code aiEnabled} from {@link AiProperties#isEnabled()}.
 */
@Component
@ControllerAdvice
public class AiEnabledModelAdvice {

	private final AiProperties aiProperties;

	public AiEnabledModelAdvice(final AiProperties aiProperties) {
		this.aiProperties = aiProperties;
	}

	@ModelAttribute
	public void addAiEnabledFlag(final Model model) {
		model.addAttribute("aiEnabled", aiProperties.isEnabled());
	}

}
