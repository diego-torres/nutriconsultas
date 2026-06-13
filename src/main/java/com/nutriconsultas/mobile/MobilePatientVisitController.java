package com.nutriconsultas.mobile;

import java.time.Instant;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.nutriconsultas.calendar.EventStatus;
import com.nutriconsultas.mobile.dto.ApiResponse;
import com.nutriconsultas.mobile.dto.PagedResponse;
import com.nutriconsultas.mobile.dto.VisitDetailDto;
import com.nutriconsultas.mobile.dto.VisitSummaryDto;
import com.nutriconsultas.paciente.Paciente;
import com.nutriconsultas.util.LogRedaction;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/rest/mobile/patient/visits")
@Slf4j
public class MobilePatientVisitController extends AbstractMobilePatientController {

	private final MobilePatientVisitService mobilePatientVisitService;

	public MobilePatientVisitController(final PatientAuthService patientAuthService,
			final MobilePatientVisitService mobilePatientVisitService) {
		super(patientAuthService);
		this.mobilePatientVisitService = mobilePatientVisitService;
	}

	@GetMapping
	public ApiResponse<PagedResponse<VisitSummaryDto>> listVisits(@AuthenticationPrincipal final Jwt jwt,
			@RequestParam(defaultValue = "0") final int page, @RequestParam(defaultValue = "20") final int size,
			@RequestParam(required = false) final EventStatus status,
			@RequestParam(required = false) final Instant from, @RequestParam(required = false) final Instant to) {
		final Paciente paciente = getAuthenticatedPaciente(jwt);
		if (log.isDebugEnabled()) {
			log.debug("Mobile list visits request for patient {}", LogRedaction.redactPaciente(paciente.getId()));
		}
		final PagedResponse<VisitSummaryDto> visits = mobilePatientVisitService.listVisits(paciente.getId(), page, size,
				status, from, to);
		return ApiResponse.ok(visits);
	}

	@GetMapping("/{visitId}")
	public ApiResponse<VisitDetailDto> getVisitDetail(@AuthenticationPrincipal final Jwt jwt,
			@PathVariable final Long visitId) {
		final Paciente paciente = getAuthenticatedPaciente(jwt);
		if (log.isDebugEnabled()) {
			log.debug("Mobile get visit {} for patient {}", visitId, LogRedaction.redactPaciente(paciente.getId()));
		}
		final VisitDetailDto visit = mobilePatientVisitService.getVisitDetail(paciente.getId(), visitId);
		return ApiResponse.ok(visit);
	}

}
