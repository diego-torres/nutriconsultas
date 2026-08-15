package com.nutriconsultas.paciente;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nutriconsultas.mobile.dto.DietPlanDetailDto;
import com.nutriconsultas.paciente.preview.PacienteMobilePreviewService;
import com.nutriconsultas.paciente.preview.PatientMobilePreviewDto;
import com.nutriconsultas.util.LogRedaction;

import lombok.extern.slf4j.Slf4j;

/**
 * Nutritionist-session REST for patient mobile-app preview (phone-frame modal).
 */
@RestController
@RequestMapping("/rest/pacientes/{pacienteId}/mobile-preview")
@Slf4j
public class PacienteMobilePreviewRestController {

	private final PacienteMobilePreviewService pacienteMobilePreviewService;

	public PacienteMobilePreviewRestController(final PacienteMobilePreviewService pacienteMobilePreviewService) {
		this.pacienteMobilePreviewService = pacienteMobilePreviewService;
	}

	@GetMapping
	public ResponseEntity<Map<String, Object>> getPreview(@PathVariable @NonNull final Long pacienteId,
			@AuthenticationPrincipal final OidcUser principal) {
		final String userId = requireUserId(principal);
		try {
			final PatientMobilePreviewDto preview = pacienteMobilePreviewService.buildPreview(pacienteId, userId);
			final Map<String, Object> body = new LinkedHashMap<>();
			body.put("success", true);
			body.put("firstName", preview.firstName());
			body.put("nutritionistDisplayName", preview.nutritionistDisplayName());
			body.put("avatarUrl", preview.avatarUrl());
			body.put("progress", preview.progress());
			body.put("activePlan", preview.activePlan());
			body.put("activePlanDetail", preview.activePlanDetail());
			body.put("plans", preview.plans());
			body.put("nextVisit", preview.nextVisit());
			if (log.isDebugEnabled()) {
				log.debug("Returning mobile preview for patient {}", LogRedaction.redactPaciente(pacienteId));
			}
			return ResponseEntity.ok(body);
		}
		catch (IllegalArgumentException ex) {
			final String message = ex.getMessage() != null ? ex.getMessage() : "Paciente no encontrado";
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("success", false, "error", message));
		}
	}

	@GetMapping("/diet-plans/{assignmentId}")
	public ResponseEntity<Map<String, Object>> getDietPlanDetail(@PathVariable @NonNull final Long pacienteId,
			@PathVariable @NonNull final Long assignmentId, @AuthenticationPrincipal final OidcUser principal) {
		final String userId = requireUserId(principal);
		try {
			final DietPlanDetailDto plan = pacienteMobilePreviewService.getPlanDetail(pacienteId, userId, assignmentId);
			final Map<String, Object> body = new LinkedHashMap<>();
			body.put("success", true);
			body.put("plan", plan);
			if (log.isDebugEnabled()) {
				log.debug("Returning mobile preview diet plan {} for patient {}",
						LogRedaction.redactPacienteDieta(assignmentId), LogRedaction.redactPaciente(pacienteId));
			}
			return ResponseEntity.ok(body);
		}
		catch (IllegalArgumentException ex) {
			final String message = ex.getMessage() != null ? ex.getMessage() : "Plan alimentario no encontrado";
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("success", false, "error", message));
		}
	}

	private static String requireUserId(final OidcUser principal) {
		if (principal == null || !StringUtils.hasText(principal.getSubject())) {
			throw new IllegalStateException("Not authenticated");
		}
		return principal.getSubject();
	}

}
