package com.nutriconsultas.booking;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class ThymeleafPublicBookingConfirmationEmailSenderTest {

	@Autowired
	private PublicBookingConfirmationEmailSender confirmationEmailSender;

	@Test
	void sendConfirmationRendersTemplateWithoutError() {
		final PublicBookingConfirmationEmailDetails details = new PublicBookingConfirmationEmailDetails("Ana Pérez",
				"Dra. Ejemplo", "23/06/2026", "10:00");

		confirmationEmailSender.sendConfirmation("patient@example.com", details);

		assertThat(confirmationEmailSender).isInstanceOf(ThymeleafPublicBookingConfirmationEmailSender.class);
	}

}
