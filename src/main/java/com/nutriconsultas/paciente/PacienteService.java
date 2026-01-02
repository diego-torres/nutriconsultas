package com.nutriconsultas.paciente;

import java.util.List;

import org.springframework.lang.NonNull;

public interface PacienteService {

	Paciente findById(@NonNull Long id);

	Paciente findByIdAndUserId(@NonNull Long id, @NonNull String userId);

	List<Paciente> findAll();

	List<Paciente> findAllByUserId(@NonNull String userId);

	Paciente save(@NonNull Paciente paciente);

	void delete(@NonNull Long id);

	void deleteByIdAndUserId(@NonNull Long id, @NonNull String userId);

}
