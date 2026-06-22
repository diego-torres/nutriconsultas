package com.nutriconsultas.subscription.maintenance;

import org.springframework.stereotype.Component;

import com.nutriconsultas.booking.NutritionistAvailabilityBlockRepository;
import com.nutriconsultas.booking.NutritionistAvailabilitySettingsRepository;
import com.nutriconsultas.booking.NutritionistWorkingHoursIntervalRepository;
import com.nutriconsultas.profile.NutritionistProfileRepository;
import com.nutriconsultas.subscription.ClinicMemberRepository;
import com.nutriconsultas.subscription.ClinicRepository;
import com.nutriconsultas.subscription.SubscriptionRepository;

@Component
public final class NutritionistTenantAccountDependencies {

	private final NutritionistProfileRepository profiles;

	private final NutritionistAvailabilitySettingsRepository availabilitySettings;

	private final NutritionistWorkingHoursIntervalRepository workingHoursIntervals;

	private final NutritionistAvailabilityBlockRepository availabilityBlocks;

	private final ClinicRepository clinics;

	private final ClinicMemberRepository clinicMembers;

	private final SubscriptionRepository subscriptions;

	public NutritionistTenantAccountDependencies(final NutritionistProfileRepository profiles,
			final NutritionistAvailabilitySettingsRepository availabilitySettings,
			final NutritionistWorkingHoursIntervalRepository workingHoursIntervals,
			final NutritionistAvailabilityBlockRepository availabilityBlocks, final ClinicRepository clinics,
			final ClinicMemberRepository clinicMembers, final SubscriptionRepository subscriptions) {
		this.profiles = profiles;
		this.availabilitySettings = availabilitySettings;
		this.workingHoursIntervals = workingHoursIntervals;
		this.availabilityBlocks = availabilityBlocks;
		this.clinics = clinics;
		this.clinicMembers = clinicMembers;
		this.subscriptions = subscriptions;
	}

	public NutritionistProfileRepository getProfiles() {
		return profiles;
	}

	public NutritionistAvailabilitySettingsRepository getAvailabilitySettings() {
		return availabilitySettings;
	}

	public NutritionistWorkingHoursIntervalRepository getWorkingHoursIntervals() {
		return workingHoursIntervals;
	}

	public NutritionistAvailabilityBlockRepository getAvailabilityBlocks() {
		return availabilityBlocks;
	}

	public ClinicRepository getClinics() {
		return clinics;
	}

	public ClinicMemberRepository getClinicMembers() {
		return clinicMembers;
	}

	public SubscriptionRepository getSubscriptions() {
		return subscriptions;
	}

}
