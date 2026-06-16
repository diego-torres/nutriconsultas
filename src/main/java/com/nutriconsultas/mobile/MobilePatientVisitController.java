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
import com.nutriconsultas.mobile.config.MobileOpenApiResponses;
import com.nutriconsultas.mobile.dto.ApiResponse;
import com.nutriconsultas.mobile.dto.PagedResponse;
import com.nutriconsultas.mobile.dto.VisitDetailDto;
import com.nutriconsultas.mobile.dto.VisitSummaryDto;
import com.nutriconsultas.util.LogRedaction;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/rest/mobile/patient/visits")
@Tag(name = "Mobile", description = "Patient mobile API")
@Slf4j
public class MobilePatientVisitController extends AbstractMobilePatientController {

	private final MobilePatientVisitService mobilePatientVisitService;

	public MobilePatientVisitController(final PatientAuthService patientAuthService,
			final MobilePatientVisitService mobilePatientVisitService) {
		super(patientAuthService);
		this.mobilePatientVisitService = mobilePatientVisitService;
	}

	@GetMapping
	@Operation(summary = "List patient visits",
			description = "Returns a paged list of visit summaries for the authenticated patient.")
	@MobileOpenApiResponses.AuthenticatedPatient
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Paged visit summaries")
	public ApiResponse<PagedResponse<VisitSummaryDto>> listVisits(@AuthenticationPrincipal final Jwt jwt,
			@Parameter(description = "Zero-based page index") @RequestParam(defaultValue = "0") final int page,
			@Parameter(description = "Page size") @RequestParam(defaultValue = "20") final int size,
			@Parameter(description = "Filter by visit status") @RequestParam(required = false) final EventStatus status,
			@Parameter(description = "Inclusive start instant (ISO-8601)") @RequestParam(
					required = false) final Instant from,
			@Parameter(description = "Inclusive end instant (ISO-8601)") @RequestParam(
					required = false) final Instant to) {
		final Long pacienteId = getAuthenticatedPacienteId(jwt);
		if (log.isDebugEnabled()) {
			log.debug("Mobile list visits request for patient {}", LogRedaction.redactPaciente(pacienteId));
		}
		final PagedResponse<VisitSummaryDto> visits = mobilePatientVisitService.listVisits(pacienteId, page, size,
				status, from, to);
		return ApiResponse.ok(visits);
	}

	@GetMapping("/{visitId}")
	@Operation(summary = "Get visit detail", description = "Returns a single visit owned by the authenticated patient.")
	@MobileOpenApiResponses.AuthenticatedPatient
	@MobileOpenApiResponses.NotFoundWhenMissing
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Visit detail")
	public ApiResponse<VisitDetailDto> getVisitDetail(@AuthenticationPrincipal final Jwt jwt,
			@Parameter(description = "Visit identifier") @PathVariable final Long visitId) {
		final Long pacienteId = getAuthenticatedPacienteId(jwt);
		if (log.isDebugEnabled()) {
			log.debug("Mobile get visit {} for patient {}", LogRedaction.redactCalendarEvent(visitId),
					LogRedaction.redactPaciente(pacienteId));
		}
		final VisitDetailDto visit = mobilePatientVisitService.getVisitDetail(pacienteId, visitId);
		return ApiResponse.ok(visit);
	}

}
