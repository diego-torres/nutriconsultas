package com.nutriconsultas.mobile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.oauth2.jwt.Jwt;

import com.nutriconsultas.calendar.EventStatus;
import com.nutriconsultas.mobile.dto.ApiResponse;
import com.nutriconsultas.mobile.dto.PagedResponse;
import com.nutriconsultas.mobile.dto.VisitDetailDto;
import com.nutriconsultas.mobile.dto.VisitSummaryDto;
import com.nutriconsultas.paciente.Paciente;

@ExtendWith(MockitoExtension.class)
class MobilePatientVisitControllerTest {

	private static final String PATIENT_SUB = "auth0|mobile-visit-patient";

	@InjectMocks
	private MobilePatientVisitController controller;

	@Mock
	private PatientAuthService patientAuthService;

	@Mock
	private MobilePatientVisitService mobilePatientVisitService;

	@Test
	void listVisits_returnsApiResponseEnvelope() {
		final Paciente paciente = new Paciente();
		paciente.setId(5L);
		final VisitSummaryDto summary = new VisitSummaryDto(1L, null, "Consulta", EventStatus.SCHEDULED, 45, null);
		final PagedResponse<VisitSummaryDto> page = PagedResponse
			.of(new PageImpl<>(List.of(summary), PageRequest.of(0, 20), 1));
		final Jwt jwt = jwtWithSub(PATIENT_SUB);

		when(patientAuthService.requirePacienteByJwt(jwt)).thenReturn(paciente);
		when(mobilePatientVisitService.listVisits(eq(5L), eq(0), eq(20), eq(null), eq(null), eq(null)))
			.thenReturn(page);

		final ApiResponse<PagedResponse<VisitSummaryDto>> response = controller.listVisits(jwt, 0, 20, null, null,
				null);

		assertThat(response.data().content()).hasSize(1);
		assertThat(response.message()).isNull();
		assertThat(response.timestamp()).isNotNull();
		verify(mobilePatientVisitService).listVisits(5L, 0, 20, null, null, null);
	}

	@Test
	void getVisitDetail_returnsApiResponseEnvelope() {
		final Paciente paciente = new Paciente();
		paciente.setId(5L);
		final VisitDetailDto detail = new VisitDetailDto(8L, null, "Consulta", EventStatus.SCHEDULED, 45, null,
				"Descripción", null, null, null, null, null, null, null, null, null, null, null);
		final Jwt jwt = jwtWithSub(PATIENT_SUB);

		when(patientAuthService.requirePacienteByJwt(jwt)).thenReturn(paciente);
		when(mobilePatientVisitService.getVisitDetail(5L, 8L)).thenReturn(detail);

		final ApiResponse<VisitDetailDto> response = controller.getVisitDetail(jwt, 8L);

		assertThat(response.data().id()).isEqualTo(8L);
		assertThat(response.data().description()).isEqualTo("Descripción");
		assertThat(response.timestamp()).isNotNull();
		verify(mobilePatientVisitService).getVisitDetail(5L, 8L);
	}

	private static Jwt jwtWithSub(final String subject) {
		return Jwt.withTokenValue("token").header("alg", "none").subject(subject).build();
	}

}
