package com.nutriconsultas.clinical.exam;

import java.util.Date;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AnthropometricMeasurementRepository extends JpaRepository<AnthropometricMeasurement, Long> {

	List<AnthropometricMeasurement> findByPacienteId(Long pacienteId);

	@Query("SELECT COUNT(m) FROM AnthropometricMeasurement m WHERE m.paciente.userId = :userId")
	long countByUserId(@Param("userId") String userId);

	@Query("SELECT COUNT(m) FROM AnthropometricMeasurement m WHERE m.paciente.userId = :userId "
			+ "AND m.measurementDateTime >= :startDate AND m.measurementDateTime <= :endDate")
	long countByUserIdAndDateRange(@Param("userId") String userId, @Param("startDate") Date startDate,
			@Param("endDate") Date endDate);

	@Query("SELECT m FROM AnthropometricMeasurement m WHERE m.paciente.userId = :userId "
			+ "AND m.measurementDateTime >= :startDate AND m.measurementDateTime <= :endDate")
	List<AnthropometricMeasurement> findByUserIdAndDateRange(@Param("userId") String userId,
			@Param("startDate") Date startDate, @Param("endDate") Date endDate);

}
