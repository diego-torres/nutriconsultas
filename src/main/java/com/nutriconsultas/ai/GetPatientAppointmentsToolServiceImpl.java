package com.nutriconsultas.ai;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.PageRequest;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.nutriconsultas.calendar.CalendarEvent;
import com.nutriconsultas.calendar.CalendarEventRepository;
import com.nutriconsultas.calendar.EventStatus;
import com.nutriconsultas.paciente.PacienteRepository;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class GetPatientAppointmentsToolServiceImpl implements GetPatientAppointmentsToolService {

	static final int DEFAULT_LIMIT = 5;

	static final int MAX_LIMIT = 10;

	private final PacienteRepository pacienteRepository;

	private final CalendarEventRepository calendarEventRepository;

	public GetPatientAppointmentsToolServiceImpl(final PacienteRepository pacienteRepository,
			final CalendarEventRepository calendarEventRepository) {
		this.pacienteRepository = pacienteRepository;
		this.calendarEventRepository = calendarEventRepository;
	}

	@Override
	@Transactional(readOnly = true)
	public AiToolResult<PatientAppointmentsData> getAppointments(@NonNull final String nutritionistId,
			@NonNull final Long patientId, @Nullable final PatientAppointmentScope scope,
			@Nullable final Integer limit) {
		if (!StringUtils.hasText(nutritionistId)) {
			return AiToolResult.error(AiToolErrorCode.VALIDATION, "Sesión de nutriólogo no válida.");
		}
		if (patientId == null || patientId <= 0) {
			return AiToolResult.error(AiToolErrorCode.VALIDATION, "Paciente no vinculado a esta conversación.");
		}
		if (!pacienteRepository.findByIdAndUserId(patientId, nutritionistId).isPresent()) {
			return AiToolResult.error(AiToolErrorCode.NOT_FOUND, "No se encontró el paciente solicitado.");
		}
		final PatientAppointmentScope effectiveScope = scope != null ? scope : PatientAppointmentScope.UPCOMING;
		final int effectiveLimit = resolveLimit(limit);
		if (effectiveLimit < 1 || effectiveLimit > MAX_LIMIT) {
			return AiToolResult.error(AiToolErrorCode.VALIDATION, "El límite debe estar entre 1 y " + MAX_LIMIT + ".");
		}

		final Date now = new Date();
		final List<PatientAppointmentItem> upcoming = new ArrayList<>();
		final List<PatientAppointmentItem> past = new ArrayList<>();
		if (effectiveScope == PatientAppointmentScope.UPCOMING || effectiveScope == PatientAppointmentScope.ALL) {
			upcoming.addAll(calendarEventRepository
				.findUpcomingByPacienteId(patientId, now, EventStatus.SCHEDULED, PageRequest.of(0, effectiveLimit))
				.stream()
				.map(this::toItem)
				.toList());
		}
		if (effectiveScope == PatientAppointmentScope.PAST || effectiveScope == PatientAppointmentScope.ALL) {
			past.addAll(calendarEventRepository
				.findPastByPacienteId(patientId, now, EventStatus.COMPLETED, PageRequest.of(0, effectiveLimit))
				.stream()
				.map(this::toItem)
				.toList());
		}
		final PatientAppointmentsData data = new PatientAppointmentsData(upcoming, past, upcoming.size() + past.size());
		if (log.isInfoEnabled()) {
			log.info("AI tool get_patient_appointments patientId={} scope={} totalReturned={}", patientId,
					effectiveScope, data.totalReturned());
		}
		return AiToolResult.success(data);
	}

	static Optional<CalendarEvent> findNextScheduledAppointment(final CalendarEventRepository repository,
			final long patientId) {
		final List<CalendarEvent> events = repository.findUpcomingByPacienteId(patientId, new Date(),
				EventStatus.SCHEDULED, PageRequest.of(0, 1));
		if (events.isEmpty()) {
			return Optional.empty();
		}
		return Optional.of(events.get(0));
	}

	static int resolveLimit(@Nullable final Integer limit) {
		if (limit == null) {
			return DEFAULT_LIMIT;
		}
		return limit;
	}

	private PatientAppointmentItem toItem(final CalendarEvent event) {
		final String iso = Instant.ofEpochMilli(event.getEventDateTime().getTime()).toString();
		final String title = StringUtils.hasText(event.getTitle()) ? event.getTitle().trim() : "Consulta";
		final int duration = event.getDurationMinutes() != null ? event.getDurationMinutes() : 0;
		final String status = event.getStatus() != null ? event.getStatus().name() : EventStatus.SCHEDULED.name();
		return new PatientAppointmentItem(event.getId(), iso, title, duration, status);
	}

}
