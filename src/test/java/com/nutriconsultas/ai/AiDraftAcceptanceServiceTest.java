package com.nutriconsultas.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;

import com.nutriconsultas.dieta.Dieta;
import com.nutriconsultas.paciente.PacienteDieta;
import com.nutriconsultas.paciente.PacienteDietaService;
import com.nutriconsultas.platillos.Platillo;

@ExtendWith(MockitoExtension.class)
class AiDraftAcceptanceServiceTest {

	private static final String NUTRITIONIST_ID = "auth0|nutritionist-a";

	@InjectMocks
	private AiDraftAcceptanceServiceImpl service;

	@Mock
	private AiGeneratedDraftRepository draftRepository;

	@Mock
	private AiDraftMaterializationService materializationService;

	@Mock
	private AiDraftLifecycleService draftLifecycleService;

	@Mock
	private AiEntitlementGuard aiEntitlementGuard;

	@Mock
	private AiAuditLogger auditLogger;

	@Mock
	private PacienteDietaService pacienteDietaService;

	@org.junit.jupiter.api.BeforeEach
	void stubEntitlement() {
		org.mockito.Mockito.lenient()
			.doNothing()
			.when(aiEntitlementGuard)
			.assertCanUseAiAssistant(org.mockito.ArgumentMatchers.anyString());
	}

	@Test
	void acceptMaterializesDishDraftAndMarksAccepted() {
		final AiGeneratedDraft draft = dishDraft();
		when(draftRepository.findByIdAndThreadNutritionistId(55L, NUTRITIONIST_ID)).thenReturn(Optional.of(draft));
		final Platillo platillo = new Platillo();
		platillo.setId(200L);
		platillo.setName("Tacos");
		when(materializationService.materializeDish(any(), eq(NUTRITIONIST_ID), any())).thenReturn(platillo);
		final AiGeneratedDraft accepted = dishDraft();
		accepted.setStatus(AiDraftStatus.ACCEPTED);
		accepted.setThread(draft.getThread());
		when(draftLifecycleService.acceptDraft(eq(55L), eq(NUTRITIONIST_ID), eq(AiDraftCreatedEntityType.PLATILLO),
				eq(200L), eq("Tacos")))
			.thenReturn(accepted);

		final AiDraftAcceptanceResult result = service.accept(55L, NUTRITIONIST_ID, principal());

		assertThat(result.createdEntityType()).isEqualTo(AiDraftCreatedEntityType.PLATILLO);
		assertThat(result.createdEntityId()).isEqualTo(200L);
		assertThat(result.createdEntityName()).isEqualTo("Tacos");
		assertThat(result.createdEntityPath()).isEqualTo("/admin/platillos/200");
		assertThat(result.summary()).contains("Tacos").doesNotContain("id=");
		assertThat(result.pacienteId()).isNull();
		assertThat(result.status()).isEqualTo(AiDraftStatus.ACCEPTED);
		verify(materializationService).materializeDish(any(), eq(NUTRITIONIST_ID), any());
		verify(pacienteDietaService, never()).assignDieta(any(), any(), any(), any());
		verify(draftLifecycleService).acceptDraft(55L, NUTRITIONIST_ID, AiDraftCreatedEntityType.PLATILLO, 200L,
				"Tacos");
	}

