package com.nutriconsultas.paciente;

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

import lombok.extern.slf4j.Slf4j;

@ExtendWith(MockitoExtension.class)
@Slf4j
@ActiveProfiles("test")
@SuppressWarnings("null")
public class ClinicalExamServiceTest {

	@InjectMocks
	private ClinicalExamServiceImpl service;

	@Mock
	private ClinicalExamRepository repository;

	private Paciente paciente;

	private ClinicalExam exam;

	@BeforeEach
	public void setup() {
		log.info("setting up ClinicalExamService test");

		paciente = new Paciente();
		paciente.setId(1L);
		paciente.setName("Juan Perez");
		paciente.setEmail("juan@example.com");

		exam = new ClinicalExam();
		exam.setId(1L);
		exam.setPaciente(paciente);
		exam.setExamDateTime(new Date());
		exam.setTitle("Examen Clínico");
		exam.setDescription("Examen de rutina");
		exam.setSummaryNotes("Notas del examen");
		exam.setPeso(70.0);
		exam.setEstatura(1.75);
		exam.setImc(22.86);
		exam.setSistolica(120);
		exam.setDiastolica(80);
		exam.setPulso(72);
		exam.setHdl(50.0);
		exam.setLdl(100.0);
		exam.setGlucosa(95.0);

		log.info("finished setting up ClinicalExamService test");
	}

	@Test
	public void testFindAll() {
		log.info("starting testFindAll");
		// Arrange
		final List<ClinicalExam> exams = new ArrayList<>();
		exams.add(exam);
		when(repository.findAll()).thenReturn(exams);

		// Act
		final List<ClinicalExam> result = service.findAll();

		// Assert
		assertThat(result).isNotNull();
		assertThat(result).hasSize(1);
		assertThat(result.get(0)).isEqualTo(exam);
		verify(repository).findAll();
		log.info("finished testFindAll");
	}

	@Test
	public void testFindAllReturnsEmptyList() {
		log.info("starting testFindAllReturnsEmptyList");
		// Arrange
		when(repository.findAll()).thenReturn(new ArrayList<>());

		// Act
		final List<ClinicalExam> result = service.findAll();

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
		when(repository.findById(1L)).thenReturn(Optional.of(exam));

		// Act
		final ClinicalExam result = service.findById(1L);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result).isEqualTo(exam);
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
		final ClinicalExam result = service.findById(999L);

