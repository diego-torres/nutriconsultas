package com.nutriconsultas.message;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import com.nutriconsultas.message.dto.PatientMessageThreadItemDto;
import com.nutriconsultas.message.dto.PatientUnreadMessageDto;
import com.nutriconsultas.paciente.Paciente;
import com.nutriconsultas.paciente.PacienteRepository;
import com.nutriconsultas.util.LogRedaction;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class PatientMessageService {

	private static final int MAX_BODY_LENGTH = 2000;

	private final PatientMessageRepository patientMessageRepository;

	private final PacienteRepository pacienteRepository;

	public PatientMessageService(final PatientMessageRepository patientMessageRepository,
			final PacienteRepository pacienteRepository) {
		this.patientMessageRepository = patientMessageRepository;
		this.pacienteRepository = pacienteRepository;
	}

	@Transactional(readOnly = true)
	public List<PatientMessageThreadItemDto> listThread(final Long pacienteId, final String userId) {
		final Paciente paciente = requireOwnedPaciente(pacienteId, userId);
		return patientMessageRepository.findThreadAscending(paciente.getId(), userId)
			.stream()
			.map(PatientMessageThreadItemDto::fromEntity)
			.toList();
	}

	@Transactional(readOnly = true)
	public List<PatientUnreadMessageDto> listUnreadSummaries(final String userId) {
		final List<PatientMessage> unreadMessages = patientMessageRepository
			.findUnreadFromPatientsByNutritionist(userId);
		final Map<Long, PatientUnreadMessageDto> summaries = new LinkedHashMap<>();
		for (final PatientMessage message : unreadMessages) {
			final Long pacienteId = message.getPaciente().getId();
			final PatientUnreadMessageDto existing = summaries.get(pacienteId);
			if (existing == null) {
				summaries.put(pacienteId, PatientUnreadMessageDto.fromLatest(message, 1));
			}
			else {
				summaries.put(pacienteId, existing.withIncrementedCount());
			}
		}
		return new ArrayList<>(summaries.values());
	}

	@Transactional(readOnly = true)
	public long countUnread(final String userId) {
		return patientMessageRepository.countUnreadFromPatientsByNutritionist(userId);
	}

	@Transactional
	public PatientMessageThreadItemDto sendAsNutritionist(final Long pacienteId, final String userId,
			final String body) {
		final Paciente paciente = requireOwnedPaciente(pacienteId, userId);
		final String trimmedBody = requireBody(body);
		final PatientMessage message = new PatientMessage();
		message.setPaciente(paciente);
		message.setNutritionistUserId(userId);
		message.setSenderRole(MessageSenderRole.NUTRITIONIST);
		message.setBody(trimmedBody);
		message.setReadByPatient(false);
		message.setReadByNutritionist(true);
		final PatientMessage saved = patientMessageRepository.save(message);
		log.info("Nutritionist sent patient message: {}", LogRedaction.redactPatientMessage(saved.getId()));
		return PatientMessageThreadItemDto.fromEntity(saved);
	}

	@Transactional
	public void markThreadReadByNutritionist(final Long pacienteId, final String userId) {
		requireOwnedPaciente(pacienteId, userId);
		final int updated = patientMessageRepository.markReadByNutritionist(pacienteId, userId);
		if (updated > 0) {
			log.info("Marked {} patient messages read for patient {}", updated,
					LogRedaction.redactPaciente(pacienteId));
		}
	}

	private Paciente requireOwnedPaciente(final Long pacienteId, final String userId) {
		return pacienteRepository.findByIdAndUserId(pacienteId, userId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient not found"));
	}

	private String requireBody(final String body) {
		if (!StringUtils.hasText(body)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Message body is required");
		}
		final String trimmed = body.trim();
		if (trimmed.length() > MAX_BODY_LENGTH) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Message body is too long");
		}
		return trimmed;
	}

}
