package com.nutriconsultas.paciente.invitation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PatientInvitationClientPlatformTest {

	@Test
	void fromUserAgent_detectsIos() {
		assertThat(
				PatientInvitationClientPlatform.fromUserAgent("Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X)"))
			.isEqualTo(PatientInvitationClientPlatform.IOS);
	}

	@Test
	void fromUserAgent_detectsAndroid() {
		assertThat(PatientInvitationClientPlatform.fromUserAgent("Mozilla/5.0 (Linux; Android 14; Pixel 8)"))
			.isEqualTo(PatientInvitationClientPlatform.ANDROID);
	}

	@Test
	void fromUserAgent_defaultsToOther() {
		assertThat(PatientInvitationClientPlatform.fromUserAgent("Mozilla/5.0 (Windows NT 10.0)"))
			.isEqualTo(PatientInvitationClientPlatform.OTHER);
		assertThat(PatientInvitationClientPlatform.fromUserAgent(null))
			.isEqualTo(PatientInvitationClientPlatform.OTHER);
	}

}
