package com.nutriconsultas.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import com.nutriconsultas.calendar.CalendarEvent;
import com.nutriconsultas.calendar.CalendarEventRepository;
import com.nutriconsultas.calendar.EventStatus;
import com.nutriconsultas.paciente.Paciente;
import com.nutriconsultas.paciente.PacienteRepository;

@ExtendWith(MockitoExtension.class)
class GetPatientAppointmentsToolServiceTest {

	private static final String NUTRITIONIST_ID = "auth0|nutritionist-a";

	private static final long PATIENT_ID = 5L;

	@InjectMocks
	private GetPatientAppointmentsToolServiceImpl service;

	@Mock
	private PacienteRepository pacienteRepository;

	@Mock
	private CalendarEventRepository calendarEventRepository;

	@Test
	void getAppointmentsReturnsUpcomingForOwnedPatient() {
		when(pacienteRepository.findByIdAndUserId(PATIENT_ID, NUTRITIONIST_ID)).thenReturn(Optional.of(new Paciente()));
		final CalendarEvent event = scheduledEvent(10L, "Consulta de seguimiento", 60);
		when(calendarEventRepository.findUpcomingByPacienteId(eq(PATIENT_ID),
				org.mockito.ArgumentMatchers.any(Date.class), eq(EventStatus.SCHEDULED),
				org.mockito.ArgumentMatchers.any(Pageable.class)))
			.thenReturn(List.of(event));

		final AiToolResult<PatientAppointmentsData> result = service.getAppointments(NUTRITIONIST_ID, PATIENT_ID,
				PatientAppointmentScope.UPCOMING, null);

		assertThat(result.success()).isTrue();
		assertThat(result.data().upcoming()).hasSize(1);
		assertThat(result.data().upcoming().get(0).eventId()).isEqualTo(10L);
		assertThat(result.data().upcoming().get(0).title()).isEqualTo("Consulta de seguimiento");
		assertThat(result.data().upcoming().get(0).status()).isEqualTo("SCHEDULED");
		assertThat(result.data().past()).isEmpty();
		verify(calendarEventRepository).findUpcomingByPacienteId(eq(PATIENT_ID),
				org.mockito.ArgumentMatchers.any(Date.class), eq(EventStatus.SCHEDULED),
				org.mockito.ArgumentMatchers.any(Pageable.class));
	}

	@Test
	void getAppointmentsReturnsNotFoundWhenPatientNotOwned() {
		when(pacienteRepository.findByIdAndUserId(PATIENT_ID, NUTRITIONIST_ID)).thenReturn(Optional.empty());

		final AiToolResult<PatientAppointmentsData> result = service.getAppointments(NUTRITIONIST_ID, PATIENT_ID,
				PatientAppointmentScope.UPCOMING, 5);

		assertThat(result.success()).isFalse();
		assertThat(result.errorCode()).isEqualTo(AiToolErrorCode.NOT_FOUND);
	}

	@Test
	void getAppointmentsIncludesPastWhenScopeAll() {
		when(pacienteRepository.findByIdAndUserId(PATIENT_ID, NUTRITIONIST_ID)).thenReturn(Optional.of(new Paciente()));
		when(calendarEventRepository.findUpcomingByPacienteId(eq(PATIENT_ID),
				org.mockito.ArgumentMatchers.any(Date.class), eq(EventStatus.SCHEDULED),
				org.mockito.ArgumentMatchers.any(Pageable.class)))
			.thenReturn(List.of(scheduledEvent(11L, "Próxima", 45)));
		when(calendarEventRepository.findPastByPacienteId(eq(PATIENT_ID), org.mockito.ArgumentMatchers.any(Date.class),
				eq(EventStatus.COMPLETED), org.mockito.ArgumentMatchers.any(Pageable.class)))
			.thenReturn(List.of(scheduledEvent(9L, "Anterior", 30)));

		final AiToolResult<PatientAppointmentsData> result = service.getAppointments(NUTRITIONIST_ID, PATIENT_ID,
				PatientAppointmentScope.ALL, 3);

		assertThat(result.success()).isTrue();
		assertThat(result.data().upcoming()).hasSize(1);
		assertThat(result.data().past()).hasSize(1);
		assertThat(result.data().totalReturned()).isEqualTo(2);
	}

	private static CalendarEvent scheduledEvent(final long id, final String title, final int durationMinutes) {
		final CalendarEvent event = new CalendarEvent();
		event.setId(id);
		event.setTitle(title);
		event.setDurationMinutes(durationMinutes);
		event.setStatus(EventStatus.SCHEDULED);
		event.setEventDateTime(new Date());
		return event;
	}

}
