package com.nutriconsultas.paciente;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Objects;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;

import com.nutriconsultas.calendar.CalendarEvent;
import com.nutriconsultas.calendar.CalendarEventService;
import com.nutriconsultas.calendar.EventStatus;

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
	private BindingResult bindingResult;

	private Paciente paciente;

	private CalendarEvent evento;

	@BeforeEach
	public void setup() {
		log.info("setting up PacienteController test");

		// Create test paciente with date of birth and gender
		paciente = new Paciente();
		paciente.setId(1L);
		paciente.setName("Juan Perez");
		paciente.setEmail("juan@example.com");
		paciente.setPhone("1234567890");
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

		log.info("finished setting up PacienteController test");
	}

	@Test
	public void testAgregarConsultaPacienteCalculatesBodyFat() {
		log.info("starting testAgregarConsultaPacienteCalculatesBodyFat");
		// Arrange
		when(pacienteRepository.findById(1L)).thenReturn(java.util.Optional.of(paciente));
		when(calendarEventService.save(Objects.requireNonNull(evento))).thenReturn(evento);
		when(bodyFatCalculatorService.calculateBodyFatPercentage(any(Double.class), any(Integer.class),
				any(String.class)))
			.thenReturn(15.5);

		// Act
		final String result = controller.agregarConsultaPaciente(1L, Objects.requireNonNull(evento), bindingResult,
				null);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result).contains("redirect:/admin/pacientes/1/historial");
		verify(pacienteRepository).findById(1L);
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
		when(pacienteRepository.findById(1L)).thenReturn(java.util.Optional.of(paciente));
		when(calendarEventService.save(Objects.requireNonNull(evento))).thenReturn(evento);

		// Act
		final String result = controller.agregarConsultaPaciente(1L, Objects.requireNonNull(evento), bindingResult,
				null);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result).contains("redirect:/admin/pacientes/1/historial");
		verify(pacienteRepository).findById(1L);
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
		when(pacienteRepository.findById(1L)).thenReturn(java.util.Optional.of(paciente));
		when(calendarEventService.save(Objects.requireNonNull(evento))).thenReturn(evento);

		// Act
		final String result = controller.agregarConsultaPaciente(1L, Objects.requireNonNull(evento), bindingResult,
				null);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result).contains("redirect:/admin/pacientes/1/historial");
		verify(pacienteRepository).findById(1L);
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
		when(pacienteRepository.findById(1L)).thenReturn(java.util.Optional.of(paciente));
		when(calendarEventService.save(any(CalendarEvent.class))).thenReturn(evento);
		when(pacienteRepository.save(any(Paciente.class))).thenReturn(paciente);
		when(bodyFatCalculatorService.calculateBodyFatPercentage(any(Double.class), any(Integer.class),
				any(String.class)))
			.thenReturn(15.5);

		// Act
		final String result = controller.agregarConsultaPaciente(1L, evento, bindingResult, null);

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
		when(pacienteRepository.findById(1L)).thenReturn(java.util.Optional.of(paciente));
		when(calendarEventService.save(any(CalendarEvent.class))).thenReturn(evento);
		when(bodyFatCalculatorService.calculateBodyFatPercentage(any(Double.class), any(Integer.class), eq("F")))
			.thenReturn(22.5);

		// Act
		final String result = controller.agregarConsultaPaciente(1L, evento, bindingResult, null);

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
		final Date pastDate = new Date(System.currentTimeMillis() - 86400000); // 1 day
																				// ago
		final CalendarEvent pastEvent = new CalendarEvent();
		pastEvent.setId(1L);
		pastEvent.setEventDateTime(pastDate);
		pastEvent.setStatus(EventStatus.COMPLETED);
		pastEvent.setPaciente(paciente);

		when(pacienteRepository.findById(1L)).thenReturn(java.util.Optional.of(paciente));
		when(calendarEventService.findByPacienteId(1L)).thenReturn(Arrays.asList(pastEvent));

		final Model model = org.mockito.Mockito.mock(Model.class);

		// Act
		final String result = controller.perfilPaciente(1L, model);

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
		final Date futureDate = new Date(System.currentTimeMillis() + 86400000); // 1 day
																					// from
																					// now
		final CalendarEvent futureEvent = new CalendarEvent();
		futureEvent.setId(2L);
		futureEvent.setEventDateTime(futureDate);
		futureEvent.setStatus(EventStatus.SCHEDULED);
		futureEvent.setPaciente(paciente);

		when(pacienteRepository.findById(1L)).thenReturn(java.util.Optional.of(paciente));
		when(calendarEventService.findByPacienteId(1L)).thenReturn(Arrays.asList(futureEvent));

		final Model model = org.mockito.Mockito.mock(Model.class);

		// Act
		final String result = controller.perfilPaciente(1L, model);

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
		final Date pastDate = new Date(System.currentTimeMillis() - 86400000); // 1 day
																				// ago
		final Date futureDate = new Date(System.currentTimeMillis() + 86400000); // 1 day
																					// from
																					// now

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

		when(pacienteRepository.findById(1L)).thenReturn(java.util.Optional.of(paciente));
		when(calendarEventService.findByPacienteId(1L)).thenReturn(Arrays.asList(pastEvent, futureEvent));

		final Model model = org.mockito.Mockito.mock(Model.class);

		// Act
		final String result = controller.perfilPaciente(1L, model);

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
		final Date pastDate = new Date(System.currentTimeMillis() - 86400000); // 1 day
																				// ago
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

		when(pacienteRepository.findById(1L)).thenReturn(java.util.Optional.of(paciente));
		when(calendarEventService.findByPacienteId(1L)).thenReturn(Arrays.asList(cancelledEvent, scheduledPastEvent));

		final Model model = org.mockito.Mockito.mock(Model.class);

		// Act
		final String result = controller.perfilPaciente(1L, model);

		// Assert
		assertThat(result).isEqualTo("sbadmin/pacientes/perfil");
		verify(model).addAttribute("citaAnterior", "");
		log.info("finished testPerfilPacienteIgnoresNonCompletedPastEvents");
	}

	@Test
	public void testPerfilPacienteIgnoresNonScheduledFutureEvents() {
		log.info("starting testPerfilPacienteIgnoresNonScheduledFutureEvents");
		// Arrange
		final Date futureDate = new Date(System.currentTimeMillis() + 86400000); // 1 day
																					// from
																					// now
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

		when(pacienteRepository.findById(1L)).thenReturn(java.util.Optional.of(paciente));
		when(calendarEventService.findByPacienteId(1L))
			.thenReturn(Arrays.asList(completedFutureEvent, cancelledFutureEvent));

		final Model model = org.mockito.Mockito.mock(Model.class);

		// Act
		final String result = controller.perfilPaciente(1L, model);

		// Assert
		assertThat(result).isEqualTo("sbadmin/pacientes/perfil");
		verify(model).addAttribute("citaSiguiente", "");
		log.info("finished testPerfilPacienteIgnoresNonScheduledFutureEvents");
	}

	@Test
	public void testPerfilPacienteSelectsMostRecentCompletedPastEvent() {
		log.info("starting testPerfilPacienteSelectsMostRecentCompletedPastEvent");
		// Arrange
		final Date olderDate = new Date(System.currentTimeMillis() - 172800000); // 2 days
																					// ago
		final Date recentDate = new Date(System.currentTimeMillis() - 86400000); // 1 day
																					// ago

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

		when(pacienteRepository.findById(1L)).thenReturn(java.util.Optional.of(paciente));
		when(calendarEventService.findByPacienteId(1L)).thenReturn(Arrays.asList(olderEvent, recentEvent));

		final Model model = org.mockito.Mockito.mock(Model.class);

		// Act
		final String result = controller.perfilPaciente(1L, model);

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
		final Date nearFutureDate = new Date(System.currentTimeMillis() + 86400000); // 1
																						// day
																						// from
																						// now
		final Date farFutureDate = new Date(System.currentTimeMillis() + 172800000); // 2
																						// days
																						// from
																						// now

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

		when(pacienteRepository.findById(1L)).thenReturn(java.util.Optional.of(paciente));
		when(calendarEventService.findByPacienteId(1L)).thenReturn(Arrays.asList(nearFutureEvent, farFutureEvent));

		final Model model = org.mockito.Mockito.mock(Model.class);

		// Act
		final String result = controller.perfilPaciente(1L, model);

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
		when(pacienteRepository.findById(1L)).thenReturn(java.util.Optional.of(paciente));
		when(calendarEventService.findByPacienteId(1L)).thenReturn(new ArrayList<>());

		final Model model = org.mockito.Mockito.mock(Model.class);

		// Act
		final String result = controller.perfilPaciente(1L, model);

		// Assert
		assertThat(result).isEqualTo("sbadmin/pacientes/perfil");
		verify(model).addAttribute("paciente", paciente);
		verify(model).addAttribute("citaAnterior", "");
		verify(model).addAttribute("citaSiguiente", "");
		log.info("finished testPerfilPacienteWithNoEvents");
	}

}
