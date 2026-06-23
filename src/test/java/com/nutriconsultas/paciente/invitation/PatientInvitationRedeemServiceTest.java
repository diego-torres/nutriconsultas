package com.nutriconsultas.paciente.invitation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.nutriconsultas.mobile.PatientInvitationInvalidTokenException;
import com.nutriconsultas.mobile.PatientInvitationPatientStatusException;
import com.nutriconsultas.mobile.PatientInvitationRedeemConflictException;
import com.nutriconsultas.mobile.PatientInvitationUnavailableException;
import com.nutriconsultas.paciente.Paciente;
import com.nutriconsultas.paciente.PacienteRepository;
import com.nutriconsultas.paciente.PacienteStatus;
import com.nutriconsultas.paciente.PatientInvitation;
import com.nutriconsultas.paciente.PatientInvitationRepository;
import com.nutriconsultas.paciente.PatientInvitationStatus;

@ExtendWith(MockitoExtension.class)
class PatientInvitationRedeemServiceTest {

	private static final String PATIENT_SUB = "auth0|patient-redeem-test";

	private static final String OTHER_SUB = "auth0|other-patient-sub";

	@Mock
	private PatientInvitationRepository patientInvitationRepository;

	@Mock
	private PacienteRepository pacienteRepository;

	private PatientInvitationTokenService tokenService;

	private PatientInvitationRedeemService service;

	@BeforeEach
	void setUp() {
		tokenService = new PatientInvitationTokenServiceImpl(new PatientInvitationProperties());
		service = new PatientInvitationRedeemServiceImpl(patientInvitationRepository, pacienteRepository);
	}

	@Test
	void redeem_withValidPendingInvitation_bindsSubAndTransitionsStatus() {
		final PatientInvitationTokenBundle bundle = tokenService.generate();
		final Paciente paciente = invitedPaciente(100L);
		final PatientInvitation invitation = pendingInvitation(bundle.tokenHash(), paciente);
		when(patientInvitationRepository.findByTokenHash(bundle.tokenHash())).thenReturn(Optional.of(invitation));
		when(pacienteRepository.findByPatientAuthSub(PATIENT_SUB)).thenReturn(Optional.empty());
		when(pacienteRepository.findById(100L)).thenReturn(Optional.of(paciente));

		final PatientInvitationRedeemResult result = service.redeem(bundle.urlToken(), PATIENT_SUB);

		assertThat(result.pacienteId()).isEqualTo(100L);
		assertThat(result.pacienteStatus()).isEqualTo(PacienteStatus.ONBOARDING);
		assertThat(result.invitationId()).isEqualTo(1L);
		assertThat(result.redeemedAt()).isNotNull();
		assertThat(paciente.getPatientAuthSub()).isEqualTo(PATIENT_SUB);
		assertThat(paciente.getStatus()).isEqualTo(PacienteStatus.ONBOARDING);
		assertThat(invitation.getStatus()).isEqualTo(PatientInvitationStatus.REDEEMED);
		assertThat(invitation.getRedeemedBySub()).isEqualTo(PATIENT_SUB);
		verify(pacienteRepository).save(paciente);
		verify(patientInvitationRepository).save(invitation);
	}

	@Test
	void redeem_withSameSubAfterRedeem_isIdempotent() {
		final PatientInvitationTokenBundle bundle = tokenService.generate();
		final Paciente paciente = invitedPaciente(100L);
		paciente.setStatus(PacienteStatus.ONBOARDING);
		paciente.setPatientAuthSub(PATIENT_SUB);
		final PatientInvitation invitation = pendingInvitation(bundle.tokenHash(), paciente);
		invitation.setStatus(PatientInvitationStatus.REDEEMED);
		invitation.setRedeemedBySub(PATIENT_SUB);
		invitation.setRedeemedAt(Instant.parse("2026-06-01T12:00:00Z"));
		when(patientInvitationRepository.findByTokenHash(bundle.tokenHash())).thenReturn(Optional.of(invitation));

		final PatientInvitationRedeemResult result = service.redeem(bundle.urlToken(), PATIENT_SUB);

		assertThat(result.pacienteId()).isEqualTo(100L);
		assertThat(result.redeemedAt()).isEqualTo(Instant.parse("2026-06-01T12:00:00Z"));
	}

	@Test
	void redeem_withDifferentSubAfterRedeem_throwsConflict() {
		final PatientInvitationTokenBundle bundle = tokenService.generate();
		final PatientInvitation invitation = pendingInvitation(bundle.tokenHash(), invitedPaciente(100L));
		invitation.setStatus(PatientInvitationStatus.REDEEMED);
		invitation.setRedeemedBySub(PATIENT_SUB);
		when(patientInvitationRepository.findByTokenHash(bundle.tokenHash())).thenReturn(Optional.of(invitation));

		assertThatThrownBy(() -> service.redeem(bundle.urlToken(), OTHER_SUB))
			.isInstanceOf(PatientInvitationRedeemConflictException.class);
	}

