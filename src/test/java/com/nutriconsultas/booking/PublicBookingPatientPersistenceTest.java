package com.nutriconsultas.booking;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import com.nutriconsultas.paciente.Paciente;
import com.nutriconsultas.paciente.PacienteRepository;
import com.nutriconsultas.paciente.PacienteStatus;

@DataJpaTest
@ActiveProfiles("test")
class PublicBookingPatientPersistenceTest {

	private static final String USER_ID = "auth0|public-booking-persist";

	@Autowired
	private PacienteRepository pacienteRepository;

	@Test
	void prospectPatientPersistsWithPlaceholderDemographics() {
		final PublicBookingRequestDto request = new PublicBookingRequestDto();
		request.setPatientName("Prospect Patient");
		request.setPatientEmail("prospect@example.com");

		final Paciente saved = pacienteRepository.save(PublicBookingPatientFactory.buildProspect(USER_ID, request));

		assertThat(saved.getId()).isNotNull();
		final Paciente reloaded = pacienteRepository.findById(saved.getId()).orElseThrow();
		assertThat(reloaded.getStatus()).isEqualTo(PacienteStatus.ONBOARDING);
		assertThat(reloaded.getGender()).isEqualTo(PublicBookingPatientFactory.PLACEHOLDER_GENDER);
		assertThat(reloaded.getDob()).isNotNull();
	}

}
