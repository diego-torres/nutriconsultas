package com.nutriconsultas.paciente;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import com.nutriconsultas.dataTables.paging.Direction;
import com.nutriconsultas.dataTables.paging.Order;
import com.nutriconsultas.dataTables.paging.PageArray;
import com.nutriconsultas.dataTables.paging.PagingRequest;
import com.nutriconsultas.dataTables.paging.Search;

import lombok.extern.slf4j.Slf4j;

@ExtendWith(MockitoExtension.class)
@Slf4j
@ActiveProfiles("test")
@SuppressWarnings("null")
public class ClinicalExamRestControllerTest {

	@InjectMocks
	private ClinicalExamRestController restController;

	@Mock
	private ClinicalExamService clinicalExamService;

	private Paciente paciente;

	private ClinicalExam exam1;

	private ClinicalExam exam2;

	@BeforeEach
	public void setup() {
		log.info("setting up ClinicalExamRestController test");

		paciente = new Paciente();
		paciente.setId(1L);
		paciente.setName("Test Paciente");

		exam1 = new ClinicalExam();
		exam1.setId(1L);
		exam1.setTitle("Examen Clínico 1");
		exam1.setExamDateTime(new Date(System.currentTimeMillis() - 86400000)); // Yesterday
		exam1.setPaciente(paciente);
		exam1.setPeso(70.0);
		exam1.setEstatura(1.75);
		exam1.setImc(22.86);
		exam1.setGlucosa(95.0);
		exam1.setColesterolTotal(200.0);
		exam1.setHemoglobina(14.0);

		exam2 = new ClinicalExam();
		exam2.setId(2L);
		exam2.setTitle("Examen Clínico 2");
		exam2.setExamDateTime(new Date()); // Today
		exam2.setPaciente(paciente);
		exam2.setPeso(71.0);
		exam2.setEstatura(1.75);
		exam2.setImc(23.18);
		exam2.setGlucosa(100.0);
		exam2.setColesterolTotal(210.0);
		exam2.setHemoglobina(14.5);

		log.info("finished setting up ClinicalExamRestController test");
	}

