package com.nutriconsultas.mobile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.nutriconsultas.dieta.AlimentoIngesta;
import com.nutriconsultas.dieta.Dieta;
import com.nutriconsultas.dieta.DietaPdfService;
import com.nutriconsultas.dieta.Ingesta;
import com.nutriconsultas.dieta.PlatilloIngesta;
import com.nutriconsultas.dieta.PlatilloIngestaRepository;
import com.nutriconsultas.mobile.dto.DietGroceryListDto;
import com.nutriconsultas.mobile.dto.DietPlanDetailDto;
import com.nutriconsultas.mobile.dto.DietPlanPdfResult;
import com.nutriconsultas.mobile.dto.DietPlatilloDetailDto;
import com.nutriconsultas.paciente.Paciente;
import com.nutriconsultas.paciente.PacienteDieta;
import com.nutriconsultas.paciente.PacienteDietaRepository;
import com.nutriconsultas.paciente.PacienteDietaStatus;

@ExtendWith(MockitoExtension.class)
class MobilePatientDietPlanServiceTest {

	@InjectMocks
	private MobilePatientDietPlanService service;

	@Mock
	private PacienteDietaRepository pacienteDietaRepository;

	@Mock
	private DietaPdfService dietaPdfService;

	@Mock
	private PlatilloIngestaRepository platilloIngestaRepository;

	@Test
	void getGroceryList_returnsAggregatedItemsWhenOwnedByPatient() {
		final PacienteDieta assignment = sampleAssignment(5L, 1L);
		when(pacienteDietaRepository.findByIdAndPacienteId(5L, 1L)).thenReturn(Optional.of(assignment));

		final DietGroceryListDto result = service.getGroceryList(1L, 5L, "current");

		assertThat(result.items()).hasSize(1);
		assertThat(result.items().get(0).nombre()).isEqualTo("Manzana");
		assertThat(result.items().get(0).cantidad()).isEqualTo("1");
		assertThat(result.items().get(0).unidad()).isEqualTo("pieza");
	}

