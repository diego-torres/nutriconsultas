package com.nutriconsultas.reports;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import com.nutriconsultas.calendar.CalendarEvent;
import com.nutriconsultas.calendar.CalendarEventRepository;
import com.nutriconsultas.calendar.EventStatus;
import com.nutriconsultas.clinical.exam.AnthropometricMeasurement;
import com.nutriconsultas.clinical.exam.AnthropometricMeasurementRepository;
import com.nutriconsultas.clinical.exam.ClinicalExam;
import com.nutriconsultas.clinical.exam.ClinicalExamRepository;
import com.nutriconsultas.paciente.NivelPeso;
import com.nutriconsultas.paciente.Paciente;
import com.nutriconsultas.paciente.PacienteDieta;
import com.nutriconsultas.paciente.PacienteDietaRepository;
import com.nutriconsultas.paciente.PacienteDietaStatus;
import com.nutriconsultas.paciente.PacienteRepository;

import lombok.extern.slf4j.Slf4j;

@ExtendWith(MockitoExtension.class)
@Slf4j
@ActiveProfiles("test")
public class ClinicStatisticsServiceTest {

	@InjectMocks
	private ClinicStatisticsService service;

	@Mock
	private PacienteRepository pacienteRepository;

	@Mock
	private CalendarEventRepository calendarEventRepository;

	@Mock
	private PacienteDietaRepository pacienteDietaRepository;

	@Mock
	private ClinicalExamRepository clinicalExamRepository;

	@Mock
	private AnthropometricMeasurementRepository anthropometricMeasurementRepository;

	private static final String TEST_USER_ID = "test-user-id-123";

	private Paciente paciente1;

	private Paciente paciente2;

	private CalendarEvent consultation1;

	private CalendarEvent consultation2;

	private PacienteDieta dieta1;

	private ClinicalExam exam1;

	private AnthropometricMeasurement measurement1;

	@BeforeEach
	public void setup() {
		log.info("Setting up ClinicStatisticsService test");

		// Create test pacientes
		paciente1 = new Paciente();
		paciente1.setId(1L);
		paciente1.setName("Juan Perez");
		paciente1.setUserId(TEST_USER_ID);
		paciente1.setGender("M");
		final Calendar dob1 = Calendar.getInstance();
		dob1.add(Calendar.YEAR, -30);
		paciente1.setDob(dob1.getTime());
		paciente1.setPeso(70.0);
		paciente1.setImc(22.0);
		paciente1.setNivelPeso(NivelPeso.NORMAL);
		paciente1.setHipertension(true);
		paciente1.setDiabetes(false);

		paciente2 = new Paciente();
		paciente2.setId(2L);
		paciente2.setName("Maria Garcia");
		paciente2.setUserId(TEST_USER_ID);
		paciente2.setGender("F");
		final Calendar dob2 = Calendar.getInstance();
		dob2.add(Calendar.YEAR, -25);
		paciente2.setDob(dob2.getTime());
		paciente2.setPeso(65.0);
		paciente2.setImc(24.0);
		paciente2.setNivelPeso(NivelPeso.ALTO);
		paciente2.setHipertension(false);
		paciente2.setDiabetes(true);

		// Create test consultations
		consultation1 = new CalendarEvent();
		consultation1.setId(1L);
		consultation1.setPaciente(paciente1);
		consultation1.setEventDateTime(new Date());
		consultation1.setStatus(EventStatus.COMPLETED);
		consultation1.setPeso(70.0);
		consultation1.setImc(22.0);

		consultation2 = new CalendarEvent();
		consultation2.setId(2L);
		consultation2.setPaciente(paciente2);
		consultation2.setEventDateTime(new Date());
		consultation2.setStatus(EventStatus.SCHEDULED);
		consultation2.setPeso(65.0);
		consultation2.setImc(24.0);

		// Create test dieta
		dieta1 = new PacienteDieta();
		dieta1.setId(1L);
		dieta1.setPaciente(paciente1);
		dieta1.setStatus(PacienteDietaStatus.ACTIVE);
		dieta1.setStartDate(new Date());

		// Create test exam
		exam1 = new ClinicalExam();
		exam1.setId(1L);
		exam1.setPaciente(paciente1);
		exam1.setExamDateTime(new Date());
		exam1.setPeso(70.0);

		// Create test measurement
		measurement1 = new AnthropometricMeasurement();
		measurement1.setId(1L);
		measurement1.setPaciente(paciente1);
		measurement1.setMeasurementDateTime(new Date());
		measurement1.setPeso(70.0);
		measurement1.setImc(22.0);

		log.info("Finished setting up ClinicStatisticsService test");
	}

