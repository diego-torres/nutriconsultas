package com.nutriconsultas.device;

import static org.assertj.core.api.Assertions.assertThat;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.ECPrivateKey;
import java.util.Base64;

import org.junit.jupiter.api.Test;

class ApnsPushClientTest {

	@Test
	void loadPrivateKey_parsesPkcs8Pem() throws Exception {
		final KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
		generator.initialize(256);
		final KeyPair keyPair = generator.generateKeyPair();
		final byte[] encoded = keyPair.getPrivate().getEncoded();
		final String pem = "-----BEGIN PRIVATE KEY-----\n"
				+ Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(encoded) + "\n-----END PRIVATE KEY-----";

		final ECPrivateKey loaded = ApnsPushClient.loadPrivateKey(pem);

		assertThat(loaded.getAlgorithm()).isEqualTo("EC");
	}

	@Test
	void loadPrivateKey_acceptsEscapedNewlines() throws Exception {
		final KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
		generator.initialize(256);
		final KeyPair keyPair = generator.generateKeyPair();
		final String body = Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded());
		final String escaped = "-----BEGIN PRIVATE KEY-----\\n" + body + "\\n-----END PRIVATE KEY-----";

		final ECPrivateKey loaded = ApnsPushClient.loadPrivateKey(escaped);

		assertThat(loaded).isNotNull();
	}

}
