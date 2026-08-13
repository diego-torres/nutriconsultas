package com.nutriconsultas.mobile;

import java.time.Instant;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import com.nutriconsultas.appointmentquestion.AppointmentQuestion;
import com.nutriconsultas.appointmentquestion.AppointmentQuestionRepository;
import com.nutriconsultas.mobile.dto.AppointmentQuestionDto;
import com.nutriconsultas.mobile.dto.PagedResponse;
import com.nutriconsultas.paciente.Paciente;
import com.nutriconsultas.paciente.PacienteRepository;
import com.nutriconsultas.paciente.projection.PacienteAuthView;
import com.nutriconsultas.util.LogRedaction;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class MobilePatientAppointmentQuestionService {

	private static final int MAX_PAGE_SIZE = 100;

	private final AppointmentQuestionRepository appointmentQuestionRepository;

	private final PacienteRepository pacienteRepository;

	private final PatientWriteRateLimiter patientWriteRateLimiter;

	public MobilePatientAppointmentQuestionService(final AppointmentQuestionRepository appointmentQuestionRepository,
			final PacienteRepository pacienteRepository, final PatientWriteRateLimiter patientWriteRateLimiter) {
		this.appointmentQuestionRepository = appointmentQuestionRepository;
		this.pacienteRepository = pacienteRepository;
		this.patientWriteRateLimiter = patientWriteRateLimiter;
	}

	@Transactional(readOnly = true)
	public PagedResponse<AppointmentQuestionDto> listQuestions(final Long pacienteId, final int page, final int size,
			final Boolean answered) {
		final int safePage = Math.max(page, 0);
		final int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
		final Pageable pageable = PageRequest.of(safePage, safeSize);
		final Page<AppointmentQuestion> questions;
		if (answered == null) {
			questions = appointmentQuestionRepository.findByPacienteIdOrderByCreatedAtDesc(pacienteId, pageable);
		}
		else {
			questions = appointmentQuestionRepository.findByPacienteIdAndAnsweredOrderByCreatedAtDesc(pacienteId,
					answered, pageable);
		}
		final Page<AppointmentQuestionDto> dtos = questions.map(AppointmentQuestionDto::fromEntity);
		if (log.isDebugEnabled()) {
			log.debug("Listed appointment questions page={} size={} count={} for patient {}", safePage, safeSize,
					dtos.getNumberOfElements(), LogRedaction.redactPaciente(pacienteId));
		}
		return PagedResponse.of(dtos);
	}

	@Transactional(readOnly = true)
	public AppointmentQuestionDto getQuestion(final Long pacienteId, final Long questionId) {
		return appointmentQuestionRepository.findByIdAndPacienteId(questionId, pacienteId)
			.map(AppointmentQuestionDto::fromEntity)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
	}

	@Transactional
	public AppointmentQuestionDto createQuestion(final PacienteAuthView authView, final String body) {
		return patientWriteRateLimiter.execute(PatientWriteRateLimiter.PATIENT_APPOINTMENT_QUESTIONS,
				authView.getPatientAuthSub(), () -> persistQuestion(authView, body));
	}

	@Transactional
	public AppointmentQuestionDto patchQuestion(final Long pacienteId, final Long questionId, final String body,
			final Boolean answered) {
		final AppointmentQuestion question = requireOwnedQuestion(pacienteId, questionId);
		if (body != null) {
			final String trimmed = body.trim();
			if (!StringUtils.hasText(trimmed)) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Question body must not be blank");
			}
			question.setBody(trimmed);
		}
		if (answered != null) {
			applyAnswered(question, answered);
		}
		final AppointmentQuestion saved = appointmentQuestionRepository.save(question);
		if (log.isInfoEnabled()) {
			log.info("Updated appointment question {}", LogRedaction.redactAppointmentQuestion(saved.getId()));
		}
		return AppointmentQuestionDto.fromEntity(saved);
	}

	@Transactional
	public void deleteQuestion(final Long pacienteId, final Long questionId) {
		final AppointmentQuestion question = requireOwnedQuestion(pacienteId, questionId);
		appointmentQuestionRepository.delete(question);
		if (log.isInfoEnabled()) {
			log.info("Deleted appointment question {}", LogRedaction.redactAppointmentQuestion(questionId));
		}
	}

	private AppointmentQuestionDto persistQuestion(final PacienteAuthView authView, final String body) {
		final AppointmentQuestion question = new AppointmentQuestion();
		final Paciente pacienteRef = pacienteRepository.getReferenceById(authView.getId());
		question.setPaciente(pacienteRef);
		question.setBody(body.trim());
		question.setAnswered(false);
		final AppointmentQuestion saved = appointmentQuestionRepository.save(question);
		if (log.isInfoEnabled()) {
			log.info("Created appointment question {}", LogRedaction.redactAppointmentQuestion(saved.getId()));
		}
		return AppointmentQuestionDto.fromEntity(saved);
	}

	private AppointmentQuestion requireOwnedQuestion(final Long pacienteId, final Long questionId) {
		return appointmentQuestionRepository.findByIdAndPacienteId(questionId, pacienteId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
	}

	private static void applyAnswered(final AppointmentQuestion question, final boolean answered) {
		question.setAnswered(answered);
		if (answered) {
			if (question.getAnsweredAt() == null) {
				question.setAnsweredAt(Instant.now());
			}
		}
		else {
			question.setAnsweredAt(null);
		}
	}

}
