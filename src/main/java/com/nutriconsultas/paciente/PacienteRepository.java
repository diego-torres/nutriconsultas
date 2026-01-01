package com.nutriconsultas.paciente;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PacienteRepository extends JpaRepository<Paciente, Long> {

	List<Paciente> findByUserId(String userId);

	Optional<Paciente> findByIdAndUserId(Long id, String userId);

}
