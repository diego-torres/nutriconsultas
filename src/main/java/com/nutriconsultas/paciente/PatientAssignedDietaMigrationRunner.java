package com.nutriconsultas.paciente;

import java.util.List;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.nutriconsultas.dieta.Dieta;
import com.nutriconsultas.dieta.DietaService;
import com.nutriconsultas.util.LogRedaction;

import lombok.extern.slf4j.Slf4j;

/**
 * One-time idempotent migration: existing {@link PacienteDieta} rows that still reference
 * shared catalog diets are repointed to patient-specific copies (#320).
 */
@Component
@Profile("!test")
@Slf4j
public class PatientAssignedDietaMigrationRunner {

	private final PacienteDietaRepository pacienteDietaRepository;

	private final DietaService dietaService;

	public PatientAssignedDietaMigrationRunner(final PacienteDietaRepository pacienteDietaRepository,
			final DietaService dietaService) {
		this.pacienteDietaRepository = pacienteDietaRepository;
		this.dietaService = dietaService;
	}

	@EventListener(ApplicationReadyEvent.class)
	@Transactional
	public void migrateSharedAssignmentsToPatientCopies() {
		final List<PacienteDieta> assignments = pacienteDietaRepository.findAssignmentsReferencingSharedDieta();
		if (assignments.isEmpty()) {
			return;
		}
		if (log.isInfoEnabled()) {
			log.info("Migrating {} patient diet assignment(s) to patient-specific copies", assignments.size());
		}
		for (final PacienteDieta assignment : assignments) {
			migrateAssignment(assignment);
		}
	}

	private void migrateAssignment(@NonNull final PacienteDieta assignment) {
		final Paciente paciente = assignment.getPaciente();
		final Dieta sharedDieta = assignment.getDieta();
		if (paciente == null || paciente.getId() == null || sharedDieta == null || sharedDieta.getId() == null) {
			return;
		}
		final String nutritionistUserId = paciente.getUserId();
		if (nutritionistUserId == null || nutritionistUserId.isBlank()) {
			log.warn("Skipping diet assignment migration for paciente {} without userId",
					LogRedaction.redactPaciente(paciente.getId()));
			return;
		}
		final Dieta patientCopy = dietaService.copyDietaForPatientAssignment(sharedDieta.getId(), paciente.getId(),
				nutritionistUserId);
		assignment.setDieta(patientCopy);
		pacienteDietaRepository.save(assignment);
		if (log.isInfoEnabled()) {
			log.info("Migrated assignment {} to patient diet copy {}", LogRedaction.redactPacienteDieta(assignment),
					patientCopy.getId());
		}
	}

}
