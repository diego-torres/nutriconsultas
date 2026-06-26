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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nutriconsultas.paciente.invitation.IssuedPatientMobileInvitationResult;
import com.nutriconsultas.paciente.invitation.PatientInvitationRevokeResult;
import com.nutriconsultas.paciente.invitation.PatientMobileInvitationNotAllowedException;
import com.nutriconsultas.paciente.invitation.PatientMobileInvitationService;
import com.nutriconsultas.paciente.invitation.PatientMobileInvitationStatus;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/rest/pacientes/{pacienteId}/mobile-invitation")
@Slf4j
public class PacienteMobileInvitationRestController {

	private final PatientMobileInvitationService patientMobileInvitationService;

	public PacienteMobileInvitationRestController(final PatientMobileInvitationService patientMobileInvitationService) {
		this.patientMobileInvitationService = patientMobileInvitationService;
	}

	@GetMapping
	public ResponseEntity<Map<String, Object>> getStatus(@PathVariable @NonNull final Long pacienteId,
			@AuthenticationPrincipal final OidcUser principal) {
		final String userId = requireUserId(principal);
		try {
			final PatientMobileInvitationStatus status = patientMobileInvitationService.getStatus(pacienteId, userId);
			final Map<String, Object> body = new LinkedHashMap<>(status.toMap());
			body.put("success", true);
			return ResponseEntity.ok(body);
		}
		catch (IllegalArgumentException ex) {
			return notFound(ex.getMessage());
		}
	}

	@PostMapping
	public ResponseEntity<Map<String, Object>> sendInvitation(@PathVariable @NonNull final Long pacienteId,
			@AuthenticationPrincipal final OidcUser principal) {
		final String userId = requireUserId(principal);
		try {
			final IssuedPatientMobileInvitationResult issued = patientMobileInvitationService.sendInvitation(pacienteId,
					userId);
			final Map<String, Object> body = new LinkedHashMap<>();
			body.put("success", true);
			body.put("invitationId", issued.invitationId());
			body.put("pacienteId", issued.pacienteId());
			body.put("humanCode", issued.humanCode());
			body.put("expiresAt", issued.expiresAt().toString());
			body.put("recipientEmailRedacted", issued.recipientEmailRedacted());
			body.put("message", "Invitación enviada correctamente.");
			return ResponseEntity.status(HttpStatus.CREATED).body(body);
		}
		catch (IllegalArgumentException ex) {
			return notFound(ex.getMessage());
		}
		catch (PatientMobileInvitationNotAllowedException ex) {
			return badRequest(ex.getMessageKey(), resolveNotAllowedMessage(ex.getMessageKey()));
		}
	}

	@DeleteMapping
	public ResponseEntity<Map<String, Object>> revokeInvitation(@PathVariable @NonNull final Long pacienteId,
			@AuthenticationPrincipal final OidcUser principal) {
		final String userId = requireUserId(principal);
		try {
			final PatientInvitationRevokeResult revoked = patientMobileInvitationService
				.revokePendingInvitation(pacienteId, userId);
			final Map<String, Object> body = new LinkedHashMap<>();
			body.put("success", true);
			body.put("invitationId", revoked.invitationId());
			body.put("pacienteId", revoked.pacienteId());
			body.put("status", revoked.status().name());
			body.put("message", "Invitación revocada.");
			return ResponseEntity.ok(body);
		}
		catch (IllegalArgumentException ex) {
			return notFound(ex.getMessage());
		}
		catch (PatientMobileInvitationNotAllowedException ex) {
			return badRequest(ex.getMessageKey(), resolveNotAllowedMessage(ex.getMessageKey()));
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

	private static String resolveNotAllowedMessage(final String code) {
		return switch (code) {
			case "NO_EMAIL" -> "El paciente debe tener un correo registrado para enviar la invitación.";
			case "NO_PENDING_INVITATION" -> "No hay una invitación pendiente para revocar.";
			case "LINKED" -> "El paciente ya está vinculado a la app móvil.";
			case "ONBOARDING" -> "El paciente se está registrando en la app.";
			case "REVOKED" -> "El acceso del paciente está revocado.";
			case "PENDING" -> "Ya existe una invitación pendiente.";
			default -> "No se puede enviar la invitación en el estado actual del paciente.";
		};
	}

}
