package com.nutriconsultas.mobile;

import java.time.Instant;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.nutriconsultas.mobile.config.MobileOpenApiResponses;
import com.nutriconsultas.mobile.dto.ApiResponse;
import com.nutriconsultas.mobile.dto.PatientProgressSnapshotDto;
import com.nutriconsultas.mobile.dto.ProgressMeasurementsDto;
import com.nutriconsultas.paciente.Paciente;
import com.nutriconsultas.util.LogRedaction;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/rest/mobile/patient/progress")
@Tag(name = "Mobile", description = "Patient mobile API")
@Slf4j
public class MobilePatientProgressController extends AbstractMobilePatientController {

	private final MobilePatientProgressService mobilePatientProgressService;

	public MobilePatientProgressController(final PatientAuthService patientAuthService,
			final MobilePatientProgressService mobilePatientProgressService) {
		super(patientAuthService);
		this.mobilePatientProgressService = mobilePatientProgressService;
	}

	@GetMapping
	@Operation(summary = "Get progress snapshot",
			description = "Returns latest BMI/BMR and related progress snapshot for the patient.")
	@MobileOpenApiResponses.AuthenticatedPatient
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Progress snapshot")
	public ApiResponse<PatientProgressSnapshotDto> getProgress(@AuthenticationPrincipal final Jwt jwt) {
		final Paciente paciente = getAuthenticatedPaciente(jwt);
		if (log.isDebugEnabled()) {
			log.debug("Mobile progress snapshot request for patient {}", LogRedaction.redactPaciente(paciente.getId()));
		}
		final PatientProgressSnapshotDto snapshot = mobilePatientProgressService.getSnapshot(paciente.getId());
		return ApiResponse.ok(snapshot);
	}

	@GetMapping("/measurements")
	@Operation(summary = "List progress measurements",
			description = "Returns anthropometric measurement time series for the patient.")
	@MobileOpenApiResponses.AuthenticatedPatient
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
			description = "Measurement time series (ASC, max 365 rows)")
	public ApiResponse<ProgressMeasurementsDto> listMeasurements(@AuthenticationPrincipal final Jwt jwt,
			@Parameter(description = "Inclusive start instant (ISO-8601)") @RequestParam(
					required = false) final Instant from,
			@Parameter(description = "Inclusive end instant (ISO-8601)") @RequestParam(
					required = false) final Instant to,
			@Parameter(description = "Maximum rows (capped at 365)") @RequestParam(
					required = false) final Integer maxRows) {
		final Paciente paciente = getAuthenticatedPaciente(jwt);
		if (log.isDebugEnabled()) {
			log.debug("Mobile progress measurements request for patient {}",
					LogRedaction.redactPaciente(paciente.getId()));
		}
		final ProgressMeasurementsDto measurements = mobilePatientProgressService.listMeasurements(paciente.getId(),
				from, to, maxRows);
		return ApiResponse.ok(measurements);
	}

}
