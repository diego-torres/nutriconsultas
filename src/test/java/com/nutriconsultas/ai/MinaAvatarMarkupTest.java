package com.nutriconsultas.ai;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

/**
 * Ensures Mina uses the illustrated avatar instead of the Font Awesome robot icon.
 */
class MinaAvatarMarkupTest {

	private static final String AVATAR_PATH = "/sbadmin/img/mina-avatar.png";

	private static final String ROBOT_ICON = "fa-robot";

	@Test
	void avatarImageAndStylesheetExist() {
		assertThat(new ClassPathResource("static/sbadmin/img/mina-avatar.png").exists()).isTrue();
		assertThat(new ClassPathResource("static/sbadmin/css/mina-avatar.css").exists()).isTrue();
	}

	@Test
	void minaSurfacesUseAvatarInsteadOfRobotIcon() throws IOException {
		for (final String template : List.of("templates/sbadmin/fragments/ai-assistant-widget.html",
				"templates/sbadmin/sidebar.html", "templates/sbadmin/ai/chat.html", "templates/sbadmin/index.html")) {
			final String html = loadTemplate(template);
			assertThat(html).as(template).contains(AVATAR_PATH);
			assertThat(html).as(template).doesNotContain(ROBOT_ICON);
		}
	}

	private static String loadTemplate(final String path) throws IOException {
		return new ClassPathResource(path).getContentAsString(StandardCharsets.UTF_8);
	}

}
