package com.nutriconsultas.calendar;

import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class CalendarEventServiceImpl implements CalendarEventService {

	@Autowired
	private CalendarEventRepository repository;

	@Override
	@Transactional(readOnly = true)
	public CalendarEvent findById(@NonNull final Long id) {
		log.info("finding CalendarEvent with id {}.", id);
		final CalendarEvent event = repository.findById(id).orElse(null);
		log.info("CalendarEvent found {}.", event);
		return event;
	}

	@Override
	@Transactional(readOnly = true)
	public List<CalendarEvent> findAll() {
		log.info("getting all CalendarEvent records.");
		return repository.findAll();
	}

	@Override
	@Transactional(readOnly = true)
	public List<CalendarEvent> findByPacienteId(@NonNull final Long pacienteId) {
		log.info("finding CalendarEvents for paciente id {}.", pacienteId);
		return repository.findByPacienteId(pacienteId);
	}

	@Override
	@Transactional(readOnly = true)
	public List<CalendarEvent> findUpcomingEvents(@NonNull final Date startDate) {
		log.info("finding upcoming CalendarEvents from {}.", startDate);
		return repository.findUpcomingEvents(startDate, EventStatus.SCHEDULED);
	}

	@Override
	@Transactional(readOnly = true)
	public List<CalendarEvent> findEventsBetweenDates(@NonNull final Date startDate, @NonNull final Date endDate) {
		log.info("finding CalendarEvents between {} and {}.", startDate, endDate);
		return repository.findEventsBetweenDates(startDate, endDate);
	}

	@Override
	@Transactional
	public CalendarEvent save(@NonNull final CalendarEvent event) {
		log.info("saving CalendarEvent {}.", event);
		final CalendarEvent saved = repository.save(event);
		log.info("CalendarEvent saved {}.", saved);
		return saved;
	}

	@Override
	@Transactional
	public void delete(@NonNull final Long id) {
		log.info("deleting CalendarEvent with id {}.", id);
		repository.deleteById(id);
		log.info("CalendarEvent {} deleted successfully.", id);
	}

}
