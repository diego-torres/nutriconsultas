package com.nutriconsultas.ai;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AiAuditRedactionTest {

	@Test
	void redactSecretsMasksOpenAiApiKey() {
		final String input = "Authorization failed for sk-proj-abc123secretkey456789";

		final String redacted = AiAuditRedaction.redactSecrets(input);

		assertThat(redacted).doesNotContain("sk-proj-abc123secretkey456789");
		assertThat(redacted).contains("sk-[REDACTED]");
		assertThat(AiAuditRedaction.containsOpenAiApiKey(redacted)).isFalse();
	}

	@Test
	void redactSecretsMasksBearerToken() {
		final String input = "Header Authorization: Bearer sk-live-secret-token-value";

		final String redacted = AiAuditRedaction.redactSecrets(input);

		assertThat(redacted).doesNotContain("sk-live-secret-token-value");
		assertThat(redacted).contains("[REDACTED_AUTH]");
		assertThat(AiAuditRedaction.containsBearerToken(redacted)).isFalse();
	}

	@Test
	void safeMessageLengthNeverExposesBody() {
		assertThat(AiAuditRedaction.safeMessageLength(null)).isZero();
		assertThat(AiAuditRedaction.safeMessageLength("Hola mundo")).isEqualTo(10);
	}

	@Test
	void redactSecretsLeavesSafeMetadataUntouched() {
		final String input = "threadId=5 tool=search_food_catalog resultCount=8";

		assertThat(AiAuditRedaction.redactSecrets(input)).isEqualTo(input);
	}

}
