package com.nutriconsultas.clinical.exam;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ClinicalExamServiceImpl implements ClinicalExamService {

	@Autowired
	private ClinicalExamRepository repository;

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
		log.debug("Saving clinical exam: {}", exam);
		return repository.save(exam);
	}

	@Override
	@Transactional
	public void deleteById(@NonNull final Long id) {
		log.debug("Deleting clinical exam with id: {}", id);
		repository.deleteById(id);
	}

	@Override
	@Transactional(readOnly = true)
	public List<ClinicalExam> findByPacienteId(@NonNull final Long pacienteId) {
		log.debug("Finding clinical exams for paciente id: {}", pacienteId);
		return repository.findByPacienteId(pacienteId);
	}

}
