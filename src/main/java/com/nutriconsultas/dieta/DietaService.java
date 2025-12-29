package com.nutriconsultas.dieta;

import java.util.List;

import org.springframework.lang.NonNull;

public interface DietaService {

	Dieta getDieta(@NonNull Long id);

	Dieta saveDieta(@NonNull Dieta dieta);

	void deleteDieta(@NonNull Long id);

	List<Dieta> getDietas();

	void addIngesta(@NonNull Long id, String nombreIngesta);

	void renameIngesta(@NonNull Long id, @NonNull Long ingestaId, String nombreIngesta);

}
