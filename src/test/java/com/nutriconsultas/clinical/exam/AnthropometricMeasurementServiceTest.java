package com.nutriconsultas.clinical.exam;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import com.nutriconsultas.paciente.NivelPeso;
import com.nutriconsultas.paciente.Paciente;

import lombok.extern.slf4j.Slf4j;

@ExtendWith(MockitoExtension.class)
@Slf4j
@ActiveProfiles("test")
@SuppressWarnings("null")
public class AnthropometricMeasurementServiceTest {

	@InjectMocks
	private AnthropometricMeasurementServiceImpl service;

	@Mock
	private AnthropometricMeasurementRepository repository;

	private Paciente paciente;

	private AnthropometricMeasurement measurement;

	@BeforeEach
	public void setup() {
		log.info("setting up AnthropometricMeasurementService test");

		paciente = new Paciente();
		paciente.setId(1L);
		paciente.setName("Juan Perez");
		paciente.setEmail("juan@example.com");

		measurement = new AnthropometricMeasurement();
		measurement.setId(1L);
		measurement.setPaciente(paciente);
		measurement.setMeasurementDateTime(new Date());
		measurement.setTitle("Medición Antropométrica");
		measurement.setDescription("Medición de rutina");
		measurement.setNotes("Notas de la medición");
		measurement.setPeso(70.0);
		measurement.setEstatura(1.75);
		measurement.setImc(22.86);
		measurement.setNivelPeso(NivelPeso.NORMAL);
		measurement.setCintura(80.0);
		measurement.setCadera(95.0);
		measurement.setCuello(35.0);
		measurement.setBrazo(30.0);
		measurement.setMuslo(55.0);
		measurement.setPorcentajeGrasaCorporal(15.5);
		measurement.setPorcentajeMasaMuscular(45.0);

		log.info("finished setting up AnthropometricMeasurementService test");
	}

	@Test
	public void testFindAll() {
		log.info("starting testFindAll");
		// Arrange
		final List<AnthropometricMeasurement> measurements = new ArrayList<>();
		measurements.add(measurement);
		when(repository.findAll()).thenReturn(measurements);

		// Act
		final List<AnthropometricMeasurement> result = service.findAll();

		// Assert
		assertThat(result).isNotNull();
		assertThat(result).hasSize(1);
		assertThat(result.get(0)).isEqualTo(measurement);
		verify(repository).findAll();
		log.info("finished testFindAll");
	}

	@Test
	public void testFindAllReturnsEmptyList() {
		log.info("starting testFindAllReturnsEmptyList");
		// Arrange
		when(repository.findAll()).thenReturn(new ArrayList<>());

		// Act
		final List<AnthropometricMeasurement> result = service.findAll();

		// Assert
		assertThat(result).isNotNull();
		assertThat(result).isEmpty();
		verify(repository).findAll();
		log.info("finished testFindAllReturnsEmptyList");
	}

	@Test
	public void testFindById() {
		log.info("starting testFindById");
		// Arrange
		when(repository.findById(1L)).thenReturn(Optional.of(measurement));

		// Act
		final AnthropometricMeasurement result = service.findById(1L);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result).isEqualTo(measurement);
		assertThat(result.getId()).isEqualTo(1L);
		assertThat(result.getPaciente()).isEqualTo(paciente);
		verify(repository).findById(1L);
		log.info("finished testFindById");
	}

	@Test
	public void testFindByIdReturnsNullWhenNotFound() {
		log.info("starting testFindByIdReturnsNullWhenNotFound");
		// Arrange
		when(repository.findById(999L)).thenReturn(Optional.empty());

		// Act
		final AnthropometricMeasurement result = service.findById(999L);

		// Assert
		assertThat(result).isNull();
		verify(repository).findById(999L);
		log.info("finished testFindByIdReturnsNullWhenNotFound");
	}

	@Test
	public void testSave() {
		log.info("starting testSave");
		// Arrange
		final AnthropometricMeasurement newMeasurement = new AnthropometricMeasurement();
		newMeasurement.setPaciente(paciente);
		newMeasurement.setMeasurementDateTime(new Date());
		newMeasurement.setTitle("Nueva Medición Antropométrica");
		newMeasurement.setPeso(75.0);
		newMeasurement.setEstatura(1.80);

		when(repository.save(any(AnthropometricMeasurement.class))).thenAnswer(invocation -> {
			final AnthropometricMeasurement savedMeasurement = invocation.getArgument(0);
			savedMeasurement.setId(2L);
			return savedMeasurement;
		});

		// Act
		final AnthropometricMeasurement result = service.save(newMeasurement);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.getId()).isEqualTo(2L);
		assertThat(result.getPaciente()).isEqualTo(paciente);
		assertThat(result.getTitle()).isEqualTo("Nueva Medición Antropométrica");
		assertThat(result.getPeso()).isEqualTo(75.0);
		assertThat(result.getEstatura()).isEqualTo(1.80);
		verify(repository).save(any(AnthropometricMeasurement.class));
		log.info("finished testSave");
	}

	@Test
	public void testDeleteById() {
		log.info("starting testDeleteById");
		// Act
		service.deleteById(1L);

		// Assert
		verify(repository).deleteById(1L);
		log.info("finished testDeleteById");
	}

	@Test
	public void testFindByPacienteId() {
		log.info("starting testFindByPacienteId");
		// Arrange
		final List<AnthropometricMeasurement> measurements = new ArrayList<>();
		measurements.add(measurement);
		when(repository.findByPacienteId(1L)).thenReturn(measurements);

		// Act
		final List<AnthropometricMeasurement> result = service.findByPacienteId(1L);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result).hasSize(1);
		assertThat(result.get(0)).isEqualTo(measurement);
		verify(repository).findByPacienteId(1L);
		log.info("finished testFindByPacienteId");
	}

	@Test
	public void testFindByPacienteIdReturnsEmptyList() {
		log.info("starting testFindByPacienteIdReturnsEmptyList");
		// Arrange
		when(repository.findByPacienteId(999L)).thenReturn(new ArrayList<>());

		// Act
		final List<AnthropometricMeasurement> result = service.findByPacienteId(999L);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result).isEmpty();
		verify(repository).findByPacienteId(999L);
		log.info("finished testFindByPacienteIdReturnsEmptyList");
	}

}

