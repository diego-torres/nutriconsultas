package com.nutriconsultas.paciente;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PatientInvitationRepository extends JpaRepository<PatientInvitation, Long> {

	Optional<PatientInvitation> findByTokenHash(String tokenHash);

	Optional<PatientInvitation> findByHumanCode(String humanCode);

	Optional<PatientInvitation> findByIdAndNutritionistUserId(Long id, String nutritionistUserId);

	List<PatientInvitation> findByPacienteId(Long pacienteId);

	List<PatientInvitation> findByPacienteIdAndStatus(Long pacienteId, PatientInvitationStatus status);

	List<PatientInvitation> findByNutritionistUserIdAndStatus(String nutritionistUserId,
			PatientInvitationStatus status);

	@Query("SELECT pi FROM PatientInvitation pi JOIN FETCH pi.paciente WHERE pi.tokenHash = :tokenHash")
	Optional<PatientInvitation> findWithPacienteByTokenHash(@Param("tokenHash") String tokenHash);

	@Query("SELECT pi FROM PatientInvitation pi JOIN FETCH pi.paciente WHERE pi.humanCode = :humanCode")
	Optional<PatientInvitation> findWithPacienteByHumanCode(@Param("humanCode") String humanCode);

	@Query("SELECT pi FROM PatientInvitation pi JOIN pi.paciente p WHERE pi.redeemedBySub = :patientAuthSub "
			+ "AND pi.status = :redeemedStatus ORDER BY pi.redeemedAt DESC")
	List<PatientInvitation> findRedeemedByPatientAuthSub(@Param("patientAuthSub") String patientAuthSub,
			@Param("redeemedStatus") PatientInvitationStatus redeemedStatus);

	@Query("SELECT pi FROM PatientInvitation pi JOIN pi.paciente p WHERE LOWER(p.email) = LOWER(:email) "
			+ "AND pi.status = :pendingStatus AND p.status = :invitedStatus AND p.patientAuthSub IS NULL "
			+ "AND pi.expiresAt >= :now ORDER BY pi.createdAt DESC")
	List<PatientInvitation> findRedeemablePendingByPacienteEmail(@Param("email") String email,
			@Param("now") Instant now, @Param("pendingStatus") PatientInvitationStatus pendingStatus,
			@Param("invitedStatus") PacienteStatus invitedStatus);

}
