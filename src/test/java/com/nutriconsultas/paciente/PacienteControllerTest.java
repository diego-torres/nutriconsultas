package com.nutriconsultas.paciente;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;

import com.nutriconsultas.calendar.CalendarEvent;
import com.nutriconsultas.calendar.CalendarEventService;
import com.nutriconsultas.calendar.EventStatus;
import com.nutriconsultas.clinical.exam.ClinicalExam;
import com.nutriconsultas.clinical.exam.ClinicalExamService;

import lombok.extern.slf4j.Slf4j;

@ExtendWith(MockitoExtension.class)
@Slf4j
@ActiveProfiles("test")
@SuppressWarnings("null")
public class PacienteControllerTest {

	@InjectMocks
	private PacienteController controller;

	@Mock
	private PacienteRepository pacienteRepository;

	@Mock
	private CalendarEventService calendarEventService;

	@Mock
	private BodyFatCalculatorService bodyFatCalculatorService;

	@Mock
	private PacienteDietaService pacienteDietaService;

	@Mock
	private com.nutriconsultas.dieta.DietaService dietaService;

	@Mock
	private com.nutriconsultas.dieta.DietaRepository dietaRepository;

	@Mock
	private ClinicalExamService clinicalExamService;

	@Mock
	private com.nutriconsultas.clinical.exam.AnthropometricMeasurementService anthropometricMeasurementService;

	@Mock
	private BindingResult bindingResult;

	private Paciente paciente;

	private CalendarEvent evento;

	private OidcUser principal;

	private static final String TEST_USER_ID = "test-user-id-123";

	@BeforeEach
	public void setup() {
		log.info("setting up PacienteController test");

		// Create test paciente with date of birth and gender
		paciente = new Paciente();
		paciente.setId(1L);
		paciente.setName("Juan Perez");
		paciente.setEmail("juan@example.com");
		paciente.setPhone("1234567890");
		paciente.setUserId(TEST_USER_ID);
		// Set date of birth (30 years ago)
		final LocalDate dob = LocalDate.now().minusYears(30);
		paciente.setDob(Date.from(dob.atStartOfDay(ZoneId.systemDefault()).toInstant()));
		paciente.setGender("M");

		// Create test evento
		evento = new CalendarEvent();
		evento.setPeso(70.0);
		evento.setEstatura(1.75);
		evento.setEventDateTime(new Date());
		evento.setTitle("Consulta");
		evento.setDurationMinutes(60);
		evento.setStatus(EventStatus.COMPLETED);

		// Create mock OidcUser principal
		principal = org.mockito.Mockito.mock(OidcUser.class);
		lenient().when(principal.getSubject()).thenReturn(TEST_USER_ID);

		log.info("finished setting up PacienteController test");
	}

