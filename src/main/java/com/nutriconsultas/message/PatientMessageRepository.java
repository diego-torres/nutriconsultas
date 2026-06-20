package com.nutriconsultas.message;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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

	@Query("""
			SELECT m FROM PatientMessage m
			WHERE m.paciente.id = :pacienteId
			AND m.nutritionistUserId = :userId
			ORDER BY m.sentAt ASC, m.id ASC
			""")
	List<PatientMessage> findThreadAscending(@Param("pacienteId") Long pacienteId, @Param("userId") String userId);

	@Query("""
			SELECT m FROM PatientMessage m
			JOIN FETCH m.paciente p
			WHERE m.nutritionistUserId = :userId
			AND m.senderRole = com.nutriconsultas.message.MessageSenderRole.PATIENT
			AND m.readByNutritionist = false
			ORDER BY m.sentAt DESC, m.id DESC
			""")
	List<PatientMessage> findUnreadFromPatientsByNutritionist(@Param("userId") String userId);

	@Query("""
			SELECT COUNT(m) FROM PatientMessage m
			WHERE m.nutritionistUserId = :userId
			AND m.senderRole = com.nutriconsultas.message.MessageSenderRole.PATIENT
			AND m.readByNutritionist = false
			""")
	long countUnreadFromPatientsByNutritionist(@Param("userId") String userId);

	@Modifying
	@Query("""
			UPDATE PatientMessage m SET m.readByNutritionist = true
			WHERE m.paciente.id = :pacienteId
			AND m.nutritionistUserId = :userId
			AND m.senderRole = com.nutriconsultas.message.MessageSenderRole.PATIENT
			AND m.readByNutritionist = false
			""")
	int markReadByNutritionist(@Param("pacienteId") Long pacienteId, @Param("userId") String userId);

	@Modifying(clearAutomatically = true)
	@Query("DELETE FROM PatientMessage m WHERE m.paciente.id = :pacienteId")
	void deleteByPacienteId(@Param("pacienteId") Long pacienteId);

}
