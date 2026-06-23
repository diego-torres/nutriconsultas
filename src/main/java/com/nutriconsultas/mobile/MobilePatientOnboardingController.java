package com.nutriconsultas.mobile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nutriconsultas.mobile.config.MobileOpenApiResponses;
import com.nutriconsultas.mobile.dto.ApiResponse;
import com.nutriconsultas.mobile.dto.PatchPatientOnboardingProfileRequest;
import com.nutriconsultas.mobile.dto.PatientOnboardingProfileDto;
import com.nutriconsultas.util.LogRedaction;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/rest/mobile/patient/me")
@Tag(name = "Mobile", description = "Patient mobile API")
@Slf4j
public class MobilePatientOnboardingController extends AbstractMobilePatientController {

	private final MobilePatientOnboardingService mobilePatientOnboardingService;

	public MobilePatientOnboardingController(final PatientAuthService patientAuthService,
			final MobilePatientOnboardingService mobilePatientOnboardingService) {
		super(patientAuthService);
		this.mobilePatientOnboardingService = mobilePatientOnboardingService;
	}

	@GetMapping
	@Operation(summary = "Get onboarding profile",
			description = "Returns patient bootstrap profile and optional assigned diet plan reference.")
	@MobileOpenApiResponses.AuthenticatedPatient
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Onboarding profile")
	public ApiResponse<PatientOnboardingProfileDto> getProfile(@AuthenticationPrincipal final Jwt jwt) {
		final Long pacienteId = getAuthenticatedPacienteId(jwt);
		if (log.isDebugEnabled()) {
			log.debug("Mobile onboarding profile request for patient {}", LogRedaction.redactPaciente(pacienteId));
		}
		return ApiResponse.ok(mobilePatientOnboardingService.getProfile(pacienteId));
	}

	@PatchMapping
	@Operation(summary = "Update onboarding profile",
			description = "Patches onboarding fields; transitions status to ACTIVE when all required fields are present.")
	@MobileOpenApiResponses.AuthenticatedPatient
	@MobileOpenApiResponses.WriteEndpoint
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
			description = "Updated onboarding profile")
	public ApiResponse<PatientOnboardingProfileDto> updateProfile(@AuthenticationPrincipal final Jwt jwt,
			@Valid @RequestBody final PatchPatientOnboardingProfileRequest request) {
		final Long pacienteId = getAuthenticatedPacienteId(jwt);
		if (log.isDebugEnabled()) {
			log.debug("Mobile onboarding profile patch for patient {}", LogRedaction.redactPaciente(pacienteId));
		}
		return ApiResponse.ok(mobilePatientOnboardingService.updateProfile(pacienteId, request));
	}

}
