package com.nutriconsultas.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.nutriconsultas.contact.ContactInquiry;
import com.nutriconsultas.contact.ContactInquiryService;
import com.nutriconsultas.contact.ContactPlanInterest;
import com.nutriconsultas.recaptcha.PublicRecaptchaForm;
import com.nutriconsultas.recaptcha.RecaptchaVerificationService;
import com.nutriconsultas.subscription.PlanTier;
import com.nutriconsultas.util.LogRedaction;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@Controller
@Slf4j
@PublicRecaptchaForm
public class WebController {

	private final ContactInquiryService contactInquiryService;

	private final RecaptchaVerificationService recaptchaVerificationService;

	public WebController(final ContactInquiryService contactInquiryService,
			final RecaptchaVerificationService recaptchaVerificationService) {
		this.contactInquiryService = contactInquiryService;
		this.recaptchaVerificationService = recaptchaVerificationService;
	}

	@GetMapping(path = "/")
	public String index(@RequestParam(value = "plan", required = false) final String plan, final Model model) {
		log.debug("Resolving index");
		addSelectedPlanToModel(plan, model);
		return "eterna/index";
	}

	@GetMapping(path = "/contact")
	public String contactPage(@RequestParam(value = "plan", required = false) final String plan, final Model model) {
		log.debug("Resolving contact page");
		addSelectedPlanToModel(plan, model);
		return "eterna/contact";
	}

	@GetMapping(path = { "/privacidad", "/aviso-de-privacidad" })
	public String privacyPage() {
		log.debug("Resolving privacy page");
		return "eterna/privacidad";
	}

	@GetMapping(path = { "/aptitud-por-edad", "/age-suitability" })
	public String ageSuitabilityPage() {
		log.debug("Resolving age suitability page");
		return "eterna/aptitud-edad";
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

		if (!recaptchaVerificationService.verifyToken(recaptchaResponse)) {
			log.warn("reCAPTCHA verification failed");
			return ResponseEntity.badRequest().body("La verificación reCAPTCHA falló. Por favor, intenta nuevamente.");
		}

		final ContactInquiry saved = contactInquiryService.saveFromForm(contactForm);
		log.info("Contact form submitted successfully: {}", LogRedaction.redactContactInquiry(saved.getId()));

		return ResponseEntity.ok("OK");
	}

	private void addSelectedPlanToModel(final String planParam, final Model model) {
		final PlanTier selectedPlan = ContactPlanInterest.resolveFromParam(planParam).orElse(null);
		model.addAttribute("selectedPlan", selectedPlan);
		if (selectedPlan != null) {
			model.addAttribute("selectedPlanDisplayName", selectedPlan.getDisplayName());
			model.addAttribute("selectedPlanSubject", ContactPlanInterest.defaultSubjectForPlan(selectedPlan));
		}
	}

}
