package com.nutriconsultas.ai;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class AiChatPersistenceRepositoryTest {

	private static final String NUTRITIONIST_A = "auth0|nutritionist-ai-a";

	private static final String NUTRITIONIST_B = "auth0|nutritionist-ai-b";

	@Autowired
	private AiChatThreadRepository threadRepository;

	@Autowired
	private AiChatMessageRepository messageRepository;

	@Autowired
	private AiGeneratedDraftRepository draftRepository;

	@Autowired
	private com.nutriconsultas.paciente.PacienteRepository pacienteRepository;

	@Autowired
	private EntityManager entityManager;

	@Test
	void threadMessageAndDraftRoundTrip() {
		final AiChatThread thread = threadRepository.saveAndFlush(sampleThread(NUTRITIONIST_A, "Menú semanal"));
		final AiChatMessage message = new AiChatMessage();
		message.setThread(thread);
		message.setRole(AiChatMessageRole.USER);
		message.setContent("Genera un menú de 1800 kcal");
		messageRepository.saveAndFlush(message);

		final AiGeneratedDraft draft = new AiGeneratedDraft();
		draft.setThread(thread);
		draft.setDraftType(AiDraftType.MENU);
		draft.setJsonPayload("{\"title\":\"Menú lunes\"}");
		draftRepository.saveAndFlush(draft);

		entityManager.clear();

		final AiChatThread loaded = threadRepository.findByIdAndNutritionistId(thread.getId(), NUTRITIONIST_A)
			.orElseThrow();
		assertThat(loaded.getTitle()).isEqualTo("Menú semanal");
		assertThat(loaded.getCreatedAt()).isNotNull();
		assertThat(loaded.getUpdatedAt()).isNotNull();
		assertThat(messageRepository.findByThreadIdOrderByCreatedAtAscIdAsc(thread.getId())).hasSize(1);
		assertThat(
				draftRepository.findByThreadIdAndStatusOrderByCreatedAtDescIdDesc(thread.getId(), AiDraftStatus.DRAFT))
			.hasSize(1);
	}

	@Test
	void findByIdAndNutritionistId_blocksCrossTenantAccess() {
		final AiChatThread thread = threadRepository.saveAndFlush(sampleThread(NUTRITIONIST_A, "Borrador platillo"));

		assertThat(threadRepository.findByIdAndNutritionistId(thread.getId(), NUTRITIONIST_A)).isPresent();
		assertThat(threadRepository.findByIdAndNutritionistId(thread.getId(), NUTRITIONIST_B)).isEmpty();
	}

	@Test
	void draftScopedLookupUsesThreadNutritionistId() {
		final AiChatThread thread = threadRepository.saveAndFlush(sampleThread(NUTRITIONIST_A, "Plan 7 días"));
		final AiGeneratedDraft draft = new AiGeneratedDraft();
		draft.setThread(thread);
		draft.setDraftType(AiDraftType.DIET_PLAN);
		draft.setJsonPayload("{\"dayCount\":7}");
		draftRepository.saveAndFlush(draft);

		assertThat(draftRepository.findByIdAndThreadNutritionistId(draft.getId(), NUTRITIONIST_A)).isPresent();
		assertThat(draftRepository.findByIdAndThreadNutritionistId(draft.getId(), NUTRITIONIST_B)).isEmpty();
	}

	@Test
	void threadCanLinkOptionalPatient() {
		final com.nutriconsultas.paciente.Paciente paciente = pacienteRepository.saveAndFlush(samplePaciente());
		final AiChatThread thread = sampleThread(NUTRITIONIST_A, "Paciente vinculado");
		thread.setPatient(paciente);
		threadRepository.saveAndFlush(thread);

		entityManager.clear();

		final AiChatThread loaded = threadRepository.findById(thread.getId()).orElseThrow();
		assertThat(loaded.getPatient().getId()).isEqualTo(paciente.getId());
	}

	@Test
	void listThreadsByNutritionistOrdersByUpdatedAtDesc() {
		final AiChatThread older = sampleThread(NUTRITIONIST_A, "Antiguo");
		threadRepository.saveAndFlush(older);
		final AiChatThread newer = sampleThread(NUTRITIONIST_A, "Reciente");
		threadRepository.saveAndFlush(newer);

		final List<AiChatThread> threads = threadRepository.findByNutritionistIdOrderByUpdatedAtDesc(NUTRITIONIST_A);

		assertThat(threads).hasSizeGreaterThanOrEqualTo(2);
		assertThat(threads.getFirst().getTitle()).isEqualTo("Reciente");
	}

	private static AiChatThread sampleThread(final String nutritionistId, final String title) {
		final AiChatThread thread = new AiChatThread();
		thread.setNutritionistId(nutritionistId);
		thread.setTitle(title);
		return thread;
	}

	private static com.nutriconsultas.paciente.Paciente samplePaciente() {
		final com.nutriconsultas.paciente.Paciente paciente = new com.nutriconsultas.paciente.Paciente();
		paciente.setName("AI Test Patient");
		paciente.setUserId(NUTRITIONIST_A);
		paciente.setStatus(com.nutriconsultas.paciente.PacienteStatus.ACTIVE);
		final LocalDate dob = LocalDate.now().minusYears(28);
		paciente.setDob(Date.from(dob.atStartOfDay(ZoneId.systemDefault()).toInstant()));
		paciente.setGender("F");
		return paciente;
	}

}
