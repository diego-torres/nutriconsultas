package com.nutriconsultas.mobile;

import static org.assertj.core.api.Assertions.assertThat;
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
import com.nutriconsultas.mobile.dto.DietGroceryListDto;
import com.nutriconsultas.mobile.dto.DietPlanPdfResult;
import com.nutriconsultas.mobile.dto.DietPlanSummaryDto;
import com.nutriconsultas.mobile.dto.DietPlatilloDetailDto;
import com.nutriconsultas.mobile.dto.PagedResponse;
import com.nutriconsultas.paciente.PacienteDietaStatus;
import com.nutriconsultas.paciente.projection.PacienteAuthView;

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
		final DietPlanSummaryDto summary = new DietPlanSummaryDto(7L, PacienteDietaStatus.ACTIVE, null, null, null,
				"Plan A", 2000, 100.0, 70.0, 250.0);
		final PagedResponse<DietPlanSummaryDto> page = PagedResponse
			.of(new PageImpl<>(List.of(summary), PageRequest.of(0, 20), 1));
		final Jwt jwt = jwtWithSub(PATIENT_SUB);

		when(patientAuthService.requireAuthViewByJwt(jwt)).thenReturn(authView(3L));
		when(mobilePatientDietPlanService.listDietPlans(3L, 0, 20, true)).thenReturn(page);

		final ApiResponse<PagedResponse<DietPlanSummaryDto>> response = controller.listDietPlans(jwt, 0, 20, true);

		assertThat(response.data().content()).hasSize(1);
		assertThat(response.data().content().get(0).dietaName()).isEqualTo("Plan A");
		assertThat(response.timestamp()).isNotNull();
		verify(mobilePatientDietPlanService).listDietPlans(3L, 0, 20, true);
	}

	@Test
	void getGroceryList_returnsApiResponseEnvelope() {
		final DietGroceryListDto groceryList = new DietGroceryListDto(List.of());
		final Jwt jwt = jwtWithSub(PATIENT_SUB);

		when(patientAuthService.requireAuthViewByJwt(jwt)).thenReturn(authView(3L));
		when(mobilePatientDietPlanService.getGroceryList(3L, 7L, "current")).thenReturn(groceryList);

		final ApiResponse<DietGroceryListDto> response = controller.getGroceryList(jwt, 7L, "current");

		assertThat(response.data().items()).isEmpty();
		assertThat(response.timestamp()).isNotNull();
		verify(mobilePatientDietPlanService).getGroceryList(3L, 7L, "current");
	}

	@Test
	void getPlatilloDetail_returnsApiResponseEnvelope() {
		final DietPlatilloDetailDto detail = new DietPlatilloDetailDto(30L, "Avena", 2, "/img.jpg", "Prep", null, null,
				List.of(), null);
		final Jwt jwt = jwtWithSub(PATIENT_SUB);

		when(patientAuthService.requireAuthViewByJwt(jwt)).thenReturn(authView(3L));
		when(mobilePatientDietPlanService.getPlatilloDetail(3L, 7L, 30L)).thenReturn(detail);

		final ApiResponse<DietPlatilloDetailDto> response = controller.getPlatilloDetail(jwt, 7L, 30L);

		assertThat(response.data().id()).isEqualTo(30L);
		assertThat(response.data().nombre()).isEqualTo("Avena");
		assertThat(response.timestamp()).isNotNull();
		verify(mobilePatientDietPlanService).getPlatilloDetail(3L, 7L, 30L);
	}

	@Test
	void getDietPlanPdf_returnsPdfResponseWithContentDisposition() {
		final Jwt jwt = jwtWithSub(PATIENT_SUB);
		final byte[] pdfBytes = new byte[] { 37, 80, 68, 70 };
		final DietPlanPdfResult pdf = new DietPlanPdfResult(pdfBytes, "Plan A.pdf");

		when(patientAuthService.requireAuthViewByJwt(jwt)).thenReturn(authView(3L));
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

	private static PacienteAuthView authView(final Long id) {
		return MobileTestPacienteAuthViews.authView(id, PATIENT_SUB, "auth0|nutritionist");
	}

}
