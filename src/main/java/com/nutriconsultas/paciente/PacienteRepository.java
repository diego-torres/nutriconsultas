package com.nutriconsultas.paciente;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.nutriconsultas.paciente.projection.PacienteAuthView;
import com.nutriconsultas.paciente.projection.PacienteCalendarView;
import com.nutriconsultas.paciente.projection.PacienteListView;

@Repository
public interface PacienteRepository extends JpaRepository<Paciente, Long> {

	String LIST_VIEW_SELECT = "SELECT p.id AS id, p.name AS name, p.email AS email, p.phone AS phone, "
			+ "p.dob AS dob, p.gender AS gender, p.responsibleName AS responsibleName ";

	List<Paciente> findByUserId(String userId);

	Page<Paciente> findByUserId(String userId, Pageable pageable);

	Optional<Paciente> findByIdAndUserId(Long id, String userId);

	Optional<Paciente> findByPatientAuthSub(String patientAuthSub);

	@Query(LIST_VIEW_SELECT + "FROM Paciente p WHERE p.userId = :userId")
	Page<PacienteListView> findListViewsByUserId(@Param("userId") String userId, Pageable pageable);

	@Query(LIST_VIEW_SELECT + "FROM Paciente p WHERE p.userId = :userId AND "
			+ "(LOWER(p.name) LIKE LOWER(:searchTerm) OR LOWER(p.email) LIKE LOWER(:searchTerm) "
			+ "OR LOWER(p.phone) LIKE LOWER(:searchTerm) OR LOWER(p.responsibleName) LIKE LOWER(:searchTerm))")
	Page<PacienteListView> findListViewsByUserIdAndSearchTerm(@Param("userId") String userId,
			@Param("searchTerm") String searchTerm, Pageable pageable);

	@Query(LIST_VIEW_SELECT + "FROM Paciente p WHERE p.userId = :userId AND "
			+ "(LOWER(p.name) LIKE LOWER(:searchTerm) OR LOWER(p.email) LIKE LOWER(:searchTerm) "
			+ "OR LOWER(p.phone) LIKE LOWER(:searchTerm))")
	List<PacienteListView> findListViewsByUserIdAndSearchTerm(@Param("userId") String userId,
			@Param("searchTerm") String searchTerm);

	@Query("SELECT p.id AS id, p.patientAuthSub AS patientAuthSub, p.userId AS userId FROM Paciente p "
			+ "WHERE p.patientAuthSub = :patientAuthSub")
	Optional<PacienteAuthView> findAuthViewByPatientAuthSub(@Param("patientAuthSub") String patientAuthSub);

	@Query("SELECT p.id AS id, p.name AS name, p.dob AS dob, p.gender AS gender FROM Paciente p "
			+ "WHERE p.userId = :userId ORDER BY p.name ASC")
	List<PacienteCalendarView> findCalendarViewsByUserId(@Param("userId") String userId);

	@Query("SELECT p FROM Paciente p WHERE p.userId = :userId AND "
			+ "(LOWER(p.name) LIKE LOWER(:searchTerm) OR LOWER(p.email) LIKE LOWER(:searchTerm) "
			+ "OR LOWER(p.phone) LIKE LOWER(:searchTerm))")
	List<Paciente> findByUserIdAndSearchTerm(@Param("userId") String userId, @Param("searchTerm") String searchTerm);

	@Query("SELECT p FROM Paciente p WHERE p.userId = :userId AND "
			+ "(LOWER(p.name) LIKE LOWER(:searchTerm) OR LOWER(p.email) LIKE LOWER(:searchTerm) "
			+ "OR LOWER(p.phone) LIKE LOWER(:searchTerm) OR LOWER(p.responsibleName) LIKE LOWER(:searchTerm))")
	Page<Paciente> findByUserIdAndSearchTerm(@Param("userId") String userId, @Param("searchTerm") String searchTerm,
			Pageable pageable);

	@Query("SELECT COUNT(p) FROM Paciente p WHERE p.userId = :userId")
	long countByUserId(@Param("userId") String userId);

	@Query("SELECT COUNT(p) FROM Paciente p WHERE p.userId = :userId AND "
			+ "(LOWER(p.name) LIKE LOWER(:searchTerm) OR LOWER(p.email) LIKE LOWER(:searchTerm) "
			+ "OR LOWER(p.phone) LIKE LOWER(:searchTerm) OR LOWER(p.responsibleName) LIKE LOWER(:searchTerm))")
	long countByUserIdAndSearchTerm(@Param("userId") String userId, @Param("searchTerm") String searchTerm);

	@Query("SELECT COUNT(p) FROM Paciente p WHERE p.userId IN :userIds")
	long countByUserIdIn(@Param("userIds") Collection<String> userIds);

}
