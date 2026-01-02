package com.nutriconsultas.paciente;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;

public interface PacienteService {

	Paciente findById(@NonNull Long id);

	Paciente findByIdAndUserId(@NonNull Long id, @NonNull String userId);

	List<Paciente> findAll();

	List<Paciente> findAllByUserId(@NonNull String userId);

	Page<Paciente> findAllByUserId(@NonNull String userId, Pageable pageable);

	Page<Paciente> findAllByUserIdAndSearchTerm(@NonNull String userId, @NonNull String searchTerm, Pageable pageable);

	long countByUserId(@NonNull String userId);

	long countByUserIdAndSearchTerm(@NonNull String userId, @NonNull String searchTerm);

	Paciente save(@NonNull Paciente paciente);

	void delete(@NonNull Long id);

	void deleteByIdAndUserId(@NonNull Long id, @NonNull String userId);

}
