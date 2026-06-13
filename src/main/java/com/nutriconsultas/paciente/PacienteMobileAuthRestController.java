package com.nutriconsultas.paciente;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nutriconsultas.mobile.Auth0ManagementNotConfiguredException;
import com.nutriconsultas.mobile.PatientAuthLinkageService;
import com.nutriconsultas.mobile.PatientAuthSubAlreadyLinkedException;
import com.nutriconsultas.mobile.PatientAuthUserNotFoundException;
import com.nutriconsultas.mobile.PatientEmailRequiredForLinkException;
import com.nutriconsultas.mobile.PatientMobileAuthStatus;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/rest/pacientes/{pacienteId}/mobile-auth")
@Slf4j
public class PacienteMobileAuthRestController {

	private final PatientAuthLinkageService patientAuthLinkageService;

	public PacienteMobileAuthRestController(final PatientAuthLinkageService patientAuthLinkageService) {
		this.patientAuthLinkageService = patientAuthLinkageService;
	}

	@GetMapping
	public ResponseEntity<Map<String, Object>> getStatus(@PathVariable @NonNull final Long pacienteId,
			@AuthenticationPrincipal final OidcUser principal) {
		final String userId = requireUserId(principal);
		try {
			final PatientMobileAuthStatus status = patientAuthLinkageService.getStatus(pacienteId, userId);
			final Map<String, Object> body = new LinkedHashMap<>(status.toMap());
			body.put("success", true);
			return ResponseEntity.ok(body);
		}
		catch (IllegalArgumentException ex) {
			return notFound(ex.getMessage());
		}
	}

	@PostMapping
	public ResponseEntity<Map<String, Object>> link(@PathVariable @NonNull final Long pacienteId,
			@RequestBody(required = false) final Map<String, String> body,
			@AuthenticationPrincipal final OidcUser principal) {
		final String userId = requireUserId(principal);
		try {
			final String patientAuthSub = body != null ? body.get("patientAuthSub") : null;
			if (StringUtils.hasText(patientAuthSub)) {
				patientAuthLinkageService.linkBySub(pacienteId, userId, patientAuthSub);
			}
			else {
				patientAuthLinkageService.linkByEmail(pacienteId, userId);
			}
			final PatientMobileAuthStatus status = patientAuthLinkageService.getStatus(pacienteId, userId);
			final Map<String, Object> response = new LinkedHashMap<>(status.toMap());
			response.put("success", true);
			return ResponseEntity.ok(response);
		}
		catch (IllegalArgumentException ex) {
			return notFound(ex.getMessage());
		}
		catch (PatientAuthSubAlreadyLinkedException ex) {
			return conflict("patient_auth_sub_already_linked", "Esta cuenta Auth0 ya está vinculada a otro paciente.");
		}
		catch (PatientAuthUserNotFoundException ex) {
			return badRequest("patient_auth_user_not_found",
					"No se encontró un usuario Auth0 con el correo del paciente.");
		}
		catch (PatientEmailRequiredForLinkException ex) {
			return badRequest("patient_email_required", "El paciente debe tener un correo registrado.");
		}
		catch (Auth0ManagementNotConfiguredException ex) {
			return badRequest("auth0_management_not_configured",
					"Vinculación por correo no disponible. Use el identificador Auth0 (sub) o configure AUTH0_MGMT_*.");
		}
	}

	@DeleteMapping
	public ResponseEntity<Map<String, Object>> unlink(@PathVariable @NonNull final Long pacienteId,
			@AuthenticationPrincipal final OidcUser principal) {
		final String userId = requireUserId(principal);
		try {
			patientAuthLinkageService.unlink(pacienteId, userId);
			final PatientMobileAuthStatus status = patientAuthLinkageService.getStatus(pacienteId, userId);
			final Map<String, Object> response = new LinkedHashMap<>(status.toMap());
			response.put("success", true);
			return ResponseEntity.ok(response);
		}
		catch (IllegalArgumentException ex) {
			return notFound(ex.getMessage());
		}
	}

	private static String requireUserId(final OidcUser principal) {
		if (principal == null || !StringUtils.hasText(principal.getSubject())) {
			throw new IllegalStateException("Not authenticated");
		}
		return principal.getSubject();
	}

	private static ResponseEntity<Map<String, Object>> notFound(final String message) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
			.body(Map.of("success", false, "error", message != null ? message : "Paciente no encontrado"));
	}

	private static ResponseEntity<Map<String, Object>> badRequest(final String code, final String message) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
			.body(Map.of("success", false, "error", code, "message", message));
	}

	private static ResponseEntity<Map<String, Object>> conflict(final String code, final String message) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
			.body(Map.of("success", false, "error", code, "message", message));
	}

}
