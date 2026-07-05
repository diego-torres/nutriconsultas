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

	static final String SAFETY_MARKER_PROMPT_SECURITY = "SEGURIDAD DE PROMPTS";

	static final String SAFETY_MARKER_LIMITED_CAPABILITIES = "CAPACIDADES LIMITADAS";

	static final String SAFETY_MARKER_JAILBREAK_DEFENSE = "DEFENSA ANTE JAILBREAK";

	static final String SAFETY_MARKER_VOLUME_LIMITS = "VOLUMEN Y LÍMITES";

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
			.replace("{{PATIENT_CONTEXT}}", formatPatientContext(resolved.patientContext()))
			.replace("{{DIETA_CONTEXT}}", formatDietaContext(resolved.dietaContext()))
			.replace("{{PLATILLO_CONTEXT}}", formatPlatilloContext(resolved.platilloContext()));
	}

	private String formatNutritionistContext(final AiSystemPromptContext context) {
		if (!StringUtils.hasText(context.nutritionistScopeHint())) {
			return "";
		}
		final String inner = "CONTEXTO DEL NUTRIÓLOGO\n- " + context.nutritionistScopeHint().trim();
		return AiPromptDelimiters.wrapSection(AiPromptDelimiters.NUTRITIONIST_CONTEXT_OPEN,
				AiPromptDelimiters.NUTRITIONIST_CONTEXT_CLOSE, inner);
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
		if (StringUtils.hasText(patient.nextAppointmentAtIso())) {
			section.append("- Próxima cita programada: ").append(patient.nextAppointmentAtIso());
			if (StringUtils.hasText(patient.nextAppointmentTitle())) {
				section.append(" — ").append(patient.nextAppointmentTitle().trim());
			}
			if (patient.nextAppointmentDurationMinutes() != null && patient.nextAppointmentDurationMinutes() > 0) {
				section.append(" (").append(patient.nextAppointmentDurationMinutes()).append(" min)");
			}
			section.append('\n');
		}
		else {
			section.append("- Próxima cita programada: ninguna registrada\n");
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
		return AiPromptDelimiters.wrapSection(AiPromptDelimiters.PATIENT_CONTEXT_OPEN,
				AiPromptDelimiters.PATIENT_CONTEXT_CLOSE, section.toString().trim());
	}

	private String formatDietaContext(final AiDietaPromptContext dieta) {
		if (dieta == null) {
			return "";
		}
		final StringBuilder section = new StringBuilder(512);
		section.append("CONTEXTO DE LA DIETA EN PANTALLA\n- dietaId interno: ").append(dieta.dietaId()).append('\n');
		if (StringUtils.hasText(dieta.nombre())) {
			section.append("- Nombre: ").append(dieta.nombre().trim()).append('\n');
		}
		if (dieta.energiaKcal() != null && dieta.energiaKcal() > 0) {
			section.append("- Energía total: ").append(dieta.energiaKcal()).append(" kcal\n");
		}
		section
			.append(String.format("- Proteína: %s g · Lípidos: %s g · H. de carbono: %s g%n",
					formatNumber(dieta.proteinaGrams()), formatNumber(dieta.lipidosGrams()),
					formatNumber(dieta.hidratosDeCarbonoGrams())))
			.append("- Ingestas configuradas: ")
			.append(dieta.ingestaCount())
			.append('\n');
		if (!dieta.ingestaNames().isEmpty()) {
			section.append("- Ingestas: ").append(String.join(", ", dieta.ingestaNames())).append('\n');
		}
		if (dieta.patientAssignment()) {
			section.append("- Dieta asignada a paciente (patientId interno: ")
				.append(dieta.linkedPatientId())
				.append(")\n");
		}
		section.append("- Usa esta dieta como punto de partida; propón ajustes como borrador.\n");
		return AiPromptDelimiters.wrapSection(AiPromptDelimiters.DIETA_CONTEXT_OPEN,
				AiPromptDelimiters.DIETA_CONTEXT_CLOSE, section.toString().trim());
	}

	private String formatPlatilloContext(final AiPlatilloPromptContext platillo) {
		if (platillo == null) {
			return "";
		}
		final StringBuilder section = new StringBuilder(512);
		section.append("CONTEXTO DEL PLATILLO EN PANTALLA\n- platilloId interno: ")
			.append(platillo.platilloId())
			.append('\n');
		if (StringUtils.hasText(platillo.name())) {
			section.append("- Nombre: ").append(platillo.name().trim()).append('\n');
		}
		if (StringUtils.hasText(platillo.descriptionSummary())) {
			section.append("- Descripción: ").append(platillo.descriptionSummary().trim()).append('\n');
		}
		if (platillo.energiaKcal() != null && platillo.energiaKcal() > 0) {
			section.append("- Energía por porción: ").append(platillo.energiaKcal()).append(" kcal\n");
		}
		section.append("- Ingredientes registrados: ").append(platillo.ingredientCount()).append('\n');
		if (!platillo.ingredientNames().isEmpty()) {
			section.append("- Ingredientes: ").append(String.join(", ", platillo.ingredientNames())).append('\n');
		}
		if (StringUtils.hasText(platillo.ingestasSugeridas())) {
			section.append("- Ingestas sugeridas: ").append(platillo.ingestasSugeridas().trim()).append('\n');
		}
		section.append("- Usa este platillo como referencia; propón variantes como borrador.\n");
		return AiPromptDelimiters.wrapSection(AiPromptDelimiters.PLATILLO_CONTEXT_OPEN,
				AiPromptDelimiters.PLATILLO_CONTEXT_CLOSE, section.toString().trim());
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
