package com.nutriconsultas.calendar;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

import com.nutriconsultas.paciente.Paciente;
import com.nutriconsultas.paciente.PacienteRepository;

@DataJpaTest
@ActiveProfiles("test")
class CalendarEventPatientVisitSpecificationsTest {

	@Autowired
	private CalendarEventRepository calendarEventRepository;

	@Autowired
	private PacienteRepository pacienteRepository;

	private Paciente paciente;

	@BeforeEach
	void seedPaciente() {
		paciente = pacienteRepository.saveAndFlush(samplePaciente());
	}

	@Test
	void findAll_withNullOptionalFilters_returnsPatientVisits() {
		calendarEventRepository.saveAndFlush(sampleEvent("Consulta A", EventStatus.SCHEDULED, 3));
		calendarEventRepository.saveAndFlush(sampleEvent("Consulta B", EventStatus.COMPLETED, -1));

		final Page<CalendarEvent> page = calendarEventRepository.findAll(
				CalendarEventPatientVisitSpecifications.forPatient(paciente.getId(), null, null, null),
				PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "eventDateTime")));

		assertThat(page.getTotalElements()).isEqualTo(2);
		assertThat(page.getContent().get(0).getTitle()).isEqualTo("Consulta A");
	}

	@Test
	void findAll_withStatusFilter_returnsMatchingVisitsOnly() {
		calendarEventRepository.saveAndFlush(sampleEvent("Programada", EventStatus.SCHEDULED, 2));
		calendarEventRepository.saveAndFlush(sampleEvent("Completada", EventStatus.COMPLETED, -2));

		final Page<CalendarEvent> page = calendarEventRepository.findAll(
				CalendarEventPatientVisitSpecifications.forPatient(paciente.getId(), EventStatus.SCHEDULED, null, null),
				PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "eventDateTime")));

		assertThat(page.getTotalElements()).isEqualTo(1);
		assertThat(page.getContent().get(0).getTitle()).isEqualTo("Programada");
	}

	private CalendarEvent sampleEvent(final String title, final EventStatus status, final int dayOffset) {
		final CalendarEvent event = new CalendarEvent();
		event.setPaciente(paciente);
		event.setTitle(title);
		event.setStatus(status);
		event.setDurationMinutes(60);
		event.setEventDateTime(
				Date.from(LocalDate.now().plusDays(dayOffset).atStartOfDay(ZoneId.systemDefault()).toInstant()));
		return event;
	}

	private static Paciente samplePaciente() {
		final Paciente pacienteEntity = new Paciente();
		pacienteEntity.setName("Spec Visit Patient");
		pacienteEntity.setUserId("auth0|nutritionist-spec");
		pacienteEntity.setPatientAuthSub("auth0|patient-spec");
		final LocalDate dob = LocalDate.now().minusYears(30);
		pacienteEntity.setDob(Date.from(dob.atStartOfDay(ZoneId.systemDefault()).toInstant()));
		pacienteEntity.setGender("F");
		return pacienteEntity;
	}

}
