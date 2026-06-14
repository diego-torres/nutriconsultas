package com.nutriconsultas.message;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.nutriconsultas.message.dto.PatientMessageThreadItemDto;
import com.nutriconsultas.message.dto.PatientUnreadMessageDto;
import com.nutriconsultas.message.dto.SendPatientMessageRequest;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/rest/patient-messages")
@Slf4j
@Validated
public class PatientMessageRestController {

	private final PatientMessageService patientMessageService;

	public PatientMessageRestController(final PatientMessageService patientMessageService) {
		this.patientMessageService = patientMessageService;
	}

	@GetMapping("/unread")
	public List<PatientUnreadMessageDto> listUnread(@AuthenticationPrincipal final OidcUser principal) {
		final String userId = requireUserId(principal);
		return patientMessageService.listUnreadSummaries(userId);
	}

	@GetMapping("/unread/count")
	public Map<String, Long> countUnread(@AuthenticationPrincipal final OidcUser principal) {
		final String userId = requireUserId(principal);
		return Map.of("count", patientMessageService.countUnread(userId));
	}

	@GetMapping("/thread/{pacienteId}")
	public List<PatientMessageThreadItemDto> listThread(@AuthenticationPrincipal final OidcUser principal,
			@PathVariable final Long pacienteId) {
		final String userId = requireUserId(principal);
		return patientMessageService.listThread(pacienteId, userId);
	}

	@PostMapping("/thread/{pacienteId}")
	public PatientMessageThreadItemDto sendMessage(@AuthenticationPrincipal final OidcUser principal,
			@PathVariable final Long pacienteId, @Valid @RequestBody final SendPatientMessageRequest request) {
		final String userId = requireUserId(principal);
		return patientMessageService.sendAsNutritionist(pacienteId, userId, request.body());
	}

	@PostMapping("/thread/{pacienteId}/read")
	public Map<String, String> markRead(@AuthenticationPrincipal final OidcUser principal,
			@PathVariable final Long pacienteId) {
		final String userId = requireUserId(principal);
		patientMessageService.markThreadReadByNutritionist(pacienteId, userId);
		return Map.of("status", "ok");
	}

	private String requireUserId(final OidcUser principal) {
		if (principal == null || principal.getSubject() == null) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
		}
		return principal.getSubject();
	}

}
