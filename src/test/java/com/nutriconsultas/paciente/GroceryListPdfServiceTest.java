package com.nutriconsultas.paciente;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import com.nutriconsultas.dieta.Dieta;
import com.nutriconsultas.mobile.dto.DietGroceryListItemDto;
import com.nutriconsultas.profile.NutritionistProfile;
import com.nutriconsultas.profile.NutritionistProfileService;

import lombok.extern.slf4j.Slf4j;

@ExtendWith(MockitoExtension.class)
@Slf4j
@ActiveProfiles("test")
public class GroceryListPdfServiceTest {

	@InjectMocks
	private GroceryListPdfService groceryListPdfService;

	@Mock
	private TemplateEngine templateEngine;

	@Mock
	private NutritionistProfileService nutritionistProfileService;

	private Paciente paciente;

	private PacienteDieta assignment;

	private List<DietGroceryListItemDto> groceryItems;

	@BeforeEach
	public void setup() {
		paciente = new Paciente();
		paciente.setId(1L);
		paciente.setName("Paciente Prueba");

		final Dieta dieta = new Dieta();
		dieta.setId(10L);
		dieta.setNombre("Dieta ejemplo");

		assignment = new PacienteDieta();
		assignment.setId(5L);
		assignment.setPaciente(paciente);
		assignment.setDieta(dieta);
		assignment.setStartDate(new Date());
		assignment.setAssignmentType(PacienteDietaAssignmentType.DATE_RANGE);

		groceryItems = List.of(new DietGroceryListItemDto("Avena", "1", "taza", "Cereales"));

		when(nutritionistProfileService.getOrCreateProfile("user-1")).thenReturn(new NutritionistProfile());
		when(nutritionistProfileService.getLogoAsBase64DataUri("user-1")).thenReturn(null);
		when(templateEngine.process(eq("sbadmin/pacientes/lista-compras-pdf"), any(Context.class)))
			.thenReturn("<html><body>Test</body></html>");
	}

	@Test
	public void generatePdf_returnsNonEmptyBytes() {
		final byte[] pdfBytes = groceryListPdfService.generatePdf(paciente, assignment, groceryItems, "user-1",
				"Lic. Test");

		assertThat(pdfBytes).isNotNull();
		assertThat(pdfBytes.length).isGreaterThan(0);
	}

	@Test
	public void generatePdf_weeklyAssignmentUsesPlanSemanalLabel() {
		assignment.setAssignmentType(PacienteDietaAssignmentType.WEEKLY);
		assignment.setDieta(null);

		final ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
		groceryListPdfService.generatePdf(paciente, assignment, groceryItems, "user-1", null);

		verify(templateEngine).process(eq("sbadmin/pacientes/lista-compras-pdf"), contextCaptor.capture());
		assertThat(contextCaptor.getValue().getVariable("assignmentLabel")).isEqualTo("Plan semanal");
	}

	@Test
	public void buildPdfResponse_setsAttachmentHeaders() {
		final ResponseEntity<byte[]> response = groceryListPdfService.buildPdfResponse(paciente, assignment,
				groceryItems, "user-1", null);

		assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
		assertThat(response.getHeaders().getFirst("Content-Disposition")).contains("lista-de-compras.pdf");
		assertThat(response.getHeaders().getContentType()).hasToString("application/pdf");
		assertThat(response.getBody()).isNotNull();
	}

}
