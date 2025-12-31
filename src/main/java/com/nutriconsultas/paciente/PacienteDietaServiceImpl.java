package com.nutriconsultas.paciente;

import java.util.List;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nutriconsultas.dieta.Dieta;
import com.nutriconsultas.dieta.DietaRepository;

import lombok.extern.slf4j.Slf4j;

@Service
@Transactional
@Slf4j
public class PacienteDietaServiceImpl implements PacienteDietaService {

	@Autowired
	private PacienteDietaRepository pacienteDietaRepository;

	@Autowired
	private PacienteRepository pacienteRepository;

	@Autowired
	private DietaRepository dietaRepository;

	@Override
	public PacienteDieta assignDieta(@NonNull final Long pacienteId, @NonNull final Long dietaId,
			@NonNull final PacienteDieta pacienteDieta) {
		log.info("Assigning dieta {} to paciente {}", dietaId, pacienteId);
		final Paciente paciente = pacienteRepository.findById(pacienteId)
			.orElseThrow(() -> new IllegalArgumentException("No se ha encontrado paciente con id " + pacienteId));
		final Dieta dieta = dietaRepository.findById(dietaId)
			.orElseThrow(() -> new IllegalArgumentException("No se ha encontrado dieta con id " + dietaId));

		// Create a new PacienteDieta to avoid issues with existing IDs from form binding
		// This ensures we're creating a new entity, not trying to update an existing one
		final PacienteDieta newAssignment = new PacienteDieta();
		newAssignment.setPaciente(paciente);
		newAssignment.setDieta(dieta);
		newAssignment.setStartDate(pacienteDieta.getStartDate());
		newAssignment.setEndDate(pacienteDieta.getEndDate());
		newAssignment
			.setStatus(pacienteDieta.getStatus() != null ? pacienteDieta.getStatus() : PacienteDietaStatus.ACTIVE);
		newAssignment.setNotes(pacienteDieta.getNotes());

		return pacienteDietaRepository.save(newAssignment);
	}

	@Override
	public PacienteDieta updateAssignment(@NonNull final Long id, @NonNull final PacienteDieta pacienteDieta) {
		log.info("Updating dieta assignment {}", id);
		final PacienteDieta existing = Objects.requireNonNull(pacienteDietaRepository.findById(id)
			.orElseThrow(() -> new IllegalArgumentException("No se ha encontrado asignación con id " + id)));

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

		return Objects.requireNonNull(pacienteDietaRepository.save(existing));
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

}
