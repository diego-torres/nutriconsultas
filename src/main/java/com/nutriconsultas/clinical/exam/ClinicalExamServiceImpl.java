package com.nutriconsultas.clinical.exam;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nutriconsultas.util.LogRedaction;

import com.nutriconsultas.paciente.metrics.BodyMetricSource;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ClinicalExamServiceImpl implements ClinicalExamService {

	@Autowired
	private ClinicalExamRepository repository;

	@Autowired
	private com.nutriconsultas.paciente.metrics.BodyMetricRecordService bodyMetricRecordService;

	@Override
	@Transactional(readOnly = true)
	public List<ClinicalExam> findAll() {
		log.debug("Finding all clinical exams");
		return repository.findAll();
	}

	@Override
	@Transactional(readOnly = true)
	public ClinicalExam findById(@NonNull final Long id) {
		log.debug("Finding clinical exam with id: {}", id);
		return repository.findById(id).orElse(null);
	}

	@Override
	@Transactional
	public ClinicalExam save(@NonNull final ClinicalExam exam) {
		log.debug("Saving clinical exam: {}", LogRedaction.redactClinicalExam(exam));
		final ClinicalExam saved = repository.save(exam);
		bodyMetricRecordService.syncFromClinicalExam(saved);
		return saved;
	}

	@Override
	@Transactional
	public void deleteById(@NonNull final Long id) {
		log.debug("Deleting clinical exam with id: {}", id);
		final ClinicalExam exam = repository.findById(id).orElse(null);
		if (exam != null) {
			final Long pacienteId = exam.getPaciente() != null ? exam.getPaciente().getId() : null;
			repository.deleteById(id);
			if (pacienteId != null) {
				bodyMetricRecordService.removeSourceAndRefreshPatient(BodyMetricSource.CLINICAL_EXAM, id, pacienteId);
			}
		}
		else {
			repository.deleteById(id);
		}
	}

	@Override
	@Transactional(readOnly = true)
	public List<ClinicalExam> findByPacienteId(@NonNull final Long pacienteId) {
		log.debug("Finding clinical exams for paciente id: {}", pacienteId);
		return repository.findByPacienteId(pacienteId);
	}

}
