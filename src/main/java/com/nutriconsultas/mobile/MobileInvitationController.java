package com.nutriconsultas.mobile;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.nutriconsultas.booking.ClientIpResolver;
import com.nutriconsultas.mobile.config.MobileOpenApiResponses;
import com.nutriconsultas.mobile.dto.ApiResponse;
import com.nutriconsultas.mobile.dto.CreatePatientInvitationRequest;
import com.nutriconsultas.mobile.dto.CreatedPatientInvitationDto;
import com.nutriconsultas.mobile.dto.PatientInvitationPreviewDto;
import com.nutriconsultas.paciente.invitation.CreatedPatientInvitationResult;
import com.nutriconsultas.paciente.invitation.PatientInvitationCreateService;
import com.nutriconsultas.paciente.invitation.PatientInvitationPreviewResult;
import com.nutriconsultas.paciente.invitation.PatientInvitationPreviewService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/rest/mobile/invitations")
@Tag(name = "Mobile Invitations", description = "Nutritionist-initiated patient onboarding")
@Slf4j
public class MobileInvitationController {

	private final PatientInvitationCreateService patientInvitationCreateService;

	private final PatientInvitationPreviewService patientInvitationPreviewService;

	private final PatientInvitationPreviewRateLimiter patientInvitationPreviewRateLimiter;

	public MobileInvitationController(final PatientInvitationCreateService patientInvitationCreateService,
			final PatientInvitationPreviewService patientInvitationPreviewService,
			final PatientInvitationPreviewRateLimiter patientInvitationPreviewRateLimiter) {
		this.patientInvitationCreateService = patientInvitationCreateService;
		this.patientInvitationPreviewService = patientInvitationPreviewService;
		this.patientInvitationPreviewRateLimiter = patientInvitationPreviewRateLimiter;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Create patient invitation",
			description = "Nutritionist JWT creates Paciente (INVITED) + pending invitation; returns deep link and human code.")
	@MobileOpenApiResponses.AuthenticatedNutritionist
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201",
			description = "Patient and invitation created")
	public ApiResponse<CreatedPatientInvitationDto> createInvitation(@AuthenticationPrincipal final Jwt jwt,
			@Valid @RequestBody final CreatePatientInvitationRequest request) {
		if (log.isDebugEnabled()) {
			log.debug("Mobile create patient invitation request from nutritionist sub present={}",
					jwt != null && jwt.getSubject() != null);
		}
		final String nutritionistUserId = requireSubject(jwt);
		final CreatedPatientInvitationResult created = patientInvitationCreateService
			.createInvitation(nutritionistUserId, request);
		return ApiResponse.ok(CreatedPatientInvitationDto.from(created));
	}

	@GetMapping("/{token}/preview")
	@Operation(summary = "Preview patient invitation",
			description = "Public, rate-limited preview returning inviter display name only (no patient PII).")
	@SecurityRequirements
	@MobileOpenApiResponses.PublicInvitationPreview
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
			description = "Invitation preview when token is valid and pending")
	public ApiResponse<PatientInvitationPreviewDto> previewInvitation(@PathVariable("token") final String token,
			final HttpServletRequest httpRequest) {
		if (log.isDebugEnabled()) {
			log.debug("Mobile invitation preview request received");
		}
		final String clientKey = ClientIpResolver.resolve(httpRequest);
		final PatientInvitationPreviewResult preview = patientInvitationPreviewRateLimiter.execute(clientKey,
				() -> patientInvitationPreviewService.preview(token));
		return ApiResponse.ok(PatientInvitationPreviewDto.from(preview));
	}

	private static String requireSubject(final Jwt jwt) {
		if (jwt == null || jwt.getSubject() == null || jwt.getSubject().isBlank()) {
			throw new IllegalArgumentException("Authenticated nutritionist subject is required");
		}
		return jwt.getSubject();
	}

}
