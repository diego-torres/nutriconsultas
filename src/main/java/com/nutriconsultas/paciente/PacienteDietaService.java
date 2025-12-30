package com.nutriconsultas.paciente;

import java.util.List;

import org.springframework.lang.NonNull;

public interface PacienteDietaService {

	PacienteDieta assignDieta(@NonNull Long pacienteId, @NonNull Long dietaId, @NonNull PacienteDieta pacienteDieta);

	PacienteDieta updateAssignment(@NonNull Long id, @NonNull PacienteDieta pacienteDieta);

	void cancelAssignment(@NonNull Long id);

	List<PacienteDieta> findByPacienteId(@NonNull Long pacienteId);

	List<PacienteDieta> findActiveByPacienteId(@NonNull Long pacienteId);

	PacienteDieta findById(@NonNull Long id);

}
