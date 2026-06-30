package com.nutriconsultas.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class EmailMaskingTest {

	@Test
	void maskForHint_withValidEmail_returnsMaskedLocalPart() {
		assertThat(EmailMasking.maskForHint("Patient@Example.com")).isEqualTo("p***@example.com");
	}

	@Test
	void maskForHint_withBlankEmail_returnsNull() {
		assertThat(EmailMasking.maskForHint(null)).isNull();
		assertThat(EmailMasking.maskForHint("  ")).isNull();
	}

	@Test
	void maskForHint_withMalformedEmail_returnsNull() {
		assertThat(EmailMasking.maskForHint("not-an-email")).isNull();
		assertThat(EmailMasking.maskForHint("@example.com")).isNull();
	}

}
