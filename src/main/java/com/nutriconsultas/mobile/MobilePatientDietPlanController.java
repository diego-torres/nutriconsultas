package com.nutriconsultas.mobile;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.nutriconsultas.mobile.config.MobileOpenApiResponses;
import com.nutriconsultas.mobile.dto.ApiResponse;
import com.nutriconsultas.mobile.dto.DietPlanDetailDto;
import com.nutriconsultas.mobile.dto.DietPlanPdfResult;
import com.nutriconsultas.mobile.dto.DietPlanSummaryDto;
import com.nutriconsultas.mobile.dto.DietPlatilloDetailDto;
import com.nutriconsultas.mobile.dto.PagedResponse;
import com.nutriconsultas.util.LogRedaction;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/rest/mobile/patient/diet-plans")
@Tag(name = "Mobile", description = "Patient mobile API")
@Slf4j
public class MobilePatientDietPlanController extends AbstractMobilePatientController {

	private final MobilePatientDietPlanService mobilePatientDietPlanService;

	public MobilePatientDietPlanController(final PatientAuthService patientAuthService,
			final MobilePatientDietPlanService mobilePatientDietPlanService) {
		super(patientAuthService);
		this.mobilePatientDietPlanService = mobilePatientDietPlanService;
	}

	@GetMapping
	@Operation(summary = "List diet plans", description = "Returns assigned diet plans for the authenticated patient.")
	@MobileOpenApiResponses.AuthenticatedPatient
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
			description = "Paged diet plan summaries")
	public ApiResponse<PagedResponse<DietPlanSummaryDto>> listDietPlans(@AuthenticationPrincipal final Jwt jwt,
			@Parameter(description = "Zero-based page index") @RequestParam(defaultValue = "0") final int page,
			@Parameter(description = "Page size") @RequestParam(defaultValue = "20") final int size,
			@Parameter(description = "When true, return only active assignments") @RequestParam(
					defaultValue = "false") final boolean activeOnly) {
		final Long pacienteId = getAuthenticatedPacienteId(jwt);
		if (log.isDebugEnabled()) {
			log.debug("Mobile list diet plans for patient {} activeOnly={}", LogRedaction.redactPaciente(pacienteId),
					activeOnly);
		}
		final PagedResponse<DietPlanSummaryDto> plans = mobilePatientDietPlanService.listDietPlans(pacienteId, page,
				size, activeOnly);
		return ApiResponse.ok(plans);
	}

	@GetMapping("/{assignmentId}")
	@Operation(summary = "Get diet plan detail",
			description = "Returns structured meal JSON for an assignment owned by the patient.")
	@MobileOpenApiResponses.AuthenticatedPatient
	@MobileOpenApiResponses.NotFoundWhenMissing
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
			description = "Diet plan detail with ingestas and platillos")
	public ApiResponse<DietPlanDetailDto> getDietPlanDetail(@AuthenticationPrincipal final Jwt jwt,
			@Parameter(description = "PacienteDieta assignment identifier") @PathVariable final Long assignmentId) {
		final Long pacienteId = getAuthenticatedPacienteId(jwt);
		if (log.isDebugEnabled()) {
			log.debug("Mobile get diet plan {} for patient {}", LogRedaction.redactPacienteDieta(assignmentId),
					LogRedaction.redactPaciente(pacienteId));
		}
		final DietPlanDetailDto plan = mobilePatientDietPlanService.getDietPlanDetail(pacienteId, assignmentId);
		return ApiResponse.ok(plan);
	}

	@GetMapping("/{assignmentId}/platillos/{platilloIngestaId}")
	@Operation(summary = "Get platillo detail",
			description = "Returns ingredients, preparation, and nutrition for a platillo in the patient's assignment.")
	@MobileOpenApiResponses.AuthenticatedPatient
	@MobileOpenApiResponses.NotFoundWhenMissing
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
			description = "Platillo detail with ingredients and nutrition facts")
	public ApiResponse<DietPlatilloDetailDto> getPlatilloDetail(@AuthenticationPrincipal final Jwt jwt,
			@Parameter(description = "PacienteDieta assignment identifier") @PathVariable final Long assignmentId,
			@Parameter(
					description = "PlatilloIngesta identifier within the assignment") @PathVariable final Long platilloIngestaId) {
		final Long pacienteId = getAuthenticatedPacienteId(jwt);
		if (log.isDebugEnabled()) {
			log.debug("Mobile get platillo {} assignment {} for patient {}", platilloIngestaId,
					LogRedaction.redactPacienteDieta(assignmentId), LogRedaction.redactPaciente(pacienteId));
		}
		final DietPlatilloDetailDto detail = mobilePatientDietPlanService.getPlatilloDetail(pacienteId, assignmentId,
				platilloIngestaId);
		return ApiResponse.ok(detail);
	}

	@GetMapping(value = "/{assignmentId}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
	@Operation(summary = "Download diet plan PDF",
			description = "Returns a printable PDF for an assignment owned by the patient.")
	@MobileOpenApiResponses.AuthenticatedPatient
	@MobileOpenApiResponses.NotFoundWhenMissing
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "PDF bytes",
			content = @Content(mediaType = MediaType.APPLICATION_PDF_VALUE))
	public ResponseEntity<byte[]> getDietPlanPdf(@AuthenticationPrincipal final Jwt jwt,
			@Parameter(description = "PacienteDieta assignment identifier") @PathVariable final Long assignmentId) {
		final Long pacienteId = getAuthenticatedPacienteId(jwt);
		if (log.isDebugEnabled()) {
			log.debug("Mobile get diet plan PDF {} for patient {}", LogRedaction.redactPacienteDieta(assignmentId),
					LogRedaction.redactPaciente(pacienteId));
		}
		final DietPlanPdfResult pdf = mobilePatientDietPlanService.generateDietPlanPdf(pacienteId, assignmentId);
		return ResponseEntity.ok()
			.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + pdf.filename() + "\"")
			.contentType(MediaType.APPLICATION_PDF)
			.body(pdf.content());
	}

}
