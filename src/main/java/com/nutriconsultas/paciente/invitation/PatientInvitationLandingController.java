package com.nutriconsultas.paciente.invitation;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.nutriconsultas.booking.ClientIpResolver;

import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

/**
 * Public web landing for patient invitation deep links (#337). Served outside
 * {@code /admin}.
 */
@Controller
@Slf4j
public class PatientInvitationLandingController {

	private final PatientInvitationLandingService patientInvitationLandingService;

	private final PatientInvitationProperties patientInvitationProperties;

	public PatientInvitationLandingController(final PatientInvitationLandingService patientInvitationLandingService,
			final PatientInvitationProperties patientInvitationProperties) {
		this.patientInvitationLandingService = patientInvitationLandingService;
		this.patientInvitationProperties = patientInvitationProperties;
	}

	@GetMapping({ "/links/i/{token}", "/i/{token}" })
	public String landingPage(@PathVariable("token") final String token, final HttpServletRequest httpRequest,
			final Model model) {
		if (log.isDebugEnabled()) {
			log.debug("Patient invitation landing page requested");
		}
		populateCommonModel(httpRequest, model);
		final String clientKey = ClientIpResolver.resolve(httpRequest);
		return patientInvitationLandingService.resolve(token, clientKey)
			.map(content -> populateValidModel(model, content))
			.orElseGet(() -> populateInvalidModel(model));
	}

	@ExceptionHandler(RequestNotPermitted.class)
	public String handleRateLimit(final HttpServletRequest httpRequest, final Model model) {
		if (log.isDebugEnabled()) {
			log.debug("Patient invitation landing rate limit exceeded");
		}
		populateCommonModel(httpRequest, model);
		model.addAttribute("valid", false);
		model.addAttribute("rateLimited", true);
		return "eterna/patient-invitation-landing";
	}

	private String populateValidModel(final Model model, final PatientInvitationLandingContent content) {
		model.addAttribute("valid", true);
		model.addAttribute("rateLimited", false);
		model.addAttribute("inviterDisplayName", content.inviterDisplayName());
		model.addAttribute("humanCode", content.humanCode());
		model.addAttribute("inviteUrl", content.inviteUrl());
		return "eterna/patient-invitation-landing";
	}

	private String populateInvalidModel(final Model model) {
		model.addAttribute("valid", false);
		model.addAttribute("rateLimited", false);
		return "eterna/patient-invitation-landing";
	}

	private void populateCommonModel(final HttpServletRequest httpRequest, final Model model) {
		final PatientInvitationClientPlatform platform = PatientInvitationClientPlatform
			.fromUserAgent(httpRequest.getHeader("User-Agent"));
		model.addAttribute("platform", platform.name());
		model.addAttribute("iosAppStoreUrl", patientInvitationProperties.getIosAppStoreUrl());
		model.addAttribute("androidPlayStoreUrl", patientInvitationProperties.getAndroidPlayStoreUrl());
	}

}
