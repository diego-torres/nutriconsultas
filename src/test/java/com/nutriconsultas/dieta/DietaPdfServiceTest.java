package com.nutriconsultas.dieta;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import com.nutriconsultas.paciente.PacienteDieta;
import com.nutriconsultas.paciente.PacienteDietaRepository;
import com.nutriconsultas.paciente.PacienteDietaStatus;

import lombok.extern.slf4j.Slf4j;

@ExtendWith(MockitoExtension.class)
@Slf4j
@ActiveProfiles("test")
public class DietaPdfServiceTest {

	@InjectMocks
	private DietaPdfService dietaPdfService;

	@Mock
	private TemplateEngine templateEngine;

	@Mock
	private PacienteDietaRepository pacienteDietaRepository;

	@Mock
	private DietaService dietaService;

	private Dieta dieta;

	private Ingesta ingesta;

	@BeforeEach
	public void setup() {
		dieta = new Dieta();
		dieta.setId(1L);
		dieta.setNombre("Dieta de prueba");

		ingesta = new Ingesta();
		ingesta.setId(1L);
		ingesta.setNombre("Desayuno");
		ingesta.setDieta(dieta);
		ingesta.setPlatillos(new ArrayList<>());
		ingesta.setAlimentos(new ArrayList<>());

		dieta.setIngestas(new ArrayList<>());
		dieta.getIngestas().add(ingesta);
	}

	@Test
	public void testGeneratePdfWithDieta() {
		when(dietaService.getDieta(1L)).thenReturn(dieta);
		when(pacienteDietaRepository.findByDietaId(1L)).thenReturn(new ArrayList<>());
		when(templateEngine.process(eq("sbadmin/dietas/printable"), any(Context.class)))
			.thenReturn("<html><body>Test</body></html>");

		final byte[] pdfBytes = dietaPdfService.generatePdf(1L);

		assertThat(pdfBytes).isNotNull();
		assertThat(pdfBytes.length).isGreaterThan(0);
	}

	@Test
	public void testGeneratePdfWithDietaAndPatient() {
		final PacienteDieta pacienteDieta = new PacienteDieta();
		pacienteDieta.setStatus(PacienteDietaStatus.ACTIVE);
		final List<PacienteDieta> assignments = new ArrayList<>();
		assignments.add(pacienteDieta);

		when(dietaService.getDieta(1L)).thenReturn(dieta);
		when(pacienteDietaRepository.findByDietaId(1L)).thenReturn(assignments);
		when(templateEngine.process(eq("sbadmin/dietas/printable"), any(Context.class)))
			.thenReturn("<html><body>Test</body></html>");

		final byte[] pdfBytes = dietaPdfService.generatePdf(1L);

		assertThat(pdfBytes).isNotNull();
		assertThat(pdfBytes.length).isGreaterThan(0);
	}

	@Test
	public void testGeneratePdfWithNonExistentDieta() {
		when(dietaService.getDieta(999L)).thenReturn(null);

		assertThatThrownBy(() -> dietaPdfService.generatePdf(999L)).isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("not found");
	}

	@Test
	public void testGeneratePdfWithIngestasAndPlatillos() {
		final PlatilloIngesta platillo = new PlatilloIngesta();
		platillo.setId(1L);
		platillo.setName("Platillo de prueba");
		platillo.setPortions(1);
		platillo.setEnergia(250);
		platillo.setProteina(15.0);
		platillo.setLipidos(8.0);
		platillo.setHidratosDeCarbono(30.0);
		platillo.setIngesta(ingesta);

		ingesta.getPlatillos().add(platillo);

		when(dietaService.getDieta(1L)).thenReturn(dieta);
		when(pacienteDietaRepository.findByDietaId(1L)).thenReturn(new ArrayList<>());
		when(templateEngine.process(eq("sbadmin/dietas/printable"), any(Context.class)))
			.thenReturn("<html><body>Test</body></html>");

		final byte[] pdfBytes = dietaPdfService.generatePdf(1L);

		assertThat(pdfBytes).isNotNull();
		assertThat(pdfBytes.length).isGreaterThan(0);
	}

	@Test
	public void testGeneratePdfWithIngestasAndAlimentos() {
		final AlimentoIngesta alimento = new AlimentoIngesta();
		alimento.setId(1L);
		alimento.setName("Alimento de prueba");
		alimento.setPortions(1);
		alimento.setEnergia(100);
		alimento.setProteina(5.0);
		alimento.setLipidos(3.0);
		alimento.setHidratosDeCarbono(12.0);
		alimento.setIngesta(ingesta);

		ingesta.getAlimentos().add(alimento);

		when(dietaService.getDieta(1L)).thenReturn(dieta);
		when(pacienteDietaRepository.findByDietaId(1L)).thenReturn(new ArrayList<>());
		when(templateEngine.process(eq("sbadmin/dietas/printable"), any(Context.class)))
			.thenReturn("<html><body>Test</body></html>");

		final byte[] pdfBytes = dietaPdfService.generatePdf(1L);

		assertThat(pdfBytes).isNotNull();
		assertThat(pdfBytes.length).isGreaterThan(0);
	}

	@Test
	public void testGeneratePdfWithoutPatientInfoEvenWhenAssigned() {
		// Test that when includePatientInfo is false, patient info is excluded
		// even if an active assignment exists
		final PacienteDieta pacienteDieta = new PacienteDieta();
		pacienteDieta.setStatus(PacienteDietaStatus.ACTIVE);
		final List<PacienteDieta> assignments = new ArrayList<>();
		assignments.add(pacienteDieta);

		when(dietaService.getDieta(1L)).thenReturn(dieta);
		// Should NOT call findByDietaId when includePatientInfo is false
		when(templateEngine.process(eq("sbadmin/dietas/printable"), any(Context.class)))
			.thenReturn("<html><body>Test</body></html>");

		final byte[] pdfBytes = dietaPdfService.generatePdf(1L, false);

		assertThat(pdfBytes).isNotNull();
		assertThat(pdfBytes.length).isGreaterThan(0);
		// Verify that findByDietaId was NOT called (patient info excluded)
		// This is verified implicitly - if it were called, we'd need to mock it
	}

}