	@Test
	public void testAgregarConsultaPacienteCalculatesBodyFat() {
		log.info("starting testAgregarConsultaPacienteCalculatesBodyFat");
		// Arrange
		when(pacienteRepository.findByIdAndUserId(1L, TEST_USER_ID)).thenReturn(java.util.Optional.of(paciente));
		when(calendarEventService.save(Objects.requireNonNull(evento))).thenReturn(evento);
		when(bodyFatCalculatorService.calculateBodyFatPercentage(any(Double.class), any(Integer.class),
				any(String.class)))
			.thenReturn(15.5);

		// Act
		final String result = controller.agregarConsultaPaciente(1L, Objects.requireNonNull(evento), bindingResult,
				null, principal);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result).contains("redirect:/admin/pacientes/1/historial");
		verify(pacienteRepository).findByIdAndUserId(1L, TEST_USER_ID);
		verify(calendarEventService).save(any(CalendarEvent.class));
		verify(bodyFatCalculatorService).calculateBodyFatPercentage(any(Double.class), any(Integer.class),
				any(String.class));
		// Verify that evento has IMC calculated
		assertThat(evento.getImc()).isNotNull();
		assertThat(evento.getNivelPeso()).isNotNull();
		// Verify that body fat was calculated
		assertThat(evento.getIndiceGrasaCorporal()).isNotNull();
		assertThat(evento.getIndiceGrasaCorporal()).isEqualTo(15.5);
		log.info("finished testAgregarConsultaPacienteCalculatesBodyFat");
	}

	@Test
	public void testAgregarConsultaPacienteWithoutDob() {
		log.info("starting testAgregarConsultaPacienteWithoutDob");
		// Arrange
		paciente.setDob(null);
		when(pacienteRepository.findByIdAndUserId(1L, TEST_USER_ID)).thenReturn(java.util.Optional.of(paciente));
		when(calendarEventService.save(Objects.requireNonNull(evento))).thenReturn(evento);

		// Act
		final String result = controller.agregarConsultaPaciente(1L, Objects.requireNonNull(evento), bindingResult,
				null, principal);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result).contains("redirect:/admin/pacientes/1/historial");
		verify(pacienteRepository).findByIdAndUserId(1L, TEST_USER_ID);
		verify(calendarEventService).save(any(CalendarEvent.class));
		// Body fat should not be calculated if DOB is missing
		verify(bodyFatCalculatorService, org.mockito.Mockito.never()).calculateBodyFatPercentage(any(Double.class),
				any(Integer.class), any(String.class));
		assertThat(evento.getIndiceGrasaCorporal()).isNull();
		log.info("finished testAgregarConsultaPacienteWithoutDob");
	}

	@Test
	public void testAgregarConsultaPacienteWithoutGender() {
		log.info("starting testAgregarConsultaPacienteWithoutGender");
		// Arrange
		paciente.setGender(null);
		when(pacienteRepository.findByIdAndUserId(1L, TEST_USER_ID)).thenReturn(java.util.Optional.of(paciente));
		when(calendarEventService.save(Objects.requireNonNull(evento))).thenReturn(evento);

		// Act
		final String result = controller.agregarConsultaPaciente(1L, Objects.requireNonNull(evento), bindingResult,
				null, principal);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result).contains("redirect:/admin/pacientes/1/historial");
		verify(pacienteRepository).findByIdAndUserId(1L, TEST_USER_ID);
		verify(calendarEventService).save(any(CalendarEvent.class));
		// Body fat should not be calculated if gender is missing
		verify(bodyFatCalculatorService, org.mockito.Mockito.never()).calculateBodyFatPercentage(any(Double.class),
				any(Integer.class), any(String.class));
		assertThat(evento.getIndiceGrasaCorporal()).isNull();
		log.info("finished testAgregarConsultaPacienteWithoutGender");
	}

	@Test
	public void testAgregarConsultaPacienteUpdatesPatientWeight() {
		log.info("starting testAgregarConsultaPacienteUpdatesPatientWeight");
		// Arrange
		when(pacienteRepository.findByIdAndUserId(1L, TEST_USER_ID)).thenReturn(java.util.Optional.of(paciente));
		when(calendarEventService.save(any(CalendarEvent.class))).thenReturn(evento);
		when(pacienteRepository.save(any(Paciente.class))).thenReturn(paciente);
		when(bodyFatCalculatorService.calculateBodyFatPercentage(any(Double.class), any(Integer.class),
				any(String.class)))
			.thenReturn(15.5);

		// Act
		final String result = controller.agregarConsultaPaciente(1L, evento, bindingResult, null, principal);

		// Assert
		assertThat(result).isNotNull();
		verify(pacienteRepository).save(Objects.requireNonNull(paciente));
		// Verify patient weight was updated
		assertThat(paciente.getPeso()).isEqualTo(70.0);
		assertThat(paciente.getEstatura()).isEqualTo(1.75);
		assertThat(paciente.getImc()).isNotNull();
		assertThat(paciente.getNivelPeso()).isNotNull();
		log.info("finished testAgregarConsultaPacienteUpdatesPatientWeight");
	}

	@Test
	public void testAgregarConsultaPacienteWithFemaleGender() {
		log.info("starting testAgregarConsultaPacienteWithFemaleGender");
		// Arrange
		paciente.setGender("F");
		when(pacienteRepository.findByIdAndUserId(1L, TEST_USER_ID)).thenReturn(java.util.Optional.of(paciente));
		when(calendarEventService.save(any(CalendarEvent.class))).thenReturn(evento);
		when(bodyFatCalculatorService.calculateBodyFatPercentage(any(Double.class), any(Integer.class), eq("F")))
			.thenReturn(22.5);

		// Act
		final String result = controller.agregarConsultaPaciente(1L, evento, bindingResult, null, principal);

		// Assert
		assertThat(result).isNotNull();
		verify(bodyFatCalculatorService).calculateBodyFatPercentage(any(Double.class), any(Integer.class), eq("F"));
		assertThat(evento.getIndiceGrasaCorporal()).isEqualTo(22.5);
		log.info("finished testAgregarConsultaPacienteWithFemaleGender");
	}

	@Test
	public void testPerfilPacienteWithCompletedPastEvent() {
		log.info("starting testPerfilPacienteWithCompletedPastEvent");
		// Arrange
		// 1 day ago
		final Date pastDate = new Date(System.currentTimeMillis() - 86400000);
		final CalendarEvent pastEvent = new CalendarEvent();
		pastEvent.setId(1L);
		pastEvent.setEventDateTime(pastDate);
		pastEvent.setStatus(EventStatus.COMPLETED);
		pastEvent.setPaciente(paciente);

		when(pacienteRepository.findByIdAndUserId(1L, TEST_USER_ID)).thenReturn(java.util.Optional.of(paciente));
		when(calendarEventService.findByPacienteId(1L)).thenReturn(Arrays.asList(pastEvent));

		final Model model = org.mockito.Mockito.mock(Model.class);

		// Act
		final String result = controller.perfilPaciente(1L, model, principal);

		// Assert
		assertThat(result).isEqualTo("sbadmin/pacientes/perfil");
		org.mockito.Mockito.verify(model).addAttribute("paciente", paciente);
		org.mockito.Mockito.verify(model).addAttribute(eq("citaAnterior"), org.mockito.ArgumentMatchers.anyString());
		org.mockito.Mockito.verify(model).addAttribute("citaSiguiente", "");
		log.info("finished testPerfilPacienteWithCompletedPastEvent");
	}

	@Test
	public void testPerfilPacienteWithScheduledFutureEvent() {
		log.info("starting testPerfilPacienteWithScheduledFutureEvent");
		// Arrange
		// 1 day from now
		final Date futureDate = new Date(System.currentTimeMillis() + 86400000);
		final CalendarEvent futureEvent = new CalendarEvent();
		futureEvent.setId(2L);
		futureEvent.setEventDateTime(futureDate);
		futureEvent.setStatus(EventStatus.SCHEDULED);
		futureEvent.setPaciente(paciente);

		when(pacienteRepository.findByIdAndUserId(1L, TEST_USER_ID)).thenReturn(java.util.Optional.of(paciente));
		when(calendarEventService.findByPacienteId(1L)).thenReturn(Arrays.asList(futureEvent));

		final Model model = org.mockito.Mockito.mock(Model.class);

		// Act
		final String result = controller.perfilPaciente(1L, model, principal);

		// Assert
		assertThat(result).isEqualTo("sbadmin/pacientes/perfil");
		verify(model).addAttribute("paciente", paciente);
		verify(model).addAttribute("citaAnterior", "");
		final ArgumentCaptor<String> citaSiguienteCaptor = ArgumentCaptor.forClass(String.class);
		verify(model).addAttribute(eq("citaSiguiente"), citaSiguienteCaptor.capture());
		assertThat(citaSiguienteCaptor.getValue()).isNotNull();
		assertThat(citaSiguienteCaptor.getValue()).isNotEqualTo("");
		log.info("finished testPerfilPacienteWithScheduledFutureEvent");
	}

	@Test
	public void testPerfilPacienteWithBothEvents() {
		log.info("starting testPerfilPacienteWithBothEvents");
		// Arrange
		// 1 day ago
		final Date pastDate = new Date(System.currentTimeMillis() - 86400000);
		// 1 day from now
		final Date futureDate = new Date(System.currentTimeMillis() + 86400000);

		final CalendarEvent pastEvent = new CalendarEvent();
		pastEvent.setId(1L);
		pastEvent.setEventDateTime(pastDate);
		pastEvent.setStatus(EventStatus.COMPLETED);
		pastEvent.setPaciente(paciente);

		final CalendarEvent futureEvent = new CalendarEvent();
		futureEvent.setId(2L);
		futureEvent.setEventDateTime(futureDate);
		futureEvent.setStatus(EventStatus.SCHEDULED);
		futureEvent.setPaciente(paciente);

		when(pacienteRepository.findByIdAndUserId(1L, TEST_USER_ID)).thenReturn(java.util.Optional.of(paciente));
		when(calendarEventService.findByPacienteId(1L)).thenReturn(Arrays.asList(pastEvent, futureEvent));

		final Model model = org.mockito.Mockito.mock(Model.class);

		// Act
		final String result = controller.perfilPaciente(1L, model, principal);

		// Assert
		assertThat(result).isEqualTo("sbadmin/pacientes/perfil");
		verify(model).addAttribute("paciente", paciente);
		final ArgumentCaptor<String> citaAnteriorCaptor = ArgumentCaptor.forClass(String.class);
		verify(model).addAttribute(eq("citaAnterior"), citaAnteriorCaptor.capture());
		assertThat(citaAnteriorCaptor.getValue()).isNotNull();
		assertThat(citaAnteriorCaptor.getValue()).isNotEqualTo("");
		final ArgumentCaptor<String> citaSiguienteCaptor = ArgumentCaptor.forClass(String.class);
		verify(model).addAttribute(eq("citaSiguiente"), citaSiguienteCaptor.capture());
		assertThat(citaSiguienteCaptor.getValue()).isNotNull();
		assertThat(citaSiguienteCaptor.getValue()).isNotEqualTo("");
		log.info("finished testPerfilPacienteWithBothEvents");
	}

	@Test
	public void testPerfilPacienteIgnoresNonCompletedPastEvents() {
		log.info("starting testPerfilPacienteIgnoresNonCompletedPastEvents");
		// Arrange
		// 1 day ago
		final Date pastDate = new Date(System.currentTimeMillis() - 86400000);
		final CalendarEvent cancelledEvent = new CalendarEvent();
		cancelledEvent.setId(1L);
		cancelledEvent.setEventDateTime(pastDate);
		cancelledEvent.setStatus(EventStatus.CANCELLED);
		cancelledEvent.setPaciente(paciente);

		final CalendarEvent scheduledPastEvent = new CalendarEvent();
		scheduledPastEvent.setId(2L);
		scheduledPastEvent.setEventDateTime(pastDate);
		scheduledPastEvent.setStatus(EventStatus.SCHEDULED);
		scheduledPastEvent.setPaciente(paciente);

		when(pacienteRepository.findByIdAndUserId(1L, TEST_USER_ID)).thenReturn(java.util.Optional.of(paciente));
		when(calendarEventService.findByPacienteId(1L)).thenReturn(Arrays.asList(cancelledEvent, scheduledPastEvent));

		final Model model = org.mockito.Mockito.mock(Model.class);

		// Act
		final String result = controller.perfilPaciente(1L, model, principal);

		// Assert
		assertThat(result).isEqualTo("sbadmin/pacientes/perfil");
		verify(model).addAttribute("citaAnterior", "");
		log.info("finished testPerfilPacienteIgnoresNonCompletedPastEvents");
	}

	@Test
	public void testPerfilPacienteIgnoresNonScheduledFutureEvents() {
		log.info("starting testPerfilPacienteIgnoresNonScheduledFutureEvents");
		// Arrange
		// 1 day from now
		final Date futureDate = new Date(System.currentTimeMillis() + 86400000);
		final CalendarEvent completedFutureEvent = new CalendarEvent();
		completedFutureEvent.setId(1L);
		completedFutureEvent.setEventDateTime(futureDate);
		completedFutureEvent.setStatus(EventStatus.COMPLETED);
		completedFutureEvent.setPaciente(paciente);

		final CalendarEvent cancelledFutureEvent = new CalendarEvent();
		cancelledFutureEvent.setId(2L);
		cancelledFutureEvent.setEventDateTime(futureDate);
		cancelledFutureEvent.setStatus(EventStatus.CANCELLED);
		cancelledFutureEvent.setPaciente(paciente);

		when(pacienteRepository.findByIdAndUserId(1L, TEST_USER_ID)).thenReturn(java.util.Optional.of(paciente));
		when(calendarEventService.findByPacienteId(1L))
			.thenReturn(Arrays.asList(completedFutureEvent, cancelledFutureEvent));

		final Model model = org.mockito.Mockito.mock(Model.class);

		// Act
		final String result = controller.perfilPaciente(1L, model, principal);

		// Assert
		assertThat(result).isEqualTo("sbadmin/pacientes/perfil");
		verify(model).addAttribute("citaSiguiente", "");
		log.info("finished testPerfilPacienteIgnoresNonScheduledFutureEvents");
	}

	@Test
	public void testPerfilPacienteSelectsMostRecentCompletedPastEvent() {
		log.info("starting testPerfilPacienteSelectsMostRecentCompletedPastEvent");
		// Arrange
		// 2 days ago
		final Date olderDate = new Date(System.currentTimeMillis() - 172800000);
		// 1 day ago
		final Date recentDate = new Date(System.currentTimeMillis() - 86400000);

		final CalendarEvent olderEvent = new CalendarEvent();
		olderEvent.setId(1L);
		olderEvent.setEventDateTime(olderDate);
		olderEvent.setStatus(EventStatus.COMPLETED);
		olderEvent.setPaciente(paciente);

		final CalendarEvent recentEvent = new CalendarEvent();
		recentEvent.setId(2L);
		recentEvent.setEventDateTime(recentDate);
		recentEvent.setStatus(EventStatus.COMPLETED);
		recentEvent.setPaciente(paciente);

		when(pacienteRepository.findByIdAndUserId(1L, TEST_USER_ID)).thenReturn(java.util.Optional.of(paciente));
		when(calendarEventService.findByPacienteId(1L)).thenReturn(Arrays.asList(olderEvent, recentEvent));

		final Model model = org.mockito.Mockito.mock(Model.class);

		// Act
		final String result = controller.perfilPaciente(1L, model, principal);

		// Assert
		assertThat(result).isEqualTo("sbadmin/pacientes/perfil");
		final ArgumentCaptor<String> citaAnteriorCaptor = ArgumentCaptor.forClass(String.class);
		verify(model).addAttribute(eq("citaAnterior"), citaAnteriorCaptor.capture());
		// The most recent event should be selected
		assertThat(citaAnteriorCaptor.getValue()).isNotNull();
		assertThat(citaAnteriorCaptor.getValue()).isNotEqualTo("");
		log.info("finished testPerfilPacienteSelectsMostRecentCompletedPastEvent");
	}

	@Test
	public void testPerfilPacienteSelectsEarliestScheduledFutureEvent() {
		log.info("starting testPerfilPacienteSelectsEarliestScheduledFutureEvent");
		// Arrange
		// 1 day from now
		final Date nearFutureDate = new Date(System.currentTimeMillis() + 86400000);
		// 2 days from now
		final Date farFutureDate = new Date(System.currentTimeMillis() + 172800000);

		final CalendarEvent nearFutureEvent = new CalendarEvent();
		nearFutureEvent.setId(1L);
		nearFutureEvent.setEventDateTime(nearFutureDate);
		nearFutureEvent.setStatus(EventStatus.SCHEDULED);
		nearFutureEvent.setPaciente(paciente);

		final CalendarEvent farFutureEvent = new CalendarEvent();
		farFutureEvent.setId(2L);
		farFutureEvent.setEventDateTime(farFutureDate);
		farFutureEvent.setStatus(EventStatus.SCHEDULED);
		farFutureEvent.setPaciente(paciente);

		when(pacienteRepository.findByIdAndUserId(1L, TEST_USER_ID)).thenReturn(java.util.Optional.of(paciente));
		when(calendarEventService.findByPacienteId(1L)).thenReturn(Arrays.asList(nearFutureEvent, farFutureEvent));

		final Model model = org.mockito.Mockito.mock(Model.class);

		// Act
		final String result = controller.perfilPaciente(1L, model, principal);

		// Assert
		assertThat(result).isEqualTo("sbadmin/pacientes/perfil");
		final ArgumentCaptor<String> citaSiguienteCaptor = ArgumentCaptor.forClass(String.class);
		verify(model).addAttribute(eq("citaSiguiente"), citaSiguienteCaptor.capture());
		// The earliest future event should be selected
		assertThat(citaSiguienteCaptor.getValue()).isNotNull();
		assertThat(citaSiguienteCaptor.getValue()).isNotEqualTo("");
		log.info("finished testPerfilPacienteSelectsEarliestScheduledFutureEvent");
	}

	@Test
	public void testPerfilPacienteWithNoEvents() {
		log.info("starting testPerfilPacienteWithNoEvents");
		// Arrange
		when(pacienteRepository.findByIdAndUserId(1L, TEST_USER_ID)).thenReturn(java.util.Optional.of(paciente));
		when(calendarEventService.findByPacienteId(1L)).thenReturn(new ArrayList<>());

		final Model model = org.mockito.Mockito.mock(Model.class);

		// Act
		final String result = controller.perfilPaciente(1L, model, principal);

		// Assert
		assertThat(result).isEqualTo("sbadmin/pacientes/perfil");
		verify(model).addAttribute("paciente", paciente);
		verify(model).addAttribute("citaAnterior", "");
		verify(model).addAttribute("citaSiguiente", "");
		log.info("finished testPerfilPacienteWithNoEvents");
	}

	@Test
	public void testPerfilPaciente() {
		log.info("starting testPerfilPaciente");
		// Arrange
		when(pacienteRepository.findByIdAndUserId(1L, TEST_USER_ID)).thenReturn(java.util.Optional.of(paciente));
		when(calendarEventService.findByPacienteId(1L)).thenReturn(new ArrayList<>());

		final Model model = org.mockito.Mockito.mock(Model.class);

		// Act
		final String result = controller.perfilPaciente(1L, model, principal);

		// Assert
		assertThat(result).isEqualTo("sbadmin/pacientes/perfil");
		verify(model).addAttribute("activeMenu", "perfil");
		verify(model).addAttribute("paciente", paciente);
		// Dietas were moved to separate page, so they should not be in perfil anymore
		verify(model, org.mockito.Mockito.never()).addAttribute(eq("dietasAsignadas"), any());
		verify(model, org.mockito.Mockito.never()).addAttribute(eq("dietasActivas"), any());
		verify(model, org.mockito.Mockito.never()).addAttribute(eq("dietasDisponibles"), any());
		log.info("finished testPerfilPaciente");
	}

	@Test
	public void testAsignarDieta() {
		log.info("starting testAsignarDieta");
		// Arrange
		when(pacienteRepository.findByIdAndUserId(1L, TEST_USER_ID)).thenReturn(java.util.Optional.of(paciente));
		when(dietaService.getDietas()).thenReturn(new ArrayList<>());

		final Model model = org.mockito.Mockito.mock(Model.class);

		// Act
		final String result = controller.asignarDieta(1L, model, principal);

		// Assert
		assertThat(result).isEqualTo("sbadmin/pacientes/asignar-dieta");
		verify(model).addAttribute("paciente", paciente);
		verify(model).addAttribute("dietasDisponibles", new ArrayList<>());
		verify(model).addAttribute(eq("pacienteDieta"), any(PacienteDieta.class));
		log.info("finished testAsignarDieta");
	}

	@Test
	public void testGuardarAsignacionDieta() {
		log.info("starting testGuardarAsignacionDieta");
		// Arrange
		final PacienteDieta pacienteDieta = new PacienteDieta();
		pacienteDieta.setStartDate(new Date());
		pacienteDieta.setStatus(PacienteDietaStatus.ACTIVE);

		final com.nutriconsultas.dieta.Dieta dieta = new com.nutriconsultas.dieta.Dieta();
		dieta.setId(1L);
		dieta.setNombre("Dieta de Prueba");

		when(pacienteRepository.findByIdAndUserId(1L, TEST_USER_ID)).thenReturn(java.util.Optional.of(paciente));
		when(dietaRepository.findById(1L)).thenReturn(java.util.Optional.of(dieta));
		when(pacienteDietaService.assignDieta(any(Long.class), any(Long.class), any(PacienteDieta.class),
				any(String.class)))
			.thenReturn(pacienteDieta);

		final Model model = org.mockito.Mockito.mock(Model.class);

		// Act
		final String result = controller.guardarAsignacionDieta(1L, pacienteDieta, bindingResult, model, 1L, principal);

		// Assert
		assertThat(result).isEqualTo("redirect:/admin/pacientes/1/dietas");
		verify(pacienteRepository).findByIdAndUserId(1L, TEST_USER_ID);
		verify(dietaRepository).findById(1L);
		verify(pacienteDietaService).assignDieta(eq(1L), eq(1L), any(PacienteDieta.class), eq(TEST_USER_ID));
		log.info("finished testGuardarAsignacionDieta");
	}

	@Test
	public void testGuardarAsignacionDietaWithErrors() {
		log.info("starting testGuardarAsignacionDietaWithErrors");
		// Arrange
		final PacienteDieta pacienteDieta = new PacienteDieta();
		// No startDate set - should trigger validation error
		// status has default value ACTIVE, so we need to set it to null explicitly
		pacienteDieta.setStatus(null);

		final com.nutriconsultas.dieta.Dieta dieta = new com.nutriconsultas.dieta.Dieta();
		dieta.setId(1L);
		dieta.setNombre("Dieta de Prueba");

		when(pacienteRepository.findByIdAndUserId(1L, TEST_USER_ID)).thenReturn(java.util.Optional.of(paciente));
		when(dietaRepository.findById(1L)).thenReturn(java.util.Optional.of(dieta));
		when(dietaService.getDietas()).thenReturn(new ArrayList<>());
		when(bindingResult.getAllErrors()).thenReturn(new ArrayList<>());

		final Model model = org.mockito.Mockito.mock(Model.class);

		// Act
		final String result = controller.guardarAsignacionDieta(1L, pacienteDieta, bindingResult, model, 1L, principal);

		// Assert
		assertThat(result).isEqualTo("sbadmin/pacientes/asignar-dieta");
		verify(pacienteRepository).findByIdAndUserId(1L, TEST_USER_ID);
		verify(dietaRepository).findById(1L);
		verify(bindingResult).rejectValue(eq("startDate"), eq("NotNull"), eq("La fecha de inicio es requerida"));
		verify(bindingResult).rejectValue(eq("status"), eq("NotNull"), eq("El estado es requerido"));
		verify(model).addAttribute("activeMenu", "perfil");
		verify(model).addAttribute("paciente", paciente);
		verify(model).addAttribute("dietasDisponibles", new ArrayList<>());
		verify(pacienteDietaService, org.mockito.Mockito.never()).assignDieta(any(Long.class), any(Long.class),
				any(PacienteDieta.class), any(String.class));
		log.info("finished testGuardarAsignacionDietaWithErrors");
	}

	@Test
	public void testGuardarAsignacionDietaWithNullStartDate() {
		log.info("starting testGuardarAsignacionDietaWithNullStartDate");
		// Arrange
		final PacienteDieta pacienteDieta = new PacienteDieta();
		pacienteDieta.setStatus(PacienteDietaStatus.ACTIVE);
		// startDate is null

		final com.nutriconsultas.dieta.Dieta dieta = new com.nutriconsultas.dieta.Dieta();
		dieta.setId(1L);
		dieta.setNombre("Dieta de Prueba");

		when(pacienteRepository.findByIdAndUserId(1L, TEST_USER_ID)).thenReturn(java.util.Optional.of(paciente));
		when(dietaRepository.findById(1L)).thenReturn(java.util.Optional.of(dieta));
		when(dietaService.getDietas()).thenReturn(new ArrayList<>());
		when(bindingResult.getAllErrors()).thenReturn(new ArrayList<>());

		final Model model = org.mockito.Mockito.mock(Model.class);

		// Act
		final String result = controller.guardarAsignacionDieta(1L, pacienteDieta, bindingResult, model, 1L, principal);

		// Assert
		assertThat(result).isEqualTo("sbadmin/pacientes/asignar-dieta");
		verify(bindingResult).rejectValue(eq("startDate"), eq("NotNull"), eq("La fecha de inicio es requerida"));
		verify(pacienteDietaService, org.mockito.Mockito.never()).assignDieta(any(Long.class), any(Long.class),
				any(PacienteDieta.class), any(String.class));
		log.info("finished testGuardarAsignacionDietaWithNullStartDate");
	}

	@Test
	public void testGuardarAsignacionDietaWithNullStatus() {
		log.info("starting testGuardarAsignacionDietaWithNullStatus");
		// Arrange
		final PacienteDieta pacienteDieta = new PacienteDieta();
		pacienteDieta.setStartDate(new Date());
		// status has default value, so we need to set it to null explicitly
		pacienteDieta.setStatus(null);

		final com.nutriconsultas.dieta.Dieta dieta = new com.nutriconsultas.dieta.Dieta();
		dieta.setId(1L);
		dieta.setNombre("Dieta de Prueba");

		when(pacienteRepository.findByIdAndUserId(1L, TEST_USER_ID)).thenReturn(java.util.Optional.of(paciente));
		when(dietaRepository.findById(1L)).thenReturn(java.util.Optional.of(dieta));
		when(dietaService.getDietas()).thenReturn(new ArrayList<>());
		when(bindingResult.getAllErrors()).thenReturn(new ArrayList<>());

		final Model model = org.mockito.Mockito.mock(Model.class);

		// Act
		final String result = controller.guardarAsignacionDieta(1L, pacienteDieta, bindingResult, model, 1L, principal);

		// Assert
		assertThat(result).isEqualTo("sbadmin/pacientes/asignar-dieta");
		verify(bindingResult).rejectValue(eq("status"), eq("NotNull"), eq("El estado es requerido"));
		verify(pacienteDietaService, org.mockito.Mockito.never()).assignDieta(any(Long.class), any(Long.class),
				any(PacienteDieta.class), any(String.class));
		log.info("finished testGuardarAsignacionDietaWithNullStatus");
	}

	@Test
	public void testGuardarAsignacionDietaThrowsExceptionWhenPacienteNotFound() {
		log.info("starting testGuardarAsignacionDietaThrowsExceptionWhenPacienteNotFound");
		// Arrange
		final PacienteDieta pacienteDieta = new PacienteDieta();
		pacienteDieta.setStartDate(new Date());
		pacienteDieta.setStatus(PacienteDietaStatus.ACTIVE);

		when(pacienteRepository.findByIdAndUserId(1L, TEST_USER_ID)).thenReturn(java.util.Optional.empty());

		final Model model = org.mockito.Mockito.mock(Model.class);

		// Act & Assert
		assertThatThrownBy(
				() -> controller.guardarAsignacionDieta(1L, pacienteDieta, bindingResult, model, 1L, principal))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("No se ha encontrado paciente con folio");
		verify(pacienteRepository).findByIdAndUserId(1L, TEST_USER_ID);
		verify(pacienteDietaService, org.mockito.Mockito.never()).assignDieta(any(Long.class), any(Long.class),
				any(PacienteDieta.class), any(String.class));
		log.info("finished testGuardarAsignacionDietaThrowsExceptionWhenPacienteNotFound");
	}

	@Test
	public void testGuardarAsignacionDietaThrowsExceptionWhenDietaNotFound() {
		log.info("starting testGuardarAsignacionDietaThrowsExceptionWhenDietaNotFound");
		// Arrange
		final PacienteDieta pacienteDieta = new PacienteDieta();
		pacienteDieta.setStartDate(new Date());
		pacienteDieta.setStatus(PacienteDietaStatus.ACTIVE);

		when(pacienteRepository.findByIdAndUserId(1L, TEST_USER_ID)).thenReturn(java.util.Optional.of(paciente));
		when(dietaRepository.findById(1L)).thenReturn(java.util.Optional.empty());

		final Model model = org.mockito.Mockito.mock(Model.class);

		// Act & Assert
		assertThatThrownBy(
				() -> controller.guardarAsignacionDieta(1L, pacienteDieta, bindingResult, model, 1L, principal))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("No se ha encontrado dieta con id");
		verify(pacienteRepository).findByIdAndUserId(1L, TEST_USER_ID);
		verify(dietaRepository).findById(1L);
		verify(pacienteDietaService, org.mockito.Mockito.never()).assignDieta(any(Long.class), any(Long.class),
				any(PacienteDieta.class), any(String.class));
		log.info("finished testGuardarAsignacionDietaThrowsExceptionWhenDietaNotFound");
	}

	@Test
	public void testGuardarAsignacionDietaThrowsExceptionWhenPacienteDietaIsNull() {
		log.info("starting testGuardarAsignacionDietaThrowsExceptionWhenPacienteDietaIsNull");
		// Arrange
		final Model model = org.mockito.Mockito.mock(Model.class);

		// Act & Assert
		assertThatThrownBy(() -> controller.guardarAsignacionDieta(1L, null, bindingResult, model, 1L, principal))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("PacienteDieta cannot be null");
		verify(pacienteDietaService, org.mockito.Mockito.never()).assignDieta(any(Long.class), any(Long.class),
				any(PacienteDieta.class), any(String.class));
		log.info("finished testGuardarAsignacionDietaThrowsExceptionWhenPacienteDietaIsNull");
	}

	@Test
	public void testEditarAsignacionDieta() {
		log.info("starting testEditarAsignacionDieta");
		// Arrange
		final PacienteDieta pacienteDieta = new PacienteDieta();
		pacienteDieta.setId(1L);
		pacienteDieta.setPaciente(paciente);

		when(pacienteRepository.findByIdAndUserId(1L, TEST_USER_ID)).thenReturn(java.util.Optional.of(paciente));
		when(pacienteDietaService.findById(1L)).thenReturn(pacienteDieta);

		final Model model = org.mockito.Mockito.mock(Model.class);

		// Act
		final String result = controller.editarAsignacionDieta(1L, 1L, model, principal);

		// Assert
		assertThat(result).isEqualTo("sbadmin/pacientes/editar-dieta");
		verify(model).addAttribute("paciente", paciente);
		verify(model).addAttribute("pacienteDieta", pacienteDieta);
		log.info("finished testEditarAsignacionDieta");
	}

	@Test
	public void testActualizarAsignacionDieta() {
		log.info("starting testActualizarAsignacionDieta");
		// Arrange
		final Date startDate = new Date();
		final Date endDate = new Date(System.currentTimeMillis() + 86400000); // Tomorrow
		final PacienteDieta pacienteDieta = new PacienteDieta();
		pacienteDieta.setStartDate(startDate);
		pacienteDieta.setEndDate(endDate);
		pacienteDieta.setStatus(PacienteDietaStatus.COMPLETED);
		pacienteDieta.setNotes("Test notes for dietary plan");

		// Create existing entity with paciente and dieta set
		final PacienteDieta existing = new PacienteDieta();
		existing.setId(1L);
		existing.setPaciente(paciente);
		final com.nutriconsultas.dieta.Dieta dieta = new com.nutriconsultas.dieta.Dieta();
		dieta.setId(1L);
		dieta.setNombre("Dieta Test");
		existing.setDieta(dieta);
		existing.setStartDate(new Date());
		existing.setStatus(PacienteDietaStatus.ACTIVE);

		when(pacienteDietaService.findById(1L)).thenReturn(existing);
		when(pacienteDietaService.updateAssignment(any(Long.class), any(PacienteDieta.class)))
			.thenReturn(pacienteDieta);

		final Model model = org.mockito.Mockito.mock(Model.class);

		// Act
		final String result = controller.actualizarAsignacionDieta(1L, 1L, pacienteDieta, bindingResult, model,
				principal);

		// Assert
		assertThat(result).isEqualTo("redirect:/admin/pacientes/1/dietas");
		// Verify that paciente and dieta were set from existing entity
		assertThat(pacienteDieta.getPaciente()).isEqualTo(paciente);
		assertThat(pacienteDieta.getDieta()).isEqualTo(dieta);
		assertThat(pacienteDieta.getId()).isEqualTo(1L);
		verify(pacienteDietaService).findById(1L);
		verify(pacienteDietaService).updateAssignment(eq(1L), any(PacienteDieta.class));
		log.info("finished testActualizarAsignacionDieta");
	}

	@Test
	public void testActualizarAsignacionDietaWithValidationErrors() {
		log.info("starting testActualizarAsignacionDietaWithValidationErrors");
		// Arrange - test with null startDate (manual validation error)
		final PacienteDieta pacienteDieta = new PacienteDieta();
		pacienteDieta.setStartDate(null); // This should trigger validation error
		pacienteDieta.setStatus(PacienteDietaStatus.ACTIVE);

		// Create existing entity with paciente and dieta set
		final PacienteDieta existing = new PacienteDieta();
		existing.setId(1L);
		existing.setPaciente(paciente);
		final com.nutriconsultas.dieta.Dieta dieta = new com.nutriconsultas.dieta.Dieta();
		dieta.setId(1L);
		dieta.setNombre("Dieta Test");
		existing.setDieta(dieta);

		when(pacienteDietaService.findById(1L)).thenReturn(existing);
		when(pacienteRepository.findByIdAndUserId(1L, TEST_USER_ID)).thenReturn(java.util.Optional.of(paciente));

		final Model model = org.mockito.Mockito.mock(Model.class);

		// Act
		final String result = controller.actualizarAsignacionDieta(1L, 1L, pacienteDieta, bindingResult, model,
				principal);

		// Assert
		assertThat(result).isEqualTo("sbadmin/pacientes/editar-dieta");
		verify(pacienteDietaService).findById(1L);
		verify(bindingResult).rejectValue("startDate", "NotNull", "La fecha de inicio es requerida");
		verify(model).addAttribute("activeMenu", "perfil");
		verify(model).addAttribute("paciente", paciente);
		verify(model).addAttribute("pacienteDieta", pacienteDieta);
		verify(pacienteDietaService, org.mockito.Mockito.never()).updateAssignment(any(Long.class),
				any(PacienteDieta.class));
		log.info("finished testActualizarAsignacionDietaWithValidationErrors");
	}

	@Test
	public void testActualizarAsignacionDietaWithNullStatus() {
		log.info("starting testActualizarAsignacionDietaWithNullStatus");
		// Arrange - test with null status (manual validation error)
		final PacienteDieta pacienteDieta = new PacienteDieta();
		pacienteDieta.setStartDate(new Date());
		pacienteDieta.setStatus(null); // This should trigger validation error

		// Create existing entity with paciente and dieta set
		final PacienteDieta existing = new PacienteDieta();
		existing.setId(1L);
		existing.setPaciente(paciente);
		final com.nutriconsultas.dieta.Dieta dieta = new com.nutriconsultas.dieta.Dieta();
		dieta.setId(1L);
		dieta.setNombre("Dieta Test");
		existing.setDieta(dieta);

		when(pacienteDietaService.findById(1L)).thenReturn(existing);
		when(pacienteRepository.findByIdAndUserId(1L, TEST_USER_ID)).thenReturn(java.util.Optional.of(paciente));

		final Model model = org.mockito.Mockito.mock(Model.class);

		// Act
		final String result = controller.actualizarAsignacionDieta(1L, 1L, pacienteDieta, bindingResult, model,
				principal);

		// Assert
		assertThat(result).isEqualTo("sbadmin/pacientes/editar-dieta");
		verify(pacienteDietaService).findById(1L);
		verify(bindingResult).rejectValue("status", "NotNull", "El estado es requerido");
		verify(model).addAttribute("activeMenu", "perfil");
		verify(model).addAttribute("paciente", paciente);
		verify(model).addAttribute("pacienteDieta", pacienteDieta);
		verify(pacienteDietaService, org.mockito.Mockito.never()).updateAssignment(any(Long.class),
				any(PacienteDieta.class));
		log.info("finished testActualizarAsignacionDietaWithNullStatus");
	}

	@Test
	public void testActualizarAsignacionDietaWhenNotFound() {
		log.info("starting testActualizarAsignacionDietaWhenNotFound");
		// Arrange
		final PacienteDieta pacienteDieta = new PacienteDieta();
		pacienteDieta.setStartDate(new Date());
		pacienteDieta.setStatus(PacienteDietaStatus.ACTIVE);

		when(pacienteDietaService.findById(1L)).thenReturn(null);

		final Model model = org.mockito.Mockito.mock(Model.class);

		// Act & Assert
		assertThatThrownBy(
				() -> controller.actualizarAsignacionDieta(1L, 1L, pacienteDieta, bindingResult, model, principal))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("No se ha encontrado asignación de dieta con id");

		verify(pacienteDietaService).findById(1L);
		verify(pacienteDietaService, org.mockito.Mockito.never()).updateAssignment(any(Long.class),
				any(PacienteDieta.class));
		log.info("finished testActualizarAsignacionDietaWhenNotFound");
	}

	@Test
	public void testCancelarAsignacionDieta() {
		log.info("starting testCancelarAsignacionDieta");
		// Arrange
		org.mockito.Mockito.doNothing().when(pacienteDietaService).cancelAssignment(1L);

		// Act
		final String result = controller.cancelarAsignacionDieta(1L, 1L);

		// Assert
		assertThat(result).isEqualTo("redirect:/admin/pacientes/1/dietas");
		verify(pacienteDietaService).cancelAssignment(1L);
		log.info("finished testCancelarAsignacionDieta");
	}

	@Test
	public void testDietasPaciente() {
		log.info("starting testDietasPaciente");
		// Arrange
		when(pacienteRepository.findByIdAndUserId(1L, TEST_USER_ID)).thenReturn(java.util.Optional.of(paciente));
		when(pacienteDietaService.findByPacienteId(1L)).thenReturn(new ArrayList<>());
		when(pacienteDietaService.findActiveByPacienteId(1L)).thenReturn(new ArrayList<>());
		when(dietaService.getDietas()).thenReturn(new ArrayList<>());

		final Model model = org.mockito.Mockito.mock(Model.class);

		// Act
		final String result = controller.dietasPaciente(1L, model, principal);

		// Assert
		assertThat(result).isEqualTo("sbadmin/pacientes/dietas");
		verify(model).addAttribute("activeMenu", "plan-alimentario");
		verify(model).addAttribute("paciente", paciente);
		verify(model).addAttribute("dietasAsignadas", new ArrayList<>());
		verify(model).addAttribute("dietasActivas", new ArrayList<>());
		verify(model).addAttribute("dietasDisponibles", new ArrayList<>());
		log.info("finished testDietasPaciente");
	}

	@Test
	public void testDietasPacienteCalculatesMacronutrientes() {
		log.info("starting testDietasPacienteCalculatesMacronutrientes");
		// Arrange
		final com.nutriconsultas.dieta.Dieta dieta = new com.nutriconsultas.dieta.Dieta();
		dieta.setId(1L);
		dieta.setNombre("Dieta de Prueba");
		// Create mock ingestas with platillos and alimentos
		final com.nutriconsultas.dieta.Ingesta ingesta = new com.nutriconsultas.dieta.Ingesta();
		ingesta.setId(1L);
		ingesta.setNombre("Desayuno");
		final com.nutriconsultas.dieta.PlatilloIngesta platillo = new com.nutriconsultas.dieta.PlatilloIngesta();
		platillo.setId(1L);
		platillo.setProteina(20.0);
		platillo.setLipidos(10.0);
		platillo.setHidratosDeCarbono(50.0);
		ingesta.getPlatillos().add(platillo);
		dieta.getIngestas().add(ingesta);

		final PacienteDieta pacienteDieta = new PacienteDieta();
		pacienteDieta.setId(1L);
		pacienteDieta.setPaciente(paciente);
		pacienteDieta.setDieta(dieta);
		pacienteDieta.setStartDate(new Date());
		pacienteDieta.setStatus(PacienteDietaStatus.ACTIVE);

		final List<PacienteDieta> dietasAsignadas = new ArrayList<>();
		dietasAsignadas.add(pacienteDieta);

		when(pacienteRepository.findByIdAndUserId(1L, TEST_USER_ID)).thenReturn(java.util.Optional.of(paciente));
		when(pacienteDietaService.findByPacienteId(1L)).thenReturn(dietasAsignadas);
		when(pacienteDietaService.findActiveByPacienteId(1L)).thenReturn(dietasAsignadas);
		when(dietaService.getDieta(1L)).thenReturn(dieta);
		when(dietaService.getDietas()).thenReturn(new ArrayList<>());

		final Model model = org.mockito.Mockito.mock(Model.class);

		// Act
		final String result = controller.dietasPaciente(1L, model, principal);

		// Assert
		assertThat(result).isEqualTo("sbadmin/pacientes/dietas");
		verify(model).addAttribute("activeMenu", "plan-alimentario");
		verify(model).addAttribute("paciente", paciente);
		// getDieta is called twice: once for dietasAsignadas and once for dietasActivas
		verify(dietaService, org.mockito.Mockito.times(2)).getDieta(1L);
		// Verify that macronutrientes were calculated and set
		assertThat(dieta.getProteina()).isEqualTo(20.0);
		assertThat(dieta.getLipidos()).isEqualTo(10.0);
		assertThat(dieta.getHidratosDeCarbono()).isEqualTo(50.0);
		// Verify that kilocalorías were calculated and set
		// Formula: protein * 4 + lipids * 9 + carbohydrates * 4
		// 20 * 4 + 10 * 9 + 50 * 4 = 80 + 90 + 200 = 370 kcal
		assertThat(dieta.getEnergia()).isEqualTo(370);
		log.info("finished testDietasPacienteCalculatesMacronutrientes");
	}

	@Test
	public void testDietasPacienteHandlesDietaWithoutIngestas() {
		log.info("starting testDietasPacienteHandlesDietaWithoutIngestas");
		// Arrange
		final com.nutriconsultas.dieta.Dieta dieta = new com.nutriconsultas.dieta.Dieta();
		dieta.setId(1L);
		dieta.setNombre("Dieta Vacía");
		// ingestas is initialized as empty ArrayList by default, so no need to set it

		final PacienteDieta pacienteDieta = new PacienteDieta();
		pacienteDieta.setId(1L);
		pacienteDieta.setPaciente(paciente);
		pacienteDieta.setDieta(dieta);
		pacienteDieta.setStartDate(new Date());
		pacienteDieta.setStatus(PacienteDietaStatus.ACTIVE);

		final List<PacienteDieta> dietasAsignadas = new ArrayList<>();
		dietasAsignadas.add(pacienteDieta);

		when(pacienteRepository.findByIdAndUserId(1L, TEST_USER_ID)).thenReturn(java.util.Optional.of(paciente));
		when(pacienteDietaService.findByPacienteId(1L)).thenReturn(dietasAsignadas);
		when(pacienteDietaService.findActiveByPacienteId(1L)).thenReturn(dietasAsignadas);
		when(dietaService.getDieta(1L)).thenReturn(dieta);
		when(dietaService.getDietas()).thenReturn(new ArrayList<>());

		final Model model = org.mockito.Mockito.mock(Model.class);

		// Act
		final String result = controller.dietasPaciente(1L, model, principal);

		// Assert
		assertThat(result).isEqualTo("sbadmin/pacientes/dietas");
		// getDieta is called twice: once for dietasAsignadas and once for dietasActivas
		verify(dietaService, org.mockito.Mockito.times(2)).getDieta(1L);
		// Verify that macronutrientes are 0 when there are no ingestas
		// Note: getTotalProteina returns 0.0 when ingestas is null or empty
		assertThat(dieta.getProteina()).isNotNull();
		assertThat(dieta.getProteina()).isEqualTo(0.0);
		assertThat(dieta.getLipidos()).isNotNull();
		assertThat(dieta.getLipidos()).isEqualTo(0.0);
		assertThat(dieta.getHidratosDeCarbono()).isNotNull();
		assertThat(dieta.getHidratosDeCarbono()).isEqualTo(0.0);
		// Verify that kilocalorías are 0 when there are no ingestas
		assertThat(dieta.getEnergia()).isNotNull();
		assertThat(dieta.getEnergia()).isEqualTo(0);
		log.info("finished testDietasPacienteHandlesDietaWithoutIngestas");
	}

	@Test
	public void testDietasPacienteThrowsExceptionWhenPacienteNotFound() {
		log.info("starting testDietasPacienteThrowsExceptionWhenPacienteNotFound");
		// Arrange
		when(pacienteRepository.findByIdAndUserId(1L, TEST_USER_ID)).thenReturn(java.util.Optional.empty());

		final Model model = org.mockito.Mockito.mock(Model.class);

		// Act & Assert
		assertThatThrownBy(() -> controller.dietasPaciente(1L, model, principal))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("No se ha encontrado paciente con folio");
		log.info("finished testDietasPacienteThrowsExceptionWhenPacienteNotFound");
	}

	@Test
	public void testClinicosPaciente() {
		log.info("starting testClinicosPaciente");
		// Arrange
		when(pacienteRepository.findByIdAndUserId(1L, TEST_USER_ID)).thenReturn(java.util.Optional.of(paciente));

		final Model model = org.mockito.Mockito.mock(Model.class);

		// Act
		final String result = controller.clinicosPaciente(1L, model, principal);

		// Assert
		assertThat(result).isEqualTo("sbadmin/pacientes/clinicos");
		verify(model).addAttribute("activeMenu", "historial");
		verify(model).addAttribute("paciente", paciente);
		verify(model).addAttribute(eq("clinicos"), any(ClinicalExam.class));
		log.info("finished testClinicosPaciente");
	}

	@Test
	public void testClinicosPacienteThrowsExceptionWhenPacienteNotFound() {
		log.info("starting testClinicosPacienteThrowsExceptionWhenPacienteNotFound");
		// Arrange
		when(pacienteRepository.findByIdAndUserId(1L, TEST_USER_ID)).thenReturn(java.util.Optional.empty());

		final Model model = org.mockito.Mockito.mock(Model.class);

		// Act & Assert
		assertThatThrownBy(() -> controller.clinicosPaciente(1L, model, principal))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("No se ha encontrado paciente con folio");
		log.info("finished testClinicosPacienteThrowsExceptionWhenPacienteNotFound");
	}

	@Test
	public void testAgregarClinicosPacienteCalculatesBodyFat() {
		log.info("starting testAgregarClinicosPacienteCalculatesBodyFat");
		// Arrange
		final ClinicalExam exam = new ClinicalExam();
		exam.setPeso(70.0);
		exam.setEstatura(1.75);
		exam.setExamDateTime(new Date());
		exam.setTitle("Examen Clínico");

		when(pacienteRepository.findByIdAndUserId(1L, TEST_USER_ID)).thenReturn(java.util.Optional.of(paciente));
		when(clinicalExamService.save(Objects.requireNonNull(exam))).thenReturn(exam);
		when(bodyFatCalculatorService.calculateBodyFatPercentage(any(Double.class), any(Integer.class),
				any(String.class)))
			.thenReturn(15.5);

		// Act
		final String result = controller.agregarClinicosPaciente(1L, Objects.requireNonNull(exam), bindingResult, null,
				principal);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result).contains("redirect:/admin/pacientes/1/historial");
		verify(pacienteRepository).findByIdAndUserId(1L, TEST_USER_ID);
		verify(clinicalExamService).save(any(ClinicalExam.class));
		verify(bodyFatCalculatorService).calculateBodyFatPercentage(any(Double.class), any(Integer.class),
				any(String.class));
		// Verify that exam has IMC calculated
		assertThat(exam.getImc()).isNotNull();
		assertThat(exam.getNivelPeso()).isNotNull();
		// Verify that body fat was calculated
		assertThat(exam.getIndiceGrasaCorporal()).isNotNull();
		assertThat(exam.getIndiceGrasaCorporal()).isEqualTo(15.5);
		// Verify title is set to "Examen Clínico"
		assertThat(exam.getTitle()).isEqualTo("Examen Clínico");
		log.info("finished testAgregarClinicosPacienteCalculatesBodyFat");
	}

	@Test
	public void testAgregarClinicosPacienteWithoutDob() {
		log.info("starting testAgregarClinicosPacienteWithoutDob");
		// Arrange
		paciente.setDob(null);
		final ClinicalExam exam = new ClinicalExam();
		exam.setPeso(70.0);
		exam.setEstatura(1.75);
		exam.setExamDateTime(new Date());
		exam.setTitle("Examen Clínico");

		when(pacienteRepository.findByIdAndUserId(1L, TEST_USER_ID)).thenReturn(java.util.Optional.of(paciente));
		when(clinicalExamService.save(Objects.requireNonNull(exam))).thenReturn(exam);

		// Act
		final String result = controller.agregarClinicosPaciente(1L, Objects.requireNonNull(exam), bindingResult, null,
				principal);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result).contains("redirect:/admin/pacientes/1/historial");
		verify(pacienteRepository).findByIdAndUserId(1L, TEST_USER_ID);
		verify(clinicalExamService).save(any(ClinicalExam.class));
		// Body fat should not be calculated if DOB is missing
		verify(bodyFatCalculatorService, org.mockito.Mockito.never()).calculateBodyFatPercentage(any(Double.class),
				any(Integer.class), any(String.class));
		assertThat(exam.getIndiceGrasaCorporal()).isNull();
		log.info("finished testAgregarClinicosPacienteWithoutDob");
	}

	@Test
	public void testAgregarClinicosPacienteUpdatesPatientWeight() {
		log.info("starting testAgregarClinicosPacienteUpdatesPatientWeight");
		// Arrange
		final ClinicalExam exam = new ClinicalExam();
		exam.setPeso(70.0);
		exam.setEstatura(1.75);
		exam.setExamDateTime(new Date());
		exam.setTitle("Examen Clínico");

		when(pacienteRepository.findByIdAndUserId(1L, TEST_USER_ID)).thenReturn(java.util.Optional.of(paciente));
		when(clinicalExamService.save(any(ClinicalExam.class))).thenReturn(exam);
		when(pacienteRepository.save(any(Paciente.class))).thenReturn(paciente);
		when(bodyFatCalculatorService.calculateBodyFatPercentage(any(Double.class), any(Integer.class),
				any(String.class)))
			.thenReturn(15.5);

		// Act
		final String result = controller.agregarClinicosPaciente(1L, exam, bindingResult, null, principal);

		// Assert
		assertThat(result).isNotNull();
		verify(pacienteRepository).save(Objects.requireNonNull(paciente));
		// Verify patient weight was updated
		assertThat(paciente.getPeso()).isEqualTo(70.0);
		assertThat(paciente.getEstatura()).isEqualTo(1.75);
		assertThat(paciente.getImc()).isNotNull();
		assertThat(paciente.getNivelPeso()).isNotNull();
		log.info("finished testAgregarClinicosPacienteUpdatesPatientWeight");
	}

	@Test
	public void testAgregarClinicosPacienteSetsDefaultValues() {
		log.info("starting testAgregarClinicosPacienteSetsDefaultValues");
		// Arrange
		final ClinicalExam exam = new ClinicalExam();
		exam.setPeso(70.0);
		exam.setEstatura(1.75);
		exam.setExamDateTime(new Date());
		// Title is null - should be set to "Examen Clínico"
		exam.setTitle(null);

		when(pacienteRepository.findByIdAndUserId(1L, TEST_USER_ID)).thenReturn(java.util.Optional.of(paciente));
		when(clinicalExamService.save(any(ClinicalExam.class))).thenReturn(exam);
		when(pacienteRepository.save(any(Paciente.class))).thenReturn(paciente);
		when(bodyFatCalculatorService.calculateBodyFatPercentage(any(Double.class), any(Integer.class),
				any(String.class)))
			.thenReturn(15.5);

		// Act
		final String result = controller.agregarClinicosPaciente(1L, exam, bindingResult, null, principal);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result).contains("redirect:/admin/pacientes/1/historial");
		// Verify default values were set
		assertThat(exam.getTitle()).isEqualTo("Examen Clínico");
		log.info("finished testAgregarClinicosPacienteSetsDefaultValues");
	}

	@Test
	public void testAgregarClinicosPacienteThrowsExceptionWhenPacienteNotFound() {
		log.info("starting testAgregarClinicosPacienteThrowsExceptionWhenPacienteNotFound");
		// Arrange
		final ClinicalExam exam = new ClinicalExam();
		exam.setPeso(70.0);
		exam.setEstatura(1.75);
		exam.setExamDateTime(new Date());
		exam.setTitle("Examen Clínico");

		when(pacienteRepository.findByIdAndUserId(1L, TEST_USER_ID)).thenReturn(java.util.Optional.empty());

		// Act & Assert
		assertThatThrownBy(() -> controller.agregarClinicosPaciente(1L, exam, bindingResult, null, principal))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("No se ha encontrado paciente con folio");
		verify(pacienteRepository).findByIdAndUserId(1L, TEST_USER_ID);
		verify(clinicalExamService, org.mockito.Mockito.never()).save(any(ClinicalExam.class));
		log.info("finished testAgregarClinicosPacienteThrowsExceptionWhenPacienteNotFound");
	}

	@Test
	public void testVerExamenClinico() {
		log.info("starting testVerExamenClinico");
		// Arrange
		final ClinicalExam exam = new ClinicalExam();
		exam.setId(1L);
		exam.setPaciente(paciente);
		exam.setTitle("Examen Clínico");
		exam.setExamDateTime(new Date());
		exam.setPeso(70.0);
		exam.setImc(22.86);
		exam.setGlucosa(95.0);

		when(clinicalExamService.findById(1L)).thenReturn(exam);

		final Model model = org.mockito.Mockito.mock(Model.class);

		// Act
		final String result = controller.verExamenClinico(1L, 1L, model, principal);

		// Assert
		assertThat(result).isEqualTo("sbadmin/pacientes/ver-examen-clinico");
		verify(model).addAttribute("activeMenu", "historial");
		verify(model).addAttribute("exam", exam);
		verify(model).addAttribute("paciente", paciente);
		verify(clinicalExamService).findById(1L);
		log.info("finished testVerExamenClinico");
	}

	@Test
	public void testVerExamenClinicoThrowsExceptionWhenExamNotFound() {
		log.info("starting testVerExamenClinicoThrowsExceptionWhenExamNotFound");
		// Arrange
		when(clinicalExamService.findById(999L)).thenReturn(null);

		final Model model = org.mockito.Mockito.mock(Model.class);

		// Act & Assert
		assertThatThrownBy(() -> controller.verExamenClinico(1L, 999L, model, principal))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("No se ha encontrado examen clínico con id");
		verify(clinicalExamService).findById(999L);
		log.info("finished testVerExamenClinicoThrowsExceptionWhenExamNotFound");
	}

	@Test
	public void testVerExamenClinicoThrowsExceptionWhenWrongPaciente() {
		log.info("starting testVerExamenClinicoThrowsExceptionWhenWrongPaciente");
		// Arrange
		final Paciente otherPaciente = new Paciente();
		otherPaciente.setId(2L);
		otherPaciente.setName("Other Paciente");

		final ClinicalExam exam = new ClinicalExam();
		exam.setId(1L);
		exam.setPaciente(otherPaciente);
		exam.setTitle("Examen Clínico");

		when(clinicalExamService.findById(1L)).thenReturn(exam);

		final Model model = org.mockito.Mockito.mock(Model.class);

		// Act & Assert
		assertThatThrownBy(() -> controller.verExamenClinico(1L, 1L, model, principal))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("El examen clínico no pertenece al paciente especificado");
		verify(clinicalExamService).findById(1L);
		log.info("finished testVerExamenClinicoThrowsExceptionWhenWrongPaciente");
	}

	@Test
	public void testAntropometricosPaciente() {
		log.info("starting testAntropometricosPaciente");
		// Arrange
		when(pacienteRepository.findByIdAndUserId(1L, TEST_USER_ID)).thenReturn(java.util.Optional.of(paciente));

		final Model model = org.mockito.Mockito.mock(Model.class);

		// Act
		final String result = controller.antropometricosPaciente(1L, model, principal);

		// Assert
		assertThat(result).isEqualTo("sbadmin/pacientes/antropometricos");
		verify(model).addAttribute("activeMenu", "historial");
		verify(model).addAttribute("paciente", paciente);
		verify(model).addAttribute(eq("antropometrico"),
				any(com.nutriconsultas.clinical.exam.AnthropometricMeasurement.class));
		verify(pacienteRepository).findByIdAndUserId(1L, TEST_USER_ID);
		log.info("finished testAntropometricosPaciente");
	}

	@Test
	public void testAntropometricosPacienteThrowsExceptionWhenPacienteNotFound() {
		log.info("starting testAntropometricosPacienteThrowsExceptionWhenPacienteNotFound");
		// Arrange
		when(pacienteRepository.findByIdAndUserId(999L, TEST_USER_ID)).thenReturn(java.util.Optional.empty());

		final Model model = org.mockito.Mockito.mock(Model.class);

		// Act & Assert
		assertThatThrownBy(() -> controller.antropometricosPaciente(999L, model, principal))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("No se ha encontrado paciente con folio");
		verify(pacienteRepository).findByIdAndUserId(999L, TEST_USER_ID);
		log.info("finished testAntropometricosPacienteThrowsExceptionWhenPacienteNotFound");
	}

	@Test
	public void testAgregarAntropometricosPaciente() {
		log.info("starting testAgregarAntropometricosPaciente");
		// Arrange
		final com.nutriconsultas.clinical.exam.AnthropometricMeasurement measurement = new com.nutriconsultas.clinical.exam.AnthropometricMeasurement();
		measurement.setMeasurementDateTime(new Date());
		measurement.setTitle("Medición Antropométrica");
		// Use convenience methods
		measurement.setPeso(70.0);
		measurement.setEstatura(1.75);

		when(pacienteRepository.findByIdAndUserId(1L, TEST_USER_ID)).thenReturn(java.util.Optional.of(paciente));
		when(anthropometricMeasurementService
			.save(any(com.nutriconsultas.clinical.exam.AnthropometricMeasurement.class))).thenReturn(measurement);
		when(pacienteRepository.save(any(Paciente.class))).thenReturn(paciente);
		when(bodyFatCalculatorService.calculateBodyFatPercentage(any(Double.class), any(Integer.class),
				any(String.class)))
			.thenReturn(15.5);

		final Model model = org.mockito.Mockito.mock(Model.class);

		// Act
		final String result = controller.agregarAntropometricosPaciente(1L, measurement, bindingResult, model,
				principal);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result).contains("redirect:/admin/pacientes/1/historial");
		verify(pacienteRepository).findByIdAndUserId(1L, TEST_USER_ID);
		verify(anthropometricMeasurementService)
			.save(any(com.nutriconsultas.clinical.exam.AnthropometricMeasurement.class));
		log.info("finished testAgregarAntropometricosPaciente");
	}

	@Test
	public void testAgregarAntropometricosPacienteWithCategoryObjects() {
		log.info("starting testAgregarAntropometricosPacienteWithCategoryObjects");
		// Arrange
		final com.nutriconsultas.clinical.exam.AnthropometricMeasurement measurement = new com.nutriconsultas.clinical.exam.AnthropometricMeasurement();
		measurement.setMeasurementDateTime(new Date());
		measurement.setTitle("Medición Antropométrica");

		// Set up category objects directly
		final com.nutriconsultas.clinical.exam.anthropometric.BodyMass bodyMass = new com.nutriconsultas.clinical.exam.anthropometric.BodyMass();
		bodyMass.setWeight(70.0);
		bodyMass.setHeight(1.75);
		measurement.setBodyMass(bodyMass);

		final com.nutriconsultas.clinical.exam.anthropometric.Circumferences circumferences = new com.nutriconsultas.clinical.exam.anthropometric.Circumferences();
		circumferences.setWaistCircumference(80.0);
		circumferences.setHipCircumference(95.0);
		measurement.setCircumferences(circumferences);

		when(pacienteRepository.findByIdAndUserId(1L, TEST_USER_ID)).thenReturn(java.util.Optional.of(paciente));
		when(anthropometricMeasurementService
			.save(any(com.nutriconsultas.clinical.exam.AnthropometricMeasurement.class))).thenReturn(measurement);
		when(pacienteRepository.save(any(Paciente.class))).thenReturn(paciente);
		when(bodyFatCalculatorService.calculateBodyFatPercentage(any(Double.class), any(Integer.class),
				any(String.class)))
			.thenReturn(15.5);

		final Model model = org.mockito.Mockito.mock(Model.class);

		// Act
		final String result = controller.agregarAntropometricosPaciente(1L, measurement, bindingResult, model,
				principal);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result).contains("redirect:/admin/pacientes/1/historial");
		// Verify category objects are preserved
		assertThat(measurement.getBodyMass()).isNotNull();
		assertThat(measurement.getBodyMass().getWeight()).isEqualTo(70.0);
		assertThat(measurement.getCircumferences()).isNotNull();
		assertThat(measurement.getCircumferences().getWaistCircumference()).isEqualTo(80.0);
		verify(pacienteRepository).findByIdAndUserId(1L, TEST_USER_ID);
		verify(anthropometricMeasurementService)
			.save(any(com.nutriconsultas.clinical.exam.AnthropometricMeasurement.class));
		log.info("finished testAgregarAntropometricosPacienteWithCategoryObjects");
	}

	@Test
	public void testAgregarAntropometricosPacienteThrowsExceptionWhenPacienteNotFound() {
		log.info("starting testAgregarAntropometricosPacienteThrowsExceptionWhenPacienteNotFound");
		// Arrange
		final com.nutriconsultas.clinical.exam.AnthropometricMeasurement measurement = new com.nutriconsultas.clinical.exam.AnthropometricMeasurement();
		measurement.setMeasurementDateTime(new Date());
		measurement.setTitle("Medición Antropométrica");

		when(pacienteRepository.findByIdAndUserId(999L, TEST_USER_ID)).thenReturn(java.util.Optional.empty());

		final Model model = org.mockito.Mockito.mock(Model.class);

		// Act & Assert
		assertThatThrownBy(
				() -> controller.agregarAntropometricosPaciente(999L, measurement, bindingResult, model, principal))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("No se ha encontrado paciente con folio");
		verify(pacienteRepository).findByIdAndUserId(999L, TEST_USER_ID);
		verify(anthropometricMeasurementService, org.mockito.Mockito.never())
			.save(any(com.nutriconsultas.clinical.exam.AnthropometricMeasurement.class));
		log.info("finished testAgregarAntropometricosPacienteThrowsExceptionWhenPacienteNotFound");
	}

	@Test
	public void testVerAntropometrico() {
		log.info("starting testVerAntropometrico");
		// Arrange
		final com.nutriconsultas.clinical.exam.AnthropometricMeasurement measurement = new com.nutriconsultas.clinical.exam.AnthropometricMeasurement();
		measurement.setId(1L);
		measurement.setPaciente(paciente);
		measurement.setTitle("Medición Antropométrica");
		measurement.setMeasurementDateTime(new Date());
		measurement.setPeso(70.0);
		measurement.setImc(22.86);

		when(anthropometricMeasurementService.findById(1L)).thenReturn(measurement);

		final Model model = org.mockito.Mockito.mock(Model.class);

		// Act
		final String result = controller.verAntropometrico(1L, 1L, model, principal);

		// Assert
		assertThat(result).isEqualTo("sbadmin/pacientes/ver-antropometrico");
		verify(model).addAttribute("activeMenu", "historial");
		verify(model).addAttribute("measurement", measurement);
		verify(model).addAttribute("paciente", paciente);
		verify(anthropometricMeasurementService).findById(1L);
		log.info("finished testVerAntropometrico");
	}

	@Test
	public void testVerAntropometricoThrowsExceptionWhenMeasurementNotFound() {
		log.info("starting testVerAntropometricoThrowsExceptionWhenMeasurementNotFound");
		// Arrange
		when(anthropometricMeasurementService.findById(999L)).thenReturn(null);

		final Model model = org.mockito.Mockito.mock(Model.class);

		// Act & Assert
		assertThatThrownBy(() -> controller.verAntropometrico(1L, 999L, model, principal))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("No se ha encontrado medición antropométrica con id");
		verify(anthropometricMeasurementService).findById(999L);
		log.info("finished testVerAntropometricoThrowsExceptionWhenMeasurementNotFound");
	}

	@Test
	public void testVerAntropometricoThrowsExceptionWhenWrongPaciente() {
		log.info("starting testVerAntropometricoThrowsExceptionWhenWrongPaciente");
		// Arrange
		final Paciente otherPaciente = new Paciente();
		otherPaciente.setId(2L);
		otherPaciente.setName("Other Paciente");

		final com.nutriconsultas.clinical.exam.AnthropometricMeasurement measurement = new com.nutriconsultas.clinical.exam.AnthropometricMeasurement();
		measurement.setId(1L);
		measurement.setPaciente(otherPaciente);
		measurement.setTitle("Medición Antropométrica");

		when(anthropometricMeasurementService.findById(1L)).thenReturn(measurement);

		final Model model = org.mockito.Mockito.mock(Model.class);

		// Act & Assert
		assertThatThrownBy(() -> controller.verAntropometrico(1L, 1L, model, principal))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("La medición antropométrica no pertenece al paciente especificado");
		verify(anthropometricMeasurementService).findById(1L);
		log.info("finished testVerAntropometricoThrowsExceptionWhenWrongPaciente");
	}

}
