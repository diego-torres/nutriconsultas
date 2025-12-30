package com.nutriconsultas.calendar;

import java.util.Date;
import java.util.List;

import org.springframework.lang.NonNull;

public interface CalendarEventService {

	CalendarEvent findById(@NonNull Long id);

	List<CalendarEvent> findAll();

	List<CalendarEvent> findByPacienteId(@NonNull Long pacienteId);

	List<CalendarEvent> findUpcomingEvents(@NonNull Date startDate);

	List<CalendarEvent> findEventsBetweenDates(@NonNull Date startDate, @NonNull Date endDate);

	CalendarEvent save(@NonNull CalendarEvent event);

	void delete(@NonNull Long id);

}
