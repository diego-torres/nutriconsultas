package com.nutriconsultas.clinical.exam;

import java.util.Date;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ClinicalExamRepository extends JpaRepository<ClinicalExam, Long> {

	List<ClinicalExam> findByPacienteId(Long pacienteId);

	@Query("SELECT e FROM ClinicalExam e WHERE e.paciente.userId = :userId AND "
			+ "(LOWER(e.title) LIKE LOWER(:searchTerm) OR LOWER(e.description) LIKE LOWER(:searchTerm) OR "
			+ "LOWER(e.summaryNotes) LIKE LOWER(:searchTerm) OR LOWER(e.paciente.name) LIKE LOWER(:searchTerm))")
	List<ClinicalExam> findByUserIdAndSearchTerm(@Param("userId") String userId,
			@Param("searchTerm") String searchTerm);

	@Query("SELECT COUNT(e) FROM ClinicalExam e WHERE e.paciente.userId = :userId")
	long countByUserId(@Param("userId") String userId);

	@Query("SELECT COUNT(e) FROM ClinicalExam e WHERE e.paciente.userId = :userId "
			+ "AND e.examDateTime >= :startDate AND e.examDateTime <= :endDate")
	long countByUserIdAndDateRange(@Param("userId") String userId, @Param("startDate") Date startDate,
			@Param("endDate") Date endDate);

	@Query("SELECT e FROM ClinicalExam e WHERE e.paciente.userId = :userId "
			+ "AND e.examDateTime >= :startDate AND e.examDateTime <= :endDate")
	List<ClinicalExam> findByUserIdAndDateRange(@Param("userId") String userId, @Param("startDate") Date startDate,
			@Param("endDate") Date endDate);

}
