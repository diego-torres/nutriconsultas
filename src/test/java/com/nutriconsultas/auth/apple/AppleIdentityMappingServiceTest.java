package com.nutriconsultas.auth.apple;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.nutriconsultas.auth0.Auth0ManagementUser;
import com.nutriconsultas.auth0.Auth0ManagementUserService;
import com.nutriconsultas.paciente.Paciente;
import com.nutriconsultas.paciente.PacienteRepository;

@ExtendWith(MockitoExtension.class)
class AppleIdentityMappingServiceTest {

	@InjectMocks
	private AppleIdentityMappingServiceImpl service;

	@Mock
	private Auth0ManagementUserService auth0ManagementUserService;

	@Mock
	private PacienteRepository pacienteRepository;

	@Test
	void mapNotificationReturnsMappedWhenPacienteHasAppleSubject() {
		final Paciente paciente = paciente(42L, "apple|001234.abc", "001234.abc");
		when(pacienteRepository.findByAppleSubject("001234.abc")).thenReturn(Optional.of(paciente));

		final AppleIdentityMappingResult result = service.mapNotification("001234.abc",
				"relay@privaterelay.appleid.com");

		assertThat(result.status()).isEqualTo(AppleIdentityMappingStatus.MAPPED);
		assertThat(result.pacienteId()).isEqualTo(42L);
		assertThat(result.auth0UserId()).isEqualTo("apple|001234.abc");
		verify(auth0ManagementUserService, never()).findUserByAppleSubject(any());
	}

	@Test
	void mapNotificationResolvesAuth0UserAndBackfillsAppleSubject() {
		when(pacienteRepository.findByAppleSubject("001234.abc")).thenReturn(Optional.empty());
		when(auth0ManagementUserService.isConfigured()).thenReturn(true);
		when(auth0ManagementUserService.findUserByAppleSubject("001234.abc"))
			.thenReturn(Optional.of(new Auth0ManagementUser("apple|001234.abc", "relay@privaterelay.appleid.com",
					java.util.Map.of(), List.of())));
		final Paciente paciente = paciente(7L, "apple|001234.abc", null);
		when(pacienteRepository.findByPatientAuthSub("apple|001234.abc")).thenReturn(Optional.of(paciente));
		when(pacienteRepository.save(paciente)).thenReturn(paciente);

		final AppleIdentityMappingResult result = service.mapNotification("001234.abc",
				"relay@privaterelay.appleid.com");

		assertThat(result.status()).isEqualTo(AppleIdentityMappingStatus.MAPPED);
		assertThat(result.pacienteId()).isEqualTo(7L);
		verify(pacienteRepository).save(paciente);
		assertThat(paciente.getAppleSubject()).isEqualTo("001234.abc");
	}

	@Test
	void mapNotificationReturnsNoLocalUserWhenAuth0UserExistsWithoutPaciente() {
		when(pacienteRepository.findByAppleSubject("001234.abc")).thenReturn(Optional.empty());
		when(auth0ManagementUserService.isConfigured()).thenReturn(true);
		when(auth0ManagementUserService.findUserByAppleSubject("001234.abc"))
			.thenReturn(Optional.of(new Auth0ManagementUser("apple|001234.abc", null, java.util.Map.of(), List.of())));
		when(pacienteRepository.findByPatientAuthSub("apple|001234.abc")).thenReturn(Optional.empty());

		final AppleIdentityMappingResult result = service.mapNotification("001234.abc", null);

		assertThat(result.status()).isEqualTo(AppleIdentityMappingStatus.NO_LOCAL_USER);
		assertThat(result.auth0UserId()).isEqualTo("apple|001234.abc");
	}

	@Test
	void mapNotificationReturnsAmbiguousWhenMultipleAppleUsersMatchEmail() {
		when(pacienteRepository.findByAppleSubject("001234.abc")).thenReturn(Optional.empty());
		when(auth0ManagementUserService.isConfigured()).thenReturn(true);
		when(auth0ManagementUserService.findUserByAppleSubject("001234.abc")).thenReturn(Optional.empty());
		when(auth0ManagementUserService.searchUsersByEmail("relay@privaterelay.appleid.com")).thenReturn(List.of(
				new Auth0ManagementUser("apple|001234.abc", "relay@privaterelay.appleid.com", java.util.Map.of(),
						List.of()),
				new Auth0ManagementUser("apple|999999.xyz", "relay@privaterelay.appleid.com", java.util.Map.of(),
						List.of())));

		final AppleIdentityMappingResult result = service.mapNotification("001234.abc",
				"relay@privaterelay.appleid.com");

		assertThat(result.status()).isEqualTo(AppleIdentityMappingStatus.AMBIGUOUS);
	}

	@Test
	void mapNotificationFallsBackToLocalPatientAuthSubWhenAuth0NotConfigured() {
		when(pacienteRepository.findByAppleSubject("001234.abc")).thenReturn(Optional.empty());
		when(auth0ManagementUserService.isConfigured()).thenReturn(false);
		final Paciente paciente = paciente(3L, "apple|001234.abc", null);
		when(pacienteRepository.findByPatientAuthSub("apple|001234.abc")).thenReturn(Optional.of(paciente));
		when(pacienteRepository.save(paciente)).thenReturn(paciente);

		final AppleIdentityMappingResult result = service.mapNotification("001234.abc", null);

		assertThat(result.status()).isEqualTo(AppleIdentityMappingStatus.MAPPED);
		verify(auth0ManagementUserService, never()).findUserByAppleSubject(eq("001234.abc"));
	}

	private static Paciente paciente(final Long id, final String patientAuthSub, final String appleSubject) {
		final Paciente paciente = new Paciente();
		paciente.setId(id);
		paciente.setPatientAuthSub(patientAuthSub);
		paciente.setAppleSubject(appleSubject);
		paciente.setUserId("nutritionist-sub");
		paciente.setName("Paciente Test");
		return paciente;
	}

}
