package com.nutriconsultas.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LogRedactionSupportTicketTest {

	@Test
	void redactSupportTicket_includesIdOnly() {
		assertThat(LogRedaction.redactSupportTicket(42L)).isEqualTo("SupportTicket[id=42]");
	}

	@Test
	void redactSupportTicket_handlesNull() {
		assertThat(LogRedaction.redactSupportTicket(null)).isEqualTo("SupportTicket[id=null]");
	}

}
