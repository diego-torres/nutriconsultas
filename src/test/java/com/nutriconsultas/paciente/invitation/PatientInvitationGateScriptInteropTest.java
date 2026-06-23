package com.nutriconsultas.paciente.invitation;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import com.nutriconsultas.util.InvitationTokenHasher;

/**
 * Ensures the Auth0 Post-Login Action script (#140) verifies JWS tokens signed by
 * {@link PatientInvitationJws} (Java #133).
 */
class PatientInvitationGateScriptInteropTest {

	private static final String SECRET = "test-jws-secret-at-least-32-chars-long";

	static boolean isNodeAvailable() {
		try {
			final Process process = new ProcessBuilder("node", "--version").start();
			return process.waitFor(5, TimeUnit.SECONDS) && process.exitValue() == 0;
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			return false;
		}
		catch (Exception ex) {
			return false;
		}
	}

	@Test
	@EnabledIf("isNodeAvailable")
	void auth0ActionScript_verifiesJavaSignedJws() throws Exception {
		final String tokenHash = InvitationTokenHasher.hashToken("sample-url-token");
		final Instant expiresAt = Instant.now().plus(7, ChronoUnit.DAYS);
		final String validJws = PatientInvitationJws.sign(SECRET, 42L, tokenHash, expiresAt);
		final String expiredJws = PatientInvitationJws.sign(SECRET, 1L, tokenHash,
				Instant.now().minus(1, ChronoUnit.SECONDS));
		final String tamperedJws = validJws.substring(0, validJws.lastIndexOf('.') + 1) + "bad-signature";

		final Path script = Path.of("scripts/test-patient-invitation-gate.cjs").toAbsolutePath();

		assertThat(verifyViaNode(script, SECRET, validJws)).isEqualTo("42");
		assertThat(verifyViaNode(script, SECRET, expiredJws)).isEqualTo("null");
		assertThat(verifyViaNode(script, SECRET, tamperedJws)).isEqualTo("null");
	}

	private static String verifyViaNode(final Path script, final String secret, final String compactJws)
			throws Exception {
		final Process process = new ProcessBuilder("node", script.toString(), "verify", secret, compactJws)
			.directory(Path.of("").toAbsolutePath().toFile())
			.redirectErrorStream(true)
			.start();
		final boolean finished = process.waitFor(30, TimeUnit.SECONDS);
		assertThat(finished).as("node interop script timed out").isTrue();
		assertThat(process.exitValue()).as("node interop script failed").isZero();
		return new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
	}

}
