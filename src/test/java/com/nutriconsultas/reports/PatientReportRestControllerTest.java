package com.nutriconsultas.reports;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import com.nutriconsultas.paciente.Paciente;
import com.nutriconsultas.paciente.PacienteService;

import lombok.extern.slf4j.Slf4j;

@ExtendWith(MockitoExtension.class)
@Slf4j
@ActiveProfiles("test")
@SuppressWarnings("null")
public class PatientReportRestControllerTest {

	@InjectMocks
	private PatientReportRestController restController;

	@Mock
	private PatientReportService reportService;

	@Mock
	private PacienteService pacienteService;

	@Mock
	private OidcUser principal;

	private Paciente paciente;

	@BeforeEach
	public void setup() {
		paciente = new Paciente();
		paciente.setId(1L);
		paciente.setName("Test Patient");
		paciente.setUserId("user123");
	}

	@Test
	public void testGeneratePatientReportSuccess() {
		final String userId = "user123";
		when(principal.getSubject()).thenReturn(userId);
		when(pacienteService.findByIdAndUserId(1L, userId)).thenReturn(paciente);
		when(reportService.generateReport(eq(1L), eq(userId), any(), any())).thenReturn(new byte[] { 1, 2, 3, 4, 5 });

		final ResponseEntity<byte[]> response = restController.generatePatientReport(1L, null, null, principal);

		assertThat(response).isNotNull();
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		final byte[] body = response.getBody();
		assertThat(body).isNotNull();
		assertThat(body.length).isGreaterThan(0);
		final var contentType = response.getHeaders().getContentType();
		assertThat(contentType).isNotNull();
		assertThat(contentType.toString()).contains("application/pdf");
		verify(reportService).generateReport(1L, userId, null, null);
	}

	@Test
	public void testGeneratePatientReportWithDateRange() {
		final String userId = "user123";
		final Date startDate = new Date(System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000);
		final Date endDate = new Date();

		when(principal.getSubject()).thenReturn(userId);
		when(pacienteService.findByIdAndUserId(1L, userId)).thenReturn(paciente);
		when(reportService.generateReport(eq(1L), eq(userId), any(), any())).thenReturn(new byte[] { 1, 2, 3, 4, 5 });

		final ResponseEntity<byte[]> response = restController.generatePatientReport(1L, startDate, endDate, principal);

		assertThat(response).isNotNull();
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isNotNull();
		verify(reportService).generateReport(1L, userId, startDate, endDate);
	}

	@Test
	public void testGeneratePatientReportPatientNotFound() {
		when(principal.getSubject()).thenReturn("user123");
		when(pacienteService.findByIdAndUserId(1L, "user123")).thenReturn(null);

		final ResponseEntity<byte[]> response = restController.generatePatientReport(1L, null, null, principal);

		assertThat(response).isNotNull();
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	public void testGeneratePatientReportUnauthorized() {
		when(principal.getSubject()).thenReturn(null);

		final ResponseEntity<byte[]> response = restController.generatePatientReport(1L, null, null, principal);

		assertThat(response).isNotNull();
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
	}

	@Test
	public void testGeneratePatientReportInternalError() {
		final String userId = "user123";
		when(principal.getSubject()).thenReturn(userId);
		when(pacienteService.findByIdAndUserId(1L, userId)).thenReturn(paciente);
		when(reportService.generateReport(eq(1L), eq(userId), any(), any()))
			.thenThrow(new IllegalStateException("Error generating PDF"));

		final ResponseEntity<byte[]> response = restController.generatePatientReport(1L, null, null, principal);

		assertThat(response).isNotNull();
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
		verify(reportService).generateReport(1L, userId, null, null);
	}

	@Test
	public void testGenerateNutritionReportSuccess() {
		final String userId = "user123";
		final Long dietaId = 1L;
		when(principal.getSubject()).thenReturn(userId);
		when(reportService.generateNutritionReport(eq(dietaId), eq(userId))).thenReturn(new byte[] { 1, 2, 3, 4, 5 });

		final ResponseEntity<byte[]> response = restController.generateNutritionReport(dietaId, principal);

		assertThat(response).isNotNull();
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		final byte[] body = response.getBody();
		assertThat(body).isNotNull();
		assertThat(body.length).isGreaterThan(0);
		final var contentType = response.getHeaders().getContentType();
		assertThat(contentType).isNotNull();
		assertThat(contentType.toString()).contains("application/pdf");
		verify(reportService).generateNutritionReport(dietaId, userId);
	}

	@Test
	public void testGenerateNutritionReportUnauthorized() {
		when(principal.getSubject()).thenReturn(null);

		final ResponseEntity<byte[]> response = restController.generateNutritionReport(1L, principal);

		assertThat(response).isNotNull();
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
	}

	@Test
	public void testGenerateNutritionReportDietNotFound() {
		final String userId = "user123";
		final Long dietaId = 999L;
		when(principal.getSubject()).thenReturn(userId);
		when(reportService.generateNutritionReport(eq(dietaId), eq(userId)))
			.thenThrow(new IllegalArgumentException("Diet with id " + dietaId + " not found or access denied"));

		final ResponseEntity<byte[]> response = restController.generateNutritionReport(dietaId, principal);

		assertThat(response).isNotNull();
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		verify(reportService).generateNutritionReport(dietaId, userId);
	}

	@Test
	public void testGenerateNutritionReportInternalError() {
		final String userId = "user123";
		final Long dietaId = 1L;
		when(principal.getSubject()).thenReturn(userId);
		when(reportService.generateNutritionReport(eq(dietaId), eq(userId)))
			.thenThrow(new IllegalStateException("Error generating PDF"));

		final ResponseEntity<byte[]> response = restController.generateNutritionReport(dietaId, principal);

		assertThat(response).isNotNull();
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
		verify(reportService).generateNutritionReport(dietaId, userId);
	}

}
