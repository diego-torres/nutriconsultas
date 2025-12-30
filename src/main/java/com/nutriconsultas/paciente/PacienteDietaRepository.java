package com.nutriconsultas.paciente;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PacienteDietaRepository extends JpaRepository<PacienteDieta, Long> {

	List<PacienteDieta> findByPacienteId(Long pacienteId);

	List<PacienteDieta> findByPacienteIdAndStatus(Long pacienteId, PacienteDietaStatus status);

	List<PacienteDieta> findByPacienteIdOrderByStartDateDesc(Long pacienteId);

}
