package com.nutriconsultas.mobile;

import java.time.Instant;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.nutriconsultas.mobile.dto.ApiResponse;
import com.nutriconsultas.mobile.dto.PatientProgressSnapshotDto;
import com.nutriconsultas.mobile.dto.ProgressMeasurementsDto;
import com.nutriconsultas.paciente.Paciente;
import com.nutriconsultas.util.LogRedaction;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/rest/mobile/patient/progress")
@Slf4j
public class MobilePatientProgressController extends AbstractMobilePatientController {

	private final MobilePatientProgressService mobilePatientProgressService;

	public MobilePatientProgressController(final PatientAuthService patientAuthService,
			final MobilePatientProgressService mobilePatientProgressService) {
		super(patientAuthService);
		this.mobilePatientProgressService = mobilePatientProgressService;
	}

	@GetMapping
	public ApiResponse<PatientProgressSnapshotDto> getProgress(@AuthenticationPrincipal final Jwt jwt) {
		final Paciente paciente = getAuthenticatedPaciente(jwt);
		if (log.isDebugEnabled()) {
			log.debug("Mobile progress snapshot request for patient {}", LogRedaction.redactPaciente(paciente.getId()));
		}
		final PatientProgressSnapshotDto snapshot = mobilePatientProgressService.getSnapshot(paciente.getId());
		return ApiResponse.ok(snapshot);
	}

	@GetMapping("/measurements")
	public ApiResponse<ProgressMeasurementsDto> listMeasurements(@AuthenticationPrincipal final Jwt jwt,
			@RequestParam(required = false) final Instant from, @RequestParam(required = false) final Instant to,
			@RequestParam(required = false) final Integer maxRows) {
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
