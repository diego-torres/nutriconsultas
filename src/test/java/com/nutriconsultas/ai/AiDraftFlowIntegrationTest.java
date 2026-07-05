package com.nutriconsultas.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import com.nutriconsultas.alimentos.Alimento;
import com.nutriconsultas.alimentos.AlimentosRepository;
import com.nutriconsultas.platillos.PlatilloRepository;

/**
 * End-to-end AI draft flow with real persistence and tool dispatch; OpenAI is mocked only
 * (#403).
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = { "nutriconsultas.ai.enabled=true", "nutriconsultas.ai.openai.api-key=test-key",
		"nutriconsultas.ai.openai.model=gpt-test", "nutriconsultas.ai.scope-classifier-enabled=false" })
class AiDraftFlowIntegrationTest {

	private static final String NUTRITIONIST_A = "auth0|ai-draft-flow-a";

	private static final String NUTRITIONIST_B = "auth0|ai-draft-flow-b";

	private static final String ASSISTANT_REPLY = "Listo. Creé un borrador de platillo para tu revisión. "
			+ "Es un Borrador IA — revisión del nutriólogo requerida.";

	@Autowired
	private AiChatService chatService;

	@Autowired
	private AiDraftLifecycleService draftLifecycleService;

	@Autowired
	private AiDraftAcceptanceService draftAcceptanceService;

	@Autowired
	private AiChatThreadRepository threadRepository;

	@Autowired
	private AiChatMessageRepository messageRepository;

	@Autowired
	private AiGeneratedDraftRepository draftRepository;

	@Autowired
	private AlimentosRepository alimentosRepository;

	@Autowired
	private PlatilloRepository platilloRepository;

	@MockBean
	private OpenAiClientService openAiClientService;

	private long catalogAlimentoId;

	@BeforeEach
	void setUpOpenAiAndCatalog() {
		when(openAiClientService.isAvailable()).thenReturn(true);
		catalogAlimentoId = alimentosRepository.saveAndFlush(sampleAlimento()).getId();
	}

	@Test
	void sendMessagePersistsAssistantReplyAndDishDraft() {
		mockDishDraftToolLoop(catalogAlimentoId);
		final AiChatThread thread = chatService.startThread(NUTRITIONIST_A, "Borrador tacos", null, null, null);

		final AiOrchestrationResult result = chatService.sendMessage(NUTRITIONIST_A, thread.getId(),
				"Crea un borrador de tacos de pollo con pechuga del catálogo.", null);

		assertThat(result.toolCallsExecuted()).isEqualTo(1);
		assertThat(result.assistantMessage().getRole()).isEqualTo(AiChatMessageRole.ASSISTANT);
		assertThat(result.assistantMessage().getContent()).contains("borrador");

		final List<AiChatMessage> messages = messageRepository.findByThreadIdOrderByCreatedAtAscIdAsc(thread.getId());
		assertThat(messages).anyMatch(message -> message.getRole() == AiChatMessageRole.USER);
		assertThat(messages).anyMatch(message -> message.getRole() == AiChatMessageRole.ASSISTANT
				&& message.getContent().contains("borrador"));
		assertThat(messages).anyMatch(message -> message.getRole() == AiChatMessageRole.TOOL
				&& CreateDishDraftToolService.TOOL_NAME.equals(message.getToolName()));

		final List<AiGeneratedDraft> drafts = draftRepository
			.findByThreadIdAndStatusOrderByCreatedAtDescIdDesc(thread.getId(), AiDraftStatus.DRAFT);
		assertThat(drafts).hasSize(1);
		assertThat(drafts.getFirst().getDraftType()).isEqualTo(AiDraftType.DISH);
		assertThat(drafts.getFirst().getJsonPayload()).contains("Tacos de pollo");
		assertThat(platilloRepository.count()).isZero();
	}

	@Test
	void discardDraftMarksDiscardedWithoutCreatingPlatillo() {
		mockDishDraftToolLoop(catalogAlimentoId);
		final AiChatThread thread = chatService.startThread(NUTRITIONIST_A, "Descartar borrador", null, null, null);
		chatService.sendMessage(NUTRITIONIST_A, thread.getId(), "Borrador de tacos de pollo.", null);

		final long draftId = draftRepository
			.findByThreadIdAndStatusOrderByCreatedAtDescIdDesc(thread.getId(), AiDraftStatus.DRAFT)
			.getFirst()
			.getId();
		final AiGeneratedDraft discarded = draftLifecycleService.discardDraft(draftId, NUTRITIONIST_A);

		assertThat(discarded.getStatus()).isEqualTo(AiDraftStatus.DISCARDED);
		assertThat(draftRepository.findByIdAndThreadNutritionistId(draftId, NUTRITIONIST_A)).isPresent();
		assertThat(platilloRepository.count()).isZero();
	}

	@Test
	void acceptDraftMaterializesPlatilloForOwner() {
		mockDishDraftToolLoop(catalogAlimentoId);
		final AiChatThread thread = chatService.startThread(NUTRITIONIST_A, "Aceptar borrador", null, null, null);
		chatService.sendMessage(NUTRITIONIST_A, thread.getId(), "Borrador de tacos de pollo para aceptar.", null);

		final long draftId = draftRepository
			.findByThreadIdAndStatusOrderByCreatedAtDescIdDesc(thread.getId(), AiDraftStatus.DRAFT)
			.getFirst()
			.getId();
		final long platillosBefore = platilloRepository.count();

		final AiDraftAcceptanceResult accepted = draftAcceptanceService.accept(draftId, NUTRITIONIST_A, principal());

		assertThat(accepted.status()).isEqualTo(AiDraftStatus.ACCEPTED);
		assertThat(accepted.createdEntityType()).isEqualTo(AiDraftCreatedEntityType.PLATILLO);
		assertThat(accepted.createdEntityId()).isNotNull();
		assertThat(platilloRepository.count()).isEqualTo(platillosBefore + 1);
		assertThat(platilloRepository.findByIdAndUserId(accepted.createdEntityId(), NUTRITIONIST_A)).isPresent();
	}

	@Test
	void crossTenantNutritionistCannotAccessThreadOrDraft() {
		mockDishDraftToolLoop(catalogAlimentoId);
		final AiChatThread thread = chatService.startThread(NUTRITIONIST_A, "IDOR", null, null, null);
		chatService.sendMessage(NUTRITIONIST_A, thread.getId(), "Borrador de tacos de pollo.", null);
		final long draftId = draftRepository
			.findByThreadIdAndStatusOrderByCreatedAtDescIdDesc(thread.getId(), AiDraftStatus.DRAFT)
			.getFirst()
			.getId();

		assertThat(threadRepository.findByIdAndNutritionistId(thread.getId(), NUTRITIONIST_B)).isEmpty();
		assertThat(draftRepository.findByIdAndThreadNutritionistId(draftId, NUTRITIONIST_B)).isEmpty();

		assertThatThrownBy(() -> chatService.getThread(NUTRITIONIST_B, thread.getId()))
			.isInstanceOf(AiChatException.class);
		assertThatThrownBy(() -> draftLifecycleService.discardDraft(draftId, NUTRITIONIST_B))
			.isInstanceOf(AiDraftLifecycleException.class)
			.hasMessageContaining("Borrador no encontrado");
		assertThatThrownBy(() -> draftAcceptanceService.accept(draftId, NUTRITIONIST_B, principal(NUTRITIONIST_B)))
			.isInstanceOf(AiDraftLifecycleException.class)
			.hasMessageContaining("Borrador no encontrado");
	}

	private void mockDishDraftToolLoop(final long alimentoId) {
		final String toolArguments = """
				{
				  "name": "Tacos de pollo",
				  "ingredients": [
				    { "alimentoId": %d, "cantidad": "1" }
				  ],
				  "portions": 2
				}
				""".formatted(alimentoId);
		when(openAiClientService.chatCompletion(any()))
			.thenReturn(new OpenAiChatCompletionResponse("id-tools", "assistant", null,
					List.of(new OpenAiToolCall("call_dish", CreateDishDraftToolService.TOOL_NAME, toolArguments)),
					"tool_calls", new OpenAiTokenUsage(10, 5, 15)))
			.thenReturn(new OpenAiChatCompletionResponse("id-final", "assistant", ASSISTANT_REPLY, List.of(), "stop",
					new OpenAiTokenUsage(20, 10, 30)));
	}

	private static Alimento sampleAlimento() {
		final Alimento alimento = new Alimento();
		alimento.setNombreAlimento("Pechuga de pollo");
		alimento.setClasificacion("AOA");
		alimento.setUnidad("g");
		alimento.setCantSugerida(1.0);
		alimento.setEnergia(165);
		alimento.setPesoNeto(100);
		alimento.setProteina(31.0);
		alimento.setLipidos(3.6);
		alimento.setHidratosDeCarbono(0.0);
		alimento.setFibra(0.0);
		alimento.setSodio(74.0);
		alimento.setPotasio(256.0);
		return alimento;
	}

	private static DefaultOidcUser principal() {
		return principal(NUTRITIONIST_A);
	}

	private static DefaultOidcUser principal(final String subject) {
		final Jwt jwt = Jwt.withTokenValue("token").header("alg", "none").subject(subject).build();
		final OidcIdToken idToken = new OidcIdToken(jwt.getTokenValue(), jwt.getIssuedAt(), jwt.getExpiresAt(),
				Map.of("sub", subject));
		return new DefaultOidcUser(List.of(), idToken);
	}

}
