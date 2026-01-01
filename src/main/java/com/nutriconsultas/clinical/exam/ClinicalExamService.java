package com.nutriconsultas.clinical.exam;

import java.util.List;

import org.springframework.lang.NonNull;

public interface ClinicalExamService {

	List<ClinicalExam> findAll();

	ClinicalExam findById(@NonNull Long id);

	ClinicalExam save(@NonNull ClinicalExam exam);

	void deleteById(@NonNull Long id);

	List<ClinicalExam> findByPacienteId(@NonNull Long pacienteId);

}

