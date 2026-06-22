package com.nutriconsultas.subscription.maintenance;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nutriconsultas.booking.NutritionistAvailabilityBlock;
import com.nutriconsultas.dieta.Dieta;
import com.nutriconsultas.paciente.Paciente;
import com.nutriconsultas.platillos.Platillo;
import com.nutriconsultas.profile.NutritionistProfile;
import com.nutriconsultas.subscription.Clinic;
import com.nutriconsultas.subscription.MembershipStatus;
import com.nutriconsultas.subscription.maintenance.MaintenanceBackupSerializer.NutritionistTenantSnapshot;
import com.nutriconsultas.util.LogRedaction;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class NutritionistTenantPurgeService {

	private final NutritionistTenantCatalogDependencies catalogDependencies;

	private final NutritionistTenantAccountDependencies accountDependencies;

	public NutritionistTenantPurgeService(final NutritionistTenantCatalogDependencies catalogDependencies,
			final NutritionistTenantAccountDependencies accountDependencies) {
		this.catalogDependencies = catalogDependencies;
		this.accountDependencies = accountDependencies;
	}

	@Transactional(readOnly = true)
	public NutritionistTenantSnapshot buildSnapshot(
			@NonNull final RevokedNutritionistEligibilityService.EligibleRevokedNutritionist eligible) {
		final String userId = eligible.userId();
		final List<Paciente> patients = catalogDependencies.getPacientes().findByUserId(userId);
		final List<Long> patientIds = patients.stream().map(Paciente::getId).toList();
		final List<Long> dietaIds = catalogDependencies.getDietas()
			.findByUserId(userId)
			.stream()
			.map(Dieta::getId)
			.toList();
		final List<Long> platilloIds = catalogDependencies.getPlatillos()
			.findByUserId(userId)
			.stream()
			.map(Platillo::getId)
			.toList();
		final Map<String, Object> profile = accountDependencies.getProfiles()
			.findByUserId(userId)
			.map(this::profileSummary)
			.orElse(Map.of());
		final Long clinicId = accountDependencies.getClinics()
			.findByDirectorUserId(userId)
			.map(Clinic::getId)
			.orElse(null);
		return new NutritionistTenantSnapshot(userId, eligible.subscriptionId(), eligible.revokedAt(),
				MaintenanceBackupSerializer.patientSummaries(patientIds), dietaIds, platilloIds, profile, clinicId,
				patientIds.size());
	}

	@Transactional
	public void purgeTenant(@NonNull final RevokedNutritionistEligibilityService.EligibleRevokedNutritionist eligible) {
		final String userId = eligible.userId();
		for (final Paciente patient : catalogDependencies.getPacientes().findByUserId(userId)) {
			catalogDependencies.getPatientDeletion().deletePatientWithHistory(patient.getId(), userId);
		}
		for (final Dieta dieta : catalogDependencies.getDietas().findByUserId(userId)) {
			catalogDependencies.getDietaService().deleteDieta(dieta.getId());
		}
		for (final Platillo platillo : catalogDependencies.getPlatillos().findByUserId(userId)) {
			catalogDependencies.getPlatilloService().deletePlatillo(platillo.getId());
		}
		deleteBookingAvailability(userId);
		accountDependencies.getProfiles()
			.findByUserId(userId)
			.ifPresent(profile -> accountDependencies.getProfiles().delete(profile));
		deleteClinicIfSoleDirector(userId);
		markTenantPurged(eligible.subscriptionId());
		if (log.isInfoEnabled()) {
			log.info("Purged tenant data for nutritionist userId={}", LogRedaction.redactUserId(userId));
		}
	}

	private void deleteBookingAvailability(final String userId) {
		accountDependencies.getAvailabilitySettings()
			.findByUserId(userId)
			.ifPresent(settings -> accountDependencies.getAvailabilitySettings().delete(settings));
		accountDependencies.getWorkingHoursIntervals().deleteByUserId(userId);
		for (final NutritionistAvailabilityBlock block : accountDependencies.getAvailabilityBlocks()
			.findByUserId(userId)) {
			accountDependencies.getAvailabilityBlocks().delete(block);
		}
	}

	private void deleteClinicIfSoleDirector(final String userId) {
		final Optional<Clinic> clinicOptional = accountDependencies.getClinics().findByDirectorUserId(userId);
		if (clinicOptional.isEmpty()) {
			accountDependencies.getClinicMembers()
				.findByUserIdWithClinicAndSubscription(userId)
				.ifPresent(member -> accountDependencies.getClinicMembers().delete(member));
			return;
		}
		final Clinic clinic = clinicOptional.get();
		final long activeMembers = accountDependencies.getClinicMembers()
			.countByClinicIdAndMembershipStatus(clinic.getId(), MembershipStatus.ACTIVE);
		if (activeMembers == 0) {
			accountDependencies.getClinics().delete(clinic);
		}
	}

	private void markTenantPurged(final Long subscriptionId) {
		accountDependencies.getSubscriptions().findById(subscriptionId).ifPresent(subscription -> {
			subscription.setTenantPurgedAt(Instant.now());
			accountDependencies.getSubscriptions().save(subscription);
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
