package com.nutriconsultas.paciente;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nutriconsultas.dieta.Dieta;
import com.nutriconsultas.dieta.DietaCatalogConstants;
import com.nutriconsultas.dieta.DietaRepository;
import com.nutriconsultas.dieta.DietaService;
import com.nutriconsultas.mobile.DietGroceryListAggregator;
import com.nutriconsultas.mobile.dto.DietGroceryListItemDto;

import lombok.extern.slf4j.Slf4j;

@Service
@Transactional
@Slf4j
public class PacienteDietaServiceImpl implements PacienteDietaService {

	private final PacienteDietaRepository pacienteDietaRepository;

	private final PacienteDietaWeekdayRepository pacienteDietaWeekdayRepository;

	private final PacienteRepository pacienteRepository;

	private final DietaRepository dietaRepository;

	private final DietaService dietaService;

	public PacienteDietaServiceImpl(final PacienteDietaRepository pacienteDietaRepository,
			final PacienteDietaWeekdayRepository pacienteDietaWeekdayRepository,
			final PacienteRepository pacienteRepository, final DietaRepository dietaRepository,
			final DietaService dietaService) {
		this.pacienteDietaRepository = pacienteDietaRepository;
		this.pacienteDietaWeekdayRepository = pacienteDietaWeekdayRepository;
		this.pacienteRepository = pacienteRepository;
		this.dietaRepository = dietaRepository;
		this.dietaService = dietaService;
	}

	@Override
	public PacienteDieta assignDieta(@NonNull final Long pacienteId, @NonNull final Long dietaId,
			@NonNull final PacienteDieta pacienteDieta, @NonNull final String userId) {
		log.info("Assigning dieta {} to paciente {} for user {}", dietaId, pacienteId, userId);
		final Paciente paciente = pacienteRepository.findByIdAndUserId(pacienteId, userId)
			.orElseThrow(() -> new IllegalArgumentException("No se ha encontrado paciente con id " + pacienteId));
		final Dieta sourceDieta = dietaRepository.findById(dietaId)
			.orElseThrow(() -> new IllegalArgumentException("No se ha encontrado dieta con id " + dietaId));
		assertAssignableCatalogDieta(sourceDieta);

		final Dieta patientCopy = dietaService.copyDietaForPatientAssignment(dietaId, pacienteId, userId);

		final PacienteDieta newAssignment = buildAssignmentShell(paciente, pacienteDieta);
		newAssignment.setAssignmentType(PacienteDietaAssignmentType.DATE_RANGE);
		newAssignment.setDieta(patientCopy);

		return pacienteDietaRepository.save(newAssignment);
	}

	@Override
	public PacienteDieta assignWeeklyDieta(@NonNull final Long pacienteId,
			@NonNull final Map<Integer, Long> weekdayCatalogDietaIds, @NonNull final PacienteDieta pacienteDieta,
			@NonNull final String userId) {
		log.info("Assigning weekly dieta plan to paciente {} for user {}", pacienteId, userId);
		if (weekdayCatalogDietaIds.isEmpty()) {
			throw new IllegalArgumentException("Seleccione al menos un día con dieta para el plan semanal");
		}
		final Paciente paciente = pacienteRepository.findByIdAndUserId(pacienteId, userId)
			.orElseThrow(() -> new IllegalArgumentException("No se ha encontrado paciente con id " + pacienteId));

		final PacienteDieta newAssignment = buildAssignmentShell(paciente, pacienteDieta);
		newAssignment.setAssignmentType(PacienteDietaAssignmentType.WEEKLY);
		newAssignment.setDieta(null);

		final PacienteDieta saved = pacienteDietaRepository.save(newAssignment);
		replaceWeekdaySlots(saved, weekdayCatalogDietaIds, pacienteId, userId);
		return pacienteDietaRepository.findById(saved.getId())
			.orElseThrow(() -> new IllegalArgumentException("No se ha encontrado asignación con id " + saved.getId()));
	}

	@Override
	public PacienteDieta updateAssignment(@NonNull final Long id, @NonNull final PacienteDieta pacienteDieta) {
		log.info("Updating dieta assignment {}", id);
		final PacienteDieta existing = loadAssignment(id);
		applyMetadataUpdate(existing, pacienteDieta);
		return Objects.requireNonNull(pacienteDietaRepository.save(existing));
	}

