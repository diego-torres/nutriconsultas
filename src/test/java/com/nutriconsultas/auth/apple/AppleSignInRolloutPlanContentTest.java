package com.nutriconsultas.auth.apple;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * Ensures the Apple Sign-In production rollout plan documents required phases (#511).
 */
class AppleSignInRolloutPlanContentTest {

	private static final Path ROLLOUT_PLAN = Path.of("docs/auth/apple-signin-rollout.md");

	@Test
	void rolloutPlanIncludesRequiredPhases() throws IOException {
		final String markdown = Files.readString(ROLLOUT_PLAN, StandardCharsets.UTF_8);

		assertThat(markdown).contains("Phase 1 — Observe only");
		assertThat(markdown).contains("Phase 2 — Metadata updates");
		assertThat(markdown).contains("Phase 3 — Restricted automation");
		assertThat(markdown).contains("Phase 4 — Optional hard deletion");
	}

	@Test
	void rolloutPlanDocumentsSafetyGates() throws IOException {
		final String markdown = Files.readString(ROLLOUT_PLAN, StandardCharsets.UTF_8);

		assertThat(markdown).contains("APPLE_SIGNIN_WEBHOOK_ENABLED");
		assertThat(markdown).contains("APPLE_SIGNIN_AUTO_PROCESS_DESTRUCTIVE_EVENTS=false");
		assertThat(markdown).contains("AUTH0_MGMT_USER_DELETE_ENABLED");
		assertThat(markdown).contains("ssm-update-apple-signin.sh");
		assertThat(markdown).contains("#511");
	}

}
