package com.nutriconsultas.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import com.nutriconsultas.paciente.Paciente;
import com.nutriconsultas.paciente.PacienteRepository;

@ExtendWith(MockitoExtension.class)
class AiChatServiceTest {

	private static final String NUTRITIONIST_ID = "auth0|nutritionist-a";

	private static final String OTHER_NUTRITIONIST_ID = "auth0|nutritionist-b";

	@InjectMocks
	private AiChatServiceImpl service;

	@Mock
	private AiChatThreadRepository threadRepository;

	@Mock
	private AiChatMessageRepository messageRepository;

	@Mock
	private AiGeneratedDraftRepository draftRepository;

	@Mock
	private AiOrchestrationService orchestrationService;

	@Mock
	private AiPatientPromptContextResolver patientContextResolver;

	@Mock
	private AiDietaPromptContextResolver dietaContextResolver;

	@Mock
	private AiPlatilloPromptContextResolver platilloContextResolver;

	@Mock
	private PacienteRepository pacienteRepository;

	@Test
	void startThreadPersistsOwnedPatient() {
		final Paciente paciente = new Paciente();
		paciente.setId(10L);
		when(pacienteRepository.findByIdAndUserId(10L, NUTRITIONIST_ID)).thenReturn(Optional.of(paciente));
		when(threadRepository.save(any(AiChatThread.class))).thenAnswer(invocation -> {
			final AiChatThread thread = invocation.getArgument(0);
			thread.setId(5L);
			thread.setCreatedAt(Instant.now());
			thread.setUpdatedAt(Instant.now());
			return thread;
		});

		final AiChatThread thread = service.startThread(NUTRITIONIST_ID, "Menú", 10L, null,
				AiChatPromptContext.empty());

		assertThat(thread.getId()).isEqualTo(5L);
		assertThat(thread.getPatient()).isNotNull();
		assertThat(thread.getPatient().getId()).isEqualTo(10L);
	}

	@Test
	void startThreadRejectsForeignPatient() {
		when(pacienteRepository.findByIdAndUserId(10L, NUTRITIONIST_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.startThread(NUTRITIONIST_ID, null, 10L, null, AiChatPromptContext.empty()))
			.isInstanceOf(AiChatException.class)
			.extracting(ex -> ((AiChatException) ex).getHttpStatus())
			.isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	void getThreadFiltersToolMessages() {
		final AiChatThread thread = sampleThread(5L, NUTRITIONIST_ID);
		when(threadRepository.findByIdAndNutritionistId(5L, NUTRITIONIST_ID)).thenReturn(Optional.of(thread));
		final AiChatMessage user = message(1L, AiChatMessageRole.USER, "Hola");
		final AiChatMessage assistant = message(2L, AiChatMessageRole.ASSISTANT, "Respuesta");
		final AiChatMessage tool = message(3L, AiChatMessageRole.TOOL, "{\"success\":true}");
		when(messageRepository.findByThreadIdOrderByCreatedAtAscIdAsc(5L)).thenReturn(List.of(user, assistant, tool));

		final AiChatThreadDetail detail = service.getThread(NUTRITIONIST_ID, 5L);

		assertThat(detail.messages()).hasSize(2);
		assertThat(detail.messages()).extracting(AiChatMessageView::role)
			.containsExactly(AiChatMessageRole.USER, AiChatMessageRole.ASSISTANT);
	}

	@Test
	void getThreadDeniesCrossTenantAccess() {
		when(threadRepository.findByIdAndNutritionistId(5L, OTHER_NUTRITIONIST_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.getThread(OTHER_NUTRITIONIST_ID, 5L)).isInstanceOf(AiChatException.class)
			.extracting(ex -> ((AiChatException) ex).getHttpStatus())
			.isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	void sendMessageDelegatesToOrchestration() {
		final AiChatThread thread = sampleThread(5L, NUTRITIONIST_ID);
		when(threadRepository.findByIdAndNutritionistId(5L, NUTRITIONIST_ID)).thenReturn(Optional.of(thread));
		final AiChatMessage assistant = message(99L, AiChatMessageRole.ASSISTANT, "Listo");
		when(patientContextResolver.resolve(null, NUTRITIONIST_ID)).thenReturn(Optional.empty());
		when(dietaContextResolver.resolve(null, NUTRITIONIST_ID)).thenReturn(Optional.empty());
		when(platilloContextResolver.resolve(null, NUTRITIONIST_ID)).thenReturn(Optional.empty());
		when(orchestrationService.processUserMessage(any(), eq("Crea un menú")))
			.thenReturn(new AiOrchestrationResult(5L, assistant, 2, null));

		final AiOrchestrationResult result = service.sendMessage(NUTRITIONIST_ID, 5L, "Crea un menú",
				AiChatPromptContext.empty());

		assertThat(result.toolCallsExecuted()).isEqualTo(2);
		verify(orchestrationService).processUserMessage(any(), eq("Crea un menú"));
	}

	@Test
	void listDraftsReturnsSummaries() {
		final AiChatThread thread = sampleThread(5L, NUTRITIONIST_ID);
		when(threadRepository.findByIdAndNutritionistId(5L, NUTRITIONIST_ID)).thenReturn(Optional.of(thread));
		final AiGeneratedDraft draft = new AiGeneratedDraft();
		draft.setId(11L);
		draft.setDraftType(AiDraftType.DISH);
		draft.setStatus(AiDraftStatus.DRAFT);
		draft.setJsonPayload(
				"{\"name\":\"Tacos\",\"ingredients\":[],\"portions\":1,\"nutrientsPerPortion\":{\"energiaKcal\":0,\"proteinaG\":0.0,\"lipidosG\":0.0,\"hidratosDeCarbonoG\":0.0,\"fibraG\":0.0,\"sodioMg\":0.0,\"potasioMg\":0.0},\"label\":\"Borrador IA\"}");
		draft.setCreatedAt(Instant.now());
		when(draftRepository.findByThreadIdOrderByCreatedAtDescIdDesc(5L)).thenReturn(List.of(draft));

		final AiChatDraftList list = service.listDrafts(NUTRITIONIST_ID, 5L);

		assertThat(list.drafts()).hasSize(1);
		assertThat(list.drafts().get(0).summary()).contains("Tacos");
	}

	@Test
	void sendMessageRejectsBlankMessage() {
		assertThatThrownBy(() -> service.sendMessage(NUTRITIONIST_ID, 5L, "  ", AiChatPromptContext.empty()))
			.isInstanceOf(AiChatException.class);
		verify(orchestrationService, never()).processUserMessage(any(), any());
	}

	private static AiChatThread sampleThread(final long id, final String nutritionistId) {
		final AiChatThread thread = new AiChatThread();
		thread.setId(id);
		thread.setNutritionistId(nutritionistId);
		thread.setTitle("Test");
		thread.setCreatedAt(Instant.now());
		thread.setUpdatedAt(Instant.now());
		return thread;
	}

	private static AiChatMessage message(final long id, final AiChatMessageRole role, final String content) {
		final AiChatMessage message = new AiChatMessage();
		message.setId(id);
		message.setRole(role);
		message.setContent(content);
		message.setCreatedAt(Instant.now());
		return message;
	}

}
