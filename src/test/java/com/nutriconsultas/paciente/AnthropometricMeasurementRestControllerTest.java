package com.nutriconsultas.paciente;

import com.nutriconsultas.clinical.exam.AnthropometricMeasurement;
import com.nutriconsultas.clinical.exam.AnthropometricMeasurementService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Date;
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
public class AnthropometricMeasurementRestControllerTest {

	@InjectMocks
	private AnthropometricMeasurementRestController restController;

	@Mock
	private AnthropometricMeasurementService anthropometricMeasurementService;

	private Paciente paciente;

	private AnthropometricMeasurement measurement1;

	private AnthropometricMeasurement measurement2;

	@BeforeEach
	public void setup() {
		log.info("setting up AnthropometricMeasurementRestController test");

		paciente = new Paciente();
		paciente.setId(1L);
		paciente.setName("Test Paciente");

		measurement1 = new AnthropometricMeasurement();
		measurement1.setId(1L);
		measurement1.setTitle("Medición Antropométrica 1");
		measurement1.setMeasurementDateTime(new Date(System.currentTimeMillis() - 86400000)); // Yesterday
		measurement1.setPaciente(paciente);
		// Use convenience methods which will create category objects
		measurement1.setPeso(70.0);
		measurement1.setEstatura(1.75);
		measurement1.setImc(22.86);
		measurement1.setCintura(80.0);
		measurement1.setCadera(95.0);
		measurement1.setPorcentajeGrasaCorporal(15.5);

		measurement2 = new AnthropometricMeasurement();
		measurement2.setId(2L);
		measurement2.setTitle("Medición Antropométrica 2");
		measurement2.setMeasurementDateTime(new Date()); // Today
		measurement2.setPaciente(paciente);
		measurement2.setPeso(71.0);
		measurement2.setEstatura(1.75);
		measurement2.setImc(23.18);
		measurement2.setCintura(81.0);
		measurement2.setCadera(96.0);
		measurement2.setPorcentajeGrasaCorporal(16.0);

		log.info("finished setting up AnthropometricMeasurementRestController test");
	}

	@Test
	public void testGetPageArray() {
		log.info("Starting testGetPageArray");
		// Arrange
		when(anthropometricMeasurementService.findByPacienteId(1L))
			.thenReturn(Arrays.asList(measurement1, measurement2));

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
		assertThat(result.getData()).hasSize(2);
		verify(anthropometricMeasurementService).findByPacienteId(1L);
		log.info("Finished testGetPageArray");
	}

	@Test
	public void testGetPageArrayWithSearch() {
		log.info("Starting testGetPageArrayWithSearch");
		// Arrange
		when(anthropometricMeasurementService.findByPacienteId(1L))
			.thenReturn(Arrays.asList(measurement1, measurement2));

		PagingRequest pagingRequest = new PagingRequest();
		pagingRequest.setStart(0);
		pagingRequest.setLength(10);
		pagingRequest.setDraw(1);
		pagingRequest.setOrder(Arrays.asList(new Order(0, Direction.asc)));
		pagingRequest.setSearch(new Search("Medición 1", "false"));

		// Act
		PageArray result = restController.getPageArray(pagingRequest, 1L);

		// Assert
		assertThat(result).isNotNull();
		verify(anthropometricMeasurementService).findByPacienteId(1L);
		log.info("Finished testGetPageArrayWithSearch");
	}

	@Test
	public void testDeleteMeasurementSuccess() {
		log.info("Starting testDeleteMeasurementSuccess");
		// Arrange
		when(anthropometricMeasurementService.findById(1L)).thenReturn(measurement1);

		// Act
		ResponseEntity<Map<String, Object>> result = restController.deleteMeasurement(1L, 1L);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(result.getBody()).isNotNull();
		assertThat(result.getBody().get("success")).isEqualTo(true);
		verify(anthropometricMeasurementService).findById(1L);
		verify(anthropometricMeasurementService).deleteById(1L);
		log.info("Finished testDeleteMeasurementSuccess");
	}

	@Test
	public void testDeleteMeasurementNotFound() {
		log.info("Starting testDeleteMeasurementNotFound");
		// Arrange
		when(anthropometricMeasurementService.findById(999L)).thenReturn(null);

		// Act
		ResponseEntity<Map<String, Object>> result = restController.deleteMeasurement(1L, 999L);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(result.getBody()).isNotNull();
		assertThat(result.getBody().get("success")).isEqualTo(false);
		verify(anthropometricMeasurementService).findById(999L);
		log.info("Finished testDeleteMeasurementNotFound");
	}

	@Test
	public void testDeleteMeasurementWrongPaciente() {
		log.info("Starting testDeleteMeasurementWrongPaciente");
		// Arrange
		Paciente otherPaciente = new Paciente();
		otherPaciente.setId(2L);
		measurement1.setPaciente(otherPaciente);
		when(anthropometricMeasurementService.findById(1L)).thenReturn(measurement1);

		// Act
		ResponseEntity<Map<String, Object>> result = restController.deleteMeasurement(1L, 1L);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
		assertThat(result.getBody()).isNotNull();
		assertThat(result.getBody().get("success")).isEqualTo(false);
		verify(anthropometricMeasurementService).findById(1L);
		log.info("Finished testDeleteMeasurementWrongPaciente");
	}

}
