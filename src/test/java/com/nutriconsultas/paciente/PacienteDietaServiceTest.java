package com.nutriconsultas.paciente;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

import com.nutriconsultas.dieta.Dieta;
import com.nutriconsultas.dieta.DietaRepository;

import lombok.extern.slf4j.Slf4j;

@ExtendWith(MockitoExtension.class)
@Slf4j
@ActiveProfiles("test")
@SuppressWarnings("null")
public class PacienteDietaServiceTest {

	@InjectMocks
	private PacienteDietaServiceImpl service;

	@Mock
	private PacienteDietaRepository pacienteDietaRepository;

	@Mock
	private PacienteRepository pacienteRepository;

	@Mock
	private DietaRepository dietaRepository;

	private Paciente paciente;

	private Dieta dieta;

	private PacienteDieta pacienteDieta;

	private static final String TEST_USER_ID = "test-user-id-123";

	@BeforeEach
	public void setup() {
		log.info("setting up PacienteDietaService test");

		paciente = new Paciente();
		paciente.setId(1L);
		paciente.setName("Juan Perez");
		paciente.setEmail("juan@example.com");
		paciente.setUserId(TEST_USER_ID);

		dieta = new Dieta();
		dieta.setId(1L);
		dieta.setNombre("Dieta de Prueba");

		pacienteDieta = new PacienteDieta();
		pacienteDieta.setId(1L);
		pacienteDieta.setPaciente(paciente);
		pacienteDieta.setDieta(dieta);
		pacienteDieta.setStartDate(new Date());
		pacienteDieta.setStatus(PacienteDietaStatus.ACTIVE);
		pacienteDieta.setNotes("Notas de prueba");

		log.info("finished setting up PacienteDietaService test");
	}

