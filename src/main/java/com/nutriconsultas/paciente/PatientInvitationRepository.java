package com.nutriconsultas.paciente;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PatientInvitationRepository extends JpaRepository<PatientInvitation, Long> {

	Optional<PatientInvitation> findByTokenHash(String tokenHash);

	List<PatientInvitation> findByPacienteId(Long pacienteId);

	List<PatientInvitation> findByNutritionistUserIdAndStatus(String nutritionistUserId,
			PatientInvitationStatus status);

}
