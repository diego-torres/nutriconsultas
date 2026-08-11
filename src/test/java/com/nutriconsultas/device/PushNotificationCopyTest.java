package com.nutriconsultas.device;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PushNotificationCopyTest {

	@Test
	void copy_isGenericSpanishWithoutMessageBodyPlaceholder() {
		assertThat(PushNotificationCopy.NEW_MESSAGE_TITLE).isEqualTo("Nuevo mensaje");
		assertThat(PushNotificationCopy.NEW_MESSAGE_BODY).isEqualTo("Nuevo mensaje de tu nutriólogo");
		assertThat(PushNotificationCopy.NEW_MESSAGE_BODY).doesNotContain("%s");
		assertThat(PushNotificationCopy.NEW_MESSAGE_BODY).doesNotContain("{");
	}

}
