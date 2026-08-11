package com.nutriconsultas.mobile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.nutriconsultas.device.PatientDevice;
import com.nutriconsultas.device.PatientDevicePlatform;
import com.nutriconsultas.device.PatientDeviceRepository;
import com.nutriconsultas.mobile.dto.PatientDeviceDto;
import com.nutriconsultas.paciente.Paciente;
import com.nutriconsultas.paciente.PacienteRepository;

@ExtendWith(MockitoExtension.class)
class MobilePatientDeviceServiceTest {

	@InjectMocks
	private MobilePatientDeviceService service;

	@Mock
	private PatientDeviceRepository patientDeviceRepository;

	@Mock
	private PacienteRepository pacienteRepository;

	@Test
	void upsertDevice_createsNewDevice() {
		final Paciente paciente = paciente(5L);
		when(patientDeviceRepository.findByToken("token-new")).thenReturn(Optional.empty());
		when(pacienteRepository.getReferenceById(5L)).thenReturn(paciente);
		when(patientDeviceRepository.save(any(PatientDevice.class))).thenAnswer(invocation -> {
			final PatientDevice device = invocation.getArgument(0);
			device.setId(99L);
			return device;
		});

		final PatientDeviceDto dto = service.upsertDevice(5L, PatientDevicePlatform.ANDROID, "token-new", "2.0.0");

		assertThat(dto.id()).isEqualTo(99L);
		assertThat(dto.platform()).isEqualTo(PatientDevicePlatform.ANDROID);
		final ArgumentCaptor<PatientDevice> captor = ArgumentCaptor.forClass(PatientDevice.class);
		verify(patientDeviceRepository).save(captor.capture());
		assertThat(captor.getValue().getToken()).isEqualTo("token-new");
		assertThat(captor.getValue().getAppVersion()).isEqualTo("2.0.0");
		assertThat(captor.getValue().getPaciente().getId()).isEqualTo(5L);
		assertThat(captor.getValue().getLastSeenAt()).isNotNull();
	}

	@Test
	void upsertDevice_reassignsExistingTokenToNewPatient() {
		final Paciente previousOwner = paciente(1L);
		final Paciente newOwner = paciente(5L);
		final PatientDevice existing = new PatientDevice();
		existing.setId(40L);
		existing.setPaciente(previousOwner);
		existing.setPlatform(PatientDevicePlatform.IOS);
		existing.setToken("shared-token");
		existing.setCreatedAt(Instant.parse("2026-01-01T00:00:00Z"));
		existing.setUpdatedAt(Instant.parse("2026-01-01T00:00:00Z"));
		existing.setLastSeenAt(Instant.parse("2026-01-01T00:00:00Z"));

		when(patientDeviceRepository.findByToken("shared-token")).thenReturn(Optional.of(existing));
		when(pacienteRepository.getReferenceById(5L)).thenReturn(newOwner);
		when(patientDeviceRepository.save(any(PatientDevice.class)))
			.thenAnswer(invocation -> invocation.getArgument(0));

		final PatientDeviceDto dto = service.upsertDevice(5L, PatientDevicePlatform.IOS, "shared-token", "1.2.3");

		assertThat(dto.id()).isEqualTo(40L);
		assertThat(existing.getPaciente().getId()).isEqualTo(5L);
		assertThat(existing.getAppVersion()).isEqualTo("1.2.3");
	}

	@Test
	void deregisterDevice_deletesByPatientAndToken() {
		when(patientDeviceRepository.deleteByPacienteIdAndToken(5L, "token-x")).thenReturn(1L);

		service.deregisterDevice(5L, "token-x");

		verify(patientDeviceRepository).deleteByPacienteIdAndToken(5L, "token-x");
		verify(patientDeviceRepository, never()).delete(any());
	}

	private static Paciente paciente(final Long id) {
		final Paciente paciente = new Paciente();
		paciente.setId(id);
		return paciente;
	}

}
