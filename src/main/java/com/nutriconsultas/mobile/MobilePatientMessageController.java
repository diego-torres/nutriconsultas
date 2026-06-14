package com.nutriconsultas.mobile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.nutriconsultas.mobile.dto.ApiResponse;
import com.nutriconsultas.mobile.dto.CursorPagedResponse;
import com.nutriconsultas.mobile.dto.PatientMessageSummaryDto;
import com.nutriconsultas.paciente.Paciente;
import com.nutriconsultas.util.LogRedaction;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/rest/mobile/patient/messages")
@Slf4j
public class MobilePatientMessageController extends AbstractMobilePatientController {

	private final MobilePatientMessageService mobilePatientMessageService;

	public MobilePatientMessageController(final PatientAuthService patientAuthService,
			final MobilePatientMessageService mobilePatientMessageService) {
		super(patientAuthService);
		this.mobilePatientMessageService = mobilePatientMessageService;
	}

	@GetMapping
	public ApiResponse<CursorPagedResponse<PatientMessageSummaryDto>> listMessages(
			@AuthenticationPrincipal final Jwt jwt, @RequestParam(required = false) final String cursor,
			@RequestParam(defaultValue = "20") final int size) {
		final Paciente paciente = getAuthenticatedPaciente(jwt);
		if (log.isDebugEnabled()) {
			log.debug("Mobile list messages request for patient {}", LogRedaction.redactPaciente(paciente.getId()));
		}
		final CursorPagedResponse<PatientMessageSummaryDto> messages = mobilePatientMessageService
			.listMessages(paciente.getId(), cursor, size);
		return ApiResponse.ok(messages);
	}

}
