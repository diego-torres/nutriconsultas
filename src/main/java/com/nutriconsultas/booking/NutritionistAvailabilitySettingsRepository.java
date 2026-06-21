package com.nutriconsultas.booking;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NutritionistAvailabilitySettingsRepository
		extends JpaRepository<NutritionistAvailabilitySettings, Long> {

	Optional<NutritionistAvailabilitySettings> findByUserId(String userId);

}
