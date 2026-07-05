package com.nutriconsultas.ai;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.nutriconsultas.calendar.CalendarEvent;
import com.nutriconsultas.calendar.CalendarEventRepository;
import com.nutriconsultas.paciente.Paciente;
import com.nutriconsultas.paciente.PacienteRepository;
import com.nutriconsultas.paciente.satellite.PacienteMedicalHistory;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class AiPatientPromptContextResolverImpl implements AiPatientPromptContextResolver {

	private static final int MAX_ALERGIAS_CHARS = 500;

	private final PacienteRepository pacienteRepository;

	private final CalendarEventRepository calendarEventRepository;

	public AiPatientPromptContextResolverImpl(final PacienteRepository pacienteRepository,
			final CalendarEventRepository calendarEventRepository) {
		this.pacienteRepository = pacienteRepository;
		this.calendarEventRepository = calendarEventRepository;
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<AiPatientPromptContext> resolve(@Nullable final Long patientId, final String nutritionistId) {
		if (patientId == null || !StringUtils.hasText(nutritionistId)) {
			return Optional.empty();
		}
		return pacienteRepository.findByIdAndUserId(patientId, nutritionistId).map(this::toPromptContext);
	}

	private AiPatientPromptContext toPromptContext(final Paciente paciente) {
		final PacienteMedicalHistory history = paciente.getMedicalHistory();
		final String activityLevel = paciente.getPhysicalActivityLevel() != null
				? paciente.getPhysicalActivityLevel().name() : null;
		final NextAppointmentSummary nextAppointment = resolveNextAppointment(paciente.getId());
		return new AiPatientPromptContext(paciente.getId(), resolveRequerimientoKcal(paciente),
				paciente.getFinalTotalKcal(), paciente.getPhysiologicalStressActive(), paciente.getGender(),
				paciente.getPregnancy(), nivelPesoLabel(paciente), paciente.getImc(), pathologyFlags(history),
				sanitizeAlergias(history != null ? history.getAlergias() : null), activityLevel,
				nextAppointment.atIso(), nextAppointment.title(), nextAppointment.durationMinutes());
	}

	private NextAppointmentSummary resolveNextAppointment(final long patientId) {
		return GetPatientAppointmentsToolServiceImpl.findNextScheduledAppointment(calendarEventRepository, patientId)
			.map(this::toNextAppointmentSummary)
			.orElse(NextAppointmentSummary.empty());
	}

	private NextAppointmentSummary toNextAppointmentSummary(final CalendarEvent event) {
		final String iso = java.time.Instant.ofEpochMilli(event.getEventDateTime().getTime()).toString();
		final String title = StringUtils.hasText(event.getTitle()) ? event.getTitle().trim() : "Consulta";
		final Integer duration = event.getDurationMinutes();
		return new NextAppointmentSummary(iso, title, duration);
	}

	private record NextAppointmentSummary(String atIso, String title, Integer durationMinutes) {

		private static NextAppointmentSummary empty() {
			return new NextAppointmentSummary(null, null, null);
		}

	}

	private static String nivelPesoLabel(final Paciente paciente) {
		return paciente.getNivelPeso() != null ? paciente.getNivelPeso().name() : null;
	}

	private static Double resolveRequerimientoKcal(final Paciente paciente) {
		if (paciente.getTotalAdjustedKcal() != null) {
			return paciente.getTotalAdjustedKcal();
		}
		return paciente.getGetKcal();
	}

	private static Map<String, Boolean> pathologyFlags(final PacienteMedicalHistory history) {
		final Map<String, Boolean> flags = new LinkedHashMap<>();
		if (history == null) {
			return flags;
		}
		flags.put("hipertension", history.getHipertension());
		flags.put("diabetes", history.getDiabetes());
		flags.put("hipotiroidismo", history.getHipotiroidismo());
		flags.put("obesidad", history.getObesidad());
		flags.put("anemia", history.getAnemia());
		flags.put("bulimia", history.getBulimia());
		flags.put("anorexia", history.getAnorexia());
		flags.put("enfermedadesHepaticas", history.getEnfermedadesHepaticas());
		return flags;
	}

	private static String sanitizeAlergias(@Nullable final String alergias) {
		if (!StringUtils.hasText(alergias)) {
			return null;
		}
		final String trimmed = alergias.replaceAll("\\p{Cntrl}", " ").trim();
		if (trimmed.isEmpty()) {
			return null;
		}
		if (trimmed.length() <= MAX_ALERGIAS_CHARS) {
			return trimmed;
		}
		return trimmed.substring(0, MAX_ALERGIAS_CHARS);
	}

}
