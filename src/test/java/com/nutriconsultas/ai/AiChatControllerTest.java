package com.nutriconsultas.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class AiChatControllerTest {

	@InjectMocks
	private AiChatController controller;

	@Mock
	private AiProperties aiProperties;

	@Test
	void chatHomeReturnsViewWhenEnabled() {
		when(aiProperties.isEnabled()).thenReturn(true);
		final ExtendedModelMap model = new ExtendedModelMap();

		final String view = controller.chatHome(null, model);

		assertThat(view).isEqualTo("sbadmin/ai/chat");
		assertThat(model.getAttribute("activeMenu")).isEqualTo("ai");
		assertThat(model.getAttribute("initialThreadId")).isNull();
		assertThat(model.asMap()).doesNotContainKeys("openaiApiKey", "openaiModel", "apiKey");
	}

	@Test
	void chatHomePassesThreadIdToModel() {
		when(aiProperties.isEnabled()).thenReturn(true);
		final ExtendedModelMap model = new ExtendedModelMap();

		final String view = controller.chatHome(12L, model);

		assertThat(view).isEqualTo("sbadmin/ai/chat");
		assertThat(model.getAttribute("initialThreadId")).isEqualTo(12L);
	}

	@Test
	void chatHomeReturnsNotFoundWhenDisabled() {
		when(aiProperties.isEnabled()).thenReturn(false);

		assertThatThrownBy(() -> controller.chatHome(null, new ExtendedModelMap()))
			.isInstanceOf(ResponseStatusException.class)
			.extracting(ex -> ((ResponseStatusException) ex).getStatusCode().value())
			.isEqualTo(404);
	}

}
