package com.nutriconsultas.mobile;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.nutriconsultas.mobile.config.MobileOpenApiResponses;
import com.nutriconsultas.mobile.dto.ApiResponse;
import com.nutriconsultas.mobile.dto.AppointmentQuestionDto;
import com.nutriconsultas.mobile.dto.CreateAppointmentQuestionRequest;
import com.nutriconsultas.mobile.dto.PagedResponse;
import com.nutriconsultas.mobile.dto.PatchAppointmentQuestionRequest;
import com.nutriconsultas.paciente.projection.PacienteAuthView;
import com.nutriconsultas.util.LogRedaction;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/rest/mobile/patient/appointment-questions")
@Tag(name = "Mobile", description = "Patient mobile API")
@Slf4j
public class MobilePatientAppointmentQuestionController extends AbstractMobilePatientController {

	private final MobilePatientAppointmentQuestionService mobilePatientAppointmentQuestionService;

	public MobilePatientAppointmentQuestionController(final PatientAuthService patientAuthService,
			final MobilePatientAppointmentQuestionService mobilePatientAppointmentQuestionService) {
		super(patientAuthService);
		this.mobilePatientAppointmentQuestionService = mobilePatientAppointmentQuestionService;
	}

	@GetMapping
	@Operation(summary = "List appointment questions",
			description = "Returns paged reminders of questions the patient wants to ask at the next appointment.")
	@MobileOpenApiResponses.AuthenticatedPatient
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
			description = "Paged appointment question summaries")
	public ApiResponse<PagedResponse<AppointmentQuestionDto>> listQuestions(@AuthenticationPrincipal final Jwt jwt,
			@Parameter(description = "Zero-based page index") @RequestParam(defaultValue = "0") final int page,
			@Parameter(description = "Page size") @RequestParam(defaultValue = "20") final int size,
			@Parameter(description = "Optional filter: true = answered, false = open") @RequestParam(
					required = false) final Boolean answered) {
		final Long pacienteId = getAuthenticatedPacienteId(jwt);
		if (log.isDebugEnabled()) {
			log.debug("Mobile list appointment questions for patient {}", LogRedaction.redactPaciente(pacienteId));
		}
		return ApiResponse.ok(mobilePatientAppointmentQuestionService.listQuestions(pacienteId, page, size, answered));
	}

	@GetMapping("/{questionId}")
	@Operation(summary = "Get appointment question", description = "Returns a single question reminder by id.")
	@MobileOpenApiResponses.AuthenticatedPatient
	@MobileOpenApiResponses.NotFoundWhenMissing
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
			description = "Appointment question detail")
	public ApiResponse<AppointmentQuestionDto> getQuestion(@AuthenticationPrincipal final Jwt jwt,
			@PathVariable final Long questionId) {
		final Long pacienteId = getAuthenticatedPacienteId(jwt);
		if (log.isDebugEnabled()) {
			log.debug("Mobile get appointment question {} for patient {}",
					LogRedaction.redactAppointmentQuestion(questionId), LogRedaction.redactPaciente(pacienteId));
		}
		return ApiResponse.ok(mobilePatientAppointmentQuestionService.getQuestion(pacienteId, questionId));
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Create appointment question",
			description = "Creates a reminder question for the patient's next appointment.")
	@MobileOpenApiResponses.AuthenticatedPatient
	@MobileOpenApiResponses.WriteEndpoint
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201",
			description = "Created appointment question")
	public ApiResponse<AppointmentQuestionDto> createQuestion(@AuthenticationPrincipal final Jwt jwt,
			@Valid @RequestBody final CreateAppointmentQuestionRequest request) {
		final PacienteAuthView authView = getAuthenticatedPacienteAuthView(jwt);
		if (log.isDebugEnabled()) {
			log.debug("Mobile create appointment question for patient {}",
					LogRedaction.redactPaciente(authView.getId()));
		}
		return ApiResponse.ok(mobilePatientAppointmentQuestionService.createQuestion(authView, request.body()));
	}

	@PatchMapping("/{questionId}")
	@Operation(summary = "Update appointment question",
			description = "Updates question text and/or answered flag. Omitted fields are unchanged.")
	@MobileOpenApiResponses.AuthenticatedPatient
	@MobileOpenApiResponses.WriteEndpoint
	@MobileOpenApiResponses.NotFoundWhenMissing
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
			description = "Updated appointment question")
	public ApiResponse<AppointmentQuestionDto> patchQuestion(@AuthenticationPrincipal final Jwt jwt,
			@PathVariable final Long questionId, @Valid @RequestBody final PatchAppointmentQuestionRequest request) {
		final Long pacienteId = getAuthenticatedPacienteId(jwt);
		if (log.isDebugEnabled()) {
			log.debug("Mobile patch appointment question {} for patient {}",
					LogRedaction.redactAppointmentQuestion(questionId), LogRedaction.redactPaciente(pacienteId));
		}
		return ApiResponse.ok(mobilePatientAppointmentQuestionService.patchQuestion(pacienteId, questionId,
				request.body(), request.answered()));
	}

	@DeleteMapping("/{questionId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "Delete appointment question",
			description = "Deletes a question reminder owned by the patient.")
	@MobileOpenApiResponses.AuthenticatedPatient
	@MobileOpenApiResponses.WriteEndpoint
	@MobileOpenApiResponses.NotFoundWhenMissing
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "Question deleted")
	public void deleteQuestion(@AuthenticationPrincipal final Jwt jwt, @PathVariable final Long questionId) {
		final Long pacienteId = getAuthenticatedPacienteId(jwt);
		if (log.isDebugEnabled()) {
			log.debug("Mobile delete appointment question {} for patient {}",
					LogRedaction.redactAppointmentQuestion(questionId), LogRedaction.redactPaciente(pacienteId));
		}
		mobilePatientAppointmentQuestionService.deleteQuestion(pacienteId, questionId);
	}

}
