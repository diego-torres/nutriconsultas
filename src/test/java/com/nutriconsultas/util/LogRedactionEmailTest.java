package com.nutriconsultas.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LogRedactionEmailTest {

	@Test
	void redactEmailShowsFirstCharacterAndDomainOnly() {
		assertThat(LogRedaction.redactEmail("invitee@example.com")).isEqualTo("email[i***@example.com]");
	}

	@Test
	void redactEmailHandlesNullAndBlank() {
		assertThat(LogRedaction.redactEmail(null)).isEqualTo("email[null]");
		assertThat(LogRedaction.redactEmail("   ")).isEqualTo("email[null]");
	}

}