	@Override
	public PacienteDieta updateWeeklyAssignment(@NonNull final Long id,
			@NonNull final Map<Integer, Long> weekdayCatalogDietaIds, @NonNull final PacienteDieta pacienteDieta) {
		log.info("Updating weekly dieta assignment {}", id);
		final PacienteDieta existing = loadAssignment(id);
		if (!existing.isWeeklyAssignment()) {
			throw new IllegalArgumentException("La asignación no es un plan semanal");
		}
		applyMetadataUpdate(existing, pacienteDieta);
		final PacienteDieta saved = pacienteDietaRepository.save(existing);
		mergeWeekdaySlots(saved, weekdayCatalogDietaIds, saved.getPaciente().getId(), saved.getPaciente().getUserId());
		return pacienteDietaRepository.findById(saved.getId())
			.orElseThrow(() -> new IllegalArgumentException("No se ha encontrado asignación con id " + saved.getId()));
	}

	@Override
	public void cancelAssignment(@NonNull final Long id) {
		log.info("Cancelling dieta assignment {}", id);
		final PacienteDieta existing = pacienteDietaRepository.findById(id)
			.orElseThrow(() -> new IllegalArgumentException("No se ha encontrado asignación con id " + id));

		existing.setStatus(PacienteDietaStatus.CANCELLED);
		pacienteDietaRepository.save(existing);
	}

	@Override
	@Transactional(readOnly = true)
	public List<PacienteDieta> findByPacienteId(@NonNull final Long pacienteId) {
		log.info("Finding all dieta assignments for paciente {}", pacienteId);
		return pacienteDietaRepository.findByPacienteIdOrderByStartDateDesc(pacienteId);
	}

	@Override
	@Transactional(readOnly = true)
	public List<PacienteDieta> findActiveByPacienteId(@NonNull final Long pacienteId) {
		log.info("Finding active dieta assignments for paciente {}", pacienteId);
		return pacienteDietaRepository.findByPacienteIdAndStatus(pacienteId, PacienteDietaStatus.ACTIVE);
	}

	@Override
	@Transactional(readOnly = true)
	public PacienteDieta findById(@NonNull final Long id) {
		log.info("Finding dieta assignment {}", id);
		return pacienteDietaRepository.findById(id)
			.orElseThrow(() -> new IllegalArgumentException("No se ha encontrado asignación con id " + id));
	}

	@Override
	@Transactional(readOnly = true)
	public List<PacienteDietaWeekday> findWeekdaySlots(@NonNull final Long assignmentId) {
		return pacienteDietaWeekdayRepository.findByPacienteDietaIdOrderByDayOfWeekAsc(assignmentId);
	}

	@Override
	@Transactional(readOnly = true)
	public List<Dieta> resolveDietsForGroceryList(@NonNull final PacienteDieta assignment) {
		if (assignment.isWeeklyAssignment()) {
			final List<Dieta> diets = new ArrayList<>();
			for (final PacienteDietaWeekday slot : findWeekdaySlots(assignment.getId())) {
				if (slot.getDieta() != null) {
					diets.add(slot.getDieta());
				}
			}
			return diets;
		}
		if (assignment.getDieta() != null) {
			return List.of(assignment.getDieta());
		}
		return List.of();
	}

	@Override
	@Transactional(readOnly = true)
	public List<DietGroceryListItemDto> buildGroceryList(@NonNull final PacienteDieta assignment) {
		return DietGroceryListAggregator.aggregate(resolveDietsForGroceryList(assignment));
	}

	@Override
	@Transactional(readOnly = true)
	@Nullable
	public Dieta resolveDietaForDate(@NonNull final PacienteDieta assignment, @NonNull final LocalDate date) {
		if (!assignment.isWeeklyAssignment()) {
			return assignment.getDieta();
		}
		final int isoDay = toIsoDayOfWeek(date.getDayOfWeek());
		return findWeekdaySlots(assignment.getId()).stream()
			.filter(slot -> slot.getDayOfWeek() != null && slot.getDayOfWeek() == isoDay)
			.map(PacienteDietaWeekday::getDieta)
			.findFirst()
			.orElse(null);
	}

	private PacienteDieta buildAssignmentShell(final Paciente paciente, final PacienteDieta pacienteDieta) {
		final PacienteDieta newAssignment = new PacienteDieta();
		newAssignment.setPaciente(paciente);
		newAssignment.setStartDate(pacienteDieta.getStartDate());
		newAssignment.setEndDate(pacienteDieta.getEndDate());
		newAssignment
			.setStatus(pacienteDieta.getStatus() != null ? pacienteDieta.getStatus() : PacienteDietaStatus.ACTIVE);
		newAssignment.setNotes(pacienteDieta.getNotes());
		return newAssignment;
	}

