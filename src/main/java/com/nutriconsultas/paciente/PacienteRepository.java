package com.nutriconsultas.paciente;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PacienteRepository extends JpaRepository<Paciente, Long> {

	List<Paciente> findByUserId(String userId);

	Page<Paciente> findByUserId(String userId, Pageable pageable);

	Optional<Paciente> findByIdAndUserId(Long id, String userId);

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

}
