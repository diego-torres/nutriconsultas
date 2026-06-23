package com.nutriconsultas.paciente.invitation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.nutriconsultas.mobile.PatientInvitationRevokeNotAllowedException;
import com.nutriconsultas.mobile.PatientInvitationRevokeNotFoundException;
import com.nutriconsultas.paciente.Paciente;
import com.nutriconsultas.paciente.PatientInvitation;
import com.nutriconsultas.paciente.PatientInvitationRepository;
import com.nutriconsultas.paciente.PatientInvitationStatus;

@ExtendWith(MockitoExtension.class)
class PatientInvitationRevokeServiceTest {

	private static final String NUTRITIONIST_SUB = "auth0|nutritionist-owner";

	private static final String OTHER_NUTRITIONIST_SUB = "auth0|other-nutritionist";

	@Mock
	private PatientInvitationRepository patientInvitationRepository;

	@InjectMocks
	private PatientInvitationRevokeServiceImpl service;

	@Test
	void revoke_pendingInvitation_setsRevokedStatus() {
		final PatientInvitation invitation = sampleInvitation(5L, 9L, PatientInvitationStatus.PENDING);
		when(patientInvitationRepository.findByIdAndNutritionistUserId(5L, NUTRITIONIST_SUB))
			.thenReturn(Optional.of(invitation));
		when(patientInvitationRepository.save(any(PatientInvitation.class)))
			.thenAnswer(invocation -> invocation.getArgument(0));

		final PatientInvitationRevokeResult result = service.revoke(5L, NUTRITIONIST_SUB);

		final ArgumentCaptor<PatientInvitation> captor = ArgumentCaptor.forClass(PatientInvitation.class);
		verify(patientInvitationRepository).save(captor.capture());
		assertThat(captor.getValue().getStatus()).isEqualTo(PatientInvitationStatus.REVOKED);
		assertThat(result.invitationId()).isEqualTo(5L);
		assertThat(result.pacienteId()).isEqualTo(9L);
		assertThat(result.status()).isEqualTo(PatientInvitationStatus.REVOKED);
	}

	@Test
	void revoke_alreadyRevoked_isIdempotent() {
		final PatientInvitation invitation = sampleInvitation(5L, 9L, PatientInvitationStatus.REVOKED);
		when(patientInvitationRepository.findByIdAndNutritionistUserId(5L, NUTRITIONIST_SUB))
			.thenReturn(Optional.of(invitation));

		final PatientInvitationRevokeResult result = service.revoke(5L, NUTRITIONIST_SUB);

		assertThat(result.status()).isEqualTo(PatientInvitationStatus.REVOKED);
	}

	@Test
	void revoke_wrongNutritionist_throwsNotFound() {
		when(patientInvitationRepository.findByIdAndNutritionistUserId(5L, OTHER_NUTRITIONIST_SUB))
			.thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.revoke(5L, OTHER_NUTRITIONIST_SUB))
			.isInstanceOf(PatientInvitationRevokeNotFoundException.class);
	}

	@Test
	void revoke_redeemedInvitation_throwsNotAllowed() {
		final PatientInvitation invitation = sampleInvitation(5L, 9L, PatientInvitationStatus.REDEEMED);
		when(patientInvitationRepository.findByIdAndNutritionistUserId(5L, NUTRITIONIST_SUB))
			.thenReturn(Optional.of(invitation));

		assertThatThrownBy(() -> service.revoke(5L, NUTRITIONIST_SUB))
			.isInstanceOf(PatientInvitationRevokeNotAllowedException.class);
	}

	private static PatientInvitation sampleInvitation(final Long id, final Long pacienteId,
			final PatientInvitationStatus status) {
		final Paciente paciente = new Paciente();
		paciente.setId(pacienteId);
		final PatientInvitation invitation = new PatientInvitation();
		invitation.setId(id);
		invitation.setPaciente(paciente);
		invitation.setNutritionistUserId(NUTRITIONIST_SUB);
		invitation.setStatus(status);
		invitation.setTokenHash("a".repeat(64));
		invitation.setExpiresAt(Instant.now().plus(14, ChronoUnit.DAYS));
		return invitation;
	}

}
