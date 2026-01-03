package com.nutriconsultas.reports;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;
import org.thymeleaf.TemplateEngine;

import com.nutriconsultas.calendar.CalendarEvent;
import com.nutriconsultas.calendar.CalendarEventService;
import com.nutriconsultas.calendar.EventStatus;
import com.nutriconsultas.clinical.exam.AnthropometricMeasurement;
import com.nutriconsultas.clinical.exam.AnthropometricMeasurementService;
import com.nutriconsultas.clinical.exam.ClinicalExamService;
import com.nutriconsultas.clinical.exam.anthropometric.BodyMass;
import com.nutriconsultas.paciente.Paciente;
import com.nutriconsultas.paciente.PacienteDieta;
import com.nutriconsultas.paciente.PacienteDietaRepository;
import com.nutriconsultas.paciente.PacienteDietaStatus;
import com.nutriconsultas.paciente.PacienteService;

import lombok.extern.slf4j.Slf4j;

@ExtendWith(MockitoExtension.class)
@Slf4j
@ActiveProfiles("test")
public class PatientReportServiceTest {

	@InjectMocks
	private PatientReportService reportService;

	@Mock
	private TemplateEngine templateEngine;

	@Mock
	private PacienteService pacienteService;

	@Mock
	private CalendarEventService calendarEventService;

	@Mock
	private AnthropometricMeasurementService anthropometricMeasurementService;

	@Mock
	private ClinicalExamService clinicalExamService;

	@Mock
	private PacienteDietaRepository pacienteDietaRepository;

	private Paciente paciente;

	@BeforeEach
	public void setup() {
		paciente = new Paciente();
		paciente.setId(1L);
		paciente.setName("Test Patient");
		paciente.setUserId("user123");
		paciente.setDob(new Date());
		paciente.setGender("M");
		paciente.setPeso(70.0);
		paciente.setEstatura(1.75);
		paciente.setImc(22.86);
	}

	@Test
	public void testGenerateReportWithValidPatient() {
		when(pacienteService.findByIdAndUserId(1L, "user123")).thenReturn(paciente);
		when(calendarEventService.findByPacienteId(1L)).thenReturn(new ArrayList<>());
		when(anthropometricMeasurementService.findByPacienteId(1L)).thenReturn(new ArrayList<>());
		when(clinicalExamService.findByPacienteId(1L)).thenReturn(new ArrayList<>());
		when(pacienteDietaRepository.findByPacienteIdOrderByStartDateDesc(1L)).thenReturn(new ArrayList<>());
		when(templateEngine.process(eq("sbadmin/reports/patient-progress"), any(org.thymeleaf.context.Context.class)))
			.thenReturn("<html><body>Test Report</body></html>");

		final byte[] pdfBytes = reportService.generateReport(1L, "user123", null, null);

		assertThat(pdfBytes).isNotNull();
		assertThat(pdfBytes.length).isGreaterThan(0);
	}

	@Test
	public void testGenerateReportWithNonExistentPatient() {
		when(pacienteService.findByIdAndUserId(999L, "user123")).thenReturn(null);

		assertThatThrownBy(() -> reportService.generateReport(999L, "user123", null, null))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("not found");
	}

	@Test
	public void testGenerateReportWithConsultations() {
		final CalendarEvent consultation = new CalendarEvent();
		consultation.setId(1L);
		consultation.setPaciente(paciente);
		consultation.setEventDateTime(new Date());
		consultation.setTitle("Consulta de seguimiento");
		consultation.setDurationMinutes(30);
		consultation.setStatus(EventStatus.COMPLETED);
		consultation.setPeso(70.5);
		consultation.setImc(23.0);

		final List<CalendarEvent> consultations = new ArrayList<>();
		consultations.add(consultation);

		when(pacienteService.findByIdAndUserId(1L, "user123")).thenReturn(paciente);
		when(calendarEventService.findByPacienteId(1L)).thenReturn(consultations);
		when(anthropometricMeasurementService.findByPacienteId(1L)).thenReturn(new ArrayList<>());
		when(clinicalExamService.findByPacienteId(1L)).thenReturn(new ArrayList<>());
		when(pacienteDietaRepository.findByPacienteIdOrderByStartDateDesc(1L)).thenReturn(new ArrayList<>());
		when(templateEngine.process(eq("sbadmin/reports/patient-progress"), any(org.thymeleaf.context.Context.class)))
			.thenReturn("<html><body>Test Report</body></html>");

		final byte[] pdfBytes = reportService.generateReport(1L, "user123", null, null);

		assertThat(pdfBytes).isNotNull();
		assertThat(pdfBytes.length).isGreaterThan(0);
	}

	@Test
	public void testGenerateReportWithMeasurements() {
		final AnthropometricMeasurement measurement = new AnthropometricMeasurement();
		measurement.setId(1L);
		measurement.setPaciente(paciente);
		measurement.setMeasurementDateTime(new Date());
		measurement.setTitle("Medición Antropométrica");
		final BodyMass bodyMass = new BodyMass();
		bodyMass.setWeight(70.0);
		bodyMass.setHeight(1.75);
		bodyMass.setImc(22.86);
		measurement.setBodyMass(bodyMass);

		final List<AnthropometricMeasurement> measurements = new ArrayList<>();
		measurements.add(measurement);

		when(pacienteService.findByIdAndUserId(1L, "user123")).thenReturn(paciente);
		when(calendarEventService.findByPacienteId(1L)).thenReturn(new ArrayList<>());
		when(anthropometricMeasurementService.findByPacienteId(1L)).thenReturn(measurements);
		when(clinicalExamService.findByPacienteId(1L)).thenReturn(new ArrayList<>());
		when(pacienteDietaRepository.findByPacienteIdOrderByStartDateDesc(1L)).thenReturn(new ArrayList<>());
		when(templateEngine.process(eq("sbadmin/reports/patient-progress"), any(org.thymeleaf.context.Context.class)))
			.thenReturn("<html><body>Test Report</body></html>");

		final byte[] pdfBytes = reportService.generateReport(1L, "user123", null, null);

		assertThat(pdfBytes).isNotNull();
		assertThat(pdfBytes.length).isGreaterThan(0);
	}

