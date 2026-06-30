package com.nutriconsultas.mobile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.nutriconsultas.mobile.dto.PatchPatientOnboardingProfileRequest;
import com.nutriconsultas.mobile.dto.PatientOnboardingProfileDto;
import com.nutriconsultas.paciente.Paciente;
import com.nutriconsultas.paciente.PacienteAvatarCatalog;
import com.nutriconsultas.paciente.PacienteDietaRepository;
import com.nutriconsultas.paciente.PacienteRepository;
import com.nutriconsultas.paciente.PacienteStatus;
import com.nutriconsultas.profile.NutritionistProfile;
import com.nutriconsultas.profile.NutritionistProfileRepository;

@ExtendWith(MockitoExtension.class)
class MobilePatientOnboardingServiceTest {

	@Mock
	private PacienteRepository pacienteRepository;

	@Mock
	private PacienteDietaRepository pacienteDietaRepository;

	@Mock
	private NutritionistProfileRepository nutritionistProfileRepository;

	@InjectMocks
	private MobilePatientOnboardingService service;

	@Test
	void updateProfile_transitionsToActiveWhenComplete() {
		final Paciente paciente = sampleOnboardingPaciente(9L);
		when(pacienteRepository.findById(9L)).thenReturn(Optional.of(paciente));
		when(pacienteRepository.save(any(Paciente.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(pacienteDietaRepository.findByPacienteIdAndStatus(9L,
				com.nutriconsultas.paciente.PacienteDietaStatus.ACTIVE))
			.thenReturn(List.of());

		final PatientOnboardingProfileDto updated = service.updateProfile(9L, new PatchPatientOnboardingProfileRequest(
				null, null, null, null, null, null, PacienteAvatarCatalog.DEFAULT_FEMALE_ID));

		final ArgumentCaptor<Paciente> captor = ArgumentCaptor.forClass(Paciente.class);
		verify(pacienteRepository).save(captor.capture());
		assertThat(captor.getValue().getStatus()).isEqualTo(PacienteStatus.ACTIVE);
		assertThat(updated.status()).isEqualTo(PacienteStatus.ACTIVE);
		assertThat(updated.profileComplete()).isTrue();
	}

	@Test
	void updateProfile_rejectsInvalidAvatar() {
		final Paciente paciente = sampleOnboardingPaciente(9L);
		when(pacienteRepository.findById(9L)).thenReturn(Optional.of(paciente));

		assertThatThrownBy(() -> service.updateProfile(9L,
				new PatchPatientOnboardingProfileRequest(null, null, null, null, null, null, "not-an-avatar")))
			.isInstanceOf(PatientOnboardingInvalidAvatarException.class);
	}

	@Test
	void getProfile_rejectsInvitedStatus() {
		final Paciente paciente = sampleOnboardingPaciente(9L);
		paciente.setStatus(PacienteStatus.INVITED);
		when(pacienteRepository.findById(9L)).thenReturn(Optional.of(paciente));

		assertThatThrownBy(() -> service.getProfile(9L)).isInstanceOf(PatientOnboardingRequiredException.class);
	}

	@Test
	void getProfile_includesNutritionistDisplayName() {
		final Paciente paciente = sampleOnboardingPaciente(9L);
		when(pacienteRepository.findById(9L)).thenReturn(Optional.of(paciente));
		when(pacienteDietaRepository.findByPacienteIdAndStatus(9L,
				com.nutriconsultas.paciente.PacienteDietaStatus.ACTIVE))
			.thenReturn(List.of());
		final NutritionistProfile profile = new NutritionistProfile();
		profile.setUserId("nutritionist-sub");
		profile.setDisplayName("Lic. Ana López");
		when(nutritionistProfileRepository.findByUserId("nutritionist-sub")).thenReturn(Optional.of(profile));

		final PatientOnboardingProfileDto result = service.getProfile(9L);

		assertThat(result.nutritionistDisplayName()).isEqualTo("Lic. Ana López");
	}

	private static Paciente sampleOnboardingPaciente(final Long id) {
		final Paciente paciente = new Paciente();
		paciente.setId(id);
		paciente.setName("María López");
		paciente.setDisplayName("María");
		paciente.setGender("F");
		paciente.setStatus(PacienteStatus.ONBOARDING);
		paciente.setUserId("nutritionist-sub");
		final LocalDate dob = LocalDate.of(1990, 5, 15);
		paciente.setDob(Date.from(dob.atStartOfDay(ZoneId.systemDefault()).toInstant()));
		return paciente;
	}

}
