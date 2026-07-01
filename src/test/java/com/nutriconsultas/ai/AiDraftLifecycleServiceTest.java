package com.nutriconsultas.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AiDraftLifecycleServiceTest {

	private static final String NUTRITIONIST_A = "auth0|nutritionist-a";

	private static final String NUTRITIONIST_B = "auth0|nutritionist-b";

	@InjectMocks
	private AiDraftLifecycleServiceImpl service;

	@Mock
	private AiChatThreadRepository threadRepository;

	@Mock
	private AiGeneratedDraftRepository draftRepository;

	private AiChatThread thread;

	@BeforeEach
	void setUp() {
		thread = new AiChatThread();
		thread.setId(10L);
		thread.setNutritionistId(NUTRITIONIST_A);
		thread.setTitle("Menú");
	}

	@Test
	void createDraftPersistsDraftStatus() {
		when(threadRepository.findByIdAndNutritionistId(10L, NUTRITIONIST_A)).thenReturn(Optional.of(thread));
		when(draftRepository.save(any(AiGeneratedDraft.class))).thenAnswer(invocation -> {
			final AiGeneratedDraft draft = invocation.getArgument(0);
			draft.setId(99L);
			return draft;
		});

		final AiGeneratedDraft created = service.createDraft(10L, NUTRITIONIST_A, AiDraftType.MENU,
				"{\"title\":\"Menú lunes\"}");

		assertThat(created.getId()).isEqualTo(99L);
		assertThat(created.getStatus()).isEqualTo(AiDraftStatus.DRAFT);
		assertThat(created.getDraftType()).isEqualTo(AiDraftType.MENU);
		assertThat(created.getAcceptedAt()).isNull();
	}

	@Test
	void createDraftRejectsUnknownThread() {
		when(threadRepository.findByIdAndNutritionistId(10L, NUTRITIONIST_B)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.createDraft(10L, NUTRITIONIST_B, AiDraftType.DISH, "{}"))
			.isInstanceOf(AiDraftLifecycleException.class)
			.hasMessageContaining("Conversación no encontrada");

		verify(draftRepository, never()).save(any());
	}

	@Test
	void acceptDraftSetsAcceptedTimestamp() {
		final AiGeneratedDraft draft = sampleDraft(AiDraftStatus.DRAFT);
		when(draftRepository.findByIdAndThreadNutritionistId(99L, NUTRITIONIST_A)).thenReturn(Optional.of(draft));
		when(draftRepository.save(draft)).thenReturn(draft);

		final AiGeneratedDraft accepted = service.acceptDraft(99L, NUTRITIONIST_A);

		assertThat(accepted.getStatus()).isEqualTo(AiDraftStatus.ACCEPTED);
		assertThat(accepted.getAcceptedAt()).isNotNull();
	}

	@Test
	void discardDraftSetsDiscardedStatus() {
		final AiGeneratedDraft draft = sampleDraft(AiDraftStatus.DRAFT);
		when(draftRepository.findByIdAndThreadNutritionistId(99L, NUTRITIONIST_A)).thenReturn(Optional.of(draft));
		when(draftRepository.save(draft)).thenReturn(draft);

		final AiGeneratedDraft discarded = service.discardDraft(99L, NUTRITIONIST_A);

		assertThat(discarded.getStatus()).isEqualTo(AiDraftStatus.DISCARDED);
		assertThat(discarded.getAcceptedAt()).isNull();
	}

	@Test
	void acceptDraftBlocksCrossTenantAccess() {
		when(draftRepository.findByIdAndThreadNutritionistId(99L, NUTRITIONIST_B)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.acceptDraft(99L, NUTRITIONIST_B)).isInstanceOf(AiDraftLifecycleException.class)
			.hasMessageContaining("Borrador no encontrado");

		verify(draftRepository, never()).save(any());
	}

	@Test
	void acceptDraftRejectsAlreadyAcceptedDraft() {
		final AiGeneratedDraft draft = sampleDraft(AiDraftStatus.ACCEPTED);
		when(draftRepository.findByIdAndThreadNutritionistId(99L, NUTRITIONIST_A)).thenReturn(Optional.of(draft));

		assertThatThrownBy(() -> service.acceptDraft(99L, NUTRITIONIST_A)).isInstanceOf(AiDraftLifecycleException.class)
			.hasMessageContaining("ya no se puede modificar");

		verify(draftRepository, never()).save(any());
	}

	@Test
	void discardDraftRejectsDiscardedDraft() {
		final AiGeneratedDraft draft = sampleDraft(AiDraftStatus.DISCARDED);
		when(draftRepository.findByIdAndThreadNutritionistId(99L, NUTRITIONIST_A)).thenReturn(Optional.of(draft));

		assertThatThrownBy(() -> service.discardDraft(99L, NUTRITIONIST_A))
			.isInstanceOf(AiDraftLifecycleException.class);

		verify(draftRepository, never()).save(any());
	}

	@Test
	void createDraftTrimsJsonPayload() {
		when(threadRepository.findByIdAndNutritionistId(10L, NUTRITIONIST_A)).thenReturn(Optional.of(thread));
		when(draftRepository.save(any(AiGeneratedDraft.class))).thenAnswer(invocation -> invocation.getArgument(0));

		service.createDraft(10L, NUTRITIONIST_A, AiDraftType.DISH, "  {\"name\":\"Tacos\"}  ");

		final ArgumentCaptor<AiGeneratedDraft> captor = ArgumentCaptor.forClass(AiGeneratedDraft.class);
		verify(draftRepository).save(captor.capture());
		assertThat(captor.getValue().getJsonPayload()).isEqualTo("{\"name\":\"Tacos\"}");
	}

	private AiGeneratedDraft sampleDraft(final AiDraftStatus status) {
		final AiGeneratedDraft draft = new AiGeneratedDraft();
		draft.setId(99L);
		draft.setThread(thread);
		draft.setDraftType(AiDraftType.MENU);
		draft.setJsonPayload("{\"title\":\"Menú\"}");
		draft.setStatus(status);
		return draft;
	}

}