	@Test
	public void testAssignDieta() {
		log.info("starting testAssignDieta");
		// Arrange
		when(pacienteRepository.findByIdAndUserId(1L, TEST_USER_ID)).thenReturn(Optional.of(paciente));
		when(dietaRepository.findById(1L)).thenReturn(Optional.of(dieta));
		when(pacienteDietaRepository.save(any(PacienteDieta.class))).thenAnswer(invocation -> {
			final PacienteDieta pd = invocation.getArgument(0);
			// Simulate database assigning ID
			pd.setId(1L);
			return pd;
		});

		final Date startDate = new Date();
		final Date endDate = new Date(System.currentTimeMillis() + 86400000); // Tomorrow
		final PacienteDieta newAssignment = new PacienteDieta();
		newAssignment.setStartDate(startDate);
		newAssignment.setEndDate(endDate);
		newAssignment.setStatus(PacienteDietaStatus.ACTIVE);
		newAssignment.setNotes("Nueva asignación");

		// Act
		final PacienteDieta result = service.assignDieta(1L, 1L, newAssignment, TEST_USER_ID);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.getId()).isNotNull(); // ID assigned by repository
		assertThat(result.getPaciente()).isEqualTo(paciente);
		assertThat(result.getDieta()).isEqualTo(dieta);
		assertThat(result.getStartDate()).isEqualTo(startDate);
		assertThat(result.getEndDate()).isEqualTo(endDate);
		assertThat(result.getStatus()).isEqualTo(PacienteDietaStatus.ACTIVE);
		assertThat(result.getNotes()).isEqualTo("Nueva asignación");
		verify(pacienteRepository).findByIdAndUserId(1L, TEST_USER_ID);
		verify(dietaRepository).findById(1L);
		verify(pacienteDietaRepository).save(any(PacienteDieta.class));
		log.info("finished testAssignDieta");
	}

	@Test
	public void testAssignDietaWithDefaultStatus() {
		log.info("starting testAssignDietaWithDefaultStatus");
		// Arrange
		when(pacienteRepository.findByIdAndUserId(1L, TEST_USER_ID)).thenReturn(Optional.of(paciente));
		when(dietaRepository.findById(1L)).thenReturn(Optional.of(dieta));
		when(pacienteDietaRepository.save(any(PacienteDieta.class))).thenAnswer(invocation -> {
			final PacienteDieta pd = invocation.getArgument(0);
			pd.setId(1L);
			return pd;
		});

		final PacienteDieta newAssignment = new PacienteDieta();
		newAssignment.setStartDate(new Date());
		newAssignment.setStatus(null); // No status set

		// Act
		final PacienteDieta result = service.assignDieta(1L, 1L, newAssignment, TEST_USER_ID);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.getStatus()).isEqualTo(PacienteDietaStatus.ACTIVE);
		assertThat(result.getPaciente()).isEqualTo(paciente);
		assertThat(result.getDieta()).isEqualTo(dieta);
		log.info("finished testAssignDietaWithDefaultStatus");
	}

	@Test
	public void testAssignDietaThrowsExceptionWhenPacienteNotFound() {
		log.info("starting testAssignDietaThrowsExceptionWhenPacienteNotFound");
		// Arrange
		when(pacienteRepository.findByIdAndUserId(1L, TEST_USER_ID)).thenReturn(Optional.empty());

		final PacienteDieta newAssignment = new PacienteDieta();
		newAssignment.setStartDate(new Date());

		// Act & Assert
		assertThatThrownBy(() -> service.assignDieta(1L, 1L, newAssignment, TEST_USER_ID))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("No se ha encontrado paciente");
		log.info("finished testAssignDietaThrowsExceptionWhenPacienteNotFound");
	}

	@Test
	public void testAssignDietaThrowsExceptionWhenDietaNotFound() {
		log.info("starting testAssignDietaThrowsExceptionWhenDietaNotFound");
		// Arrange
		when(pacienteRepository.findByIdAndUserId(1L, TEST_USER_ID)).thenReturn(Optional.of(paciente));
		when(dietaRepository.findById(1L)).thenReturn(Optional.empty());

		final PacienteDieta newAssignment = new PacienteDieta();
		newAssignment.setStartDate(new Date());

		// Act & Assert
		assertThatThrownBy(() -> service.assignDieta(1L, 1L, newAssignment, TEST_USER_ID))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("No se ha encontrado dieta");
		log.info("finished testAssignDietaThrowsExceptionWhenDietaNotFound");
	}

	@Test
	public void testAssignDietaCreatesNewObjectEvenWhenInputHasId() {
		log.info("starting testAssignDietaCreatesNewObjectEvenWhenInputHasId");
		// Arrange - This test verifies that even if the input object has an ID (from form
		// binding),
		// a new object is created to avoid StaleObjectStateException
		when(pacienteRepository.findByIdAndUserId(1L, TEST_USER_ID)).thenReturn(Optional.of(paciente));
		when(dietaRepository.findById(1L)).thenReturn(Optional.of(dieta));
		when(pacienteDietaRepository.save(any(PacienteDieta.class))).thenAnswer(invocation -> {
			final PacienteDieta pd = invocation.getArgument(0);
			// Verify that the saved object has no ID (is a new entity)
			assertThat(pd.getId()).isNull();
			pd.setId(2L); // Simulate database assigning new ID
			return pd;
		});

		final Date startDate = new Date();
		final PacienteDieta inputWithId = new PacienteDieta();
		inputWithId.setId(1L); // Input has an ID (simulating form binding issue)
		inputWithId.setStartDate(startDate);
		inputWithId.setStatus(PacienteDietaStatus.ACTIVE);
		inputWithId.setNotes("Test notes");

		// Act
		final PacienteDieta result = service.assignDieta(1L, 1L, inputWithId, TEST_USER_ID);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.getId()).isEqualTo(2L); // New ID assigned by repository
		assertThat(result.getPaciente()).isEqualTo(paciente);
		assertThat(result.getDieta()).isEqualTo(dieta);
		assertThat(result.getStartDate()).isEqualTo(startDate);
		assertThat(result.getStatus()).isEqualTo(PacienteDietaStatus.ACTIVE);
		assertThat(result.getNotes()).isEqualTo("Test notes");
		verify(pacienteRepository).findByIdAndUserId(1L, TEST_USER_ID);
		verify(dietaRepository).findById(1L);
		verify(pacienteDietaRepository).save(any(PacienteDieta.class));
		log.info("finished testAssignDietaCreatesNewObjectEvenWhenInputHasId");
	}

	@Test
	public void testAssignDietaCopiesAllFieldsCorrectly() {
		log.info("starting testAssignDietaCopiesAllFieldsCorrectly");
		// Arrange
		when(pacienteRepository.findByIdAndUserId(1L, TEST_USER_ID)).thenReturn(Optional.of(paciente));
		when(dietaRepository.findById(1L)).thenReturn(Optional.of(dieta));
		when(pacienteDietaRepository.save(any(PacienteDieta.class))).thenAnswer(invocation -> {
			final PacienteDieta pd = invocation.getArgument(0);
			pd.setId(1L);
			return pd;
		});

		final Date startDate = new Date();
		final Date endDate = new Date(System.currentTimeMillis() + 86400000);
		final String notes = "Detailed notes about the diet assignment";

		final PacienteDieta input = new PacienteDieta();
		input.setStartDate(startDate);
		input.setEndDate(endDate);
		input.setStatus(PacienteDietaStatus.COMPLETED);
		input.setNotes(notes);

		// Act
		final PacienteDieta result = service.assignDieta(1L, 1L, input, TEST_USER_ID);

		// Assert - Verify all fields are copied correctly
		assertThat(result).isNotNull();
		assertThat(result.getStartDate()).isEqualTo(startDate);
		assertThat(result.getEndDate()).isEqualTo(endDate);
		assertThat(result.getStatus()).isEqualTo(PacienteDietaStatus.COMPLETED);
		assertThat(result.getNotes()).isEqualTo(notes);
		assertThat(result.getPaciente()).isEqualTo(paciente);
		assertThat(result.getDieta()).isEqualTo(dieta);
		log.info("finished testAssignDietaCopiesAllFieldsCorrectly");
	}

	@Test
	public void testUpdateAssignment() {
		log.info("starting testUpdateAssignment");
		// Arrange
		when(pacienteDietaRepository.findById(1L)).thenReturn(Optional.of(pacienteDieta));
		when(pacienteDietaRepository.save(any(PacienteDieta.class))).thenReturn(pacienteDieta);

		final PacienteDieta updateData = new PacienteDieta();
		final Date newEndDate = new Date();
		updateData.setEndDate(newEndDate);
		updateData.setStatus(PacienteDietaStatus.COMPLETED);
		updateData.setNotes("Notas actualizadas");

		// Act
		final PacienteDieta result = service.updateAssignment(1L, updateData);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.getEndDate()).isEqualTo(newEndDate);
		assertThat(result.getStatus()).isEqualTo(PacienteDietaStatus.COMPLETED);
		assertThat(result.getNotes()).isEqualTo("Notas actualizadas");
		verify(pacienteDietaRepository).findById(1L);
		verify(pacienteDietaRepository).save(any(PacienteDieta.class));
		log.info("finished testUpdateAssignment");
	}

	@Test
	public void testUpdateAssignmentThrowsExceptionWhenNotFound() {
		log.info("starting testUpdateAssignmentThrowsExceptionWhenNotFound");
		// Arrange
		when(pacienteDietaRepository.findById(1L)).thenReturn(Optional.empty());

		final PacienteDieta updateData = new PacienteDieta();

		// Act & Assert
		assertThatThrownBy(() -> service.updateAssignment(1L, updateData)).isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("No se ha encontrado asignación");
		log.info("finished testUpdateAssignmentThrowsExceptionWhenNotFound");
	}

	@Test
	public void testCancelAssignment() {
		log.info("starting testCancelAssignment");
		// Arrange
		when(pacienteDietaRepository.findById(1L)).thenReturn(Optional.of(pacienteDieta));
		when(pacienteDietaRepository.save(any(PacienteDieta.class))).thenReturn(pacienteDieta);

		// Act
		service.cancelAssignment(1L);

		// Assert
		assertThat(pacienteDieta.getStatus()).isEqualTo(PacienteDietaStatus.CANCELLED);
		verify(pacienteDietaRepository).findById(1L);
		verify(pacienteDietaRepository).save(any(PacienteDieta.class));
		log.info("finished testCancelAssignment");
	}

	@Test
	public void testCancelAssignmentThrowsExceptionWhenNotFound() {
		log.info("starting testCancelAssignmentThrowsExceptionWhenNotFound");
		// Arrange
		when(pacienteDietaRepository.findById(1L)).thenReturn(Optional.empty());

		// Act & Assert
		assertThatThrownBy(() -> service.cancelAssignment(1L)).isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("No se ha encontrado asignación");
		log.info("finished testCancelAssignmentThrowsExceptionWhenNotFound");
	}

	@Test
	public void testFindByPacienteId() {
		log.info("starting testFindByPacienteId");
		// Arrange
		final List<PacienteDieta> assignments = new ArrayList<>();
		assignments.add(pacienteDieta);
		when(pacienteDietaRepository.findByPacienteIdOrderByStartDateDesc(1L)).thenReturn(assignments);

		// Act
		final List<PacienteDieta> result = service.findByPacienteId(1L);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result).hasSize(1);
		assertThat(result.get(0)).isEqualTo(pacienteDieta);
		verify(pacienteDietaRepository).findByPacienteIdOrderByStartDateDesc(1L);
		log.info("finished testFindByPacienteId");
	}

	@Test
	public void testFindActiveByPacienteId() {
		log.info("starting testFindActiveByPacienteId");
		// Arrange
		final List<PacienteDieta> activeAssignments = new ArrayList<>();
		activeAssignments.add(pacienteDieta);
		when(pacienteDietaRepository.findByPacienteIdAndStatus(1L, PacienteDietaStatus.ACTIVE))
			.thenReturn(activeAssignments);

		// Act
		final List<PacienteDieta> result = service.findActiveByPacienteId(1L);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result).hasSize(1);
		assertThat(result.get(0)).isEqualTo(pacienteDieta);
		verify(pacienteDietaRepository).findByPacienteIdAndStatus(1L, PacienteDietaStatus.ACTIVE);
		log.info("finished testFindActiveByPacienteId");
	}

	@Test
	public void testFindById() {
		log.info("starting testFindById");
		// Arrange
		when(pacienteDietaRepository.findById(1L)).thenReturn(Optional.of(pacienteDieta));

		// Act
		final PacienteDieta result = service.findById(1L);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result).isEqualTo(pacienteDieta);
		verify(pacienteDietaRepository).findById(1L);
		log.info("finished testFindById");
	}

	@Test
	public void testFindByIdThrowsExceptionWhenNotFound() {
		log.info("starting testFindByIdThrowsExceptionWhenNotFound");
		// Arrange
		when(pacienteDietaRepository.findById(1L)).thenReturn(Optional.empty());

		// Act & Assert
		assertThatThrownBy(() -> service.findById(1L)).isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("No se ha encontrado asignación");
		log.info("finished testFindByIdThrowsExceptionWhenNotFound");
	}

}
