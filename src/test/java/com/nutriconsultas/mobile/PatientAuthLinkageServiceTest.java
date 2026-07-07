package com.nutriconsultas.mobile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.nutriconsultas.auth0.Auth0UserLookup;
import com.nutriconsultas.paciente.Paciente;
import com.nutriconsultas.paciente.PacienteRepository;
import com.nutriconsultas.paciente.projection.PacienteAuthView;

@ExtendWith(MockitoExtension.class)
class PatientAuthLinkageServiceTest {

	private static final String NUTRITIONIST_SUB = "auth0|nutritionist-1";

	private static final String PATIENT_SUB = "auth0|patient-mobile-1";

	private static final String OTHER_SUB = "auth0|patient-mobile-other";

	@InjectMocks
	private PatientAuthLinkageService service;

	@Mock
	private PacienteRepository pacienteRepository;

	@Mock
	private Auth0UserLookup auth0UserLookup;

	private Paciente paciente;

	@BeforeEach
	void setUp() {
		paciente = samplePaciente(1L, "patient@example.com");
		lenient().when(pacienteRepository.findByIdAndUserId(1L, NUTRITIONIST_SUB)).thenReturn(Optional.of(paciente));
	}

	@Test
	void getStatus_whenUnlinked_reportsNotLinked() {
		when(auth0UserLookup.isConfigured()).thenReturn(true);

		final PatientMobileAuthStatus status = service.getStatus(1L, NUTRITIONIST_SUB);

		assertThat(status.isLinked()).isFalse();
		assertThat(status.isEmailLookupAvailable()).isTrue();
	}

	@Test
	void linkBySub_setsPatientAuthSub() {
		when(pacienteRepository.findAuthViewByPatientAuthSub(PATIENT_SUB)).thenReturn(Optional.empty());
		when(pacienteRepository.save(any(Paciente.class))).thenAnswer(inv -> inv.getArgument(0));

		final Paciente linked = service.linkBySub(1L, NUTRITIONIST_SUB, PATIENT_SUB);

		assertThat(linked.getPatientAuthSub()).isEqualTo(PATIENT_SUB);
		verify(pacienteRepository).save(paciente);
	}

	@Test
	void linkBySub_whenSubAlreadyUsedByOtherPatient_throws() {
		final Paciente other = samplePaciente(99L, "other@example.com");
		other.setPatientAuthSub(PATIENT_SUB);
		when(pacienteRepository.findAuthViewByPatientAuthSub(PATIENT_SUB)).thenReturn(Optional.of(authView(other)));

		assertThatThrownBy(() -> service.linkBySub(1L, NUTRITIONIST_SUB, PATIENT_SUB))
			.isInstanceOf(PatientAuthSubAlreadyLinkedException.class);
		verify(pacienteRepository, never()).save(any());
	}

	@Test
	void linkByEmail_usesAuth0Lookup() {
		when(auth0UserLookup.isConfigured()).thenReturn(true);
		when(auth0UserLookup.findUserIdByEmail("patient@example.com")).thenReturn(Optional.of(PATIENT_SUB));
		when(pacienteRepository.findAuthViewByPatientAuthSub(PATIENT_SUB)).thenReturn(Optional.empty());
		when(pacienteRepository.save(any(Paciente.class))).thenAnswer(inv -> inv.getArgument(0));

		final Paciente linked = service.linkByEmail(1L, NUTRITIONIST_SUB);

		assertThat(linked.getPatientAuthSub()).isEqualTo(PATIENT_SUB);
		verify(auth0UserLookup).findUserIdByEmail("patient@example.com");
	}

	@Test
	void linkByEmail_whenMgmtNotConfigured_throws() {
		when(auth0UserLookup.isConfigured()).thenReturn(false);

		assertThatThrownBy(() -> service.linkByEmail(1L, NUTRITIONIST_SUB))
			.isInstanceOf(Auth0ManagementNotConfiguredException.class);
	}

	@Test
	void linkByEmail_whenPatientHasNoEmail_throws() {
		paciente.setEmail(null);
		when(auth0UserLookup.isConfigured()).thenReturn(true);

		assertThatThrownBy(() -> service.linkByEmail(1L, NUTRITIONIST_SUB))
			.isInstanceOf(PatientEmailRequiredForLinkException.class);
	}

	@Test
	void linkByEmail_whenAuth0UserMissing_throws() {
		when(auth0UserLookup.isConfigured()).thenReturn(true);
		when(auth0UserLookup.findUserIdByEmail("patient@example.com")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.linkByEmail(1L, NUTRITIONIST_SUB))
			.isInstanceOf(PatientAuthUserNotFoundException.class);
	}

	@Test
	void unlink_clearsPatientAuthSub() {
		paciente.setPatientAuthSub(PATIENT_SUB);
		when(pacienteRepository.save(any(Paciente.class))).thenAnswer(inv -> inv.getArgument(0));

		final Paciente unlinked = service.unlink(1L, NUTRITIONIST_SUB);

		assertThat(unlinked.getPatientAuthSub()).isNull();
		verify(pacienteRepository).save(paciente);
	}

	@Test
	void getStatus_whenLinked_redactsSub() {
		paciente.setPatientAuthSub(PATIENT_SUB);
		when(auth0UserLookup.isConfigured()).thenReturn(false);

		final PatientMobileAuthStatus status = service.getStatus(1L, NUTRITIONIST_SUB);

		assertThat(status.isLinked()).isTrue();
		assertThat(status.getPatientAuthSubRedacted()).contains("…");
		assertThat(status.getPatientAuthSubRedacted()).doesNotContain(PATIENT_SUB);
	}

	private static PacienteAuthView authView(final Paciente entity) {
		return new PacienteAuthView() {
			@Override
			public Long getId() {
				return entity.getId();
			}

			@Override
			public String getPatientAuthSub() {
				return entity.getPatientAuthSub();
			}

			@Override
			public String getUserId() {
				return entity.getUserId();
			}

			@Override
			public com.nutriconsultas.paciente.PacienteStatus getStatus() {
				return entity.getStatus();
			}

			@Override
			public com.nutriconsultas.paciente.ApplePacienteLifecycleStatus getAppleLifecycleStatus() {
				return entity.getAppleLifecycleStatus();
			}
		};
	}

	private static Paciente samplePaciente(final Long id, final String email) {
		final Paciente entity = new Paciente();
		entity.setId(id);
		entity.setName("Test Patient");
		entity.setUserId(NUTRITIONIST_SUB);
		entity.setEmail(email);
		final LocalDate dob = LocalDate.now().minusYears(30);
		entity.setDob(Date.from(dob.atStartOfDay(ZoneId.systemDefault()).toInstant()));
		entity.setGender("F");
		return entity;
	}

}
