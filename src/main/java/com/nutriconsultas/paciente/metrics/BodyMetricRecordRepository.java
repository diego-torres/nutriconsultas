package com.nutriconsultas.paciente.metrics;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BodyMetricRecordRepository extends JpaRepository<BodyMetricRecord, Long> {

	List<BodyMetricRecord> findByPacienteIdOrderByRecordedAtAsc(Long pacienteId);

	Optional<BodyMetricRecord> findFirstByPacienteIdOrderByRecordedAtDescIdDesc(Long pacienteId);

	Optional<BodyMetricRecord> findBySourceAndSourceId(BodyMetricSource source, Long sourceId);

	boolean existsByPacienteId(Long pacienteId);

	void deleteBySourceAndSourceId(BodyMetricSource source, Long sourceId);

}
