package com.nutriconsultas.booking;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.nutriconsultas.paciente.Paciente;
import com.nutriconsultas.paciente.PacienteStatus;

class PublicBookingPatientFactoryTest {

	private static final String USER_ID = "auth0|nutritionist";

	@Test
	void buildProspectSetsRequiredFieldsAndOnboardingStatus() {
		final PublicBookingRequestDto request = new PublicBookingRequestDto();
		request.setPatientName("  Ana Pérez  ");
		request.setPatientEmail("ana@example.com");
		request.setPatientPhone(" 5551234 ");

		final Paciente paciente = PublicBookingPatientFactory.buildProspect(USER_ID, request);

		assertThat(paciente.getUserId()).isEqualTo(USER_ID);
		assertThat(paciente.getName()).isEqualTo("Ana Pérez");
		assertThat(paciente.getEmail()).isEqualTo("ana@example.com");
		assertThat(paciente.getPhone()).isEqualTo("5551234");
		assertThat(paciente.getStatus()).isEqualTo(PacienteStatus.ONBOARDING);
		assertThat(paciente.getGender()).isEqualTo(PublicBookingPatientFactory.PLACEHOLDER_GENDER);
		assertThat(paciente.getDob()).isNotNull();
	}

	@Test
	void buildProspectOmitsPhoneWhenBlank() {
		final PublicBookingRequestDto request = new PublicBookingRequestDto();
		request.setPatientName("Carlos");
		request.setPatientEmail("carlos@example.com");

		final Paciente paciente = PublicBookingPatientFactory.buildProspect(USER_ID, request);

		assertThat(paciente.getPhone()).isNull();
	}

}
