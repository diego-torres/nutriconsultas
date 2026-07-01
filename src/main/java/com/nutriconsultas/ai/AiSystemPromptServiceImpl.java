package com.nutriconsultas.ai;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class AiSystemPromptServiceImpl implements AiSystemPromptService {

	static final String SAFETY_MARKER_DRAFT_LABEL = "Borrador IA — revisión del nutriólogo requerida";

	static final String SAFETY_MARKER_NO_ASSIGN = "no asignes dietas al paciente";

	static final String SAFETY_MARKER_CATALOG_TOOLS = "herramientas del backend";

	static final String SAFETY_MARKER_NO_CLINICAL_CLAIM = "requiere revisión profesional del nutriólogo";

	private static final String TEMPLATE_PATH = "ai/system-prompt-base.txt";

	private final String baseTemplate;

	public AiSystemPromptServiceImpl() {
		this.baseTemplate = loadBaseTemplate();
	}

	@Override
	public String buildSystemPrompt(final AiSystemPromptContext context) {
		final AiSystemPromptContext resolved = context != null ? context : AiSystemPromptContext.defaultNutritionist();
		final String localeTag = resolved.locale().toLanguageTag();
		return baseTemplate.replace("{{LOCALE}}", localeTag)
			.replace("{{NUTRITIONIST_CONTEXT}}", formatNutritionistContext(resolved))
			.replace("{{PATIENT_CONTEXT}}", formatPatientContext(resolved.patientContext()));
	}

	private String formatNutritionistContext(final AiSystemPromptContext context) {
		if (!StringUtils.hasText(context.nutritionistScopeHint())) {
			return "";
		}
		return "CONTEXTO DEL NUTRIÓLOGO\n- " + context.nutritionistScopeHint().trim();
	}

	private String formatPatientContext(final AiPatientPromptContext patient) {
		if (patient == null) {
			return "";
		}
		final StringBuilder section = new StringBuilder(512);
		section.append("CONTEXTO DEL PACIENTE (SIN DATOS IDENTIFICABLES)\n");
		if (patient.patientId() != null) {
			section.append("- patientId interno: ").append(patient.patientId()).append('\n');
		}
		if (patient.requerimientoKcal() != null) {
			section.append("- Objetivo calórico (requerimientoKcal): ")
				.append(formatNumber(patient.requerimientoKcal()))
				.append(" kcal/día\n");
		}
		if (Boolean.TRUE.equals(patient.physiologicalStressActive()) && patient.finalTotalKcal() != null) {
			section.append("- Objetivo con estrés fisiológico (finalTotalKcal): ")
				.append(formatNumber(patient.finalTotalKcal()))
				.append(" kcal/día\n");
		}
		if (StringUtils.hasText(patient.alergias())) {
			section.append("- Alergias / exclusiones: ").append(patient.alergias().trim()).append('\n');
		}
		if (StringUtils.hasText(patient.gender())) {
			section.append("- Sexo biológico (referencia): ").append(patient.gender()).append('\n');
		}
		if (patient.pregnancy() != null) {
			section.append("- Embarazo: ").append(Boolean.TRUE.equals(patient.pregnancy()) ? "sí" : "no").append('\n');
		}
		if (StringUtils.hasText(patient.nivelPeso())) {
			section.append("- Nivel de peso: ").append(patient.nivelPeso()).append('\n');
		}
		if (patient.imc() != null) {
			section.append("- IMC: ").append(formatNumber(patient.imc())).append('\n');
		}
		if (StringUtils.hasText(patient.activityLevel())) {
			section.append("- Nivel de actividad: ").append(patient.activityLevel()).append('\n');
		}
		if (!patient.pathologyFlags().isEmpty()) {
			final String flags = patient.pathologyFlags()
				.entrySet()
				.stream()
				.filter(Map.Entry::getValue)
				.map(Map.Entry::getKey)
				.sorted()
				.collect(Collectors.joining(", "));
			if (StringUtils.hasText(flags)) {
				section.append("- Patologías marcadas: ").append(flags).append('\n');
			}
		}
		section.append("- No preguntes de nuevo el objetivo calórico ni las alergias listadas arriba.\n");
		return section.toString().trim();
	}

	private static String formatNumber(final Number value) {
		if (value instanceof Double doubleValue) {
			return String.format(Locale.ROOT, "%.1f", doubleValue);
		}
		return value.toString();
	}

	private static String loadBaseTemplate() {
		final ClassPathResource resource = new ClassPathResource(TEMPLATE_PATH);
		try (InputStream inputStream = resource.getInputStream()) {
			return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
		}
		catch (final IOException ex) {
			throw new IllegalStateException("Missing AI system prompt template: " + TEMPLATE_PATH, ex);
		}
	}

}