	@Test
	public void testGenerateStatisticsWithNoDateRange() {
		log.info("Starting testGenerateStatisticsWithNoDateRange");

		// Arrange
		final List<Paciente> pacientes = new ArrayList<>();
		pacientes.add(paciente1);
		pacientes.add(paciente2);

		final List<CalendarEvent> consultations = new ArrayList<>();
		consultations.add(consultation1);
		consultations.add(consultation2);

		final List<PacienteDieta> dietas = new ArrayList<>();
		dietas.add(dieta1);

		final List<ClinicalExam> exams = new ArrayList<>();
		exams.add(exam1);

		final List<AnthropometricMeasurement> measurements = new ArrayList<>();
		measurements.add(measurement1);

		when(pacienteRepository.findByUserId(TEST_USER_ID)).thenReturn(pacientes);
		when(calendarEventRepository.findByPacienteId(1L)).thenReturn(consultations);
		when(calendarEventRepository.findByPacienteId(2L)).thenReturn(new ArrayList<>());
		when(pacienteDietaRepository.findByUserId(TEST_USER_ID)).thenReturn(dietas);
		when(clinicalExamRepository.findByPacienteId(1L)).thenReturn(exams);
		when(clinicalExamRepository.findByPacienteId(2L)).thenReturn(new ArrayList<>());
		when(anthropometricMeasurementRepository.findByPacienteId(1L)).thenReturn(measurements);
		when(anthropometricMeasurementRepository.findByPacienteId(2L)).thenReturn(new ArrayList<>());
		when(pacienteDietaRepository.countByUserIdAndStatus(eq(TEST_USER_ID), eq(PacienteDietaStatus.ACTIVE)))
			.thenReturn(1L);
		when(calendarEventRepository.countByUserIdAndStatus(eq(TEST_USER_ID), eq(EventStatus.COMPLETED)))
			.thenReturn(1L);

		// Act
		final ClinicStatistics statistics = service.generateStatistics(TEST_USER_ID, null, null);

		// Assert
		assertThat(statistics).isNotNull();
		assertThat(statistics.getTotalPatients()).isEqualTo(2L);
		assertThat(statistics.getTotalConsultations()).isEqualTo(2L);
		assertThat(statistics.getTotalDietaryPlans()).isEqualTo(1L);
		assertThat(statistics.getTotalClinicalExams()).isEqualTo(1L);
		assertThat(statistics.getTotalAnthropometricMeasurements()).isEqualTo(1L);
		assertThat(statistics.getActiveDietaryPlans()).isEqualTo(1L);
		assertThat(statistics.getCompletedConsultations()).isEqualTo(1L);
		assertThat(statistics.getGenderDistribution()).isNotNull();
		assertThat(statistics.getGenderDistribution().get("M")).isEqualTo(1L);
		assertThat(statistics.getGenderDistribution().get("F")).isEqualTo(1L);
		assertThat(statistics.getConditionFrequency()).isNotNull();
		assertThat(statistics.getConditionFrequency().get("Hipertensión")).isEqualTo(1L);
		assertThat(statistics.getConditionFrequency().get("Diabetes")).isEqualTo(1L);
		assertThat(statistics.getAverageConsultationsPerPatient()).isEqualTo(1.0);
		assertThat(statistics.getAverageWeight()).isEqualTo(67.5);
		assertThat(statistics.getAverageBMI()).isEqualTo(23.0);

		log.info("Finished testGenerateStatisticsWithNoDateRange");
	}

	@Test
	public void testGenerateStatisticsWithDateRange() {
		log.info("Starting testGenerateStatisticsWithDateRange");

		// Arrange
		final List<Paciente> pacientes = new ArrayList<>();
		pacientes.add(paciente1);

		final Calendar startDate = Calendar.getInstance();
		startDate.add(Calendar.MONTH, -1);
		final Calendar endDate = Calendar.getInstance();

		when(pacienteRepository.findByUserId(TEST_USER_ID)).thenReturn(pacientes);
		when(calendarEventRepository.findByUserIdAndDateRange(eq(TEST_USER_ID), any(Date.class), any(Date.class)))
			.thenReturn(new ArrayList<>());
		when(pacienteDietaRepository.findByUserIdAndDateRange(eq(TEST_USER_ID), any(Date.class), any(Date.class)))
			.thenReturn(new ArrayList<>());
		when(clinicalExamRepository.findByUserIdAndDateRange(eq(TEST_USER_ID), any(Date.class), any(Date.class)))
			.thenReturn(new ArrayList<>());
		when(anthropometricMeasurementRepository.findByUserIdAndDateRange(eq(TEST_USER_ID), any(Date.class),
				any(Date.class)))
			.thenReturn(new ArrayList<>());
		when(pacienteDietaRepository.countByUserIdAndStatus(eq(TEST_USER_ID), eq(PacienteDietaStatus.ACTIVE)))
			.thenReturn(0L);
		when(calendarEventRepository.countByUserIdAndStatus(eq(TEST_USER_ID), eq(EventStatus.COMPLETED)))
			.thenReturn(0L);

		// Act
		final ClinicStatistics statistics = service.generateStatistics(TEST_USER_ID, startDate.getTime(),
				endDate.getTime());

		// Assert
		assertThat(statistics).isNotNull();
		assertThat(statistics.getTotalPatients()).isEqualTo(1L);
		assertThat(statistics.getStartDate()).isNotNull();
		assertThat(statistics.getEndDate()).isNotNull();

		log.info("Finished testGenerateStatisticsWithDateRange");
	}

	@Test
	public void testGenerateStatisticsWithEmptyData() {
		log.info("Starting testGenerateStatisticsWithEmptyData");

		// Arrange
		when(pacienteRepository.findByUserId(TEST_USER_ID)).thenReturn(new ArrayList<>());
		when(pacienteDietaRepository.findByUserId(TEST_USER_ID)).thenReturn(new ArrayList<>());
		when(pacienteDietaRepository.countByUserIdAndStatus(eq(TEST_USER_ID), eq(PacienteDietaStatus.ACTIVE)))
			.thenReturn(0L);
		when(calendarEventRepository.countByUserIdAndStatus(eq(TEST_USER_ID), eq(EventStatus.COMPLETED)))
			.thenReturn(0L);

		// Act
		final ClinicStatistics statistics = service.generateStatistics(TEST_USER_ID, null, null);

		// Assert
		assertThat(statistics).isNotNull();
		assertThat(statistics.getTotalPatients()).isEqualTo(0L);
		assertThat(statistics.getTotalConsultations()).isEqualTo(0L);
		assertThat(statistics.getAverageConsultationsPerPatient()).isEqualTo(0.0);
		assertThat(statistics.getGenderDistribution()).isNotNull();
		assertThat(statistics.getConditionFrequency()).isNotNull();

		log.info("Finished testGenerateStatisticsWithEmptyData");
	}

}
