package com.nutriconsultas.ai;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * Deterministic pre-orchestration guard against excessive bulk generation requests
 * (#447).
 */
@Component
@Slf4j
public class AiRequestScopeGuard {

	private static final Pattern DAY_COUNT_PATTERN = Pattern.compile("(\\d{1,4})\\s*d[ií]as?", Pattern.UNICODE_CASE);

	private static final Pattern WEEK_COUNT_PATTERN = Pattern.compile("(\\d{1,3})\\s*semanas?", Pattern.UNICODE_CASE);

	private static final Pattern DISH_COUNT_PATTERN = Pattern.compile("(\\d{1,4})\\s*(platillos?|recetas?|platos?)",
			Pattern.UNICODE_CASE | Pattern.CASE_INSENSITIVE);

	private static final Pattern PLAN_COUNT_PATTERN = Pattern.compile(
			"(\\d{1,4})\\s*(planes?(?:\\s+nutricionales?|" + "\\s+alimenticios?)?)",
			Pattern.UNICODE_CASE | Pattern.CASE_INSENSITIVE);

	private static final Pattern MENU_COUNT_PATTERN = Pattern.compile("(\\d{1,4})\\s*men[uú]s?",
			Pattern.UNICODE_CASE | Pattern.CASE_INSENSITIVE);

	private static final Pattern LARGE_BULK_PATTERN = Pattern.compile(
			"(\\d{2,})\\s*(planes?|platillos?|recetas?" + "|men[uú]s?|pacientes?)",
			Pattern.UNICODE_CASE | Pattern.CASE_INSENSITIVE);

	private static final Pattern MULTI_PATIENT_PATTERN = Pattern
		.compile("todos\\s+(mis\\s+)?pacientes|cada\\s+paciente|para\\s+todos\\s+(mis\\s+)?pacientes|"
				+ "todos\\s+los\\s+pacientes", Pattern.UNICODE_CASE | Pattern.CASE_INSENSITIVE);

	private static final Pattern YEAR_SCOPE_PATTERN = Pattern.compile(
			"cada\\s+d[ií]a\\s+del\\s+a[nñ]o|todo\\s+el\\s+a[nñ]o|365\\s*d[ií]as",
			Pattern.UNICODE_CASE | Pattern.CASE_INSENSITIVE);

	private static final Pattern WORD_BULK_PATTERN = Pattern.compile(
			"\\b(mil|cien|cientos?)\\s+(planes?|platillos?|recetas?|men[uú]s?)",
			Pattern.UNICODE_CASE | Pattern.CASE_INSENSITIVE);

	private static final Pattern PLAN_CONTEXT_PATTERN = Pattern.compile(
			"plan(?:es)?(?:\\s+nutricional(?:es)?|\\s+alimenticio(?:s)?)?|planificaci[oó]n",
			Pattern.UNICODE_CASE | Pattern.CASE_INSENSITIVE);

	private static final Pattern MENU_CONTEXT_PATTERN = Pattern.compile("men[uú](?:\\s+semanal|\\s+diario)?",
			Pattern.UNICODE_CASE | Pattern.CASE_INSENSITIVE);

	private final AiProperties properties;

	public AiRequestScopeGuard(final AiProperties properties) {
		this.properties = properties;
	}

	public Optional<AiRequestScopeViolation> evaluate(final String sanitizedMessage) {
		if (!StringUtils.hasText(sanitizedMessage)) {
			return Optional.empty();
		}
		final String normalized = sanitizedMessage.toLowerCase(Locale.ROOT);

		final Optional<AiRequestScopeViolation> multiPatient = evaluateMultiPatient(normalized);
		if (multiPatient.isPresent()) {
			return multiPatient;
		}
		final Optional<AiRequestScopeViolation> yearScope = evaluateYearScope(normalized);
		if (yearScope.isPresent()) {
			return yearScope;
		}
		final Optional<AiRequestScopeViolation> wordBulk = evaluateWordBulk(normalized);
		if (wordBulk.isPresent()) {
			return wordBulk;
		}
		final Optional<AiRequestScopeViolation> dishViolation = evaluateDishCount(normalized);
		if (dishViolation.isPresent()) {
			return dishViolation;
		}
		final Optional<AiRequestScopeViolation> planViolation = evaluatePlanDays(normalized);
		if (planViolation.isPresent()) {
			return planViolation;
		}
		final Optional<AiRequestScopeViolation> menuViolation = evaluateMenuScope(normalized);
		if (menuViolation.isPresent()) {
			return menuViolation;
		}
		return evaluateLargeBulkCounts(normalized);
	}

	private Optional<AiRequestScopeViolation> evaluateMultiPatient(final String normalized) {
		if (MULTI_PATIENT_PATTERN.matcher(normalized).find()) {
			return Optional.of(violation(AiRequestScopeKind.MULTI_PATIENT, 0,
					"No puedo generar planes para todos tus pacientes en un solo turno. "
							+ "Trabajemos con un paciente o un borrador de ejemplo a la vez; "
							+ "después puedes pedir variaciones en mensajes separados."));
		}
		return Optional.empty();
	}

	private Optional<AiRequestScopeViolation> evaluateYearScope(final String normalized) {
		if (YEAR_SCOPE_PATTERN.matcher(normalized).find()) {
			return Optional.of(violation(AiRequestScopeKind.DIET_PLAN_DAYS, 365,
					refusalFor(AiRequestScopeKind.DIET_PLAN_DAYS, 365, "planes alimenticios")));
		}
		return Optional.empty();
	}

	private Optional<AiRequestScopeViolation> evaluateWordBulk(final String normalized) {
		final Matcher matcher = WORD_BULK_PATTERN.matcher(normalized);
		if (!matcher.find()) {
			return Optional.empty();
		}
		final String noun = matcher.group(2);
		if (noun.startsWith("plan")) {
			return Optional.of(violation(AiRequestScopeKind.BULK_COUNT, 100,
					refusalFor(AiRequestScopeKind.DIET_PLAN_DAYS, 100, "planes nutricionales")));
		}
		if (noun.startsWith("plat") || noun.startsWith("rec")) {
			return Optional.of(violation(AiRequestScopeKind.BULK_COUNT, 100,
					refusalFor(AiRequestScopeKind.DISH_COUNT, 100, "platillos")));
		}
		return Optional
			.of(violation(AiRequestScopeKind.BULK_COUNT, 100, refusalFor(AiRequestScopeKind.MENU_DAYS, 100, "menús")));
	}

	private Optional<AiRequestScopeViolation> evaluateDishCount(final String normalized) {
		final Matcher matcher = DISH_COUNT_PATTERN.matcher(normalized);
		while (matcher.find()) {
			final int count = Integer.parseInt(matcher.group(1));
			if (count > properties.getMaxDishesPerTurn()) {
				return Optional.of(violation(AiRequestScopeKind.DISH_COUNT, count,
						refusalFor(AiRequestScopeKind.DISH_COUNT, count, "platillos")));
			}
		}
		return Optional.empty();
	}

	private Optional<AiRequestScopeViolation> evaluatePlanDays(final String normalized) {
		if (!PLAN_CONTEXT_PATTERN.matcher(normalized).find() && !normalized.contains("plan")) {
			return Optional.empty();
		}
		final int dayCount = resolveDayCount(normalized);
		if (dayCount > properties.getMaxDaysPerTurn()) {
			return Optional.of(violation(AiRequestScopeKind.DIET_PLAN_DAYS, dayCount,
					refusalFor(AiRequestScopeKind.DIET_PLAN_DAYS, dayCount, "planes alimenticios")));
		}
		final Matcher planMatcher = PLAN_COUNT_PATTERN.matcher(normalized);
		while (planMatcher.find()) {
			final int count = Integer.parseInt(planMatcher.group(1));
			if (count > 1) {
				return Optional.of(violation(AiRequestScopeKind.DIET_PLAN_DAYS, count,
						refusalFor(AiRequestScopeKind.DIET_PLAN_DAYS, count, "planes nutricionales")));
			}
		}
		return Optional.empty();
	}

	private Optional<AiRequestScopeViolation> evaluateMenuScope(final String normalized) {
		if (!MENU_CONTEXT_PATTERN.matcher(normalized).find() && !normalized.contains("menu")) {
			return Optional.empty();
		}
		final int dayCount = resolveDayCount(normalized);
		if (dayCount > properties.getMaxMenuDaysPerTurn()) {
			return Optional.of(violation(AiRequestScopeKind.MENU_DAYS, dayCount,
					refusalFor(AiRequestScopeKind.MENU_DAYS, dayCount, "menús")));
		}
		final Matcher menuMatcher = MENU_COUNT_PATTERN.matcher(normalized);
		while (menuMatcher.find()) {
			final int count = Integer.parseInt(menuMatcher.group(1));
			if (count > 1) {
				return Optional.of(violation(AiRequestScopeKind.MENU_DAYS, count,
						refusalFor(AiRequestScopeKind.MENU_DAYS, count, "menús")));
			}
		}
		return Optional.empty();
	}

	private Optional<AiRequestScopeViolation> evaluateLargeBulkCounts(final String normalized) {
		final Matcher matcher = LARGE_BULK_PATTERN.matcher(normalized);
		while (matcher.find()) {
			final int count = Integer.parseInt(matcher.group(1));
			final String noun = matcher.group(2).toLowerCase(Locale.ROOT);
			if (noun.startsWith("plan") && count > properties.getMaxDaysPerTurn()) {
				return Optional.of(violation(AiRequestScopeKind.DIET_PLAN_DAYS, count,
						refusalFor(AiRequestScopeKind.DIET_PLAN_DAYS, count, "planes nutricionales")));
			}
			if ((noun.startsWith("plat") || noun.startsWith("rec")) && count > properties.getMaxDishesPerTurn()) {
				return Optional.of(violation(AiRequestScopeKind.DISH_COUNT, count,
						refusalFor(AiRequestScopeKind.DISH_COUNT, count, "platillos")));
			}
			if (noun.startsWith("men") && count > properties.getMaxMenuDaysPerTurn()) {
				return Optional.of(violation(AiRequestScopeKind.MENU_DAYS, count,
						refusalFor(AiRequestScopeKind.MENU_DAYS, count, "menús")));
			}
			if (noun.startsWith("paciente")) {
				return Optional.of(violation(AiRequestScopeKind.MULTI_PATIENT, count,
						"No puedo generar planes para " + count + " pacientes en un solo turno. "
								+ "Trabajemos con un paciente o un borrador de ejemplo a la vez."));
			}
		}
		return Optional.empty();
	}

	private int resolveDayCount(final String normalized) {
		int maxDays = 0;
		final Matcher dayMatcher = DAY_COUNT_PATTERN.matcher(normalized);
		while (dayMatcher.find()) {
			maxDays = Math.max(maxDays, Integer.parseInt(dayMatcher.group(1)));
		}
		final Matcher weekMatcher = WEEK_COUNT_PATTERN.matcher(normalized);
		while (weekMatcher.find()) {
			maxDays = Math.max(maxDays, Integer.parseInt(weekMatcher.group(1)) * 7);
		}
		return maxDays;
	}

	private AiRequestScopeViolation violation(final AiRequestScopeKind kind, final int requestedAmount,
			final String refusalMessage) {
		if (log.isWarnEnabled()) {
			log.warn("AI chat scope blocked kind={} requestedAmount={}", kind, requestedAmount);
		}
		return new AiRequestScopeViolation(kind, requestedAmount, refusalMessage);
	}

	static String refusalFor(final AiRequestScopeKind kind, final int requestedAmount, final String noun) {
		if (kind == AiRequestScopeKind.DISH_COUNT) {
			return "No puedo generar " + requestedAmount + " " + noun + " en un solo turno. "
					+ "Puedo ayudarte con 1 borrador de ejemplo que revises y apruebes; "
					+ "después puedes pedir variaciones en mensajes separados.";
		}
		if (kind == AiRequestScopeKind.MENU_DAYS) {
			return "No puedo generar un menú de " + requestedAmount + " días en un solo turno. "
					+ "Puedo ayudarte con un menú de hasta 7 días o un borrador de ejemplo; "
					+ "pide días adicionales en mensajes separados.";
		}
		return "No puedo generar " + requestedAmount + " " + noun + " en un solo turno. "
				+ "Puedo ayudarte con 1 borrador de ejemplo que revises y apruebes; "
				+ "después puedes pedir variaciones o días adicionales en mensajes separados.";
	}

	/**
	 * Builds deterministic refusal copy from classifier-estimated units (#448).
	 */
	public Optional<String> refusalMessageForUnits(final AiRequestScopeRequestedUnits units) {
		if (units == null) {
			return Optional.empty();
		}
		if (units.patients() != null && units.patients() > 1) {
			return Optional.of("No puedo generar planes para " + units.patients() + " pacientes en un solo turno. "
					+ "Trabajemos con un paciente o un borrador de ejemplo a la vez.");
		}
		if (units.patients() != null && units.patients() == 0) {
			return Optional.of("No puedo generar planes para todos tus pacientes en un solo turno. "
					+ "Trabajemos con un paciente o un borrador de ejemplo a la vez; "
					+ "después puedes pedir variaciones en mensajes separados.");
		}
		if (units.dishes() != null && units.dishes() > properties.getMaxDishesPerTurn()) {
			return Optional.of(refusalFor(AiRequestScopeKind.DISH_COUNT, units.dishes(), "platillos"));
		}
		if (units.plans() != null && units.plans() > 1) {
			return Optional.of(refusalFor(AiRequestScopeKind.DIET_PLAN_DAYS, units.plans(), "planes nutricionales"));
		}
		if (units.days() != null && units.days() > properties.getMaxDaysPerTurn()) {
			return Optional.of(refusalFor(AiRequestScopeKind.DIET_PLAN_DAYS, units.days(), "planes alimenticios"));
		}
		if (units.days() != null && units.days() > properties.getMaxMenuDaysPerTurn()) {
			return Optional.of(refusalFor(AiRequestScopeKind.MENU_DAYS, units.days(), "menús"));
		}
		return Optional.empty();
	}

}