	@Test
	public void testGetPageArray() {
		log.info("Starting testGetPageArray");
		// Arrange
		when(clinicalExamService.findByPacienteId(1L)).thenReturn(Arrays.asList(exam1, exam2));

		PagingRequest pagingRequest = new PagingRequest();
		pagingRequest.setStart(0);
		pagingRequest.setLength(10);
		pagingRequest.setDraw(1);
		pagingRequest.setOrder(Arrays.asList(new Order(0, Direction.asc)));
		pagingRequest.setSearch(new Search("", "false"));

		// Act
		PageArray result = restController.getPageArray(pagingRequest, 1L);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.getRecordsTotal()).isEqualTo(2);
		assertThat(result.getRecordsFiltered()).isEqualTo(2);
		assertThat(result.getDraw()).isEqualTo(1);
		assertThat(result.getData()).isNotEmpty();
		assertThat(result.getData().size()).isEqualTo(2);
		verify(clinicalExamService).findByPacienteId(1L);
		log.info("Finishing testGetPageArray");
	}

	@Test
	public void testGetPageArrayWithSearch() {
		log.info("Starting testGetPageArrayWithSearch");
		// Arrange
		when(clinicalExamService.findByPacienteId(1L)).thenReturn(Arrays.asList(exam1, exam2));

		PagingRequest pagingRequest = new PagingRequest();
		pagingRequest.setStart(0);
		pagingRequest.setLength(10);
		pagingRequest.setDraw(1);
		pagingRequest.setOrder(Arrays.asList(new Order(0, Direction.asc)));
		pagingRequest.setSearch(new Search("Examen Clínico 1", "false"));

		// Act
		PageArray result = restController.getPageArray(pagingRequest, 1L);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.getRecordsTotal()).isEqualTo(2);
		assertThat(result.getRecordsFiltered()).isEqualTo(1);
		assertThat(result.getData()).isNotEmpty();
		log.info("Finishing testGetPageArrayWithSearch");
	}

	@Test
	public void testGetPageArrayNotImplemented() {
		log.info("Starting testGetPageArrayNotImplemented");
		// Arrange
		PagingRequest pagingRequest = new PagingRequest();

		// Act
		PageArray result = restController.getPageArray(pagingRequest);

		// Assert
		assertThat(result).isNull();
		log.info("Finishing testGetPageArrayNotImplemented");
	}

	@Test
	public void testToStringList() {
		log.info("Starting testToStringList");
		// Act
		List<String> result = restController.toStringList(exam1);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.size()).isEqualTo(8); // fecha, titulo, peso, imc, glucosa, colesterolTotal, hemoglobina, actions
		assertThat(result.get(0)).isNotEmpty(); // Date formatted
		assertThat(result.get(1)).contains("Examen Clínico 1");
		assertThat(result.get(1)).contains("/admin/pacientes/1/examen-clinico/1");
		assertThat(result.get(2)).contains("70.0 kg"); // Peso
		assertThat(result.get(3)).contains("22.9"); // IMC
		assertThat(result.get(4)).contains("95 mg/dL"); // Glucosa
		assertThat(result.get(5)).contains("200 mg/dL"); // Colesterol Total
		assertThat(result.get(6)).contains("14.0 g/dL"); // Hemoglobina
		assertThat(result.get(7)).contains("delete-exam-btn");
		log.info("Finishing testToStringList");
	}

	@Test
	public void testToStringListWithNullValues() {
		log.info("Starting testToStringListWithNullValues");
		// Arrange
		ClinicalExam exam = new ClinicalExam();
		exam.setId(1L);
		exam.setTitle("Test");
		exam.setExamDateTime(new Date());
		exam.setPaciente(paciente);

		// Act
		List<String> result = restController.toStringList(exam);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.size()).isEqualTo(8);
		assertThat(result.get(2)).isEqualTo("-"); // Peso null
		assertThat(result.get(3)).isEqualTo("-"); // IMC null
		assertThat(result.get(4)).isEqualTo("-"); // Glucosa null
		assertThat(result.get(5)).isEqualTo("-"); // Colesterol Total null
		assertThat(result.get(6)).isEqualTo("-"); // Hemoglobina null
		log.info("Finishing testToStringListWithNullValues");
	}

	@Test
	public void testGetData() {
		log.info("Starting testGetData");
		// Arrange
		when(clinicalExamService.findAll()).thenReturn(Arrays.asList(exam1, exam2));

		// Act
		List<ClinicalExam> result = restController.getData();

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.size()).isEqualTo(2);
		verify(clinicalExamService).findAll();
		log.info("Finishing testGetData");
	}

	@Test
	public void testGetRows() {
		log.info("Starting testGetRows");
		// Arrange
		when(clinicalExamService.findByPacienteId(1L)).thenReturn(Arrays.asList(exam1, exam2));

		PagingRequest pagingRequest = new PagingRequest();
		pagingRequest.setStart(0);
		pagingRequest.setLength(10);
		pagingRequest.setDraw(1);
		pagingRequest.setOrder(Arrays.asList(new Order(0, Direction.asc)));
		pagingRequest.setSearch(new Search("", "false"));
		pagingRequest.setColumns(restController.getColumns());

		// Act
		var page = restController.getRows(pagingRequest, 1L);

		// Assert
		assertThat(page).isNotNull();
		assertThat(page.getRecordsTotal()).isEqualTo(2);
		verify(clinicalExamService).findByPacienteId(1L);
		log.info("Finishing testGetRows");
	}

	@Test
	public void testGetRowsWithoutPacienteId() {
		log.info("Starting testGetRowsWithoutPacienteId");
		// Arrange
		when(clinicalExamService.findAll()).thenReturn(Arrays.asList(exam1, exam2));

		PagingRequest pagingRequest = new PagingRequest();
		pagingRequest.setStart(0);
		pagingRequest.setLength(10);
		pagingRequest.setDraw(1);
		pagingRequest.setOrder(Arrays.asList(new Order(0, Direction.asc)));
		pagingRequest.setSearch(new Search("", "false"));
		pagingRequest.setColumns(restController.getColumns());

		// Act
		var page = restController.getRows(pagingRequest);

		// Assert
		assertThat(page).isNotNull();
		assertThat(page.getRecordsTotal()).isEqualTo(2);
		verify(clinicalExamService).findAll();
		log.info("Finishing testGetRowsWithoutPacienteId");
	}

	@Test
	public void testGetComparator() {
		log.info("Starting testGetComparator");
		// Test title comparator
		var titleComparator = restController.getComparator("title", Direction.asc);
		assertThat(titleComparator).isNotNull();
		int result = titleComparator.compare(exam1, exam2);
		assertThat(result).isLessThan(0); // "Examen Clínico 1" < "Examen Clínico 2"

		// Test date comparator
		var dateComparator = restController.getComparator("examDateTime", Direction.asc);
		assertThat(dateComparator).isNotNull();
		int dateResult = dateComparator.compare(exam1, exam2);
		assertThat(dateResult).isLessThan(0); // Yesterday < Today

		// Test peso comparator
		var pesoComparator = restController.getComparator("peso", Direction.asc);
		assertThat(pesoComparator).isNotNull();
		int pesoResult = pesoComparator.compare(exam1, exam2);
		assertThat(pesoResult).isLessThan(0); // 70.0 < 71.0

		// Test imc comparator
		var imcComparator = restController.getComparator("imc", Direction.asc);
		assertThat(imcComparator).isNotNull();
		int imcResult = imcComparator.compare(exam1, exam2);
		assertThat(imcResult).isLessThan(0); // 22.86 < 23.18

		// Test glucosa comparator
		var glucosaComparator = restController.getComparator("glucosa", Direction.asc);
		assertThat(glucosaComparator).isNotNull();
		int glucosaResult = glucosaComparator.compare(exam1, exam2);
		assertThat(glucosaResult).isLessThan(0); // 95.0 < 100.0

		// Test colesterolTotal comparator
		var colesterolComparator = restController.getComparator("colesterolTotal", Direction.asc);
		assertThat(colesterolComparator).isNotNull();
		int colesterolResult = colesterolComparator.compare(exam1, exam2);
		assertThat(colesterolResult).isLessThan(0); // 200.0 < 210.0

		// Test hemoglobina comparator
		var hemoglobinaComparator = restController.getComparator("hemoglobina", Direction.asc);
		assertThat(hemoglobinaComparator).isNotNull();
		int hemoglobinaResult = hemoglobinaComparator.compare(exam1, exam2);
		assertThat(hemoglobinaResult).isLessThan(0); // 14.0 < 14.5

		// Test descending
		var descComparator = restController.getComparator("title", Direction.desc);
		int descResult = descComparator.compare(exam1, exam2);
		assertThat(descResult).isGreaterThan(0);

		// Test default comparator
		var defaultComparator = restController.getComparator("unknown", Direction.asc);
		assertThat(defaultComparator).isNotNull();
		log.info("Finishing testGetComparator");
	}

	@Test
	public void testGetColumns() {
		log.info("Starting testGetColumns");
		// Act
		var result = restController.getColumns();

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.size()).isEqualTo(8);
		assertThat(result.get(0).getData()).isEqualTo("fecha");
		assertThat(result.get(1).getData()).isEqualTo("titulo");
		assertThat(result.get(2).getData()).isEqualTo("peso");
		assertThat(result.get(3).getData()).isEqualTo("imc");
		assertThat(result.get(4).getData()).isEqualTo("glucosa");
		assertThat(result.get(5).getData()).isEqualTo("colesterolTotal");
		assertThat(result.get(6).getData()).isEqualTo("hemoglobina");
		assertThat(result.get(7).getData()).isEqualTo("actions");
		log.info("Finishing testGetColumns");
	}

	@Test
	public void testGetPredicate() {
		log.info("Starting testGetPredicate");
		// Test title match
		var predicate = restController.getPredicate("Examen Clínico 1");
		assertThat(predicate.test(exam1)).isTrue();
		assertThat(predicate.test(exam2)).isFalse();

		// Test no match
		var noMatchPredicate = restController.getPredicate("NonExistent");
		assertThat(noMatchPredicate.test(exam1)).isFalse();
		log.info("Finishing testGetPredicate");
	}

	@Test
	public void testDeleteExam() {
		log.info("Starting testDeleteExam");
		// Arrange
		when(clinicalExamService.findById(1L)).thenReturn(exam1);

		// Act
		ResponseEntity<Map<String, Object>> result = restController.deleteExam(1L, 1L);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(result.getBody()).isNotNull();
		assertThat(result.getBody().get("success")).isEqualTo(true);
		verify(clinicalExamService).findById(1L);
		verify(clinicalExamService).deleteById(1L);
		log.info("Finishing testDeleteExam");
	}

	@Test
	public void testDeleteExamNotFound() {
		log.info("Starting testDeleteExamNotFound");
		// Arrange
		when(clinicalExamService.findById(999L)).thenReturn(null);

		// Act
		ResponseEntity<Map<String, Object>> result = restController.deleteExam(1L, 999L);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(result.getBody()).isNotNull();
		assertThat(result.getBody().get("success")).isEqualTo(false);
		assertThat(result.getBody().get("error")).isEqualTo("Examen clínico no encontrado");
		verify(clinicalExamService).findById(999L);
		log.info("Finishing testDeleteExamNotFound");
	}

	@Test
	public void testDeleteExamWrongPaciente() {
		log.info("Starting testDeleteExamWrongPaciente");
		// Arrange
		Paciente otherPaciente = new Paciente();
		otherPaciente.setId(2L);
		exam1.setPaciente(otherPaciente);
		when(clinicalExamService.findById(1L)).thenReturn(exam1);

		// Act
		ResponseEntity<Map<String, Object>> result = restController.deleteExam(1L, 1L);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
		assertThat(result.getBody()).isNotNull();
		assertThat(result.getBody().get("success")).isEqualTo(false);
		assertThat(result.getBody().get("error")).isEqualTo("El examen clínico no pertenece al paciente especificado");
		verify(clinicalExamService).findById(1L);
		log.info("Finishing testDeleteExamWrongPaciente");
	}

}

