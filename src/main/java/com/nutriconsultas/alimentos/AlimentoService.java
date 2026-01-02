package com.nutriconsultas.alimentos;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;

public interface AlimentoService {

	List<Alimento> findAll();

	Page<Alimento> findAll(@NonNull Pageable pageable);

	Page<Alimento> findBySearchTerm(@NonNull String searchTerm, @NonNull Pageable pageable);

	long count();

	long countBySearchTerm(@NonNull String searchTerm);

	Alimento findById(@NonNull Long id);

	Alimento save(@NonNull Alimento alimento);

}
