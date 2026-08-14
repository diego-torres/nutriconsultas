package com.nutriconsultas.message;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

/**
 * Nutritionists can pick Unicode emoticons from the patient chat composer.
 */
class PatientChatEmojiMarkupTest {

	@Test
	void composerExposesEmojiPickerControls() throws IOException {
		final String html = load("templates/sbadmin/fragments/patient-messages-widget.html");
		assertThat(html).contains("id=\"patientChatEmojiBtn\"");
		assertThat(html).contains("aria-label=\"Emoticones\"");
		assertThat(html).contains("aria-controls=\"patientChatEmojiPicker\"");
		assertThat(html).contains("id=\"patientChatEmojiPicker\"");
		assertThat(html).contains("aria-label=\"Elegir emoticon\"");
		assertThat(html).contains("fa-smile");
	}

	@Test
	void widgetScriptDefinesNutritionEmojiCatalog() throws IOException {
		final String js = load("static/sbadmin/js/patient-messages-widget.js");
		assertThat(js).contains("EMOJI_GROUPS");
		assertThat(js).contains("Reacciones");
		assertThat(js).contains("Alimentos");
		assertThat(js).contains("Hábitos");
		assertThat(js).contains("insertEmoji");
		assertThat(js).contains("🥗");
		assertThat(js).contains("💪");
		assertThat(js).contains("😊");
	}

	@Test
	void widgetStylesPositionPickerAboveComposer() throws IOException {
		final String css = load("static/sbadmin/css/patient-messages-widget.css");
		assertThat(css).contains("#patientChatEmojiPicker");
		assertThat(css).contains("#patientChatEmojiBtn");
		assertThat(css).contains(".patient-chat-emoji-item");
	}

	private static String load(final String path) throws IOException {
		return new ClassPathResource(path).getContentAsString(StandardCharsets.UTF_8);
	}

}
