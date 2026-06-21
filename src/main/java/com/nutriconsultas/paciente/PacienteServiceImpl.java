package com.nutriconsultas.paciente;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nutriconsultas.paciente.projection.PacienteListView;
import com.nutriconsultas.subscription.SubscriptionEntitlementService;
import com.nutriconsultas.util.LogRedaction;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class PacienteServiceImpl implements PacienteService {

	private final PacienteRepository repo;

	private final SubscriptionEntitlementService subscriptionEntitlementService;

	public PacienteServiceImpl(final PacienteRepository repo,
			final SubscriptionEntitlementService subscriptionEntitlementService) {
		this.repo = repo;
		this.subscriptionEntitlementService = subscriptionEntitlementService;
	}

	@Override
	public void delete(@NonNull final Long id) {
		log.info("deleting Paciente with id {}.", id);
		repo.deleteById(id);
		log.info("Paciente {} deleted successfully.", id);
	}

	@Override
	public Paciente findById(@NonNull final Long id) {
		log.info("finding Paciente with id {}.", id);
		final Paciente paciente = repo.findById(id).orElse(null);
		log.info("Paciente found {}.", LogRedaction.redactPaciente(paciente));
		return paciente;
	}

	@Override
	@Transactional
	public Paciente save(@NonNull final Paciente paciente) {
		log.info("saving Paciente {}.", LogRedaction.redactPaciente(paciente));
		if (paciente.getId() == null && paciente.getUserId() != null) {
			subscriptionEntitlementService.assertCanCreatePatient(paciente.getUserId());
		}
		final Paciente saved = repo.save(paciente);
		log.info("Paciente saved {}.", LogRedaction.redactPaciente(saved));
		return saved;
	}

	@Override
	@Transactional
	public Paciente updateAvatar(@NonNull final Long id, @NonNull final String userId, @NonNull final String avatarId) {
		if (!PacienteAvatarCatalog.isValid(avatarId)) {
			throw new IllegalArgumentException("Avatar no válido");
		}
		final Paciente paciente = findByIdAndUserId(id, userId);
		if (paciente == null) {
			throw new IllegalArgumentException("Paciente no encontrado");
		}
		paciente.setAvatarId(avatarId);
		final Paciente saved = repo.save(paciente);
		log.info("Updated avatar for patient id {}", id);
		return saved;
	}

	@Override
	public List<Paciente> findAll() {
		log.info("getting all Paciente records.");
		return repo.findAll();
	}

	@Override
	public Paciente findByIdAndUserId(@NonNull final Long id, @NonNull final String userId) {
		log.info("finding Paciente with id {} and userId {}.", id, userId);
		final Paciente paciente = repo.findByIdAndUserId(id, userId).orElse(null);
		log.info("Paciente found {}.", LogRedaction.redactPaciente(paciente));
		return paciente;
	}

	@Override
	@Transactional(readOnly = true)
	public List<Paciente> findAllByUserId(@NonNull final String userId) {
		log.info("getting all Paciente records for userId {}.", userId);
		return repo.findByUserId(userId);
	}

	@Override
	@Transactional(readOnly = true)
	public Page<Paciente> findAllByUserId(@NonNull final String userId, final Pageable pageable) {
		log.info("getting paginated Paciente records for userId {} with pageable {}.", userId, pageable);
		return repo.findByUserId(userId, pageable);
	}

	@Override
	@Transactional(readOnly = true)
	public Page<PacienteListView> findListViewsByUserId(@NonNull final String userId, final Pageable pageable) {
		log.info("getting paginated Paciente list views for userId {} with pageable {}.", userId, pageable);
		return repo.findListViewsByUserId(userId, pageable);
	}

	@Override
	@Transactional(readOnly = true)
	public List<PacienteListView> findListViewsByUserIdAndSearchTerm(@NonNull final String userId,
			@NonNull final String searchTerm) {
		log.info("searching Paciente list views for userId {} with search term '{}'.", userId, searchTerm);
		final String searchPattern = "%" + searchTerm.toLowerCase() + "%";
		return repo.findListViewsByUserIdAndSearchTerm(userId, searchPattern);
	}

	@Override
	@Transactional(readOnly = true)
	public Page<PacienteListView> findListViewsByUserIdAndSearchTerm(@NonNull final String userId,
			@NonNull final String searchTerm, final Pageable pageable) {
		log.info("searching paginated Paciente list views for userId {} with search term '{}' and pageable {}.", userId,
				searchTerm, pageable);
		final String searchPattern = "%" + searchTerm.toLowerCase() + "%";
		return repo.findListViewsByUserIdAndSearchTerm(userId, searchPattern, pageable);
	}

	@Override
	@Transactional(readOnly = true)
	public Page<Paciente> findAllByUserIdAndSearchTerm(@NonNull final String userId, @NonNull final String searchTerm,
			final Pageable pageable) {
		log.info("searching Paciente records for userId {} with search term '{}' and pageable {}.", userId, searchTerm,
				pageable);
		final String searchPattern = "%" + searchTerm.toLowerCase() + "%";
		return repo.findByUserIdAndSearchTerm(userId, searchPattern, pageable);
	}

	@Override
	@Transactional(readOnly = true)
	public long countByUserId(@NonNull final String userId) {
		log.info("counting Paciente records for userId {}.", userId);
		return repo.countByUserId(userId);
	}

	@Override
	@Transactional(readOnly = true)
	public long countByUserIdAndSearchTerm(@NonNull final String userId, @NonNull final String searchTerm) {
		log.info("counting Paciente records for userId {} with search term '{}'.", userId, searchTerm);
		final String searchPattern = "%" + searchTerm.toLowerCase() + "%";
		return repo.countByUserIdAndSearchTerm(userId, searchPattern);
	}

	@Override
	public void deleteByIdAndUserId(@NonNull final Long id, @NonNull final String userId) {
		log.info("deleting Paciente with id {} and userId {}.", id, userId);
		final Paciente paciente = repo.findByIdAndUserId(id, userId).orElse(null);
		if (paciente != null) {
			repo.deleteById(id);
			log.info("Paciente {} deleted successfully.", id);
		}
		else {
			log.warn("Paciente with id {} and userId {} not found, cannot delete.", id, userId);
		}
	}

}
