package com.nutriconsultas.mobile;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.nutriconsultas.mobile.config.MobileOpenApiResponses;
import com.nutriconsultas.mobile.dto.ApiResponse;
import com.nutriconsultas.mobile.dto.CreatePatientInvitationRequest;
import com.nutriconsultas.mobile.dto.CreatedPatientInvitationDto;
import com.nutriconsultas.paciente.invitation.CreatedPatientInvitationResult;
import com.nutriconsultas.paciente.invitation.PatientInvitationCreateService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/rest/mobile/invitations")
@Tag(name = "Mobile Invitations", description = "Nutritionist-initiated patient onboarding")
@Slf4j
public class MobileInvitationController {

	private final PatientInvitationCreateService patientInvitationCreateService;

	public MobileInvitationController(final PatientInvitationCreateService patientInvitationCreateService) {
		this.patientInvitationCreateService = patientInvitationCreateService;
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

	private static String requireSubject(final Jwt jwt) {
		if (jwt == null || jwt.getSubject() == null || jwt.getSubject().isBlank()) {
			throw new IllegalArgumentException("Authenticated nutritionist subject is required");
		}
		return jwt.getSubject();
	}

}
