package com.nutriconsultas.paciente;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import com.nutriconsultas.dieta.Dieta;
import com.nutriconsultas.mobile.dto.DietGroceryListItemDto;

public interface PacienteDietaService {

	PacienteDieta assignDieta(@NonNull Long pacienteId, @NonNull Long dietaId, @NonNull PacienteDieta pacienteDieta,
			@NonNull String userId);

	PacienteDieta assignWeeklyDieta(@NonNull Long pacienteId, @NonNull Map<Integer, Long> weekdayCatalogDietaIds,
			@NonNull PacienteDieta pacienteDieta, @NonNull String userId);

	PacienteDieta updateAssignment(@NonNull Long id, @NonNull PacienteDieta pacienteDieta);

	PacienteDieta updateWeeklyAssignment(@NonNull Long id, @NonNull Map<Integer, Long> weekdayCatalogDietaIds,
			@NonNull PacienteDieta pacienteDieta);

	void cancelAssignment(@NonNull Long id);

	List<PacienteDieta> findByPacienteId(@NonNull Long pacienteId);

	List<PacienteDieta> findActiveByPacienteId(@NonNull Long pacienteId);

	PacienteDieta findById(@NonNull Long id);

	List<PacienteDietaWeekday> findWeekdaySlots(@NonNull Long assignmentId);

	List<Dieta> resolveDietsForGroceryList(@NonNull PacienteDieta assignment);

	List<DietGroceryListItemDto> buildGroceryList(@NonNull PacienteDieta assignment);

	@Nullable
	Dieta resolveDietaForDate(@NonNull PacienteDieta assignment, @NonNull LocalDate date);

}