	@Test
	void redeem_withSubLinkedToDifferentPatient_throwsConflict() {
		final PatientInvitationTokenBundle bundle = tokenService.generate();
		final Paciente paciente = invitedPaciente(100L);
		final PatientInvitation invitation = pendingInvitation(bundle.tokenHash(), paciente);
		final Paciente otherPatient = invitedPaciente(200L);
		otherPatient.setPatientAuthSub(PATIENT_SUB);
		when(patientInvitationRepository.findByTokenHash(bundle.tokenHash())).thenReturn(Optional.of(invitation));
		when(pacienteRepository.findByPatientAuthSub(PATIENT_SUB)).thenReturn(Optional.of(otherPatient));

		assertThatThrownBy(() -> service.redeem(bundle.urlToken(), PATIENT_SUB))
			.isInstanceOf(PatientInvitationRedeemConflictException.class);
	}

	@Test
	void redeem_withNonInvitedPatient_throwsPatientStatus() {
		final PatientInvitationTokenBundle bundle = tokenService.generate();
		final Paciente paciente = invitedPaciente(100L);
		paciente.setStatus(PacienteStatus.ACTIVE);
		final PatientInvitation invitation = pendingInvitation(bundle.tokenHash(), paciente);
		when(patientInvitationRepository.findByTokenHash(bundle.tokenHash())).thenReturn(Optional.of(invitation));
		when(pacienteRepository.findByPatientAuthSub(PATIENT_SUB)).thenReturn(Optional.empty());
		when(pacienteRepository.findById(100L)).thenReturn(Optional.of(paciente));

		assertThatThrownBy(() -> service.redeem(bundle.urlToken(), PATIENT_SUB))
			.isInstanceOf(PatientInvitationPatientStatusException.class);
	}

	@Test
	void redeem_withExpiredInvitation_throwsUnavailable() {
		final PatientInvitationTokenBundle bundle = tokenService.generate();
		final PatientInvitation invitation = pendingInvitation(bundle.tokenHash(), invitedPaciente(100L));
		invitation.setExpiresAt(Instant.now().minus(1, ChronoUnit.HOURS));
		when(patientInvitationRepository.findByTokenHash(bundle.tokenHash())).thenReturn(Optional.of(invitation));

		assertThatThrownBy(() -> service.redeem(bundle.urlToken(), PATIENT_SUB))
			.isInstanceOf(PatientInvitationUnavailableException.class);
	}

	@Test
	void redeem_withRevokedInvitation_throwsUnavailable() {
		final PatientInvitationTokenBundle bundle = tokenService.generate();
		final PatientInvitation invitation = pendingInvitation(bundle.tokenHash(), invitedPaciente(100L));
		invitation.setStatus(PatientInvitationStatus.REVOKED);
		when(patientInvitationRepository.findByTokenHash(bundle.tokenHash())).thenReturn(Optional.of(invitation));

		assertThatThrownBy(() -> service.redeem(bundle.urlToken(), PATIENT_SUB))
			.isInstanceOf(PatientInvitationUnavailableException.class);
	}

	@Test
	void redeem_withMalformedToken_throwsInvalid() {
		assertThatThrownBy(() -> service.redeem("bad-token", PATIENT_SUB))
			.isInstanceOf(PatientInvitationInvalidTokenException.class);
	}

	@Test
	void redeem_persistsPatientAuthSubWithoutLoggingIt() {
		final PatientInvitationTokenBundle bundle = tokenService.generate();
		final Paciente paciente = invitedPaciente(100L);
		final PatientInvitation invitation = pendingInvitation(bundle.tokenHash(), paciente);
		when(patientInvitationRepository.findByTokenHash(bundle.tokenHash())).thenReturn(Optional.of(invitation));
		when(pacienteRepository.findByPatientAuthSub(PATIENT_SUB)).thenReturn(Optional.empty());
		when(pacienteRepository.findById(100L)).thenReturn(Optional.of(paciente));

		service.redeem(bundle.urlToken(), PATIENT_SUB);

		final ArgumentCaptor<Paciente> captor = ArgumentCaptor.forClass(Paciente.class);
		verify(pacienteRepository).save(captor.capture());
		assertThat(captor.getValue().getPatientAuthSub()).isEqualTo(PATIENT_SUB);
	}

	private static Paciente invitedPaciente(final Long id) {
		final Paciente paciente = new Paciente();
		paciente.setId(id);
		paciente.setStatus(PacienteStatus.INVITED);
		return paciente;
	}

	private static PatientInvitation pendingInvitation(final String tokenHash, final Paciente paciente) {
		final PatientInvitation invitation = new PatientInvitation();
		invitation.setId(1L);
		invitation.setTokenHash(tokenHash);
		invitation.setPaciente(paciente);
		invitation.setStatus(PatientInvitationStatus.PENDING);
		invitation.setExpiresAt(Instant.now().plus(7, ChronoUnit.DAYS));
		return invitation;
	}

}
