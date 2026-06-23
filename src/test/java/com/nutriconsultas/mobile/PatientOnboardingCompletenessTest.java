package com.nutriconsultas.mobile;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

import org.junit.jupiter.api.Test;

import com.nutriconsultas.paciente.Paciente;
import com.nutriconsultas.paciente.PacienteAvatarCatalog;
import com.nutriconsultas.paciente.PacienteStatus;

class PatientOnboardingCompletenessTest {

	@Test
	void isComplete_returnsFalseWhenAvatarMissing() {
		final Paciente paciente = samplePaciente(PacienteStatus.ONBOARDING);
		paciente.setAvatarId(null);

		assertThat(PatientOnboardingCompleteness.isComplete(paciente)).isFalse();
	}

	@Test
	void isComplete_returnsTrueWhenRequiredFieldsPresent() {
		final Paciente paciente = samplePaciente(PacienteStatus.ONBOARDING);
		paciente.setAvatarId(PacienteAvatarCatalog.DEFAULT_FEMALE_ID);

		assertThat(PatientOnboardingCompleteness.isComplete(paciente)).isTrue();
	}

	private static Paciente samplePaciente(final PacienteStatus status) {
		final Paciente paciente = new Paciente();
		paciente.setName("María López");
		paciente.setDisplayName("María");
		paciente.setGender("F");
		paciente.setStatus(status);
		paciente.setUserId("nutritionist-sub");
		final LocalDate dob = LocalDate.of(1990, 5, 15);
		paciente.setDob(Date.from(dob.atStartOfDay(ZoneId.systemDefault()).toInstant()));
		return paciente;
	}

}
