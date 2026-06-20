package com.nutriconsultas.paciente;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import com.nutriconsultas.calendar.CalendarEvent;
import com.nutriconsultas.calendar.CalendarEventService;
import com.nutriconsultas.clinical.exam.AnthropometricMeasurement;
import com.nutriconsultas.clinical.exam.AnthropometricMeasurementService;
import com.nutriconsultas.clinical.exam.ClinicalExam;
import com.nutriconsultas.clinical.exam.ClinicalExamService;
import com.nutriconsultas.message.PatientMessageRepository;
import com.nutriconsultas.paciente.metrics.BodyMetricRecordRepository;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
@SuppressWarnings("null")
class PacienteDeletionServiceTest {

	private static final String USER_ID = "nutritionist-owner";

	@InjectMocks
	private PacienteDeletionServiceImpl service;

	@Mock
	private PacienteRepository pacienteRepository;

	@Mock
	private PatientMessageRepository patientMessageRepository;

	@Mock
	private PatientInvitationRepository patientInvitationRepository;

	@Mock
	private PacienteDietaRepository pacienteDietaRepository;

	@Mock
	private CalendarEventService calendarEventService;

	@Mock
	private ClinicalExamService clinicalExamService;

	@Mock
	private AnthropometricMeasurementService anthropometricMeasurementService;

	@Mock
	private BodyMetricRecordRepository bodyMetricRecordRepository;

	private Paciente paciente;

	@BeforeEach
	void setUp() {
		paciente = new Paciente();
		paciente.setId(7L);
		paciente.setUserId(USER_ID);
		paciente.setName("Test Patient");
		paciente.setPatientAuthSub("auth0|linked-patient");
	}

	@Test
	void deletePatientWithHistory_removesHistoryAndPatient() {
		final CalendarEvent event = new CalendarEvent();
		event.setId(10L);
		final ClinicalExam exam = new ClinicalExam();
		exam.setId(20L);
		final AnthropometricMeasurement measurement = new AnthropometricMeasurement();
		measurement.setId(30L);
		final PacienteDieta assignment = new PacienteDieta();
		assignment.setId(40L);
		final PatientInvitation invitation = new PatientInvitation();
		invitation.setId(50L);

		when(pacienteRepository.findByIdAndUserId(7L, USER_ID)).thenReturn(Optional.of(paciente));
		when(patientInvitationRepository.findByPacienteId(7L)).thenReturn(List.of(invitation));
		when(pacienteDietaRepository.findByPacienteId(7L)).thenReturn(List.of(assignment));
		when(calendarEventService.findByPacienteId(7L)).thenReturn(List.of(event));
		when(clinicalExamService.findByPacienteId(7L)).thenReturn(List.of(exam));
		when(anthropometricMeasurementService.findByPacienteId(7L)).thenReturn(List.of(measurement));

		service.deletePatientWithHistory(7L, USER_ID);

		verify(patientMessageRepository).deleteByPacienteId(7L);
		verify(patientInvitationRepository).deleteAll(List.of(invitation));
		verify(pacienteDietaRepository).deleteAll(List.of(assignment));
		verify(calendarEventService).delete(10L);
		verify(clinicalExamService).deleteById(20L);
		verify(anthropometricMeasurementService).deleteById(30L);
		verify(bodyMetricRecordRepository).deleteByPacienteId(7L);
		verify(pacienteRepository).delete(paciente);
	}

	@Test
	void deletePatientWithHistory_throwsWhenPatientNotOwned() {
		when(pacienteRepository.findByIdAndUserId(7L, USER_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.deletePatientWithHistory(7L, USER_ID))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Paciente no encontrado");

		verify(patientMessageRepository, never()).deleteByPacienteId(7L);
		verify(pacienteRepository, never()).delete(paciente);
	}

}
