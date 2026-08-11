package com.nutriconsultas.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LogRedactionDeviceTokenTest {

	@Test
	void redactDeviceToken_masksAllButPrefix() {
		assertThat(LogRedaction.redactDeviceToken("abcdef1234567890")).isEqualTo("deviceToken[abcdef...REDACTED]");
	}

	@Test
	void redactDeviceToken_handlesNullAndShortValues() {
		assertThat(LogRedaction.redactDeviceToken(null)).isEqualTo("deviceToken[null]");
		assertThat(LogRedaction.redactDeviceToken("")).isEqualTo("deviceToken[null]");
		assertThat(LogRedaction.redactDeviceToken("abc")).isEqualTo("deviceToken[abc...REDACTED]");
	}

}
