package com.nutriconsultas.mobile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.nutriconsultas.calendar.CalendarEvent;
import com.nutriconsultas.calendar.CalendarEventRepository;
import com.nutriconsultas.calendar.EventStatus;
import com.nutriconsultas.mobile.dto.PagedResponse;
import com.nutriconsultas.mobile.dto.VisitDetailDto;
import com.nutriconsultas.mobile.dto.VisitSummaryDto;
import com.nutriconsultas.paciente.NivelPeso;
import com.nutriconsultas.paciente.Paciente;

@ExtendWith(MockitoExtension.class)
class MobilePatientVisitServiceTest {

	@InjectMocks
	private MobilePatientVisitService service;

	@Mock
	private CalendarEventRepository calendarEventRepository;

	@Test
	void listVisits_mapsEventsToSummaryDtos() {
		final CalendarEvent event = sampleEvent(10L, "Consulta inicial", EventStatus.COMPLETED);
		final Page<CalendarEvent> page = new PageImpl<>(List.of(event), PageRequest.of(0, 20), 1);
		when(calendarEventRepository.findPatientVisits(eq(1L), eq(null), eq(null), eq(null), any(Pageable.class)))
			.thenReturn(page);

		final PagedResponse<VisitSummaryDto> result = service.listVisits(1L, 0, 20, null, null, null);

		assertThat(result.content()).hasSize(1);
		assertThat(result.content().get(0).id()).isEqualTo(10L);
		assertThat(result.content().get(0).title()).isEqualTo("Consulta inicial");
		assertThat(result.content().get(0).status()).isEqualTo(EventStatus.COMPLETED);
		assertThat(result.totalElements()).isEqualTo(1);
	}

	@Test
	void listVisits_capsPageSizeAt100() {
		when(calendarEventRepository.findPatientVisits(eq(1L), eq(null), eq(null), eq(null), any(Pageable.class)))
			.thenReturn(Page.empty());

		service.listVisits(1L, 0, 500, null, null, null);

		final ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
		verify(calendarEventRepository).findPatientVisits(eq(1L), eq(null), eq(null), eq(null),
				pageableCaptor.capture());
		assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(100);
		assertThat(pageableCaptor.getValue().getSort()).isEqualTo(Sort.by(Sort.Direction.DESC, "eventDateTime"));
	}

	@Test
	void listVisits_passesStatusAndDateFilters() {
		final Instant from = Instant.parse("2026-01-01T00:00:00Z");
		final Instant to = Instant.parse("2026-06-01T00:00:00Z");
		when(calendarEventRepository.findPatientVisits(eq(2L), eq(EventStatus.SCHEDULED), any(Date.class),
				any(Date.class), any(Pageable.class)))
			.thenReturn(Page.empty());

		service.listVisits(2L, 1, 10, EventStatus.SCHEDULED, from, to);

		verify(calendarEventRepository).findPatientVisits(eq(2L), eq(EventStatus.SCHEDULED), any(Date.class),
				any(Date.class), any(Pageable.class));
	}

	@Test
	void getVisitDetail_returnsDetailWhenOwnedByPatient() {
		final CalendarEvent event = sampleEvent(10L, "Consulta inicial", EventStatus.COMPLETED);
		event.setDescription("Notas de la consulta");
		event.setPeso(72.5);
		event.setNivelPeso(NivelPeso.NORMAL);
		when(calendarEventRepository.findByIdAndPacienteId(10L, 1L)).thenReturn(Optional.of(event));

		final VisitDetailDto result = service.getVisitDetail(1L, 10L);

		assertThat(result.id()).isEqualTo(10L);
		assertThat(result.description()).isEqualTo("Notas de la consulta");
		assertThat(result.peso()).isEqualTo(72.5);
		assertThat(result.nivelPeso()).isEqualTo(NivelPeso.NORMAL);
	}

	@Test
	void getVisitDetail_throwsNotFoundWhenMissingOrNotOwned() {
		when(calendarEventRepository.findByIdAndPacienteId(99L, 1L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.getVisitDetail(1L, 99L)).isInstanceOf(ResponseStatusException.class)
			.extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
			.isEqualTo(HttpStatus.NOT_FOUND);
	}

	private static CalendarEvent sampleEvent(final Long id, final String title, final EventStatus status) {
		final Paciente paciente = new Paciente();
		paciente.setId(1L);
		final CalendarEvent event = new CalendarEvent();
		event.setId(id);
		event.setPaciente(paciente);
		event.setTitle(title);
		event.setStatus(status);
		event.setDurationMinutes(60);
		event.setEventDateTime(Date.from(Instant.parse("2026-03-15T14:30:00Z")));
		event.setSummaryNotes("Seguimiento nutricional");
		return event;
	}

}
