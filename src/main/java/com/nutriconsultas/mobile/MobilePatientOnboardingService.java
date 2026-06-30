package com.nutriconsultas.mobile;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.nutriconsultas.mobile.dto.AssignedDietPlanReferenceDto;
import com.nutriconsultas.mobile.dto.PatchPatientOnboardingProfileRequest;
import com.nutriconsultas.mobile.dto.PatientOnboardingProfileDto;
import com.nutriconsultas.paciente.Paciente;
import com.nutriconsultas.paciente.PacienteAvatarCatalog;
import com.nutriconsultas.paciente.PacienteDieta;
import com.nutriconsultas.paciente.PacienteDietaRepository;
import com.nutriconsultas.paciente.PacienteDietaStatus;
import com.nutriconsultas.paciente.PacienteRepository;
import com.nutriconsultas.paciente.PacienteStatus;
import com.nutriconsultas.profile.NutritionistBrandingHelper;
import com.nutriconsultas.profile.NutritionistProfileRepository;
import com.nutriconsultas.util.LogRedaction;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class MobilePatientOnboardingService {

	private final PacienteRepository pacienteRepository;

	private final PacienteDietaRepository pacienteDietaRepository;

	private final NutritionistProfileRepository nutritionistProfileRepository;

	public MobilePatientOnboardingService(final PacienteRepository pacienteRepository,
			final PacienteDietaRepository pacienteDietaRepository,
			final NutritionistProfileRepository nutritionistProfileRepository) {
		this.pacienteRepository = pacienteRepository;
		this.pacienteDietaRepository = pacienteDietaRepository;
		this.nutritionistProfileRepository = nutritionistProfileRepository;
	}

	@Transactional(readOnly = true)
	public PatientOnboardingProfileDto getProfile(final Long pacienteId) {
		final Paciente paciente = requireOnboardingEligiblePatient(pacienteId);
		final List<PacienteDieta> activeAssignments = pacienteDietaRepository.findByPacienteIdAndStatus(pacienteId,
				PacienteDietaStatus.ACTIVE);
		final AssignedDietPlanReferenceDto assignedDietPlan = PatientOnboardingProfileDto
			.resolvePrimaryAssignment(activeAssignments);
		final String nutritionistDisplayName = resolveNutritionistDisplayName(paciente);
		if (log.isDebugEnabled()) {
			log.debug("Loaded mobile onboarding profile for patient {}", LogRedaction.redactPaciente(pacienteId));
		}
		return PatientOnboardingProfileDto.fromEntity(paciente, assignedDietPlan, nutritionistDisplayName);
	}

	@Transactional
	public PatientOnboardingProfileDto updateProfile(final Long pacienteId,
			final PatchPatientOnboardingProfileRequest request) {
		final Paciente paciente = requireOnboardingEligiblePatient(pacienteId);
		applyPatch(paciente, request);
		maybeActivate(paciente);
		final Paciente saved = pacienteRepository.save(paciente);
		if (log.isInfoEnabled()) {
			log.info("Updated mobile onboarding profile for patient {} status={}",
					LogRedaction.redactPaciente(pacienteId), saved.getStatus());
		}
		return getProfile(saved.getId());
	}

	private Paciente requireOnboardingEligiblePatient(final Long pacienteId) {
		final Paciente paciente = pacienteRepository.findById(pacienteId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
		if (paciente.getStatus() != PacienteStatus.ONBOARDING && paciente.getStatus() != PacienteStatus.ACTIVE) {
			throw new PatientOnboardingRequiredException();
		}
		return paciente;
	}

	private void applyPatch(final Paciente paciente, final PatchPatientOnboardingProfileRequest request) {
		if (request.name() != null) {
			paciente.setName(request.name().trim());
		}
		if (request.displayName() != null) {
			paciente.setDisplayName(request.displayName().trim());
		}
		if (request.dob() != null) {
			paciente.setDob(toDate(request.dob()));
		}
		if (request.gender() != null) {
			paciente.setGender(request.gender().trim().toUpperCase(Locale.ROOT));
		}
		if (request.email() != null) {
			paciente.setEmail(request.email().trim().toLowerCase(Locale.ROOT));
		}
		if (request.phone() != null) {
			paciente.setPhone(request.phone().trim());
		}
		if (request.avatarId() != null) {
			final String avatarId = request.avatarId().trim();
			if (!PacienteAvatarCatalog.isValid(avatarId)) {
				throw new PatientOnboardingInvalidAvatarException();
			}
			paciente.setAvatarId(avatarId);
		}
	}

	private void maybeActivate(final Paciente paciente) {
		if (paciente.getStatus() == PacienteStatus.ONBOARDING && PatientOnboardingCompleteness.isComplete(paciente)) {
			paciente.setStatus(PacienteStatus.ACTIVE);
		}
	}

	private static Date toDate(final LocalDate dob) {
		return Date.from(dob.atStartOfDay(ZoneId.systemDefault()).toInstant());
	}

	private String resolveNutritionistDisplayName(final Paciente paciente) {
		final String nutritionistUserId = paciente.getUserId();
		if (nutritionistUserId == null || nutritionistUserId.isBlank()) {
			return null;
		}
		return nutritionistProfileRepository.findByUserId(nutritionistUserId)
			.map(profile -> NutritionistBrandingHelper.resolveDisplayName(profile, null))
			.orElse(null);
	}

}
