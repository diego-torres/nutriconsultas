package com.nutriconsultas.paciente.metrics;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface BodyMetricRecordRepository extends JpaRepository<BodyMetricRecord, Long> {

	List<BodyMetricRecord> findByPacienteIdOrderByRecordedAtAsc(Long pacienteId);

	Optional<BodyMetricRecord> findFirstByPacienteIdOrderByRecordedAtDescIdDesc(Long pacienteId);

	List<BodyMetricRecord> findTop2ByPacienteIdOrderByRecordedAtDescIdDesc(Long pacienteId);

	Optional<BodyMetricRecord> findBySourceAndSourceId(BodyMetricSource source, Long sourceId);

	boolean existsByPacienteId(Long pacienteId);

	void deleteBySourceAndSourceId(BodyMetricSource source, Long sourceId);

	@Query("SELECT b FROM BodyMetricRecord b WHERE b.paciente.id = :pacienteId "
			+ "AND (:from IS NULL OR b.recordedAt >= :from) AND (:to IS NULL OR b.recordedAt <= :to) "
			+ "ORDER BY b.recordedAt ASC, b.id ASC")
	List<BodyMetricRecord> findPatientTimeline(@Param("pacienteId") Long pacienteId, @Param("from") Date from,
			@Param("to") Date to, Pageable pageable);

	@Query("SELECT COUNT(b) FROM BodyMetricRecord b WHERE b.paciente.id = :pacienteId "
			+ "AND (:from IS NULL OR b.recordedAt >= :from) AND (:to IS NULL OR b.recordedAt <= :to)")
	long countPatientTimeline(@Param("pacienteId") Long pacienteId, @Param("from") Date from, @Param("to") Date to);

}
