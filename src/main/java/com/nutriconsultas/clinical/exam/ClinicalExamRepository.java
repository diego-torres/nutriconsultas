package com.nutriconsultas.clinical.exam;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClinicalExamRepository extends JpaRepository<ClinicalExam, Long> {

	List<ClinicalExam> findByPacienteId(Long pacienteId);

}

