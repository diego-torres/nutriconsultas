package com.nutriconsultas.profile;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA repository for {@link NutritionistProfile}.
 */
public interface NutritionistProfileRepository extends JpaRepository<NutritionistProfile, Long> {

	/**
	 * Finds a profile by the Auth0 user subject identifier.
	 * @param userId the Auth0 {@code sub} claim
	 * @return the profile wrapped in an Optional
	 */
	Optional<NutritionistProfile> findByUserId(String userId);

	Optional<NutritionistProfile> findByPublicBookingId(String publicBookingId);

}
