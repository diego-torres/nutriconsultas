package com.nutriconsultas.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import com.nutriconsultas.alimentos.Alimento;
import com.nutriconsultas.alimentos.AlimentosRepository;
import com.nutriconsultas.calendar.CalendarEvent;
import com.nutriconsultas.calendar.CalendarEventRepository;
import com.nutriconsultas.clinical.exam.ClinicalExam;
import com.nutriconsultas.clinical.exam.ClinicalExamRepository;
import com.nutriconsultas.paciente.Paciente;
import com.nutriconsultas.paciente.PacienteRepository;
import com.nutriconsultas.platillos.Platillo;
import com.nutriconsultas.platillos.PlatilloRepository;

import lombok.extern.slf4j.Slf4j;

@ExtendWith(MockitoExtension.class)
@Slf4j
@ActiveProfiles("test")
public class SearchServiceTest {

	@InjectMocks
	private SearchServiceImpl service;

	@Mock
	private PacienteRepository pacienteRepository;

	@Mock
	private AlimentosRepository alimentosRepository;

	@Mock
	private PlatilloRepository platilloRepository;

	@Mock
	private CalendarEventRepository calendarEventRepository;

	@Mock
	private ClinicalExamRepository clinicalExamRepository;

	private static final String TEST_USER_ID = "test-user-id-123";

	private Paciente paciente;

	private Alimento alimento;

	private Platillo platillo;

	private CalendarEvent calendarEvent;

	private ClinicalExam clinicalExam;

	@BeforeEach
	public void setup() {
		log.info("setting up SearchService test");

		// Create test paciente
		paciente = new Paciente();
		paciente.setId(1L);
		paciente.setName("Juan Perez");
		paciente.setEmail("juan@example.com");
		paciente.setPhone("1234567890");
		paciente.setUserId(TEST_USER_ID);

		// Create test alimento
		alimento = new Alimento();
		alimento.setId(1L);
		alimento.setNombreAlimento("Manzana");
		alimento.setClasificacion("Fruta");

		// Create test platillo
		platillo = new Platillo();
		platillo.setId(1L);
		platillo.setName("Ensalada");
		platillo.setDescription("Ensalada fresca");

		// Create test calendar event
		calendarEvent = new CalendarEvent();
		calendarEvent.setId(1L);
		calendarEvent.setPaciente(paciente);
		calendarEvent.setTitle("Consulta");
		calendarEvent.setDescription("Consulta de rutina");
		calendarEvent.setEventDateTime(new Date());

		// Create test clinical exam
		clinicalExam = new ClinicalExam();
		clinicalExam.setId(1L);
		clinicalExam.setPaciente(paciente);
		clinicalExam.setTitle("Examen Clínico");
		clinicalExam.setDescription("Examen de rutina");
		clinicalExam.setSummaryNotes("Notas del examen");
		clinicalExam.setExamDateTime(new Date());

		log.info("finished setting up SearchService test");
	}

	@Test
	public void testSearchWithResults() {
		log.info("starting testSearchWithResults");
		// Arrange
		final String query = "Juan";
		when(pacienteRepository.findByUserIdAndSearchTerm(eq(TEST_USER_ID), any(String.class)))
			.thenReturn(Arrays.asList(paciente));
		when(alimentosRepository.findByNombreAlimentoContainingIgnoreCase(query)).thenReturn(new ArrayList<>());
		when(platilloRepository.findByNameContainingIgnoreCase(query)).thenReturn(new ArrayList<>());
		when(calendarEventRepository.findByUserIdAndSearchTerm(eq(TEST_USER_ID), any(String.class)))
			.thenReturn(Arrays.asList(calendarEvent));
		when(clinicalExamRepository.findByUserIdAndSearchTerm(eq(TEST_USER_ID), any(String.class)))
			.thenReturn(Arrays.asList(clinicalExam));

		// Act
		final SearchResponse result = service.search(query, TEST_USER_ID, "pacientes", 1);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.getQuery()).isEqualTo(query);
		assertThat(result.getPacientes().getResults()).hasSize(1);
		assertThat(result.getPacientes().getResults().get(0).getTitle()).isEqualTo("Juan Perez");
		assertThat(result.getPacientes().getTotalCount()).isEqualTo(1);
		assertThat(result.getCalendarEvents().getResults()).hasSize(1);
		assertThat(result.getCalendarEvents().getTotalCount()).isEqualTo(1);
		assertThat(result.getClinicalExams().getResults()).hasSize(1);
		assertThat(result.getClinicalExams().getTotalCount()).isEqualTo(1);
		assertThat(result.getTotalResults()).isEqualTo(3);

