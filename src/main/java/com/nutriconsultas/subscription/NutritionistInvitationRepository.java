package com.nutriconsultas.subscription;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface NutritionistInvitationRepository extends JpaRepository<NutritionistInvitation, Long> {

	Optional<NutritionistInvitation> findByTokenHash(String tokenHash);

}
