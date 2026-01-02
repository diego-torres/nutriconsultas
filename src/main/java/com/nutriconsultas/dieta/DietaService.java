package com.nutriconsultas.dieta;

import java.util.List;

import org.springframework.lang.NonNull;

public interface DietaService {

	Dieta getDieta(@NonNull Long id);

	Dieta getDietaByIdAndUserId(@NonNull Long id, @NonNull String userId);

	Dieta saveDieta(@NonNull Dieta dieta);

	void deleteDieta(@NonNull Long id);

	void deleteDietaByIdAndUserId(@NonNull Long id, @NonNull String userId);

	List<Dieta> getDietas();

	void addIngesta(@NonNull Long id, String nombreIngesta);

	void renameIngesta(@NonNull Long id, @NonNull Long ingestaId, String nombreIngesta);

	void recalculateAlimentoIngestaNutrients(@NonNull AlimentoIngesta alimentoIngesta, @NonNull Integer portions);

	void recalculatePlatilloIngestaNutrients(@NonNull PlatilloIngesta platilloIngesta, @NonNull Integer portions);

	Dieta duplicateDieta(@NonNull Long id, @NonNull String userId);

}
