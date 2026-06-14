package com.nutriconsultas.message;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PatientMessageRepository extends JpaRepository<PatientMessage, Long> {

	@Query("""
			SELECT m FROM PatientMessage m
			WHERE m.paciente.id = :pacienteId
			AND (:cursorId IS NULL OR m.id < :cursorId)
			ORDER BY m.id DESC
			""")
	List<PatientMessage> findThreadForPatient(@Param("pacienteId") Long pacienteId, @Param("cursorId") Long cursorId,
			Pageable pageable);

}