		// Assert
		assertThat(result).isNull();
		verify(repository).findById(999L);
		log.info("finished testFindByIdReturnsNullWhenNotFound");
	}

	@Test
	public void testSave() {
		log.info("starting testSave");
		// Arrange
		final ClinicalExam newExam = new ClinicalExam();
		newExam.setPaciente(paciente);
		newExam.setExamDateTime(new Date());
		newExam.setTitle("Nuevo Examen Clínico");
		newExam.setPeso(75.0);
		newExam.setEstatura(1.80);

		when(repository.save(any(ClinicalExam.class))).thenAnswer(invocation -> {
			final ClinicalExam savedExam = invocation.getArgument(0);
			savedExam.setId(2L);
			return savedExam;
		});

		// Act
		final ClinicalExam result = service.save(newExam);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.getId()).isEqualTo(2L);
		assertThat(result.getPaciente()).isEqualTo(paciente);
		assertThat(result.getTitle()).isEqualTo("Nuevo Examen Clínico");
		assertThat(result.getPeso()).isEqualTo(75.0);
		assertThat(result.getEstatura()).isEqualTo(1.80);
		verify(repository).save(any(ClinicalExam.class));
		log.info("finished testSave");
	}

	@Test
	public void testSaveUpdatesExistingExam() {
		log.info("starting testSaveUpdatesExistingExam");
		// Arrange
		final ClinicalExam existingExam = new ClinicalExam();
		existingExam.setId(1L);
		existingExam.setPaciente(paciente);
		existingExam.setExamDateTime(new Date());
		existingExam.setTitle("Examen Original");
		existingExam.setPeso(70.0);

		// Update the exam
		existingExam.setPeso(72.0);
		existingExam.setTitle("Examen Actualizado");

		when(repository.save(any(ClinicalExam.class))).thenReturn(existingExam);

		// Act
		final ClinicalExam result = service.save(existingExam);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.getId()).isEqualTo(1L);
		assertThat(result.getPeso()).isEqualTo(72.0);
		assertThat(result.getTitle()).isEqualTo("Examen Actualizado");
		verify(repository).save(any(ClinicalExam.class));
		log.info("finished testSaveUpdatesExistingExam");
	}

	@Test
	public void testDeleteById() {
		log.info("starting testDeleteById");
		// Arrange
		org.mockito.Mockito.doNothing().when(repository).deleteById(1L);

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
		final List<ClinicalExam> exams = new ArrayList<>();
		exams.add(exam);

		final ClinicalExam exam2 = new ClinicalExam();
		exam2.setId(2L);
		exam2.setPaciente(paciente);
		exam2.setExamDateTime(new Date(System.currentTimeMillis() + 86400000));
		exam2.setTitle("Segundo Examen");
		exams.add(exam2);

		when(repository.findByPacienteId(1L)).thenReturn(exams);

		// Act
		final List<ClinicalExam> result = service.findByPacienteId(1L);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result).hasSize(2);
		assertThat(result).contains(exam, exam2);
		verify(repository).findByPacienteId(1L);
		log.info("finished testFindByPacienteId");
	}

	@Test
	public void testFindByPacienteIdReturnsEmptyList() {
		log.info("starting testFindByPacienteIdReturnsEmptyList");
		// Arrange
		when(repository.findByPacienteId(999L)).thenReturn(new ArrayList<>());

		// Act
		final List<ClinicalExam> result = service.findByPacienteId(999L);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result).isEmpty();
		verify(repository).findByPacienteId(999L);
		log.info("finished testFindByPacienteIdReturnsEmptyList");
	}

	@Test
	public void testSaveWithAllBiochemicalFields() {
		log.info("starting testSaveWithAllBiochemicalFields");
		// Arrange
		final ClinicalExam completeExam = new ClinicalExam();
		completeExam.setPaciente(paciente);
		completeExam.setExamDateTime(new Date());
		completeExam.setTitle("Examen Completo");
		// Vital signs
		completeExam.setPeso(70.0);
		completeExam.setEstatura(1.75);
		completeExam.setSistolica(120);
		completeExam.setDiastolica(80);
		completeExam.setPulso(72);
		completeExam.setSpo2(98.0);
		completeExam.setTemperatura(36.5);
		// Lipid profile
		completeExam.setHdl(50.0);
		completeExam.setLdl(100.0);
		completeExam.setTrigliceridos(150.0);
		completeExam.setColesterolTotal(200.0);
		// Blood chemistry
		completeExam.setGlucosa(95.0);
		completeExam.setHba1c(5.5);
		completeExam.setCreatinina(1.0);
		completeExam.setUrea(15.0);
		completeExam.setBun(10.0);
		// Liver function
		completeExam.setAlt(25.0);
		completeExam.setAst(30.0);
		completeExam.setBilirrubina(0.8);
		// Complete blood count
		completeExam.setHemoglobina(14.0);
		completeExam.setHematocrito(42.0);
		completeExam.setLeucocitos(7.0);
		completeExam.setPlaquetas(250.0);
		// Other tests
		completeExam.setVitaminaD(35.0);
		completeExam.setVitaminaB12(500.0);
		completeExam.setHierro(100.0);
		completeExam.setFerritina(50.0);

		when(repository.save(any(ClinicalExam.class))).thenAnswer(invocation -> {
			final ClinicalExam savedExam = invocation.getArgument(0);
			savedExam.setId(3L);
			return savedExam;
		});

		// Act
		final ClinicalExam result = service.save(completeExam);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.getId()).isEqualTo(3L);
		// Verify vital signs
		assertThat(result.getPeso()).isEqualTo(70.0);
		assertThat(result.getEstatura()).isEqualTo(1.75);
		assertThat(result.getSistolica()).isEqualTo(120);
		assertThat(result.getDiastolica()).isEqualTo(80);
		// Verify lipid profile
		assertThat(result.getHdl()).isEqualTo(50.0);
		assertThat(result.getLdl()).isEqualTo(100.0);
		assertThat(result.getTrigliceridos()).isEqualTo(150.0);
		assertThat(result.getColesterolTotal()).isEqualTo(200.0);
		// Verify blood chemistry
		assertThat(result.getGlucosa()).isEqualTo(95.0);
		assertThat(result.getHba1c()).isEqualTo(5.5);
		// Verify liver function
		assertThat(result.getAlt()).isEqualTo(25.0);
		assertThat(result.getAst()).isEqualTo(30.0);
		// Verify complete blood count
		assertThat(result.getHemoglobina()).isEqualTo(14.0);
		assertThat(result.getHematocrito()).isEqualTo(42.0);
		// Verify other tests
		assertThat(result.getVitaminaD()).isEqualTo(35.0);
		assertThat(result.getVitaminaB12()).isEqualTo(500.0);
		verify(repository).save(any(ClinicalExam.class));
		log.info("finished testSaveWithAllBiochemicalFields");
	}

}

