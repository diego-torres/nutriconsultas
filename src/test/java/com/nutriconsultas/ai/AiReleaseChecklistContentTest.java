package com.nutriconsultas.ai;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * Ensures the production release checklist documents required gates (#408).
 */
class AiReleaseChecklistContentTest {

	private static final Path CHECKLIST = Path.of("docs/ai/RELEASE-CHECKLIST.md");

	@Test
	void releaseChecklistIncludesRequiredSections() throws IOException {
		final String markdown = Files.readString(CHECKLIST, StandardCharsets.UTF_8);

		assertThat(markdown).contains("AI disabled by default");
		assertThat(markdown).contains("API key in staging/production secret store");
		assertThat(markdown).contains("Rate limiting enabled");
		assertThat(markdown).contains("Audit logging enabled");
		assertThat(markdown).contains("Draft approval flow tested");
		assertThat(markdown).contains("Security tests passing");
		assertThat(markdown).contains("Liquibase migration tested");
		assertThat(markdown).contains("Rollback plan documented");
		assertThat(markdown).contains("Nutritionist guidance published");
	}

	@Test
	void releaseChecklistDocumentsHardBlockers() throws IOException {
		final String markdown = Files.readString(CHECKLIST, StandardCharsets.UTF_8);

		assertThat(markdown).contains("#409");
		assertThat(markdown).contains("#438");
		assertThat(markdown).contains("AI_ENABLED=false");
	}

}
