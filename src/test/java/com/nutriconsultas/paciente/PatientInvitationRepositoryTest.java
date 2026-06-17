package com.nutriconsultas.paciente;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class PatientInvitationRepositoryTest {

	private static final String NUTRITIONIST_SUB = "auth0|nutritionist-invite-test";

	private static final String TOKEN_HASH = "a".repeat(64);

	@Autowired
	private PacienteRepository pacienteRepository;

	@Autowired
	private PatientInvitationRepository patientInvitationRepository;

	@Test
	void saveAndFindByTokenHash_roundTripsInvitation() {
		final Paciente paciente = pacienteRepository.save(invitedPaciente());
		final PatientInvitation invitation = sampleInvitation(paciente);
		patientInvitationRepository.saveAndFlush(invitation);

		assertThat(patientInvitationRepository.findByTokenHash(TOKEN_HASH)).isPresent();
	}

	@Test
	void findByPacienteId_returnsInvitationsForPatient() {
		final Paciente paciente = pacienteRepository.save(invitedPaciente());
		patientInvitationRepository.saveAndFlush(sampleInvitation(paciente));

		final List<PatientInvitation> found = patientInvitationRepository.findByPacienteId(paciente.getId());

		assertThat(found).hasSize(1);
		assertThat(found.getFirst().getTokenHash()).isEqualTo(TOKEN_HASH);
	}

	@Test
	void findByNutritionistUserIdAndStatus_filtersPending() {
		final Paciente paciente = pacienteRepository.save(invitedPaciente());
		patientInvitationRepository.saveAndFlush(sampleInvitation(paciente));

		final List<PatientInvitation> pending = patientInvitationRepository
			.findByNutritionistUserIdAndStatus(NUTRITIONIST_SUB, PatientInvitationStatus.PENDING);

		assertThat(pending).hasSize(1);
		assertThat(pending.getFirst().getPaciente().getId()).isEqualTo(paciente.getId());
	}

	private static Paciente invitedPaciente() {
		final Paciente paciente = new Paciente();
		paciente.setName("Invite Test Patient");
		paciente.setUserId(NUTRITIONIST_SUB);
		paciente.setStatus(PacienteStatus.INVITED);
		paciente.setAssignedId("PAT-INV-001");
		paciente.setEmailHint("patient@example.com");
		paciente.setDisplayName("Invite Test");
		final LocalDate dob = LocalDate.now().minusYears(30);
		paciente.setDob(Date.from(dob.atStartOfDay(ZoneId.systemDefault()).toInstant()));
		paciente.setGender("M");
		return paciente;
	}

	private static PatientInvitation sampleInvitation(final Paciente paciente) {
		final PatientInvitation invitation = new PatientInvitation();
		invitation.setPaciente(paciente);
		invitation.setNutritionistUserId(NUTRITIONIST_SUB);
		invitation.setTokenHash(TOKEN_HASH);
		invitation.setStatus(PatientInvitationStatus.PENDING);
		invitation.setExpiresAt(Instant.now().plus(14, ChronoUnit.DAYS));
		invitation.setMaxUses(1);
		return invitation;
	}

}
