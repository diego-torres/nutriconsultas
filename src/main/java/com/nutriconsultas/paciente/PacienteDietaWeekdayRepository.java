package com.nutriconsultas.paciente;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PacienteDietaWeekdayRepository extends JpaRepository<PacienteDietaWeekday, Long> {

	List<PacienteDietaWeekday> findByPacienteDietaIdOrderByDayOfWeekAsc(Long pacienteDietaId);

	void deleteByPacienteDietaId(Long pacienteDietaId);

}
