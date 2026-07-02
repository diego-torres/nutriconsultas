package com.nutriconsultas.ai;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.nutriconsultas.paciente.Paciente;
import com.nutriconsultas.paciente.PacienteRepository;
import com.nutriconsultas.paciente.satellite.PacienteMedicalHistory;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class AiPatientPromptContextResolverImpl implements AiPatientPromptContextResolver {

	private static final int MAX_ALERGIAS_CHARS = 500;

	private final PacienteRepository pacienteRepository;

	public AiPatientPromptContextResolverImpl(final PacienteRepository pacienteRepository) {
		this.pacienteRepository = pacienteRepository;
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
		return new AiPatientPromptContext(paciente.getId(), resolveRequerimientoKcal(paciente),
				paciente.getFinalTotalKcal(), paciente.getPhysiologicalStressActive(), paciente.getGender(),
				paciente.getPregnancy(), nivelPesoLabel(paciente), paciente.getImc(), pathologyFlags(history),
				sanitizeAlergias(history != null ? history.getAlergias() : null), activityLevel);
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
