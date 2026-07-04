package com.nutriconsultas.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.nutriconsultas.ai.AiBulkScopeGoldenPrompt.Scenario;

/**
 * Golden prompt evaluation for bulk scope guards (#450). Uses mocked OpenAI only — no live API calls.
 */
class AiBulkScopeGoldenPromptTest {

	private AiProperties properties;

	private AiRequestScopeGuard guard;

	private AiRequestScopeClassifier classifier;

	private AiRequestScopePipeline pipeline;

	@BeforeEach
	void setUp() {
		properties = new AiProperties();
		properties.setScopeClassifierEnabled(true);
		guard = new AiRequestScopeGuard(properties);
		classifier = mock(AiRequestScopeClassifier.class);
		when(classifier.evaluate(any(String.class))).thenReturn(Optional.empty());
		pipeline = new AiRequestScopePipeline(guard, classifier);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("mustRefuseScenarios")
	void deterministicGuardMustRefuse(final Scenario scenario) {
		final Optional<AiRequestScopeViolation> violation = guard.evaluate(scenario.prompt());

		assertThat(violation).as("scenario %s", scenario.id()).isPresent();
		AiBulkScopeGoldenPrompt.assertConstructiveAlternative(violation.orElseThrow().refusalMessage());
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("mustAllowScenarios")
	void deterministicGuardMustAllow(final Scenario scenario) {
		assertThat(guard.evaluate(scenario.prompt())).as("scenario %s", scenario.id()).isEmpty();
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("refuseOrClarifyScenarios")
	void refuseOrClarifyHandledByGuardOrClassifier(final Scenario scenario) {
		final Optional<AiRequestScopeViolation> guardOutcome = guard.evaluate(scenario.prompt());
		if (guardOutcome.isPresent()) {
			AiBulkScopeGoldenPrompt.assertConstructiveAlternative(guardOutcome.orElseThrow().refusalMessage());
			return;
		}
		when(classifier.evaluate(scenario.prompt())).thenReturn(Optional.of(new AiRequestScopeClassifierOutcome(
				AiRequestScopeDecision.CLARIFY, "¿Para cuántos días quieres el borrador de ejemplo?", null)));

		final Optional<AiRequestScopePipeline.ScopeShortCircuit> shortCircuit = pipeline.evaluate(scenario.prompt());

		assertThat(shortCircuit).as("scenario %s", scenario.id()).isPresent();
		assertThat(shortCircuit.orElseThrow().assistantMessage()).isNotBlank();
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("mustRefuseScenarios")
	void pipelineShortCircuitsWithoutClassifierWhenGuardRefuses(final Scenario scenario) {
		final Optional<AiRequestScopePipeline.ScopeShortCircuit> shortCircuit = pipeline.evaluate(scenario.prompt());

		assertThat(shortCircuit).isPresent();
		verify(classifier, never()).evaluate(any(String.class));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("mustRefuseScenarios")
	void orchestrationMustNotCallOpenAiOnGuardRefusal(final Scenario scenario) {
		final OrchestrationTestDependencies dependencies = new OrchestrationTestDependencies();
		when(dependencies.threadRepository().findByIdAndNutritionistId(10L, "auth0|nutritionist-a"))
			.thenReturn(Optional.of(thread()));
		when(dependencies.messageRepository().save(any(AiChatMessage.class))).thenAnswer(invocation -> {
			final AiChatMessage message = invocation.getArgument(0);
			message.setId(100L);
			return message;
		});

		dependencies.service().processUserMessage(context(), scenario.prompt());

		verify(dependencies.openAiClientService(), never()).chatCompletion(any());
	}

	@Test
	void ambiguousClinicPlanUsesClassifierWhenGuardAllows() {
		final Scenario scenario = AiBulkScopeGoldenPrompt.scenarios()
			.filter(s -> "ambiguous-clinic-plan".equals(s.id()))
			.findFirst()
			.orElseThrow();
		assertThat(guard.evaluate(scenario.prompt())).isEmpty();

		when(classifier.evaluate(scenario.prompt())).thenReturn(Optional.of(new AiRequestScopeClassifierOutcome(
				AiRequestScopeDecision.REFUSE,
				"No puedo generar planes para todos tus pacientes en un solo turno. "
						+ "Puedo ayudarte con 1 borrador de ejemplo que revises y apruebes.",
				new AiRequestScopeRequestedUnits(null, null, null, 0))));

		final Optional<AiRequestScopePipeline.ScopeShortCircuit> shortCircuit = pipeline.evaluate(scenario.prompt());

		assertThat(shortCircuit).isPresent();
		AiBulkScopeGoldenPrompt.assertConstructiveAlternative(shortCircuit.orElseThrow().assistantMessage());
		verify(classifier).evaluate(scenario.prompt());
	}

	private static Stream<Scenario> mustRefuseScenarios() {
		return AiBulkScopeGoldenPrompt.mustRefuseScenarios();
	}

	private static Stream<Scenario> mustAllowScenarios() {
		return AiBulkScopeGoldenPrompt.mustAllowScenarios();
	}

	private static Stream<Scenario> refuseOrClarifyScenarios() {
		return AiBulkScopeGoldenPrompt.ambiguousScenarios();
	}

	private static AiOrchestrationContext context() {
		return new AiOrchestrationContext("auth0|nutritionist-a", 10L, null, null, null);
	}

	private static AiChatThread thread() {
		final AiChatThread thread = new AiChatThread();
		thread.setId(10L);
		thread.setNutritionistId("auth0|nutritionist-a");
		return thread;
	}

	private static final class OrchestrationTestDependencies {

		private final AiProperties properties = mock(AiProperties.class);

		private final OpenAiClientService openAiClientService = mock(OpenAiClientService.class);

		private final AiSystemPromptService systemPromptService = mock(AiSystemPromptService.class);

		private final AiChatThreadRepository threadRepository = mock(AiChatThreadRepository.class);

		private final AiChatMessageRepository messageRepository = mock(AiChatMessageRepository.class);

		private final AiOpenAiToolCatalog toolCatalog = mock(AiOpenAiToolCatalog.class);

		private final AiOrchestrationToolDispatcher toolDispatcher = mock(AiOrchestrationToolDispatcher.class);

		private final AiUserMessageGuard userMessageGuard;

		private final AiRequestScopePipeline requestScopePipeline;

		private final AiOrchestrationServiceImpl service;

		private OrchestrationTestDependencies() {
			final AiProperties guardProperties = new AiProperties();
			final AiUserMessageGuard realGuard = new AiUserMessageGuard(guardProperties);
			userMessageGuard = mock(AiUserMessageGuard.class);
			when(userMessageGuard.validateAndSanitize(any(String.class)))
				.thenAnswer(invocation -> realGuard.validateAndSanitize(invocation.getArgument(0)));
			requestScopePipeline = new AiRequestScopePipeline(new AiRequestScopeGuard(guardProperties),
					mock(AiRequestScopeClassifier.class));
			when(properties.isOperational()).thenReturn(true);
			when(openAiClientService.isAvailable()).thenReturn(true);
			service = new AiOrchestrationServiceImpl(properties, openAiClientService, systemPromptService,
					new AiChatPersistence(threadRepository, messageRepository, mock(org.springframework.transaction.support.TransactionTemplate.class)),
					new AiOrchestrationTools(toolCatalog, toolDispatcher), userMessageGuard, requestScopePipeline);
		}

		private OpenAiClientService openAiClientService() {
			return openAiClientService;
		}

		private AiChatThreadRepository threadRepository() {
			return threadRepository;
		}

		private AiChatMessageRepository messageRepository() {
			return messageRepository;
		}

		private AiOrchestrationServiceImpl service() {
			return service;
		}

	}

}
