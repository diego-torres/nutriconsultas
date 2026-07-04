package com.nutriconsultas.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AiRequestScopeClassifierTest {

	private AiRequestScopeClassifierImpl classifier;

	@Mock
	private OpenAiClientService openAiClientService;

	private AiRequestScopeGuard requestScopeGuard;

	@BeforeEach
	void setUp() {
		final AiProperties properties = new AiProperties();
		properties.setScopeClassifierEnabled(true);
		requestScopeGuard = new AiRequestScopeGuard(properties);
		classifier = new AiRequestScopeClassifierImpl(properties, openAiClientService, requestScopeGuard);
	}

	@Test
	void returnsEmptyWhenDisabled() {
		final AiProperties properties = new AiProperties();
		properties.setScopeClassifierEnabled(false);
		final AiRequestScopeClassifierImpl disabledClassifier = new AiRequestScopeClassifierImpl(properties,
				openAiClientService, requestScopeGuard);

		assertThat(disabledClassifier.evaluate("Genera variaciones para cada paciente")).isEmpty();

		verify(openAiClientService, never()).chatCompletion(any());
	}

	@Test
	void allowDecisionProceedsWithoutOutcome() {
		when(openAiClientService.chatCompletion(any())).thenReturn(classifierResponse("""
				{
				  "decision": "ALLOW",
				  "requestedUnits": { "days": 7, "dishes": 1, "plans": 1, "patients": 1 },
				  "reason": "Dentro de límites",
				  "suggestedPrompt": ""
				}
				"""));

		assertThat(classifier.evaluate("Genera un menú de 7 días")).isEmpty();
	}

	@Test
	void refuseDecisionUsesDeterministicCopyFromUnits() {
		when(openAiClientService.chatCompletion(any())).thenReturn(classifierResponse("""
				{
				  "decision": "REFUSE",
				  "requestedUnits": { "days": null, "dishes": 5, "plans": null, "patients": null },
				  "reason": "Demasiados platillos",
				  "suggestedPrompt": ""
				}
				"""));

		final AiRequestScopeClassifierOutcome outcome = classifier
			.evaluate("Genera variaciones de platillos para todo el consultorio")
			.orElseThrow();

		assertThat(outcome.decision()).isEqualTo(AiRequestScopeDecision.REFUSE);
		assertThat(outcome.assistantMessage()).contains("5 platillos").contains("borrador de ejemplo");
	}

	@Test
	void clarifyDecisionUsesSuggestedPrompt() {
		when(openAiClientService.chatCompletion(any())).thenReturn(classifierResponse("""
				{
				  "decision": "CLARIFY",
				  "requestedUnits": { "days": null, "dishes": null, "plans": null, "patients": null },
				  "reason": "Falta precisar alcance",
				  "suggestedPrompt": "¿Para cuántos días quieres el borrador?"
				}
				"""));

		final AiRequestScopeClassifierOutcome outcome = classifier.evaluate("Arma un plan muy completo").orElseThrow();

		assertThat(outcome.decision()).isEqualTo(AiRequestScopeDecision.CLARIFY);
		assertThat(outcome.assistantMessage()).isEqualTo("¿Para cuántos días quieres el borrador?");
	}

	@Test
	void usesLowTemperatureJsonCompletionRequest() {
		when(openAiClientService.chatCompletion(any())).thenReturn(classifierResponse("""
				{"decision":"ALLOW","requestedUnits":{},"reason":"","suggestedPrompt":""}
				"""));

		classifier.evaluate("Busca avena en el catálogo");

		final ArgumentCaptor<OpenAiChatCompletionRequest> captor = ArgumentCaptor
			.forClass(OpenAiChatCompletionRequest.class);
		verify(openAiClientService).chatCompletion(captor.capture());
		assertThat(captor.getValue().parameters().temperature()).isEqualTo(0.0);
		assertThat(captor.getValue().parameters().maxTokens()).isEqualTo(200);
		assertThat(captor.getValue().parameters().responseFormatType()).isEqualTo("json_object");
		assertThat(captor.getValue().tools()).isEmpty();
	}

	@Test
	void failsOpenOnInvalidJson() {
		when(openAiClientService.chatCompletion(any()))
			.thenReturn(new OpenAiChatCompletionResponse("id", "assistant", "not json", List.of(), "stop", null));

		assertThat(classifier.evaluate("Plan ambiguo para el consultorio")).isEmpty();
	}

	private static OpenAiChatCompletionResponse classifierResponse(final String json) {
		return new OpenAiChatCompletionResponse("id-classifier", "assistant", json, List.of(), "stop", null);
	}

}
