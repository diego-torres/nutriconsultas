package com.nutriconsultas.ai;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;

import lombok.extern.slf4j.Slf4j;

/**
 * Lightweight OpenAI scope classifier before main orchestration (#448).
 */
@Component
@Slf4j
public class AiRequestScopeClassifierImpl implements AiRequestScopeClassifier {

	private static final String PROMPT_PATH = "ai/scope-classifier-prompt.txt";

	private static final String DEFAULT_CLARIFY_MESSAGE = "¿Podrías indicar cuántos días, platillos o pacientes "
			+ "quieres abordar en este turno? Puedo ayudarte con un borrador de ejemplo a la vez.";

	private static final String GENERIC_REFUSAL_MESSAGE = "No puedo generar esa cantidad en un solo turno. "
			+ "Puedo ayudarte con 1 borrador de ejemplo que revises y apruebes; "
			+ "después puedes pedir variaciones en mensajes separados.";

	private final AiProperties properties;

	private final OpenAiClientService openAiClientService;

	private final AiRequestScopeGuard requestScopeGuard;

	private final String classifierPrompt;

	public AiRequestScopeClassifierImpl(final AiProperties properties, final OpenAiClientService openAiClientService,
			final AiRequestScopeGuard requestScopeGuard) {
		this.properties = properties;
		this.openAiClientService = openAiClientService;
		this.requestScopeGuard = requestScopeGuard;
		this.classifierPrompt = loadClassifierPrompt();
	}

	@Override
	public Optional<AiRequestScopeClassifierOutcome> evaluate(final String sanitizedUserMessage) {
		if (!properties.isScopeClassifierEnabled() || !StringUtils.hasText(sanitizedUserMessage)) {
			return Optional.empty();
		}
		try {
			final OpenAiChatCompletionResponse response = openAiClientService
				.chatCompletion(new OpenAiChatCompletionRequest(
						List.of(OpenAiChatMessage.system(classifierPrompt),
								OpenAiChatMessage.user(sanitizedUserMessage)),
						List.of(),
						OpenAiCompletionParameters.scopeClassifier(properties.getScopeClassifierMaxTokens())));
			final ParsedClassification parsed = parseClassification(response.content());
			if (parsed.decision() == AiRequestScopeDecision.ALLOW) {
				if (log.isInfoEnabled()) {
					log.info("AI scope classifier decision=ALLOW");
				}
				return Optional.empty();
			}
			final String assistantMessage = buildAssistantMessage(parsed);
			if (log.isInfoEnabled()) {
				log.info("AI scope classifier decision={} days={} dishes={} plans={} patients={}", parsed.decision(),
						parsed.requestedUnits().days(), parsed.requestedUnits().dishes(),
						parsed.requestedUnits().plans(), parsed.requestedUnits().patients());
			}
			return Optional
				.of(new AiRequestScopeClassifierOutcome(parsed.decision(), assistantMessage, parsed.requestedUnits()));
		}
		catch (final RuntimeException ex) {
			if (log.isWarnEnabled()) {
				log.warn("AI scope classifier failed, allowing request");
			}
			return Optional.empty();
		}
	}

	private String buildAssistantMessage(final ParsedClassification parsed) {
		if (parsed.decision() == AiRequestScopeDecision.CLARIFY) {
			if (StringUtils.hasText(parsed.suggestedPrompt())) {
				return parsed.suggestedPrompt().trim();
			}
			if (StringUtils.hasText(parsed.reason())) {
				return parsed.reason().trim();
			}
			return DEFAULT_CLARIFY_MESSAGE;
		}
		final Optional<String> refusalFromUnits = requestScopeGuard.refusalMessageForUnits(parsed.requestedUnits());
		if (refusalFromUnits.isPresent()) {
			return refusalFromUnits.get();
		}
		if (StringUtils.hasText(parsed.reason())) {
			return parsed.reason().trim();
		}
		return GENERIC_REFUSAL_MESSAGE;
	}

	private ParsedClassification parseClassification(final String content) {
		if (!StringUtils.hasText(content)) {
			throw new AiOrchestrationException("Classifier response empty");
		}
		final JsonNode root = AiToolJsonSerializer.parseJson(stripCodeFence(content.trim()));
		final AiRequestScopeDecision decision = parseDecision(root.path("decision").asText(null));
		final AiRequestScopeRequestedUnits units = parseRequestedUnits(root.path("requestedUnits"));
		final String reason = nullableText(root.path("reason"));
		final String suggestedPrompt = nullableText(root.path("suggestedPrompt"));
		return new ParsedClassification(decision, units, reason, suggestedPrompt);
	}

	private static AiRequestScopeDecision parseDecision(final String rawDecision) {
		if (!StringUtils.hasText(rawDecision)) {
			throw new AiOrchestrationException("Classifier decision missing");
		}
		try {
			return AiRequestScopeDecision.valueOf(rawDecision.trim().toUpperCase(Locale.ROOT));
		}
		catch (final IllegalArgumentException ex) {
			throw new AiOrchestrationException("Classifier decision invalid", ex);
		}
	}

	private static AiRequestScopeRequestedUnits parseRequestedUnits(final JsonNode node) {
		if (node == null || node.isMissingNode() || node.isNull()) {
			return AiRequestScopeRequestedUnits.empty();
		}
		return new AiRequestScopeRequestedUnits(nullableInteger(node.path("days")),
				nullableInteger(node.path("dishes")), nullableInteger(node.path("plans")),
				nullableInteger(node.path("patients")));
	}

	private static Integer nullableInteger(final JsonNode node) {
		if (node == null || node.isMissingNode() || node.isNull()) {
			return null;
		}
		if (node.isNumber()) {
			return node.intValue();
		}
		return null;
	}

	private static String nullableText(final JsonNode node) {
		if (node == null || node.isMissingNode() || node.isNull()) {
			return null;
		}
		final String text = node.asText(null);
		return StringUtils.hasText(text) ? text : null;
	}

	private static String stripCodeFence(final String content) {
		if (content.startsWith("```")) {
			final int firstNewline = content.indexOf('\n');
			final int closingFence = content.lastIndexOf("```");
			if (firstNewline >= 0 && closingFence > firstNewline) {
				return content.substring(firstNewline + 1, closingFence).trim();
			}
		}
		return content;
	}

	private static String loadClassifierPrompt() {
		final ClassPathResource resource = new ClassPathResource(PROMPT_PATH);
		try (InputStream inputStream = resource.getInputStream()) {
			return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
		}
		catch (final IOException ex) {
			throw new IllegalStateException("Missing AI scope classifier prompt: " + PROMPT_PATH, ex);
		}
	}

	private record ParsedClassification(AiRequestScopeDecision decision, AiRequestScopeRequestedUnits requestedUnits,
			String reason, String suggestedPrompt) {
	}

}
