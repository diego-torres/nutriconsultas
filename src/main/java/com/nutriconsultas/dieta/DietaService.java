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

	List<Dieta> getDietasForCatalogFilter(DietaCatalogFilter filter, String userId);

	DietaPickerPageDto findPickerPage(String search, int page, int size, Double requerimientoKcal);

	void addIngesta(@NonNull Long id, String nombreIngesta);

	void renameIngesta(@NonNull Long id, @NonNull Long ingestaId, String nombreIngesta);

	void reorderIngestas(@NonNull Long id, @NonNull List<Long> orderedIngestaIds);

	void reorderAlimentosInIngesta(@NonNull Long dietaId, @NonNull Long ingestaId,
			@NonNull List<Long> orderedAlimentoIngestaIds);

	void recalculateAlimentoIngestaNutrients(@NonNull AlimentoIngesta alimentoIngesta, @NonNull Integer portions);

	void recalculatePlatilloIngestaNutrients(@NonNull PlatilloIngesta platilloIngesta, @NonNull Integer portions);

	IngredientePlatilloIngesta addIngredientePlatilloIngesta(@NonNull PlatilloIngesta platilloIngesta,
			@NonNull Long alimentoId, @NonNull String cantidad, @NonNull Integer peso);

	void deleteIngredientePlatilloIngesta(@NonNull PlatilloIngesta platilloIngesta, @NonNull Long ingredienteId);

	void updateIngredientePlatilloIngesta(@NonNull PlatilloIngesta platilloIngesta, @NonNull Long ingredienteId,
			@NonNull String cantidad, @NonNull Integer peso);

	void recalculatePlatilloIngestaFromIngredientes(@NonNull PlatilloIngesta platilloIngesta);

	Dieta duplicateDieta(@NonNull Long id, @NonNull String userId);

	Dieta copyDietaForPatientAssignment(@NonNull Long sourceDietaId, @NonNull Long pacienteId,
			@NonNull String nutritionistUserId);

}
