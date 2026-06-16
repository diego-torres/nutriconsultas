package com.nutriconsultas.mobile;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.nutriconsultas.mobile.config.MobileOpenApiResponses;
import com.nutriconsultas.mobile.dto.ApiResponse;
import com.nutriconsultas.mobile.dto.CursorPagedResponse;
import com.nutriconsultas.mobile.dto.PatientMessageSummaryDto;
import com.nutriconsultas.mobile.dto.SendPatientMessageRequest;
import com.nutriconsultas.paciente.projection.PacienteAuthView;
import com.nutriconsultas.util.LogRedaction;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/rest/mobile/patient/messages")
@Tag(name = "Mobile", description = "Patient mobile API")
@Slf4j
public class MobilePatientMessageController extends AbstractMobilePatientController {

	private final MobilePatientMessageService mobilePatientMessageService;

	public MobilePatientMessageController(final PatientAuthService patientAuthService,
			final MobilePatientMessageService mobilePatientMessageService) {
		super(patientAuthService);
		this.mobilePatientMessageService = mobilePatientMessageService;
	}

	@GetMapping
	@Operation(summary = "List messages", description = "Returns cursor-paged messages for the authenticated patient.")
	@MobileOpenApiResponses.AuthenticatedPatient
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
			description = "Cursor-paged message summaries")
	public ApiResponse<CursorPagedResponse<PatientMessageSummaryDto>> listMessages(
			@AuthenticationPrincipal final Jwt jwt,
			@Parameter(description = "Opaque cursor from a previous page") @RequestParam(
					required = false) final String cursor,
			@Parameter(description = "Page size") @RequestParam(defaultValue = "20") final int size) {
		final Long pacienteId = getAuthenticatedPacienteId(jwt);
		if (log.isDebugEnabled()) {
			log.debug("Mobile list messages request for patient {}", LogRedaction.redactPaciente(pacienteId));
		}
		final CursorPagedResponse<PatientMessageSummaryDto> messages = mobilePatientMessageService
			.listMessages(pacienteId, cursor, size);
		return ApiResponse.ok(messages);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Send message", description = "Creates a patient-to-nutritionist message.")
	@MobileOpenApiResponses.AuthenticatedPatient
	@MobileOpenApiResponses.WriteEndpoint
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Created message summary")
	public ApiResponse<PatientMessageSummaryDto> sendMessage(@AuthenticationPrincipal final Jwt jwt,
			@Valid @RequestBody final SendPatientMessageRequest request) {
		final PacienteAuthView authView = getAuthenticatedPacienteAuthView(jwt);
		if (log.isDebugEnabled()) {
			log.debug("Mobile send message request for patient {}", LogRedaction.redactPaciente(authView.getId()));
		}
		final PatientMessageSummaryDto sent = mobilePatientMessageService.sendMessage(authView, request.body());
		return ApiResponse.ok(sent);
	}

}
