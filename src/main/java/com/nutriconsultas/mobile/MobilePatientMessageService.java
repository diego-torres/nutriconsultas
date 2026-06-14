package com.nutriconsultas.mobile;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.nutriconsultas.message.MessageSenderRole;
import com.nutriconsultas.message.PatientMessage;
import com.nutriconsultas.message.PatientMessageRepository;
import com.nutriconsultas.mobile.dto.CursorPagedResponse;
import com.nutriconsultas.mobile.dto.PatientMessageSummaryDto;
import com.nutriconsultas.paciente.Paciente;
import com.nutriconsultas.util.LogRedaction;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class MobilePatientMessageService {

	private static final int DEFAULT_PAGE_SIZE = 20;

	private static final int MAX_PAGE_SIZE = 100;

	private final PatientMessageRepository patientMessageRepository;

	private final PatientWriteRateLimiter patientWriteRateLimiter;

	public MobilePatientMessageService(final PatientMessageRepository patientMessageRepository,
			final PatientWriteRateLimiter patientWriteRateLimiter) {
		this.patientMessageRepository = patientMessageRepository;
		this.patientWriteRateLimiter = patientWriteRateLimiter;
	}

	@Transactional(readOnly = true)
	public CursorPagedResponse<PatientMessageSummaryDto> listMessages(final Long pacienteId, final String cursor,
			final int size) {
		final int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
		final Long cursorId = parseCursor(cursor);
		final List<PatientMessage> fetched = patientMessageRepository.findThreadForPatient(pacienteId, cursorId,
				PageRequest.of(0, safeSize + 1));
		final boolean hasMore = fetched.size() > safeSize;
		final List<PatientMessage> page = hasMore ? fetched.subList(0, safeSize) : fetched;
		final List<PatientMessageSummaryDto> summaries = page.stream()
			.map(PatientMessageSummaryDto::fromEntity)
			.toList();
		final String nextCursor = hasMore && !page.isEmpty() ? String.valueOf(page.get(page.size() - 1).getId()) : null;
		if (log.isDebugEnabled()) {
			log.debug("Listed mobile messages count={} hasMore={} for patient {}", summaries.size(), hasMore,
					LogRedaction.redactPaciente(pacienteId));
		}
		return CursorPagedResponse.of(summaries, nextCursor);
	}

	@Transactional
	public PatientMessageSummaryDto sendMessage(final Paciente paciente, final String body) {
		return patientWriteRateLimiter.execute(PatientWriteRateLimiter.PATIENT_MESSAGES, paciente.getPatientAuthSub(),
				() -> persistPatientMessage(paciente, body));
	}

	private PatientMessageSummaryDto persistPatientMessage(final Paciente paciente, final String body) {
		final PatientMessage message = new PatientMessage();
		message.setPaciente(paciente);
		message.setNutritionistUserId(paciente.getUserId());
		message.setSenderRole(MessageSenderRole.PATIENT);
		message.setBody(body.trim());
		final PatientMessage saved = patientMessageRepository.save(message);
		log.info("Patient sent message: {}", LogRedaction.redactPatientMessage(saved.getId()));
		return PatientMessageSummaryDto.fromEntity(saved);
	}

	private Long parseCursor(final String cursor) {
		if (!StringUtils.hasText(cursor)) {
			return null;
		}
		try {
			return Long.valueOf(cursor.trim());
		}
		catch (NumberFormatException ex) {
			return null;
		}
	}

}