	@Test
	void getGroceryList_throwsNotFoundWhenMissingOrNotOwned() {
		when(pacienteDietaRepository.findByIdAndPacienteId(99L, 1L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.getGroceryList(1L, 99L, "current")).isInstanceOf(ResponseStatusException.class)
			.extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
			.isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	void getGroceryList_throwsBadRequestForUnsupportedWeek() {
		final PacienteDieta assignment = sampleAssignment(5L, 1L);
		when(pacienteDietaRepository.findByIdAndPacienteId(5L, 1L)).thenReturn(Optional.of(assignment));

		assertThatThrownBy(() -> service.getGroceryList(1L, 5L, "next")).isInstanceOf(ResponseStatusException.class)
			.extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
			.isEqualTo(HttpStatus.BAD_REQUEST);
	}

	@Test
	void getPlatilloDetail_returnsDetailWhenOwnedByPatient() {
		final PacienteDieta assignment = sampleAssignment(5L, 1L);
		final PlatilloIngesta platillo = assignment.getDieta().getIngestas().get(0).getPlatillos().get(0);
		when(platilloIngestaRepository.findByIdForPatientAssignment(30L, 5L, 1L)).thenReturn(Optional.of(platillo));

		final DietPlatilloDetailDto result = service.getPlatilloDetail(1L, 5L, 30L);

		assertThat(result.id()).isEqualTo(30L);
		assertThat(result.nombre()).isEqualTo("Avena con fruta");
		assertThat(result.porciones()).isEqualTo(2);
		assertThat(result.description()).isEqualTo("Servir tibia");
		assertThat(result.nutritionFacts().kcal()).isEqualTo(320);
		assertThat(result.nutritionFacts().proteina()).isEqualTo(12.5);
	}

	@Test
	void getPlatilloDetail_throwsNotFoundWhenMissingOrNotOwned() {
		when(platilloIngestaRepository.findByIdForPatientAssignment(99L, 5L, 1L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.getPlatilloDetail(1L, 5L, 99L)).isInstanceOf(ResponseStatusException.class)
			.extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
			.isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	void getDietPlanDetail_returnsStructuredMealTreeWhenOwnedByPatient() {
		final PacienteDieta assignment = sampleAssignment(5L, 1L);
		when(pacienteDietaRepository.findByIdAndPacienteId(5L, 1L)).thenReturn(Optional.of(assignment));

		final DietPlanDetailDto result = service.getDietPlanDetail(1L, 5L);

		assertThat(result.assignmentId()).isEqualTo(5L);
		assertThat(result.dietaName()).isEqualTo("Plan hipocalórico");
		assertThat(result.totalKcal()).isEqualTo(1800);
		assertThat(result.ingestas()).hasSize(1);
		assertThat(result.ingestas().get(0).tipo()).isEqualTo("Desayuno");
		assertThat(result.ingestas().get(0).platillos()).hasSize(1);
		assertThat(result.ingestas().get(0).platillos().get(0).id()).isEqualTo(30L);
		assertThat(result.ingestas().get(0).platillos().get(0).nombre()).isEqualTo("Avena con fruta");
		assertThat(result.ingestas().get(0).platillos().get(0).porciones()).isEqualTo(2);
		assertThat(result.ingestas().get(0).platillos().get(0).kcal()).isEqualTo(320);
		assertThat(result.ingestas().get(0).platillos().get(0).proteina()).isEqualTo(12.5);
		assertThat(result.ingestas().get(0).platillos().get(0).carbohidratos()).isEqualTo(48.0);
		assertThat(result.ingestas().get(0).platillos().get(0).grasas()).isEqualTo(8.0);
		assertThat(result.ingestas().get(0).alimentos()).hasSize(1);
		assertThat(result.ingestas().get(0).alimentos().get(0).nombre()).isEqualTo("Manzana");
		assertThat(result.ingestas().get(0).alimentos().get(0).unidad()).isEqualTo("pieza");
		assertThat(result.ingestas().get(0).alimentos().get(0).proteina()).isEqualTo(0.3);
	}

	@Test
	void getDietPlanDetail_throwsNotFoundWhenMissingOrNotOwned() {
		when(pacienteDietaRepository.findByIdAndPacienteId(99L, 1L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.getDietPlanDetail(1L, 99L)).isInstanceOf(ResponseStatusException.class)
			.extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
			.isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	void generateDietPlanPdf_returnsPdfWithFilenameWhenOwnedByPatient() {
		final PacienteDieta assignment = sampleAssignment(5L, 1L);
		final byte[] pdfBytes = new byte[] { 37, 80, 68, 70 };
		when(pacienteDietaRepository.findByIdAndPacienteId(5L, 1L)).thenReturn(Optional.of(assignment));
		when(dietaPdfService.generatePdfForAssignment(assignment)).thenReturn(pdfBytes);

		final DietPlanPdfResult result = service.generateDietPlanPdf(1L, 5L);

		assertThat(result.content()).isEqualTo(pdfBytes);
		assertThat(result.filename()).isEqualTo("Plan hipocalórico.pdf");
		verify(dietaPdfService).generatePdfForAssignment(assignment);
	}

	@Test
	void generateDietPlanPdf_throwsNotFoundWhenMissingOrNotOwned() {
		when(pacienteDietaRepository.findByIdAndPacienteId(99L, 1L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.generateDietPlanPdf(1L, 99L)).isInstanceOf(ResponseStatusException.class)
			.extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
			.isEqualTo(HttpStatus.NOT_FOUND);
	}

	private static PacienteDieta sampleAssignment(final Long assignmentId, final Long pacienteId) {
		final Paciente paciente = new Paciente();
		paciente.setId(pacienteId);

		final Dieta dieta = new Dieta();
		dieta.setId(10L);
		dieta.setNombre("Plan hipocalórico");
		dieta.setUserId("nutritionist-sub");
		dieta.setEnergia(1800);
		dieta.setProteina(90.0);
		dieta.setLipidos(60.0);
		dieta.setHidratosDeCarbono(200.0);

		final Ingesta ingesta = new Ingesta("Desayuno");
		ingesta.setId(20L);
		ingesta.setDieta(dieta);
		ingesta.setEnergia(450);
		ingesta.setProteina(20.0);
		ingesta.setLipidos(12.0);
		ingesta.setHidratosDeCarbono(55.0);

		final PlatilloIngesta platillo = new PlatilloIngesta();
		platillo.setId(30L);
		platillo.setName("Avena con fruta");
		platillo.setPortions(2);
		platillo.setEnergia(320);
		platillo.setProteina(12.5);
		platillo.setHidratosDeCarbono(48.0);
		platillo.setLipidos(8.0);
		platillo.setRecommendations("Servir tibia");
		platillo.setIngesta(ingesta);

		final AlimentoIngesta alimento = new AlimentoIngesta();
		alimento.setId(40L);
		alimento.setName("Manzana");
		alimento.setPortions(1);
		alimento.setEnergia(52);
		alimento.setUnidad("pieza");
		alimento.setProteina(0.3);
		alimento.setHidratosDeCarbono(14.0);
		alimento.setLipidos(0.2);
		alimento.setIngesta(ingesta);

		ingesta.setPlatillos(List.of(platillo));
		ingesta.setAlimentos(List.of(alimento));
		dieta.setIngestas(List.of(ingesta));

		final PacienteDieta assignment = new PacienteDieta();
		assignment.setId(assignmentId);
		assignment.setPaciente(paciente);
		assignment.setDieta(dieta);
		assignment.setStatus(PacienteDietaStatus.ACTIVE);
		assignment
			.setStartDate(Date.from(LocalDate.now().minusDays(3).atStartOfDay(ZoneId.systemDefault()).toInstant()));
		assignment.setNotes("Seguimiento semanal");
		return assignment;
	}

}
