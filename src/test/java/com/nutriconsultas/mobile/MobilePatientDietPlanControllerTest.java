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
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;

import com.nutriconsultas.mobile.dto.ApiResponse;
import com.nutriconsultas.mobile.dto.DietPlanPdfResult;
import com.nutriconsultas.mobile.dto.DietPlanSummaryDto;
import com.nutriconsultas.mobile.dto.PagedResponse;
import com.nutriconsultas.paciente.Paciente;
import com.nutriconsultas.paciente.PacienteDietaStatus;

@ExtendWith(MockitoExtension.class)
class MobilePatientDietPlanControllerTest {

	private static final String PATIENT_SUB = "auth0|mobile-diet-plan-patient";

	@InjectMocks
	private MobilePatientDietPlanController controller;

	@Mock
	private PatientAuthService patientAuthService;

	@Mock
	private MobilePatientDietPlanService mobilePatientDietPlanService;

	@Test
	void listDietPlans_returnsApiResponseEnvelope() {
		final Paciente paciente = new Paciente();
		paciente.setId(3L);
		final DietPlanSummaryDto summary = new DietPlanSummaryDto(7L, PacienteDietaStatus.ACTIVE, null, null, null,
				"Plan A", 2000, 100.0, 70.0, 250.0);
		final PagedResponse<DietPlanSummaryDto> page = PagedResponse
			.of(new PageImpl<>(List.of(summary), PageRequest.of(0, 20), 1));
		final Jwt jwt = jwtWithSub(PATIENT_SUB);

		when(patientAuthService.requirePacienteByJwt(jwt)).thenReturn(paciente);
		when(mobilePatientDietPlanService.listDietPlans(3L, 0, 20, true)).thenReturn(page);

		final ApiResponse<PagedResponse<DietPlanSummaryDto>> response = controller.listDietPlans(jwt, 0, 20, true);

		assertThat(response.data().content()).hasSize(1);
		assertThat(response.data().content().get(0).dietaName()).isEqualTo("Plan A");
		assertThat(response.timestamp()).isNotNull();
		verify(mobilePatientDietPlanService).listDietPlans(3L, 0, 20, true);
	}

	@Test
	void getDietPlanPdf_returnsPdfResponseWithContentDisposition() {
		final Paciente paciente = new Paciente();
		paciente.setId(3L);
		final Jwt jwt = jwtWithSub(PATIENT_SUB);
		final byte[] pdfBytes = new byte[] { 37, 80, 68, 70 };
		final DietPlanPdfResult pdf = new DietPlanPdfResult(pdfBytes, "Plan A.pdf");

		when(patientAuthService.requirePacienteByJwt(jwt)).thenReturn(paciente);
		when(mobilePatientDietPlanService.generateDietPlanPdf(3L, 7L)).thenReturn(pdf);

		final ResponseEntity<byte[]> response = controller.getDietPlanPdf(jwt, 7L);

		assertThat(response.getStatusCode().value()).isEqualTo(200);
		assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION))
			.isEqualTo("attachment; filename=\"Plan A.pdf\"");
		assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PDF);
		assertThat(response.getBody()).isEqualTo(pdfBytes);
		verify(mobilePatientDietPlanService).generateDietPlanPdf(3L, 7L);
	}

	private static Jwt jwtWithSub(final String subject) {
		return Jwt.withTokenValue("token").header("alg", "none").subject(subject).build();
	}

}
