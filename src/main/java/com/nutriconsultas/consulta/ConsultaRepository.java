package com.nutriconsultas.consulta;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ConsultaRepository extends JpaRepository<Consulta, Long> {
  public List<Consulta> findByPacienteId(Long pacienteId);
}
