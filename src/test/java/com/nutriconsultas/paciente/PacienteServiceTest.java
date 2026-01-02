package com.nutriconsultas.paciente;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import lombok.extern.slf4j.Slf4j;

@ExtendWith(MockitoExtension.class)
@Slf4j
@ActiveProfiles("test")
@SuppressWarnings("null")
public class PacienteServiceTest {

	@InjectMocks
	private PacienteServiceImpl service;

	@Mock
	private PacienteRepository repository;

	private Paciente paciente1;

	private Paciente paciente2;

	private static final String TEST_USER_ID = "test-user-id-123";

	@BeforeEach
	public void setup() {
		log.info("setting up PacienteService test");

		paciente1 = new Paciente();
		paciente1.setId(1L);
		paciente1.setName("Juan Perez");
		paciente1.setEmail("juan@example.com");
		paciente1.setPhone("1234567890");
		paciente1.setUserId(TEST_USER_ID);
		final LocalDate dob1 = LocalDate.now().minusYears(30);
		paciente1.setDob(Date.from(dob1.atStartOfDay(ZoneId.systemDefault()).toInstant()));
		paciente1.setGender("M");
		paciente1.setResponsibleName("Maria Perez");

		paciente2 = new Paciente();
		paciente2.setId(2L);
		paciente2.setName("Maria Garcia");
		paciente2.setEmail("maria@example.com");
		paciente2.setPhone("0987654321");
		paciente2.setUserId(TEST_USER_ID);
		final LocalDate dob2 = LocalDate.now().minusYears(25);
		paciente2.setDob(Date.from(dob2.atStartOfDay(ZoneId.systemDefault()).toInstant()));
		paciente2.setGender("F");
		paciente2.setResponsibleName("Carlos Garcia");

		log.info("finished setting up PacienteService test");
	}

