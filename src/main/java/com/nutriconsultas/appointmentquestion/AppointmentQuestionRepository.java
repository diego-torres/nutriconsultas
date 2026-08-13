package com.nutriconsultas.appointmentquestion;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AppointmentQuestionRepository extends JpaRepository<AppointmentQuestion, Long> {

	Page<AppointmentQuestion> findByPacienteIdOrderByCreatedAtDesc(Long pacienteId, Pageable pageable);

	Page<AppointmentQuestion> findByPacienteIdAndAnsweredOrderByCreatedAtDesc(Long pacienteId, boolean answered,
			Pageable pageable);

	List<AppointmentQuestion> findByPacienteId(Long pacienteId);

	Optional<AppointmentQuestion> findByIdAndPacienteId(Long id, Long pacienteId);

	void deleteByPacienteId(Long pacienteId);

}
