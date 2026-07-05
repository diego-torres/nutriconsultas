package com.nutriconsultas.ai;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.lang.Nullable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.nutriconsultas.dieta.Dieta;
import com.nutriconsultas.dieta.DietaCatalogConstants;
import com.nutriconsultas.dieta.DietaService;
import com.nutriconsultas.paciente.PacienteRepository;
import com.nutriconsultas.platillos.Platillo;
import com.nutriconsultas.platillos.PlatilloCatalogConstants;
import com.nutriconsultas.platillos.PlatilloService;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Enables the floating AI assistant on patient, dieta, and platillo admin pages.
 */
@ControllerAdvice
public class AiAssistantWidgetAdvice {

	private static final Pattern PATIENT_DETAIL = Pattern.compile("^/admin/pacientes/(\\d+)(?:/.*)?$");

	private static final Pattern PATIENT_DIETA_EDIT = Pattern
		.compile("^/admin/pacientes/(\\d+)/dietas/(\\d+)(?:/.*)?$");

	private static final Pattern DIETA_DETAIL = Pattern.compile("^/admin/dietas/(\\d+)(?:/.*)?$");

	private static final Pattern PLATILLO_DETAIL = Pattern.compile("^/admin/platillos/(\\d+)(?:/.*)?$");

	private final AiProperties aiProperties;

	private final PacienteRepository pacienteRepository;

	private final DietaService dietaService;

	private final PlatilloService platilloService;

	private final AiEntitlementGuard aiEntitlementGuard;

	public AiAssistantWidgetAdvice(final AiProperties aiProperties, final PacienteRepository pacienteRepository,
			final DietaService dietaService, final PlatilloService platilloService,
			final AiEntitlementGuard aiEntitlementGuard) {
		this.aiProperties = aiProperties;
		this.pacienteRepository = pacienteRepository;
		this.dietaService = dietaService;
		this.platilloService = platilloService;
		this.aiEntitlementGuard = aiEntitlementGuard;
	}

	@ModelAttribute("aiAssistantWidgetContext")
	@Nullable
	public AiAssistantWidgetContext aiAssistantWidgetContext(final HttpServletRequest request,
			@AuthenticationPrincipal final OidcUser principal) {
		if (!aiProperties.isEnabled() || principal == null || principal.getSubject() == null) {
			return null;
		}
		if (!aiEntitlementGuard.canUseAiAssistant(principal.getSubject())) {
			return null;
		}
		final String path = normalizePath(request.getRequestURI());
		if (path == null) {
			return null;
		}
		final String userId = principal.getSubject();
		if ("/admin/dietas".equals(path)) {
			return new AiAssistantWidgetContext("Catálogo de dietas", null, null, null);
		}
		if ("/admin/platillos".equals(path)) {
			return new AiAssistantWidgetContext("Catálogo de platillos", null, null, null);
		}
		final Matcher patientDietaEdit = PATIENT_DIETA_EDIT.matcher(path);
		if (patientDietaEdit.matches()) {
			return buildPatientDietaContext(Long.parseLong(patientDietaEdit.group(1)),
					Long.parseLong(patientDietaEdit.group(2)), userId);
		}
		final Matcher patientDetail = PATIENT_DETAIL.matcher(path);
		if (patientDetail.matches() && !path.endsWith("/nuevo")) {
			final long patientId = Long.parseLong(patientDetail.group(1));
			if (pacienteRepository.findByIdAndUserId(patientId, userId).isEmpty()) {
				return null;
			}
			return new AiAssistantWidgetContext("Registro del paciente", patientId, null, null);
		}
		final Matcher dietaDetail = DIETA_DETAIL.matcher(path);
		if (dietaDetail.matches()) {
			return buildDietaContext(Long.parseLong(dietaDetail.group(1)), userId);
		}
		final Matcher platilloDetail = PLATILLO_DETAIL.matcher(path);
		if (platilloDetail.matches()) {
			return buildPlatilloContext(Long.parseLong(platilloDetail.group(1)), userId);
		}
		return null;
	}

	@Nullable
	private AiAssistantWidgetContext buildPatientDietaContext(final long patientId, final long dietaId,
			final String userId) {
		if (pacienteRepository.findByIdAndUserId(patientId, userId).isEmpty()) {
			return null;
		}
		final Dieta dieta = dietaService.getDieta(dietaId);
		if (dieta == null || !canAccessDieta(dieta, userId)) {
			return null;
		}
		final String label = dieta.getNombre() != null ? "Dieta del paciente: " + dieta.getNombre()
				: "Dieta del paciente";
		return new AiAssistantWidgetContext(label, patientId, dietaId, null);
	}

	@Nullable
	private AiAssistantWidgetContext buildDietaContext(final long dietaId, final String userId) {
		final Dieta dieta = dietaService.getDieta(dietaId);
		if (dieta == null || !canAccessDieta(dieta, userId)) {
			return null;
		}
		final Long patientId = DietaCatalogConstants.isPatientAssignment(dieta) ? dieta.getPacienteId() : null;
		final String label = dieta.getNombre() != null ? "Dieta: " + dieta.getNombre() : "Dieta en edición";
		return new AiAssistantWidgetContext(label, patientId, dietaId, null);
	}

	@Nullable
	private AiAssistantWidgetContext buildPlatilloContext(final long platilloId, final String userId) {
		Platillo platillo = platilloService.findByIdAndUserId(platilloId, userId);
		if (platillo == null) {
			platillo = platilloService.findById(platilloId);
			if (platillo == null || !PlatilloCatalogConstants.isSystemCatalog(platillo)) {
				return null;
			}
		}
		final String label = platillo.getName() != null ? "Platillo: " + platillo.getName() : "Platillo en edición";
		return new AiAssistantWidgetContext(label, null, null, platilloId);
	}

	private boolean canAccessDieta(final Dieta dieta, final String userId) {
		return !DietaCatalogConstants.isPatientAssignment(dieta) || (dieta.getPacienteId() != null
				&& pacienteRepository.findByIdAndUserId(dieta.getPacienteId(), userId).isPresent());
	}

	@Nullable
	private static String normalizePath(final String requestUri) {
		if (requestUri == null || requestUri.isBlank()) {
			return null;
		}
		final int queryIndex = requestUri.indexOf('?');
		String path = queryIndex >= 0 ? requestUri.substring(0, queryIndex) : requestUri;
		if (path.length() > 1 && path.endsWith("/")) {
			path = path.substring(0, path.length() - 1);
		}
		return path;
	}

}
