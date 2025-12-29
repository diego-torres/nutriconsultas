package com.nutriconsultas.alimentos;

import java.util.List;

import org.springframework.lang.NonNull;

public interface AlimentoService {

	List<Alimento> findAll();

	Alimento findById(@NonNull Long id);

	Alimento save(@NonNull Alimento alimento);

}
