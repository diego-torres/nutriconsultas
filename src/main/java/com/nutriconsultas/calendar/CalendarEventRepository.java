package com.nutriconsultas.calendar;

import java.util.Date;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CalendarEventRepository extends JpaRepository<CalendarEvent, Long> {

	List<CalendarEvent> findByPacienteId(Long pacienteId);

	List<CalendarEvent> findByStatus(EventStatus status);

	@Query("SELECT e FROM CalendarEvent e WHERE e.eventDateTime >= :startDate AND e.eventDateTime < :endDate "
			+ "ORDER BY e.eventDateTime ASC")
	List<CalendarEvent> findEventsBetweenDates(@Param("startDate") Date startDate, @Param("endDate") Date endDate);

	@Query("SELECT e FROM CalendarEvent e WHERE e.eventDateTime >= :startDate AND e.status = :status "
			+ "ORDER BY e.eventDateTime ASC")
	List<CalendarEvent> findUpcomingEvents(@Param("startDate") Date startDate, @Param("status") EventStatus status);

}
