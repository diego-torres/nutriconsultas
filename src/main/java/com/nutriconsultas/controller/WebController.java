package com.nutriconsultas.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@Controller
@Slf4j
public class WebController {

	private static final String RECAPTCHA_VERIFY_URL = "https://www.google.com/recaptcha/api/siteverify";

	private final RestClient restClient;

	@Value("${recaptcha.secret-key:6LeIxAcTAAAAAGG-vFI1TnRWxMZNFuojJ4WifJWe}")
	private String recaptchaSecretKey;

	public WebController() {
		this.restClient = RestClient.create();
	}

	@GetMapping(path = "/")
	public String index() {
		log.debug("Resolving index");
		return "eterna/index";
	}

	@PostMapping(path = "/contact")
	public ResponseEntity<String> contact(@Valid final ContactForm contactForm, final BindingResult bindingResult,
			@RequestParam(value = "recaptcha-response", required = false) final String recaptchaResponse) {
		log.debug("Contact form submission received");

		if (bindingResult.hasErrors()) {
			log.warn("Contact form has validation errors");
			return ResponseEntity.badRequest().body("Por favor, completa todos los campos requeridos correctamente.");
		}

		if (recaptchaResponse == null || recaptchaResponse.isEmpty()) {
			log.warn("reCAPTCHA response is missing");
			return ResponseEntity.badRequest().body("Por favor, completa la verificación reCAPTCHA.");
		}

		if (!verifyRecaptcha(recaptchaResponse)) {
			log.warn("reCAPTCHA verification failed");
			return ResponseEntity.badRequest().body("La verificación reCAPTCHA falló. Por favor, intenta nuevamente.");
		}

		log.info("Contact form submitted successfully from: {}", contactForm.getEmail());
		// TODO: Send email notification or save to database
		// For now, we just log and return success

		return ResponseEntity.ok("OK");
	}

	private boolean verifyRecaptcha(final String recaptchaResponse) {
		try {
			final RecaptchaResponse response = restClient.post()
				.uri(RECAPTCHA_VERIFY_URL + "?secret={secret}&response={response}", recaptchaSecretKey,
						recaptchaResponse)
				.retrieve()
				.body(RecaptchaResponse.class);

			if (response != null && Boolean.TRUE.equals(response.getSuccess())) {
				log.debug("reCAPTCHA verification successful");
				return true;
			}
			log.warn("reCAPTCHA verification failed: {}", response);
			return false;
		}
		catch (Exception e) {
			log.error("Error verifying reCAPTCHA", e);
			return false;
		}
	}

	/**
	 * Response DTO for reCAPTCHA verification.
	 */
	@JsonIgnoreProperties(ignoreUnknown = true)
	private static final class RecaptchaResponse {

		private Boolean success;

		public Boolean getSuccess() {
			return this.success;
		}

		@SuppressWarnings("unused")
		public void setSuccess(final Boolean success) {
			this.success = success;
		}

	}

}
