package com.nutriconsultas.mobile;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.nutriconsultas.auth0.Auth0UserLookup;
import com.nutriconsultas.paciente.Paciente;
import com.nutriconsultas.paciente.PacienteRepository;
import com.nutriconsultas.util.LogRedaction;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class PatientAuthLinkageService {

	private final PacienteRepository pacienteRepository;

	private final Auth0UserLookup auth0UserLookup;

	public PatientAuthLinkageService(final PacienteRepository pacienteRepository,
			final Auth0UserLookup auth0UserLookup) {
		this.pacienteRepository = pacienteRepository;
		this.auth0UserLookup = auth0UserLookup;
	}

	@Transactional(readOnly = true)
	public PatientMobileAuthStatus getStatus(final Long pacienteId, final String nutritionistUserId) {
		final Paciente paciente = requireOwnedPaciente(pacienteId, nutritionistUserId);
		return PatientMobileAuthStatus.of(paciente.getPatientAuthSub(), auth0UserLookup.isConfigured());
	}

	@Transactional
	public Paciente linkByEmail(final Long pacienteId, final String nutritionistUserId) {
		if (!auth0UserLookup.isConfigured()) {
			throw new Auth0ManagementNotConfiguredException();
		}
		final Paciente paciente = requireOwnedPaciente(pacienteId, nutritionistUserId);
		if (!StringUtils.hasText(paciente.getEmail())) {
			throw new PatientEmailRequiredForLinkException();
		}
		final String patientAuthSub = auth0UserLookup.findUserIdByEmail(paciente.getEmail().trim())
			.orElseThrow(PatientAuthUserNotFoundException::new);
		return assignSub(paciente, patientAuthSub);
	}

	@Transactional
	public Paciente linkBySub(final Long pacienteId, final String nutritionistUserId, final String patientAuthSub) {
		if (!StringUtils.hasText(patientAuthSub)) {
			throw new IllegalArgumentException("patientAuthSub is required");
		}
		final Paciente paciente = requireOwnedPaciente(pacienteId, nutritionistUserId);
		return assignSub(paciente, patientAuthSub.trim());
	}

	@Transactional
	public Paciente unlink(final Long pacienteId, final String nutritionistUserId) {
		final Paciente paciente = requireOwnedPaciente(pacienteId, nutritionistUserId);
		paciente.setPatientAuthSub(null);
		final Paciente saved = pacienteRepository.save(paciente);
		if (log.isInfoEnabled()) {
			log.info("Unlinked mobile Auth0 account for patient {}", LogRedaction.redactPaciente(saved.getId()));
		}
		return saved;
	}

	private Paciente assignSub(final Paciente paciente, final String patientAuthSub) {
		pacienteRepository.findByPatientAuthSub(patientAuthSub).ifPresent(existing -> {
			if (!existing.getId().equals(paciente.getId())) {
				throw new PatientAuthSubAlreadyLinkedException();
			}
		});
		paciente.setPatientAuthSub(patientAuthSub);
		final Paciente saved = pacienteRepository.save(paciente);
		if (log.isInfoEnabled()) {
			log.info("Linked mobile Auth0 account for patient {} sub={}", LogRedaction.redactPaciente(saved.getId()),
					LogRedaction.redactUserId(patientAuthSub));
		}
		return saved;
	}

	private Paciente requireOwnedPaciente(final Long pacienteId, final String nutritionistUserId) {
		return pacienteRepository.findByIdAndUserId(pacienteId, nutritionistUserId)
			.orElseThrow(() -> new IllegalArgumentException("Paciente no encontrado"));
	}

}
