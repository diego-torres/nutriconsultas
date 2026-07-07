package com.nutriconsultas.ai;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Detects prompt-injection and jailbreak patterns in nutritionist chat input (#439,
 * #440).
 */
public final class AiPromptThreatDetector {

	private static final List<Pattern> INJECTION_PATTERNS = List
		.of(Pattern.compile("ignore\\s+(all\\s+)?(previous|prior|above)\\s+instructions", Pattern.CASE_INSENSITIVE),
				Pattern.compile("disregard\\s+(all\\s+)?(previous|prior|above)\\s+instructions",
						Pattern.CASE_INSENSITIVE),
				Pattern.compile("forget\\s+(all\\s+)?(your\\s+)?(previous\\s+)?instructions", Pattern.CASE_INSENSITIVE),
				Pattern.compile("ignor(a|ar)\\s+(las\\s+)?(instrucciones|indicaciones)\\s+(anteriores|previas)",
						Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE),
				Pattern.compile("olvida(r)?\\s+(tus\\s+)?(instrucciones|indicaciones)",
						Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE),
				Pattern.compile("you\\s+are\\s+now\\s+(a|an|the)\\b", Pattern.CASE_INSENSITIVE),
				Pattern.compile("act\\s+as\\s+(if\\s+you\\s+are\\s+)?(a|an|the)?\\s*(DAN|jailbroken|unrestricted)",
						Pattern.CASE_INSENSITIVE),
				Pattern.compile("new\\s+instructions?\\s*:", Pattern.CASE_INSENSITIVE),
				Pattern.compile("(?:override|reveal|show|print|dump)\\s+(?:the\\s+)?system\\s+prompt",
						Pattern.CASE_INSENSITIVE),
				Pattern.compile("```\\s*system\\b", Pattern.CASE_INSENSITIVE),
				Pattern.compile("<\\|im_start\\|>\\s*system", Pattern.CASE_INSENSITIVE),
				Pattern.compile("\\[INST\\]|\\[/INST\\]", Pattern.CASE_INSENSITIVE),
				Pattern.compile("developer\\s+mode\\s+(enabled|on)", Pattern.CASE_INSENSITIVE),
				Pattern.compile("pretend\\s+you\\s+are\\s+(not|no longer)", Pattern.CASE_INSENSITIVE),
				Pattern.compile("role\\s*:\\s*system\\b", Pattern.CASE_INSENSITIVE));

	private static final List<Pattern> JAILBREAK_PATTERNS = List.of(
			Pattern.compile(
					"act\\s+as\\s+(an?\\s+)?(admin|administrator|platform\\s+admin|root|superuser|system\\s+admin)",
					Pattern.CASE_INSENSITIVE),
			Pattern.compile("you\\s+are\\s+(the|a|an)?\\s*(admin|administrator|platform\\s+admin|root|superuser)",
					Pattern.CASE_INSENSITIVE),
			Pattern.compile(
					"role-?play\\s+as\\s+(an?\\s+)?((platform|system)\\s+)?(admin|administrator|developer|unrestricted\\s+ai)",
					Pattern.CASE_INSENSITIVE),
			Pattern.compile("simulate\\s+(unrestricted|unfiltered|developer)\\s+mode", Pattern.CASE_INSENSITIVE),
			Pattern.compile("bypass\\s+(safety|restrictions|guardrails|content\\s+filters?)", Pattern.CASE_INSENSITIVE),
			Pattern.compile("\\bjailbreak\\b", Pattern.CASE_INSENSITIVE),
			Pattern.compile("do\\s+anything\\s+now|\\bDAN\\s+mode\\b", Pattern.CASE_INSENSITIVE),
			Pattern.compile("\\b(god|sudo)\\s+mode\\b", Pattern.CASE_INSENSITIVE),
			Pattern.compile("without\\s+(any\\s+)?restrictions", Pattern.CASE_INSENSITIVE),
			Pattern.compile("ignore\\s+(all\\s+)?(safety|security)\\s+(rules|guidelines)", Pattern.CASE_INSENSITIVE),
			Pattern.compile("pretend\\s+(you\\s+are|to\\s+be)\\s+(the|a|an)?\\s*(admin|system|developer)",
					Pattern.CASE_INSENSITIVE),
			Pattern.compile("actúa\\s+como\\s+(admin|administrador|sistema|desarrollador)",
					Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE),
			Pattern.compile("eres\\s+(el|un|una)?\\s*(admin|administrador|sistema)",
					Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE),
			Pattern.compile("modo\\s+sin\\s+restricciones|sin\\s+filtros\\s+de\\s+seguridad",
					Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE),
			Pattern.compile("reveal\\s+(your|the)\\s+(tools|functions|api\\s+keys?|secrets?)",
					Pattern.CASE_INSENSITIVE),
			Pattern.compile("list\\s+(all\\s+)?(tools|functions)\\b", Pattern.CASE_INSENSITIVE),
			Pattern.compile("repeat\\s+(the|your)\\s+(system|initial)\\s+(prompt|instructions)",
					Pattern.CASE_INSENSITIVE),
			Pattern.compile("output\\s+(your|the)\\s+system\\s+prompt", Pattern.CASE_INSENSITIVE),
			Pattern.compile("what\\s+(are|is)\\s+your\\s+(system\\s+)?instructions", Pattern.CASE_INSENSITIVE),
			Pattern.compile("mu[eé]strame\\s+(el|tu)\\s+(prompt|instrucciones)\\s+(del\\s+)?sistema",
					Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE),
			Pattern.compile("imprime\\s+(el|tu)\\s+prompt", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE),
			Pattern.compile("exfiltrat(e|ar)\\s+(the|el|la|los|las)?\\s*(prompt|secret|tool|schema)",
					Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE));

	private AiPromptThreatDetector() {
	}

	public static Optional<AiPromptThreatCategory> detect(final String sanitizedMessage) {
		if (matchesAny(sanitizedMessage, INJECTION_PATTERNS)) {
			return Optional.of(AiPromptThreatCategory.INJECTION);
		}
		if (matchesAny(sanitizedMessage, JAILBREAK_PATTERNS)) {
			return Optional.of(AiPromptThreatCategory.JAILBREAK);
		}
		return Optional.empty();
	}

	private static boolean matchesAny(final String message, final List<Pattern> patterns) {
		for (final Pattern pattern : patterns) {
			if (pattern.matcher(message).find()) {
				return true;
			}
		}
		return false;
	}

}
