package com.nutriconsultas.paciente.invitation;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.nutriconsultas.util.InvitationTokenHasher;

class PatientInvitationJwsTest {

	private static final String SECRET = "test-jws-secret-at-least-32-chars-long";

	@Test
	void signAndVerify_returnsPatientId() {
		final String tokenHash = InvitationTokenHasher.hashToken("sample-url-token");
		final Instant expiresAt = Instant.now().plus(7, ChronoUnit.DAYS);

		final String jws = PatientInvitationJws.sign(SECRET, 42L, tokenHash, expiresAt);

		assertThat(PatientInvitationJws.verify(SECRET, jws)).contains(42L);
	}

	@Test
	void verify_rejectsExpiredJws() {
		final String tokenHash = InvitationTokenHasher.hashToken("sample-url-token");
		final String jws = PatientInvitationJws.sign(SECRET, 1L, tokenHash, Instant.now().minus(1, ChronoUnit.SECONDS));

		assertThat(PatientInvitationJws.verify(SECRET, jws)).isEmpty();
	}

	@Test
	void verify_rejectsTamperedSignature() {
		final String tokenHash = InvitationTokenHasher.hashToken("sample-url-token");
		final String jws = PatientInvitationJws.sign(SECRET, 1L, tokenHash, Instant.now().plus(1, ChronoUnit.DAYS));
		final String tampered = jws.substring(0, jws.lastIndexOf('.') + 1) + "bad-signature";

		assertThat(PatientInvitationJws.verify(SECRET, tampered)).isEmpty();
	}

	@Test
	void verify_rejectsNonHs256Header() {
		final String tokenHash = InvitationTokenHasher.hashToken("sample-url-token");
		final String payload = Base64.getUrlEncoder()
			.withoutPadding()
			.encodeToString(("{\"patientId\":1,\"tokenHash\":\"" + tokenHash + "\",\"exp\":"
					+ Instant.now().plus(1, ChronoUnit.DAYS).getEpochSecond() + "}")
				.getBytes());
		final String noneAlgHeader = Base64.getUrlEncoder()
			.withoutPadding()
			.encodeToString("{\"alg\":\"none\",\"typ\":\"JWT\"}".getBytes());
		final String jws = noneAlgHeader + "." + payload + ".signature";

		final Optional<Long> result = PatientInvitationJws.verify(SECRET, jws);

		assertThat(result).isEmpty();
	}

}
