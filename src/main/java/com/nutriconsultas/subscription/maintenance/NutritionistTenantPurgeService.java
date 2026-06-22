package com.nutriconsultas.subscription.maintenance;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nutriconsultas.booking.NutritionistAvailabilityBlockRepository;
import com.nutriconsultas.booking.NutritionistAvailabilitySettingsRepository;
import com.nutriconsultas.booking.NutritionistWorkingHoursIntervalRepository;
import com.nutriconsultas.dieta.Dieta;
import com.nutriconsultas.dieta.DietaRepository;
import com.nutriconsultas.dieta.DietaService;
import com.nutriconsultas.paciente.Paciente;
import com.nutriconsultas.paciente.PacienteDeletionService;
import com.nutriconsultas.paciente.PacienteRepository;
import com.nutriconsultas.platillos.Platillo;
import com.nutriconsultas.platillos.PlatilloRepository;
import com.nutriconsultas.platillos.PlatilloService;
import com.nutriconsultas.profile.NutritionistProfile;
import com.nutriconsultas.profile.NutritionistProfileRepository;
import com.nutriconsultas.subscription.Clinic;
import com.nutriconsultas.subscription.ClinicMember;
import com.nutriconsultas.subscription.ClinicMemberRepository;
import com.nutriconsultas.subscription.ClinicRepository;
import com.nutriconsultas.subscription.MembershipStatus;
import com.nutriconsultas.subscription.SubscriptionRepository;
import com.nutriconsultas.subscription.maintenance.MaintenanceBackupSerializer.NutritionistTenantSnapshot;
import com.nutriconsultas.util.LogRedaction;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class NutritionistTenantPurgeService {

	private final PacienteRepository pacienteRepository;

	private final PacienteDeletionService pacienteDeletionService;

	private final DietaRepository dietaRepository;

	private final DietaService dietaService;

	private final PlatilloRepository platilloRepository;

	private final PlatilloService platilloService;

	private final NutritionistProfileRepository profileRepository;

	private final NutritionistAvailabilitySettingsRepository availabilitySettingsRepository;

	private final NutritionistWorkingHoursIntervalRepository workingHoursIntervalRepository;

	private final NutritionistAvailabilityBlockRepository availabilityBlockRepository;

	private final ClinicRepository clinicRepository;

	private final ClinicMemberRepository clinicMemberRepository;

	private final SubscriptionRepository subscriptionRepository;

	public NutritionistTenantPurgeService(final PacienteRepository pacienteRepository,
			final PacienteDeletionService pacienteDeletionService, final DietaRepository dietaRepository,
			final DietaService dietaService, final PlatilloRepository platilloRepository,
			final PlatilloService platilloService, final NutritionistProfileRepository profileRepository,
			final NutritionistAvailabilitySettingsRepository availabilitySettingsRepository,
			final NutritionistWorkingHoursIntervalRepository workingHoursIntervalRepository,
			final NutritionistAvailabilityBlockRepository availabilityBlockRepository,
			final ClinicRepository clinicRepository, final ClinicMemberRepository clinicMemberRepository,
			final SubscriptionRepository subscriptionRepository) {
		this.pacienteRepository = pacienteRepository;
		this.pacienteDeletionService = pacienteDeletionService;
		this.dietaRepository = dietaRepository;
		this.dietaService = dietaService;
		this.platilloRepository = platilloRepository;
		this.platilloService = platilloService;
		this.profileRepository = profileRepository;
		this.availabilitySettingsRepository = availabilitySettingsRepository;
		this.workingHoursIntervalRepository = workingHoursIntervalRepository;
		this.availabilityBlockRepository = availabilityBlockRepository;
		this.clinicRepository = clinicRepository;
		this.clinicMemberRepository = clinicMemberRepository;
		this.subscriptionRepository = subscriptionRepository;
	}

	@Transactional(readOnly = true)
	public NutritionistTenantSnapshot buildSnapshot(
			@NonNull final RevokedNutritionistEligibilityService.EligibleRevokedNutritionist eligible) {
		final String userId = eligible.userId();
		final List<Paciente> patients = pacienteRepository.findByUserId(userId);
		final List<Long> patientIds = patients.stream().map(Paciente::getId).toList();
		final List<Long> dietaIds = dietaRepository.findByUserId(userId).stream().map(Dieta::getId).toList();
		final List<Long> platilloIds = platilloRepository.findByUserId(userId).stream().map(Platillo::getId).toList();
		final Map<String, Object> profile = profileRepository.findByUserId(userId)
			.map(this::profileSummary)
			.orElse(Map.of());
		final Long clinicId = clinicRepository.findByDirectorUserId(userId).map(Clinic::getId).orElse(null);
		return new NutritionistTenantSnapshot(userId, eligible.subscriptionId(), eligible.revokedAt(),
				MaintenanceBackupSerializer.patientSummaries(patientIds), dietaIds, platilloIds, profile, clinicId,
				patientIds.size());
	}

	@Transactional
	public void purgeTenant(@NonNull final RevokedNutritionistEligibilityService.EligibleRevokedNutritionist eligible) {
		final String userId = eligible.userId();
		for (final Paciente patient : pacienteRepository.findByUserId(userId)) {
			pacienteDeletionService.deletePatientWithHistory(patient.getId(), userId);
		}
		for (final Dieta dieta : dietaRepository.findByUserId(userId)) {
			dietaService.deleteDieta(dieta.getId());
		}
		for (final Platillo platillo : platilloRepository.findByUserId(userId)) {
			platilloService.deletePlatillo(platillo.getId());
		}
		deleteBookingAvailability(userId);
		profileRepository.findByUserId(userId).ifPresent(profileRepository::delete);
		deleteClinicIfSoleDirector(userId);
		markTenantPurged(eligible.subscriptionId());
		if (log.isInfoEnabled()) {
			log.info("Purged tenant data for nutritionist userId={}", LogRedaction.redactUserId(userId));
		}
	}

	private void deleteBookingAvailability(final String userId) {
		availabilitySettingsRepository.findByUserId(userId).ifPresent(availabilitySettingsRepository::delete);
		workingHoursIntervalRepository.deleteByUserId(userId);
		for (final com.nutriconsultas.booking.NutritionistAvailabilityBlock block : availabilityBlockRepository
			.findByUserId(userId)) {
			availabilityBlockRepository.delete(block);
		}
	}

	private void deleteClinicIfSoleDirector(final String userId) {
		final Optional<Clinic> clinicOptional = clinicRepository.findByDirectorUserId(userId);
		if (clinicOptional.isEmpty()) {
			clinicMemberRepository.findByUserIdWithClinicAndSubscription(userId)
				.ifPresent(clinicMemberRepository::delete);
			return;
		}
		final Clinic clinic = clinicOptional.get();
		final long activeMembers = clinicMemberRepository.countByClinicIdAndMembershipStatus(clinic.getId(),
				MembershipStatus.ACTIVE);
		if (activeMembers == 0) {
			clinicRepository.delete(clinic);
		}
	}

	private void markTenantPurged(final Long subscriptionId) {
		subscriptionRepository.findById(subscriptionId).ifPresent(subscription -> {
			subscription.setTenantPurgedAt(Instant.now());
			subscriptionRepository.save(subscription);
		});
	}

	private Map<String, Object> profileSummary(final NutritionistProfile profile) {
		final Map<String, Object> summary = new HashMap<>();
		summary.put("id", profile.getId());
		summary.put("publicBookingId", profile.getPublicBookingId());
		summary.put("logoExtension", profile.getLogoExtension());
		return summary;
	}

}
