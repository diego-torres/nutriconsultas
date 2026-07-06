package com.nutriconsultas.mobile;

import java.time.Instant;
import java.util.Date;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.nutriconsultas.calendar.CalendarEvent;
import com.nutriconsultas.calendar.CalendarEventPatientVisitSpecifications;
import com.nutriconsultas.calendar.CalendarEventRepository;
import com.nutriconsultas.calendar.EventStatus;
import com.nutriconsultas.mobile.dto.PagedResponse;
import com.nutriconsultas.mobile.dto.VisitDetailDto;
import com.nutriconsultas.mobile.dto.VisitSummaryDto;
import com.nutriconsultas.util.LogRedaction;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class MobilePatientVisitService {

	private static final int MAX_PAGE_SIZE = 100;

	private final CalendarEventRepository calendarEventRepository;

	public MobilePatientVisitService(final CalendarEventRepository calendarEventRepository) {
		this.calendarEventRepository = calendarEventRepository;
	}

	@Transactional(readOnly = true)
	public PagedResponse<VisitSummaryDto> listVisits(final Long pacienteId, final int page, final int size,
			final EventStatus status, final Instant from, final Instant to) {
		final int safePage = Math.max(page, 0);
		final int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
		final Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "eventDateTime"));
		final Date fromDate = from != null ? Date.from(from) : null;
		final Date toDate = to != null ? Date.from(to) : null;
		final Page<CalendarEvent> events = calendarEventRepository.findAll(
				CalendarEventPatientVisitSpecifications.forPatient(pacienteId, status, fromDate, toDate), pageable);
		final Page<VisitSummaryDto> summaries = events.map(VisitSummaryDto::fromEntity);
		if (log.isDebugEnabled()) {
			log.debug("Listed mobile visits page={} size={} count={} for patient {}", safePage, safeSize,
					summaries.getNumberOfElements(), LogRedaction.redactPaciente(pacienteId));
		}
		return PagedResponse.of(summaries);
	}

	@Transactional(readOnly = true)
	public VisitDetailDto getVisitDetail(final Long pacienteId, final Long visitId) {
		return calendarEventRepository.findByIdAndPacienteId(visitId, pacienteId)
			.map(VisitDetailDto::fromEntity)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
	}

}