	@Test
	public void testFindAllByUserId() {
		log.info("starting testFindAllByUserId");
		// Arrange
		when(repository.findByUserId(TEST_USER_ID)).thenReturn(Arrays.asList(paciente1, paciente2));

		// Act
		final List<Paciente> result = service.findAllByUserId(TEST_USER_ID);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result).hasSize(2);
		assertThat(result).contains(paciente1, paciente2);
		verify(repository).findByUserId(TEST_USER_ID);
		log.info("finished testFindAllByUserId");
	}

	@Test
	public void testFindAllByUserIdWithPagination() {
		log.info("starting testFindAllByUserIdWithPagination");
		// Arrange
		final Pageable pageable = PageRequest.of(0, 10);
		final Page<Paciente> page = new PageImpl<>(Arrays.asList(paciente1, paciente2), pageable, 2);
		when(repository.findByUserId(eq(TEST_USER_ID), any(Pageable.class))).thenReturn(page);

		// Act
		final Page<Paciente> result = service.findAllByUserId(TEST_USER_ID, pageable);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.getContent()).hasSize(2);
		assertThat(result.getContent()).contains(paciente1, paciente2);
		assertThat(result.getTotalElements()).isEqualTo(2);
		verify(repository).findByUserId(TEST_USER_ID, pageable);
		log.info("finished testFindAllByUserIdWithPagination");
	}

	@Test
	public void testFindAllByUserIdAndSearchTerm() {
		log.info("starting testFindAllByUserIdAndSearchTerm");
		// Arrange
		final Pageable pageable = PageRequest.of(0, 10);
		final Page<Paciente> page = new PageImpl<>(Arrays.asList(paciente1), pageable, 1);
		when(repository.findByUserIdAndSearchTerm(eq(TEST_USER_ID), eq("%juan%"), any(Pageable.class)))
			.thenReturn(page);

		// Act
		final Page<Paciente> result = service.findAllByUserIdAndSearchTerm(TEST_USER_ID, "juan", pageable);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.getContent()).hasSize(1);
		assertThat(result.getContent()).contains(paciente1);
		verify(repository).findByUserIdAndSearchTerm(TEST_USER_ID, "%juan%", pageable);
		log.info("finished testFindAllByUserIdAndSearchTerm");
	}

	@Test
	public void testFindAllByUserIdAndSearchTermWithEmptyResult() {
		log.info("starting testFindAllByUserIdAndSearchTermWithEmptyResult");
		// Arrange
		final Pageable pageable = PageRequest.of(0, 10);
		final Page<Paciente> page = new PageImpl<>(new ArrayList<>(), pageable, 0);
		when(repository.findByUserIdAndSearchTerm(eq(TEST_USER_ID), eq("%nonexistent%"), any(Pageable.class)))
			.thenReturn(page);

		// Act
		final Page<Paciente> result = service.findAllByUserIdAndSearchTerm(TEST_USER_ID, "nonexistent", pageable);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.getContent()).isEmpty();
		verify(repository).findByUserIdAndSearchTerm(TEST_USER_ID, "%nonexistent%", pageable);
		log.info("finished testFindAllByUserIdAndSearchTermWithEmptyResult");
	}

	@Test
	public void testCountByUserId() {
		log.info("starting testCountByUserId");
		// Arrange
		when(repository.countByUserId(TEST_USER_ID)).thenReturn(2L);

		// Act
		final long result = service.countByUserId(TEST_USER_ID);

		// Assert
		assertThat(result).isEqualTo(2L);
		verify(repository).countByUserId(TEST_USER_ID);
		log.info("finished testCountByUserId");
	}

	@Test
	public void testCountByUserIdAndSearchTerm() {
		log.info("starting testCountByUserIdAndSearchTerm");
		// Arrange
		when(repository.countByUserIdAndSearchTerm(TEST_USER_ID, "%juan%")).thenReturn(1L);

		// Act
		final long result = service.countByUserIdAndSearchTerm(TEST_USER_ID, "juan");

		// Assert
		assertThat(result).isEqualTo(1L);
		verify(repository).countByUserIdAndSearchTerm(TEST_USER_ID, "%juan%");
		log.info("finished testCountByUserIdAndSearchTerm");
	}

	@Test
	public void testFindByIdAndUserId() {
		log.info("starting testFindByIdAndUserId");
		// Arrange
		when(repository.findByIdAndUserId(1L, TEST_USER_ID)).thenReturn(java.util.Optional.of(paciente1));

		// Act
		final Paciente result = service.findByIdAndUserId(1L, TEST_USER_ID);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result).isEqualTo(paciente1);
		verify(repository).findByIdAndUserId(1L, TEST_USER_ID);
		log.info("finished testFindByIdAndUserId");
	}

	@Test
	public void testSave() {
		log.info("starting testSave");
		// Arrange
		when(repository.save(paciente1)).thenReturn(paciente1);

		// Act
		final Paciente result = service.save(paciente1);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result).isEqualTo(paciente1);
		verify(repository).save(paciente1);
		log.info("finished testSave");
	}

	@Test
	public void testDelete() {
		log.info("starting testDelete");
		// Act
		service.delete(1L);

		// Assert
		verify(repository).deleteById(1L);
		log.info("finished testDelete");
	}

	@Test
	public void testDeleteByIdAndUserId() {
		log.info("starting testDeleteByIdAndUserId");
		// Arrange
		when(repository.findByIdAndUserId(1L, TEST_USER_ID)).thenReturn(java.util.Optional.of(paciente1));

		// Act
		service.deleteByIdAndUserId(1L, TEST_USER_ID);

		// Assert
		verify(repository).findByIdAndUserId(1L, TEST_USER_ID);
		verify(repository).deleteById(1L);
		log.info("finished testDeleteByIdAndUserId");
	}

	@Test
	public void testDeleteByIdAndUserIdNotFound() {
		log.info("starting testDeleteByIdAndUserIdNotFound");
		// Arrange
		when(repository.findByIdAndUserId(1L, TEST_USER_ID)).thenReturn(java.util.Optional.empty());

		// Act
		service.deleteByIdAndUserId(1L, TEST_USER_ID);

		// Assert
		verify(repository).findByIdAndUserId(1L, TEST_USER_ID);
		verify(repository, org.mockito.Mockito.never()).deleteById(1L);
		log.info("finished testDeleteByIdAndUserIdNotFound");
	}

}
