package com.nutriconsultas.subscription;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface NutritionistInvitationRepository
		extends JpaRepository<NutritionistInvitation, Long>, JpaSpecificationExecutor<NutritionistInvitation> {

	Optional<NutritionistInvitation> findByTokenHash(String tokenHash);

	Optional<NutritionistInvitation> findBySubscriptionId(Long subscriptionId);

	List<NutritionistInvitation> findAllByOrderByCreatedAtDesc();

	Optional<NutritionistInvitation> findByEmailIgnoreCaseAndStatus(String email, InvitationStatus status);

	Optional<NutritionistInvitation> findFirstByEmailIgnoreCaseAndStatusOrderByRedeemedAtDesc(String email,
			InvitationStatus status);

	Optional<NutritionistInvitation> findFirstByRedeemedByUserIdAndStatusOrderByRedeemedAtDesc(String redeemedByUserId,
			InvitationStatus status);

}
