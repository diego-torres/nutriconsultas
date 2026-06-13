package com.nutriconsultas.mobile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.nutriconsultas.mobile.dto.ApiResponse;
import com.nutriconsultas.mobile.dto.DietPlanSummaryDto;
import com.nutriconsultas.mobile.dto.PagedResponse;
import com.nutriconsultas.paciente.Paciente;
import com.nutriconsultas.util.LogRedaction;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/rest/mobile/patient/diet-plans")
@Slf4j
public class MobilePatientDietPlanController extends AbstractMobilePatientController {

	private final MobilePatientDietPlanService mobilePatientDietPlanService;

	public MobilePatientDietPlanController(final PatientAuthService patientAuthService,
			final MobilePatientDietPlanService mobilePatientDietPlanService) {
		super(patientAuthService);
		this.mobilePatientDietPlanService = mobilePatientDietPlanService;
	}

	@GetMapping
	public ApiResponse<PagedResponse<DietPlanSummaryDto>> listDietPlans(@AuthenticationPrincipal final Jwt jwt,
			@RequestParam(defaultValue = "0") final int page, @RequestParam(defaultValue = "20") final int size,
			@RequestParam(defaultValue = "false") final boolean activeOnly) {
		final Paciente paciente = getAuthenticatedPaciente(jwt);
		if (log.isDebugEnabled()) {
			log.debug("Mobile list diet plans for patient {} activeOnly={}",
					LogRedaction.redactPaciente(paciente.getId()), activeOnly);
		}
		final PagedResponse<DietPlanSummaryDto> plans = mobilePatientDietPlanService.listDietPlans(paciente.getId(),
				page, size, activeOnly);
		return ApiResponse.ok(plans);
	}

}
