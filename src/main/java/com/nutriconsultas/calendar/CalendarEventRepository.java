package com.nutriconsultas.calendar;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CalendarEventRepository extends JpaRepository<CalendarEvent, Long> {

	List<CalendarEvent> findByPacienteId(Long pacienteId);

	@Query("SELECT e FROM CalendarEvent e WHERE e.paciente.id = :pacienteId "
			+ "AND (:status IS NULL OR e.status = :status) "
			+ "AND (:fromDate IS NULL OR e.eventDateTime >= :fromDate) "
			+ "AND (:toDate IS NULL OR e.eventDateTime <= :toDate)")
	Page<CalendarEvent> findPatientVisits(@Param("pacienteId") Long pacienteId, @Param("status") EventStatus status,
			@Param("fromDate") Date fromDate, @Param("toDate") Date toDate, Pageable pageable);

	Optional<CalendarEvent> findByIdAndPacienteId(Long id, Long pacienteId);

	List<CalendarEvent> findByStatus(EventStatus status);

	@Query("SELECT e FROM CalendarEvent e WHERE e.eventDateTime >= :startDate AND e.eventDateTime < :endDate "
			+ "ORDER BY e.eventDateTime ASC")
	List<CalendarEvent> findEventsBetweenDates(@Param("startDate") Date startDate, @Param("endDate") Date endDate);

	@Query("SELECT e FROM CalendarEvent e WHERE e.eventDateTime >= :startDate AND e.status = :status "
			+ "ORDER BY e.eventDateTime ASC")
	List<CalendarEvent> findUpcomingEvents(@Param("startDate") Date startDate, @Param("status") EventStatus status);

	@Query("SELECT e FROM CalendarEvent e WHERE e.paciente.userId = :userId AND "
			+ "(LOWER(e.title) LIKE LOWER(:searchTerm) OR LOWER(e.description) LIKE LOWER(:searchTerm) "
			+ "OR LOWER(e.paciente.name) LIKE LOWER(:searchTerm))")
	List<CalendarEvent> findByUserIdAndSearchTerm(@Param("userId") String userId,
			@Param("searchTerm") String searchTerm);

	@Query("SELECT COUNT(e) FROM CalendarEvent e WHERE e.paciente.userId = :userId")
	long countByUserId(@Param("userId") String userId);

	@Query("SELECT COUNT(e) FROM CalendarEvent e WHERE e.paciente.userId = :userId "
			+ "AND e.eventDateTime >= :startDate AND e.eventDateTime <= :endDate")
	long countByUserIdAndDateRange(@Param("userId") String userId, @Param("startDate") Date startDate,
			@Param("endDate") Date endDate);

	@Query("SELECT e FROM CalendarEvent e WHERE e.paciente.userId = :userId "
			+ "AND e.eventDateTime >= :startDate AND e.eventDateTime <= :endDate")
	List<CalendarEvent> findByUserIdAndDateRange(@Param("userId") String userId, @Param("startDate") Date startDate,
			@Param("endDate") Date endDate);

	@Query("SELECT COUNT(e) FROM CalendarEvent e WHERE e.paciente.userId = :userId AND e.status = :status")
	long countByUserIdAndStatus(@Param("userId") String userId, @Param("status") EventStatus status);

	@Query("SELECT e FROM CalendarEvent e WHERE e.paciente.userId = :userId "
			+ "AND e.eventDateTime >= :startDate AND e.status = :status ORDER BY e.eventDateTime ASC")
	List<CalendarEvent> findUpcomingEventsByUserId(@Param("userId") String userId, @Param("startDate") Date startDate,
			@Param("status") EventStatus status);

	@Query("SELECT e FROM CalendarEvent e WHERE e.paciente.id = :pacienteId AND e.status = :status "
			+ "AND e.eventDateTime >= :fromDate ORDER BY e.eventDateTime ASC")
	List<CalendarEvent> findUpcomingByPacienteId(@Param("pacienteId") Long pacienteId, @Param("fromDate") Date fromDate,
			@Param("status") EventStatus status, Pageable pageable);

	@Query("SELECT e FROM CalendarEvent e WHERE e.paciente.id = :pacienteId AND e.status = :status "
			+ "AND e.eventDateTime < :beforeDate ORDER BY e.eventDateTime DESC")
	List<CalendarEvent> findPastByPacienteId(@Param("pacienteId") Long pacienteId, @Param("beforeDate") Date beforeDate,
			@Param("status") EventStatus status, Pageable pageable);

}