	private void applyMetadataUpdate(final PacienteDieta existing, final PacienteDieta pacienteDieta) {
		if (pacienteDieta.getStartDate() != null) {
			existing.setStartDate(pacienteDieta.getStartDate());
		}
		if (pacienteDieta.getEndDate() != null) {
			existing.setEndDate(pacienteDieta.getEndDate());
		}
		if (pacienteDieta.getStatus() != null) {
			existing.setStatus(pacienteDieta.getStatus());
		}
		if (pacienteDieta.getNotes() != null) {
			existing.setNotes(pacienteDieta.getNotes());
		}
	}

	private PacienteDieta loadAssignment(final Long id) {
		return pacienteDietaRepository.findById(id)
			.orElseThrow(() -> new IllegalArgumentException("No se ha encontrado asignación con id " + id));
	}

	private void mergeWeekdaySlots(final PacienteDieta assignment, final Map<Integer, Long> weekdayCatalogDietaIds,
			final Long pacienteId, final String userId) {
		final Map<Integer, PacienteDietaWeekday> existingByDay = new LinkedHashMap<>();
		for (final PacienteDietaWeekday slot : pacienteDietaWeekdayRepository
			.findByPacienteDietaIdOrderByDayOfWeekAsc(assignment.getId())) {
			if (slot.getDayOfWeek() != null) {
				existingByDay.put(slot.getDayOfWeek(), slot);
			}
		}
		pacienteDietaWeekdayRepository.deleteByPacienteDietaId(assignment.getId());
		final Map<Integer, Long> normalized = normalizeWeekdayMap(weekdayCatalogDietaIds);
		for (final int day : PacienteDietaWeekdayLabels.ISO_DAYS_MONDAY_FIRST) {
			final Long catalogDietaId = normalized.get(day);
			if (catalogDietaId != null) {
				persistWeekdaySlot(assignment, day, catalogDietaId, pacienteId, userId);
			}
			else if (existingByDay.containsKey(day)) {
				final PacienteDietaWeekday retained = existingByDay.get(day);
				final PacienteDietaWeekday slot = new PacienteDietaWeekday();
				slot.setPacienteDieta(assignment);
				slot.setDayOfWeek(day);
				slot.setDieta(retained.getDieta());
				pacienteDietaWeekdayRepository.save(slot);
			}
		}
	}

	private void replaceWeekdaySlots(final PacienteDieta assignment, final Map<Integer, Long> weekdayCatalogDietaIds,
			final Long pacienteId, final String userId) {
		pacienteDietaWeekdayRepository.deleteByPacienteDietaId(assignment.getId());
		final Map<Integer, Long> normalized = normalizeWeekdayMap(weekdayCatalogDietaIds);
		for (final Map.Entry<Integer, Long> entry : normalized.entrySet()) {
			persistWeekdaySlot(assignment, entry.getKey(), entry.getValue(), pacienteId, userId);
		}
	}

	private void persistWeekdaySlot(final PacienteDieta assignment, final int day, final Long catalogDietaId,
			final Long pacienteId, final String userId) {
		final Dieta sourceDieta = dietaRepository.findById(catalogDietaId)
			.orElseThrow(() -> new IllegalArgumentException("No se ha encontrado dieta con id " + catalogDietaId));
		assertAssignableCatalogDieta(sourceDieta);
		final Dieta patientCopy = dietaService.copyDietaForPatientAssignment(catalogDietaId, pacienteId, userId);
		final PacienteDietaWeekday slot = new PacienteDietaWeekday();
		slot.setPacienteDieta(assignment);
		slot.setDayOfWeek(day);
		slot.setDieta(patientCopy);
		pacienteDietaWeekdayRepository.save(slot);
	}

	private static Map<Integer, Long> normalizeWeekdayMap(final Map<Integer, Long> weekdayCatalogDietaIds) {
		final Map<Integer, Long> normalized = new LinkedHashMap<>();
		for (final int day : PacienteDietaWeekdayLabels.ISO_DAYS_MONDAY_FIRST) {
			final Long dietaId = weekdayCatalogDietaIds.get(day);
			if (dietaId != null) {
				normalized.put(day, dietaId);
			}
		}
		return normalized;
	}

	private static void assertAssignableCatalogDieta(final Dieta sourceDieta) {
		if (DietaCatalogConstants.isPatientAssignment(sourceDieta)) {
			throw new IllegalArgumentException("No se puede asignar una dieta exclusiva de otro paciente");
		}
	}

	private static int toIsoDayOfWeek(final DayOfWeek dayOfWeek) {
		return dayOfWeek.getValue();
	}

}