	@Test
	public void testGenerateReportWithDietaryPlans() {
		final PacienteDieta plan = new PacienteDieta();
		plan.setId(1L);
		plan.setPaciente(paciente);
		plan.setStartDate(new Date());
		plan.setStatus(PacienteDietaStatus.ACTIVE);

		final List<PacienteDieta> plans = new ArrayList<>();
		plans.add(plan);

		when(pacienteService.findByIdAndUserId(1L, "user123")).thenReturn(paciente);
		when(calendarEventService.findByPacienteId(1L)).thenReturn(new ArrayList<>());
		when(anthropometricMeasurementService.findByPacienteId(1L)).thenReturn(new ArrayList<>());
		when(clinicalExamService.findByPacienteId(1L)).thenReturn(new ArrayList<>());
		when(pacienteDietaRepository.findByPacienteIdOrderByStartDateDesc(1L)).thenReturn(plans);
		when(templateEngine.process(eq("sbadmin/reports/patient-progress"), any(org.thymeleaf.context.Context.class)))
			.thenReturn("<html><body>Test Report</body></html>");

		final byte[] pdfBytes = reportService.generateReport(1L, "user123", null, null);

		assertThat(pdfBytes).isNotNull();
		assertThat(pdfBytes.length).isGreaterThan(0);
	}

	@Test
	public void testGenerateReportWithDateRange() {
		// 30 days ago
		final long thirtyDaysAgo = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000;
		final Date startDate = new Date(thirtyDaysAgo);
		final Date endDate = new Date();

		when(pacienteService.findByIdAndUserId(1L, "user123")).thenReturn(paciente);
		when(calendarEventService.findByPacienteId(1L)).thenReturn(new ArrayList<>());
		when(anthropometricMeasurementService.findByPacienteId(1L)).thenReturn(new ArrayList<>());
		when(clinicalExamService.findByPacienteId(1L)).thenReturn(new ArrayList<>());
		when(pacienteDietaRepository.findByPacienteIdOrderByStartDateDesc(1L)).thenReturn(new ArrayList<>());
		when(templateEngine.process(eq("sbadmin/reports/patient-progress"), any(org.thymeleaf.context.Context.class)))
			.thenReturn("<html><body>Test Report</body></html>");

		final byte[] pdfBytes = reportService.generateReport(1L, "user123", startDate, endDate);

		assertThat(pdfBytes).isNotNull();
		assertThat(pdfBytes.length).isGreaterThan(0);
	}

	@Test
	public void testGenerateReportWithDateRangeFiltering() {
		// 30 days ago
		final long thirtyDaysAgo = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000;
		final Date startDate = new Date(thirtyDaysAgo);
		final Date endDate = new Date();

		final CalendarEvent consultation1 = new CalendarEvent();
		consultation1.setId(1L);
		consultation1.setPaciente(paciente);
		// 10 days ago
		final long tenDaysAgo = System.currentTimeMillis() - 10L * 24 * 60 * 60 * 1000;
		consultation1.setEventDateTime(new Date(tenDaysAgo));
		consultation1.setTitle("Consulta dentro del rango");
		consultation1.setPeso(70.0);

		final CalendarEvent consultation2 = new CalendarEvent();
		consultation2.setId(2L);
		consultation2.setPaciente(paciente);
		// 40 days ago
		final long fortyDaysAgo = System.currentTimeMillis() - 40L * 24 * 60 * 60 * 1000;
		consultation2.setEventDateTime(new Date(fortyDaysAgo));
		consultation2.setTitle("Consulta fuera del rango");
		consultation2.setPeso(71.0);

		final List<CalendarEvent> allConsultations = new ArrayList<>();
		allConsultations.add(consultation1);
		allConsultations.add(consultation2);

		when(pacienteService.findByIdAndUserId(1L, "user123")).thenReturn(paciente);
		when(calendarEventService.findByPacienteId(1L)).thenReturn(allConsultations);
		when(anthropometricMeasurementService.findByPacienteId(1L)).thenReturn(new ArrayList<>());
		when(clinicalExamService.findByPacienteId(1L)).thenReturn(new ArrayList<>());
		when(pacienteDietaRepository.findByPacienteIdOrderByStartDateDesc(1L)).thenReturn(new ArrayList<>());
		when(templateEngine.process(eq("sbadmin/reports/patient-progress"), any(org.thymeleaf.context.Context.class)))
			.thenReturn("<html><body>Test Report</body></html>");

		final byte[] pdfBytes = reportService.generateReport(1L, "user123", startDate, endDate);

		assertThat(pdfBytes).isNotNull();
		assertThat(pdfBytes.length).isGreaterThan(0);
	}

}
