package com.nutriconsultas.auth.apple;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AppleRelayEmailSupportTest {

	@Test
	void isApplePrivateRelayEmailDetectsRelayDomain() {
		assertThat(AppleRelayEmailSupport.isApplePrivateRelayEmail("relay@privaterelay.appleid.com")).isTrue();
		assertThat(AppleRelayEmailSupport.isApplePrivateRelayEmail("  Relay@PrivateRelay.Appleid.com ")).isTrue();
	}

	@Test
	void isApplePrivateRelayEmailRejectsNonRelayAddresses() {
		assertThat(AppleRelayEmailSupport.isApplePrivateRelayEmail("paciente@example.com")).isFalse();
		assertThat(AppleRelayEmailSupport.isApplePrivateRelayEmail(null)).isFalse();
		assertThat(AppleRelayEmailSupport.isApplePrivateRelayEmail("")).isFalse();
	}

}