		verify(pacienteRepository).findByUserIdAndSearchTerm(eq(TEST_USER_ID), any(String.class));
		verify(alimentosRepository).findByNombreAlimentoContainingIgnoreCase(query);
		verify(platilloRepository).findByNameContainingIgnoreCase(query);
		verify(calendarEventRepository).findByUserIdAndSearchTerm(eq(TEST_USER_ID), any(String.class));
		verify(clinicalExamRepository).findByUserIdAndSearchTerm(eq(TEST_USER_ID), any(String.class));
		log.info("finished testSearchWithResults");
	}

	@Test
	public void testSearchWithNoResults() {
		log.info("starting testSearchWithNoResults");
		// Arrange
		final String query = "Nonexistent";
		when(pacienteRepository.findByUserIdAndSearchTerm(eq(TEST_USER_ID), any(String.class)))
			.thenReturn(new ArrayList<>());
		when(alimentosRepository.findByNombreAlimentoContainingIgnoreCase(query)).thenReturn(new ArrayList<>());
		when(platilloRepository.findByNameContainingIgnoreCase(query)).thenReturn(new ArrayList<>());
		when(calendarEventRepository.findByUserIdAndSearchTerm(eq(TEST_USER_ID), any(String.class)))
			.thenReturn(new ArrayList<>());
		when(clinicalExamRepository.findByUserIdAndSearchTerm(eq(TEST_USER_ID), any(String.class)))
			.thenReturn(new ArrayList<>());

		// Act
		final SearchResponse result = service.search(query, TEST_USER_ID, "pacientes", 1);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.getQuery()).isEqualTo(query);
		assertThat(result.getTotalResults()).isEqualTo(0);
		assertThat(result.getPacientes().getResults()).isEmpty();
		assertThat(result.getAlimentos().getResults()).isEmpty();
		assertThat(result.getPlatillos().getResults()).isEmpty();
		assertThat(result.getCalendarEvents().getResults()).isEmpty();
		assertThat(result.getClinicalExams().getResults()).isEmpty();
		log.info("finished testSearchWithNoResults");
	}

	@Test
	public void testSearchAlimentos() {
		log.info("starting testSearchAlimentos");
		// Arrange
		final String query = "Manzana";
		when(pacienteRepository.findByUserIdAndSearchTerm(eq(TEST_USER_ID), any(String.class)))
			.thenReturn(new ArrayList<>());
		when(alimentosRepository.findByNombreAlimentoContainingIgnoreCase(query))
			.thenReturn(Arrays.asList(alimento));
		when(platilloRepository.findByNameContainingIgnoreCase(query)).thenReturn(new ArrayList<>());
		when(calendarEventRepository.findByUserIdAndSearchTerm(eq(TEST_USER_ID), any(String.class)))
			.thenReturn(new ArrayList<>());
		when(clinicalExamRepository.findByUserIdAndSearchTerm(eq(TEST_USER_ID), any(String.class)))
			.thenReturn(new ArrayList<>());

		// Act
		final SearchResponse result = service.search(query, TEST_USER_ID, "alimentos", 1);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.getAlimentos().getResults()).hasSize(1);
		assertThat(result.getAlimentos().getResults().get(0).getTitle()).isEqualTo("Manzana");
		assertThat(result.getAlimentos().getTotalCount()).isEqualTo(1);
		assertThat(result.getTotalResults()).isEqualTo(1);
		log.info("finished testSearchAlimentos");
	}

	@Test
	public void testSearchPlatillos() {
		log.info("starting testSearchPlatillos");
		// Arrange
		final String query = "Ensalada";
		when(pacienteRepository.findByUserIdAndSearchTerm(eq(TEST_USER_ID), any(String.class)))
			.thenReturn(new ArrayList<>());
		when(alimentosRepository.findByNombreAlimentoContainingIgnoreCase(query)).thenReturn(new ArrayList<>());
		when(platilloRepository.findByNameContainingIgnoreCase(query)).thenReturn(Arrays.asList(platillo));
		when(calendarEventRepository.findByUserIdAndSearchTerm(eq(TEST_USER_ID), any(String.class)))
			.thenReturn(new ArrayList<>());
		when(clinicalExamRepository.findByUserIdAndSearchTerm(eq(TEST_USER_ID), any(String.class)))
			.thenReturn(new ArrayList<>());

		// Act
		final SearchResponse result = service.search(query, TEST_USER_ID, "platillos", 1);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.getPlatillos().getResults()).hasSize(1);
		assertThat(result.getPlatillos().getResults().get(0).getTitle()).isEqualTo("Ensalada");
		assertThat(result.getPlatillos().getTotalCount()).isEqualTo(1);
		assertThat(result.getTotalResults()).isEqualTo(1);
		log.info("finished testSearchPlatillos");
	}

}