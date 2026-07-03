package com.nutriconsultas.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;

@ExtendWith(MockitoExtension.class)
class AiDraftRestControllerTest {

	private static final String NUTRITIONIST_ID = "auth0|nutritionist-a";

	@InjectMocks
	private AiDraftRestController controller;

	@Mock
	private AiDraftAcceptanceService draftAcceptanceService;

	@Mock
	private AiDraftLifecycleService draftLifecycleService;

	@Mock
	private AiDraftPreviewService draftPreviewService;

	@Test
	void getDraftPreviewReturnsStructuredBody() {
		final AiDraftPreviewView preview = new AiDraftPreviewView(10L, 5L, AiDraftType.DISH, AiDraftStatus.DRAFT,
				"Platillo (receta)", AiDraftSummaryExtractor.REVIEW_LABEL, "Tacos", "Resumen", 2, null,
				new NutrientSummary(250, 20.0, 8.0, 30.0, null, null, null), List.of(), List.of(), List.of("Paso 1"),
				List.of("Supuesto"), List.of("Advertencia"), null);
		when(draftPreviewService.getPreview(10L, NUTRITIONIST_ID)).thenReturn(preview);

		final ResponseEntity<Map<String, Object>> response = controller.getDraftPreview(10L, principal());

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).containsEntry("success", true)
			.containsEntry("draftId", 10L)
			.containsEntry("draftType", "DISH")
			.containsEntry("reviewLabel", AiDraftSummaryExtractor.REVIEW_LABEL)
			.containsEntry("title", "Tacos");
	}

	@Test
	void acceptDraftReturnsCreatedEntity() {
		when(draftAcceptanceService.accept(eq(10L), eq(NUTRITIONIST_ID), any()))
			.thenReturn(new AiDraftAcceptanceResult(10L, AiDraftType.DISH, AiDraftStatus.ACCEPTED,
					AiDraftCreatedEntityType.PLATILLO, 42L, "Platillo creado en catálogo (id=42)."));

		final ResponseEntity<Map<String, Object>> response = controller.acceptDraft(10L, principal());

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).containsEntry("success", true)
			.containsEntry("createdEntityType", "PLATILLO")
			.containsEntry("createdEntityId", 42L);
	}

	@Test
	void discardDraftReturnsDiscardedStatus() {
		final AiGeneratedDraft discarded = new AiGeneratedDraft();
		discarded.setId(11L);
		discarded.setDraftType(AiDraftType.MENU);
		discarded.setStatus(AiDraftStatus.DISCARDED);
		when(draftLifecycleService.discardDraft(11L, NUTRITIONIST_ID)).thenReturn(discarded);

		final ResponseEntity<Map<String, Object>> response = controller.discardDraft(11L, principal());

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).containsEntry("success", true).containsEntry("status", "DISCARDED");
		verify(draftLifecycleService).discardDraft(11L, NUTRITIONIST_ID);
	}

	private static DefaultOidcUser principal() {
		final Jwt jwt = Jwt.withTokenValue("token").header("alg", "none").subject(NUTRITIONIST_ID).build();
		final OidcIdToken idToken = new OidcIdToken(jwt.getTokenValue(), jwt.getIssuedAt(), jwt.getExpiresAt(),
				Map.of("sub", NUTRITIONIST_ID));
		return new DefaultOidcUser(List.of(), idToken);
	}

}
