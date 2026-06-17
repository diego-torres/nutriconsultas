package com.nutriconsultas.subscription;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface NutritionistInvitationRepository extends JpaRepository<NutritionistInvitation, Long> {

	Optional<NutritionistInvitation> findByTokenHash(String tokenHash);

	Optional<NutritionistInvitation> findBySubscriptionId(Long subscriptionId);

	List<NutritionistInvitation> findAllByOrderByCreatedAtDesc();

	Optional<NutritionistInvitation> findByEmailIgnoreCaseAndStatus(String email, InvitationStatus status);

}
