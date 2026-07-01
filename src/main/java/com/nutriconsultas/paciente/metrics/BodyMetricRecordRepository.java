package com.nutriconsultas.paciente.metrics;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BodyMetricRecordRepository extends JpaRepository<BodyMetricRecord, Long> {

	List<BodyMetricRecord> findByPacienteIdOrderByRecordedAtAsc(Long pacienteId);

	void deleteByPacienteId(Long pacienteId);

	Optional<BodyMetricRecord> findFirstByPacienteIdOrderByRecordedAtDescIdDesc(Long pacienteId);

	List<BodyMetricRecord> findTop2ByPacienteIdOrderByRecordedAtDescIdDesc(Long pacienteId);

	Optional<BodyMetricRecord> findBySourceAndSourceId(BodyMetricSource source, Long sourceId);

	boolean existsByPacienteId(Long pacienteId);

	void deleteBySourceAndSourceId(BodyMetricSource source, Long sourceId);

	long countByPacienteId(Long pacienteId);

	long countByPacienteIdAndRecordedAtGreaterThanEqual(Long pacienteId, Date from);

	long countByPacienteIdAndRecordedAtLessThanEqual(Long pacienteId, Date to);

	long countByPacienteIdAndRecordedAtBetween(Long pacienteId, Date from, Date to);

	List<BodyMetricRecord> findByPacienteIdOrderByRecordedAtAscIdAsc(Long pacienteId, Pageable pageable);

	List<BodyMetricRecord> findByPacienteIdAndRecordedAtGreaterThanEqualOrderByRecordedAtAscIdAsc(Long pacienteId,
			Date from, Pageable pageable);

	List<BodyMetricRecord> findByPacienteIdAndRecordedAtLessThanEqualOrderByRecordedAtAscIdAsc(Long pacienteId, Date to,
			Pageable pageable);

	List<BodyMetricRecord> findByPacienteIdAndRecordedAtBetweenOrderByRecordedAtAscIdAsc(Long pacienteId, Date from,
			Date to, Pageable pageable);

}
