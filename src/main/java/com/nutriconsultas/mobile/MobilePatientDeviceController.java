package com.nutriconsultas.mobile;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.nutriconsultas.mobile.config.MobileOpenApiResponses;
import com.nutriconsultas.mobile.dto.ApiResponse;
import com.nutriconsultas.mobile.dto.DeregisterPatientDeviceRequest;
import com.nutriconsultas.mobile.dto.PatientDeviceDto;
import com.nutriconsultas.mobile.dto.RegisterPatientDeviceRequest;
import com.nutriconsultas.util.LogRedaction;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/rest/mobile/patient/devices")
@Tag(name = "Mobile", description = "Patient mobile API")
@Slf4j
public class MobilePatientDeviceController extends AbstractMobilePatientController {

	private final MobilePatientDeviceService mobilePatientDeviceService;

	public MobilePatientDeviceController(final PatientAuthService patientAuthService,
			final MobilePatientDeviceService mobilePatientDeviceService) {
		super(patientAuthService);
		this.mobilePatientDeviceService = mobilePatientDeviceService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.OK)
	@Operation(summary = "Register device",
			description = "Upserts an APNs or FCM registration token for the authenticated patient.")
	@MobileOpenApiResponses.AuthenticatedPatient
	@MobileOpenApiResponses.WriteEndpoint
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
			description = "Registered device summary")
	public ApiResponse<PatientDeviceDto> registerDevice(@AuthenticationPrincipal final Jwt jwt,
			@Valid @RequestBody final RegisterPatientDeviceRequest request) {
		final Long pacienteId = getAuthenticatedPacienteId(jwt);
		if (log.isDebugEnabled()) {
			log.debug("Mobile device register request for patient {} platform={}",
					LogRedaction.redactPaciente(pacienteId), request.platform());
		}
		final PatientDeviceDto device = mobilePatientDeviceService.upsertDevice(pacienteId, request.platform(),
				request.token(), request.appVersion());
		return ApiResponse.ok(device);
	}

	@DeleteMapping
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "Deregister device",
			description = "Removes a push token for the authenticated patient. Idempotent when already gone.")
	@MobileOpenApiResponses.AuthenticatedPatient
	@MobileOpenApiResponses.WriteEndpoint
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204",
			description = "Token removed or already absent")
	public void deregisterDevice(@AuthenticationPrincipal final Jwt jwt,
			@Valid @RequestBody final DeregisterPatientDeviceRequest request) {
		final Long pacienteId = getAuthenticatedPacienteId(jwt);
		if (log.isDebugEnabled()) {
			log.debug("Mobile device deregister request for patient {}", LogRedaction.redactPaciente(pacienteId));
		}
		mobilePatientDeviceService.deregisterDevice(pacienteId, request.token());
	}

}
