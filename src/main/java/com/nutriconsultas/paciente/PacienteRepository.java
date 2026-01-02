package com.nutriconsultas.paciente;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PacienteRepository extends JpaRepository<Paciente, Long> {

	List<Paciente> findByUserId(String userId);

	Optional<Paciente> findByIdAndUserId(Long id, String userId);

	@Query("SELECT p FROM Paciente p WHERE p.userId = :userId AND "
			+ "(LOWER(p.name) LIKE LOWER(:searchTerm) OR LOWER(p.email) LIKE LOWER(:searchTerm) "
			+ "OR LOWER(p.phone) LIKE LOWER(:searchTerm))")
	List<Paciente> findByUserIdAndSearchTerm(@Param("userId") String userId,
			@Param("searchTerm") String searchTerm);

}