	@Test
	void acceptRejectsUnknownDraft() {
		when(draftRepository.findByIdAndThreadNutritionistId(55L, NUTRITIONIST_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.accept(55L, NUTRITIONIST_ID, principal()))
			.isInstanceOf(AiDraftLifecycleException.class)
			.hasMessageContaining("Borrador no encontrado");
	}

	@Test
	void acceptMaterializesMenuDraftAsDietaWithoutPatient() {
		final AiGeneratedDraft draft = menuDraft();
		when(draftRepository.findByIdAndThreadNutritionistId(56L, NUTRITIONIST_ID)).thenReturn(Optional.of(draft));
		final Dieta dieta = new Dieta();
		dieta.setId(300L);
		dieta.setNombre("Menú del día");
		when(materializationService.materializeMenu(any(), eq(NUTRITIONIST_ID), any())).thenReturn(dieta);
		final AiGeneratedDraft accepted = menuDraft();
		accepted.setStatus(AiDraftStatus.ACCEPTED);
		accepted.setThread(draft.getThread());
		when(draftLifecycleService.acceptDraft(eq(56L), eq(NUTRITIONIST_ID), eq(AiDraftCreatedEntityType.DIETA),
				eq(300L), eq("Menú del día")))
			.thenReturn(accepted);

		final AiDraftAcceptanceResult result = service.accept(56L, NUTRITIONIST_ID, principal());

		assertThat(result.createdEntityType()).isEqualTo(AiDraftCreatedEntityType.DIETA);
		assertThat(result.createdEntityId()).isEqualTo(300L);
		assertThat(result.createdEntityName()).isEqualTo("Menú del día");
		assertThat(result.createdEntityPath()).isEqualTo("/admin/dietas/300");
		assertThat(result.summary()).contains("Menú del día").doesNotContain("id=");
		assertThat(result.pacienteId()).isNull();
		verify(pacienteDietaService, never()).assignDieta(any(), any(), any(), any());
	}

	@Test
	void acceptMenuDraftWithPacienteAssignsDietForOneMonth() {
		final AiGeneratedDraft draft = menuDraft();
		draft.setPacienteId(77L);
		when(draftRepository.findByIdAndThreadNutritionistId(56L, NUTRITIONIST_ID)).thenReturn(Optional.of(draft));
		final Dieta dieta = new Dieta();
		dieta.setId(300L);
		dieta.setNombre("Menú del día");
		when(materializationService.materializeMenu(any(), eq(NUTRITIONIST_ID), any())).thenReturn(dieta);
		final PacienteDieta assignment = new PacienteDieta();
		assignment.setId(901L);
		when(pacienteDietaService.assignDieta(eq(77L), eq(300L), any(PacienteDieta.class), eq(NUTRITIONIST_ID)))
			.thenReturn(assignment);
		final AiGeneratedDraft accepted = menuDraft();
		accepted.setStatus(AiDraftStatus.ACCEPTED);
		accepted.setPacienteId(77L);
		accepted.setThread(draft.getThread());
		when(draftLifecycleService.acceptDraft(eq(56L), eq(NUTRITIONIST_ID), eq(AiDraftCreatedEntityType.DIETA),
				eq(300L), eq("Menú del día")))
			.thenReturn(accepted);

		final AiDraftAcceptanceResult result = service.accept(56L, NUTRITIONIST_ID, principal());

		assertThat(result.pacienteId()).isEqualTo(77L);
		assertThat(result.pacienteDietaAssignmentId()).isEqualTo(901L);
		assertThat(result.pacienteAssignmentPath()).isEqualTo("/admin/pacientes/77/dietas");
		assertThat(result.summary()).contains("asignó al paciente por 1 mes");
		final ArgumentCaptor<PacienteDieta> shellCaptor = ArgumentCaptor.forClass(PacienteDieta.class);
		verify(pacienteDietaService).assignDieta(eq(77L), eq(300L), shellCaptor.capture(), eq(NUTRITIONIST_ID));
		assertThat(shellCaptor.getValue().getStartDate()).isNotNull();
		assertThat(shellCaptor.getValue().getEndDate()).isNotNull();
	}

	private static AiGeneratedDraft dishDraft() {
		final AiChatThread thread = sampleThread();
		final AiGeneratedDraft draft = new AiGeneratedDraft();
		draft.setId(55L);
		draft.setThread(thread);
		draft.setDraftType(AiDraftType.DISH);
		draft.setStatus(AiDraftStatus.DRAFT);
		draft.setJsonPayload(
				"{\"name\":\"Tacos\",\"ingredients\":[{\"alimentoId\":1,\"cantidad\":\"1\"}],\"portions\":1,"
						+ "\"nutrientsPerPortion\":{\"energiaKcal\":100,\"proteinaG\":10.0,\"lipidosG\":5.0,"
						+ "\"hidratosDeCarbonoG\":12.0,\"fibraG\":1.0,\"sodioMg\":100.0,\"potasioMg\":200.0},"
						+ "\"label\":\"Borrador IA\"}");
		return draft;
	}

	private static AiGeneratedDraft menuDraft() {
		final AiChatThread thread = sampleThread();
		final AiGeneratedDraft draft = new AiGeneratedDraft();
		draft.setId(56L);
		draft.setThread(thread);
		draft.setDraftType(AiDraftType.MENU);
		draft.setStatus(AiDraftStatus.DRAFT);
		draft.setJsonPayload("{\"ingestas\":[{\"nombre\":\"Desayuno\",\"orden\":1,"
				+ "\"items\":[{\"type\":\"ALIMENTO\",\"alimentoId\":1,\"portions\":1}]}],"
				+ "\"nutrientsTotal\":{\"energiaKcal\":500,\"proteinaG\":20.0,\"lipidosG\":10.0,"
				+ "\"hidratosDeCarbonoG\":60.0,\"fibraG\":5.0,\"sodioMg\":400.0,\"potasioMg\":800.0},"
				+ "\"label\":\"Borrador IA\"}");
		return draft;
	}

	private static DefaultOidcUser principal() {
		final Jwt jwt = Jwt.withTokenValue("token").header("alg", "none").subject(NUTRITIONIST_ID).build();
		final OidcIdToken idToken = new OidcIdToken(jwt.getTokenValue(), jwt.getIssuedAt(), jwt.getExpiresAt(),
				Map.of("sub", NUTRITIONIST_ID));
		return new DefaultOidcUser(List.of(), idToken);
	}

	private static AiChatThread sampleThread() {
		final AiChatThread thread = new AiChatThread();
		thread.setId(10L);
		thread.setNutritionistId(NUTRITIONIST_ID);
		return thread;
	}

}
