package com.nutriconsultas.device;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.nutriconsultas.paciente.Paciente;

@ExtendWith(MockitoExtension.class)
class PatientPushSenderImplTest {

	@Mock
	private PatientDeviceRepository patientDeviceRepository;

	@Mock
	private ApnsPushClient apnsPushClient;

	@Mock
	private FcmHttpV1Client fcmHttpV1Client;

	private PatientPushProperties properties;

	private PatientPushSenderImpl sender;

	@BeforeEach
	void setUp() {
		properties = new PatientPushProperties();
		properties.setEnabled(true);
		properties.getApns().setKeyId("KEYID");
		properties.getApns().setTeamId("TEAMID");
		properties.getApns().setBundleId("com.example.app");
		properties.getApns().setP8Key("pem");
		sender = new PatientPushSenderImpl(properties, patientDeviceRepository, apnsPushClient, fcmHttpV1Client);
	}

	@Test
	void send_whenDisabled_isNoOp() {
		properties.setEnabled(false);

		sender.send(1L, PushEvent.newMessage(99L));

		verifyNoInteractions(patientDeviceRepository, apnsPushClient, fcmHttpV1Client);
	}

	@Test
	void send_whenNoDevices_isNoOp() {
		when(patientDeviceRepository.findByPacienteId(1L)).thenReturn(List.of());

		sender.send(1L, PushEvent.newMessage(99L));

		verify(patientDeviceRepository).findByPacienteId(1L);
		verifyNoInteractions(apnsPushClient, fcmHttpV1Client);
	}

	@Test
	void send_fansOutByPlatformAndDeletesInvalidTokens() {
		final PatientDevice ios = device(10L, PatientDevicePlatform.IOS, "ios-token");
		final PatientDevice android = device(11L, PatientDevicePlatform.ANDROID, "android-token");
		when(patientDeviceRepository.findByPacienteId(1L)).thenReturn(List.of(ios, android));
		when(apnsPushClient.send(eq(ios), any(PushEvent.class))).thenReturn(PushDeliveryResult.INVALID_TOKEN);
		when(fcmHttpV1Client.send(eq(android), any(PushEvent.class))).thenReturn(PushDeliveryResult.SUCCESS);

		sender.send(1L, PushEvent.newMessage(42L));

		verify(apnsPushClient).send(eq(ios), eq(PushEvent.newMessage(42L)));
		verify(fcmHttpV1Client).send(eq(android), eq(PushEvent.newMessage(42L)));
		verify(patientDeviceRepository).delete(ios);
		verify(patientDeviceRepository, never()).delete(android);
	}

	@Test
	void send_swallowsClientExceptions() {
		final PatientDevice ios = device(10L, PatientDevicePlatform.IOS, "ios-token");
		when(patientDeviceRepository.findByPacienteId(1L)).thenReturn(List.of(ios));
		when(apnsPushClient.send(eq(ios), any(PushEvent.class))).thenThrow(new RuntimeException("boom"));

		sender.send(1L, PushEvent.newMessage(7L));

		verify(patientDeviceRepository, never()).delete(any());
	}

	private static PatientDevice device(final Long id, final PatientDevicePlatform platform, final String token) {
		final Paciente paciente = new Paciente();
		paciente.setId(1L);
		final PatientDevice device = new PatientDevice();
		device.setId(id);
		device.setPaciente(paciente);
		device.setPlatform(platform);
		device.setToken(token);
		return device;
	}

}
