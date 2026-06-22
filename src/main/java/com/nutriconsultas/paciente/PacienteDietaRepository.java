package com.nutriconsultas.paciente;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PacienteDietaRepository extends JpaRepository<PacienteDieta, Long> {

	Optional<PacienteDieta> findByIdAndPacienteId(Long id, Long pacienteId);

	List<PacienteDieta> findByPacienteId(Long pacienteId);

	List<PacienteDieta> findByPacienteIdAndStatus(Long pacienteId, PacienteDietaStatus status);

	List<PacienteDieta> findByPacienteIdOrderByStartDateDesc(Long pacienteId);

	Page<PacienteDieta> findByPacienteId(Long pacienteId, Pageable pageable);

	Page<PacienteDieta> findByPacienteIdAndStatus(Long pacienteId, PacienteDietaStatus status, Pageable pageable);

	List<PacienteDieta> findByDietaId(Long dietaId);

	@Query("SELECT pd FROM PacienteDieta pd WHERE pd.paciente.userId = :userId")
	List<PacienteDieta> findByUserId(@Param("userId") String userId);

	@Query("SELECT COUNT(pd) FROM PacienteDieta pd WHERE pd.paciente.userId = :userId")
	long countByUserId(@Param("userId") String userId);

	@Query("SELECT COUNT(pd) FROM PacienteDieta pd WHERE pd.paciente.userId = :userId AND pd.status = :status")
	long countByUserIdAndStatus(@Param("userId") String userId, @Param("status") PacienteDietaStatus status);

	@Query("SELECT COUNT(pd) FROM PacienteDieta pd WHERE pd.dieta.id = :dietaId")
	long countByDietaId(@Param("dietaId") Long dietaId);

	@Query("SELECT COUNT(pd) FROM PacienteDieta pd WHERE pd.dieta.id = :dietaId AND pd.paciente.userId = :userId")
	long countByDietaIdAndPacienteUserId(@Param("dietaId") Long dietaId, @Param("userId") String userId);

	@Query("SELECT pd FROM PacienteDieta pd WHERE pd.paciente.userId = :userId "
			+ "AND pd.startDate >= :startDate AND pd.startDate <= :endDate")
	List<PacienteDieta> findByUserIdAndDateRange(@Param("userId") String userId, @Param("startDate") Date startDate,
			@Param("endDate") Date endDate);

	@Query("SELECT pd FROM PacienteDieta pd JOIN pd.dieta d WHERE d.pacienteId IS NULL")
	List<PacienteDieta> findAssignmentsReferencingSharedDieta();

}
