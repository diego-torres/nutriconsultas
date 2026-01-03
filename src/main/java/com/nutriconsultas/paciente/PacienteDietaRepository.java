package com.nutriconsultas.paciente;

import java.util.Date;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PacienteDietaRepository extends JpaRepository<PacienteDieta, Long> {

	List<PacienteDieta> findByPacienteId(Long pacienteId);

	List<PacienteDieta> findByPacienteIdAndStatus(Long pacienteId, PacienteDietaStatus status);

	List<PacienteDieta> findByPacienteIdOrderByStartDateDesc(Long pacienteId);

	List<PacienteDieta> findByDietaId(Long dietaId);

	@Query("SELECT pd FROM PacienteDieta pd WHERE pd.paciente.userId = :userId")
	List<PacienteDieta> findByUserId(@Param("userId") String userId);

	@Query("SELECT COUNT(pd) FROM PacienteDieta pd WHERE pd.paciente.userId = :userId")
	long countByUserId(@Param("userId") String userId);

	@Query("SELECT COUNT(pd) FROM PacienteDieta pd WHERE pd.paciente.userId = :userId AND pd.status = :status")
	long countByUserIdAndStatus(@Param("userId") String userId, @Param("status") PacienteDietaStatus status);

	@Query("SELECT pd FROM PacienteDieta pd WHERE pd.paciente.userId = :userId "
			+ "AND pd.startDate >= :startDate AND pd.startDate <= :endDate")
	List<PacienteDieta> findByUserIdAndDateRange(@Param("userId") String userId, @Param("startDate") Date startDate,
			@Param("endDate") Date endDate);

}
