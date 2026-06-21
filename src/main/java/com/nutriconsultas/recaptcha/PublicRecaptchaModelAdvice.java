package com.nutriconsultas.recaptcha;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice(annotations = PublicRecaptchaForm.class)
public class PublicRecaptchaModelAdvice {

	private final RecaptchaProperties properties;

	public PublicRecaptchaModelAdvice(final RecaptchaProperties properties) {
		this.properties = properties;
	}

	@ModelAttribute
	public void addRecaptchaAttributes(final Model model) {
		model.addAttribute("recaptchaSiteKey", properties.getSiteKey());
		model.addAttribute("recaptchaEnabled", properties.isConfigured());
	}

}
