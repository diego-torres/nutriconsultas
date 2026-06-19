package com.nutriconsultas.paciente;

import java.util.List;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.nutriconsultas.calendar.CalendarEvent;
import com.nutriconsultas.calendar.CalendarEventService;
import com.nutriconsultas.clinical.exam.AnthropometricMeasurement;
import com.nutriconsultas.clinical.exam.AnthropometricMeasurementService;
import com.nutriconsultas.clinical.exam.ClinicalExam;
import com.nutriconsultas.clinical.exam.ClinicalExamService;
import com.nutriconsultas.message.PatientMessageRepository;
import com.nutriconsultas.paciente.metrics.BodyMetricRecordRepository;
import com.nutriconsultas.util.LogRedaction;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class PacienteDeletionServiceImpl implements PacienteDeletionService {

	private final PacienteRepository pacienteRepository;

	private final PatientMessageRepository patientMessageRepository;

	private final PatientInvitationRepository patientInvitationRepository;

	private final PacienteDietaRepository pacienteDietaRepository;

	private final CalendarEventService calendarEventService;

	private final ClinicalExamService clinicalExamService;

	private final AnthropometricMeasurementService anthropometricMeasurementService;

	private final BodyMetricRecordRepository bodyMetricRecordRepository;

	public PacienteDeletionServiceImpl(final PacienteRepository pacienteRepository,
			final PatientMessageRepository patientMessageRepository,
			final PatientInvitationRepository patientInvitationRepository,
			final PacienteDietaRepository pacienteDietaRepository, final CalendarEventService calendarEventService,
			final ClinicalExamService clinicalExamService,
			final AnthropometricMeasurementService anthropometricMeasurementService,
			final BodyMetricRecordRepository bodyMetricRecordRepository) {
		this.pacienteRepository = pacienteRepository;
		this.patientMessageRepository = patientMessageRepository;
		this.patientInvitationRepository = patientInvitationRepository;
		this.pacienteDietaRepository = pacienteDietaRepository;
		this.calendarEventService = calendarEventService;
		this.clinicalExamService = clinicalExamService;
		this.anthropometricMeasurementService = anthropometricMeasurementService;
		this.bodyMetricRecordRepository = bodyMetricRecordRepository;
	}

	@Override
	@Transactional
	public void deletePatientWithHistory(@NonNull final Long pacienteId, @NonNull final String userId) {
		final Paciente paciente = pacienteRepository.findByIdAndUserId(pacienteId, userId)
			.orElseThrow(() -> new IllegalArgumentException("Paciente no encontrado"));
		logMobileLinkageClear(paciente);
		deleteRelatedHistory(pacienteId);
		pacienteRepository.delete(paciente);
		if (log.isInfoEnabled()) {
			log.info("Deleted patient {} and clinical history for nutritionist {}",
					LogRedaction.redactPaciente(pacienteId), LogRedaction.redactUserId(userId));
		}
	}

	private void logMobileLinkageClear(final Paciente paciente) {
		if (StringUtils.hasText(paciente.getPatientAuthSub()) && log.isInfoEnabled()) {
			log.info("Clearing mobile linkage for patient {} sub={}", LogRedaction.redactPaciente(paciente.getId()),
					LogRedaction.redactUserId(paciente.getPatientAuthSub()));
		}
	}

	private void deleteRelatedHistory(final Long pacienteId) {
		patientMessageRepository.deleteByPacienteId(pacienteId);
		final List<PatientInvitation> invitations = patientInvitationRepository.findByPacienteId(pacienteId);
		if (!invitations.isEmpty()) {
			patientInvitationRepository.deleteAll(invitations);
		}
		final List<PacienteDieta> dietAssignments = pacienteDietaRepository.findByPacienteId(pacienteId);
		if (!dietAssignments.isEmpty()) {
			pacienteDietaRepository.deleteAll(dietAssignments);
		}
		for (final CalendarEvent event : calendarEventService.findByPacienteId(pacienteId)) {
			calendarEventService.delete(event.getId());
		}
		for (final ClinicalExam exam : clinicalExamService.findByPacienteId(pacienteId)) {
			clinicalExamService.deleteById(exam.getId());
		}
		for (final AnthropometricMeasurement measurement : anthropometricMeasurementService
			.findByPacienteId(pacienteId)) {
			anthropometricMeasurementService.deleteById(measurement.getId());
		}
		bodyMetricRecordRepository.deleteByPacienteId(pacienteId);
	}

}
