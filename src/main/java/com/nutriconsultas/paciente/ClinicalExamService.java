package com.nutriconsultas.paciente;

import java.util.List;

public interface ClinicalExamService {

	List<ClinicalExam> findAll();

	ClinicalExam findById(Long id);

	ClinicalExam save(ClinicalExam exam);

	void deleteById(Long id);

	List<ClinicalExam> findByPacienteId(Long pacienteId);

}

