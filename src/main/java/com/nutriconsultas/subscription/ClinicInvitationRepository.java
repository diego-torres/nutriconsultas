package com.nutriconsultas.subscription;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ClinicInvitationRepository extends JpaRepository<ClinicInvitation, Long> {

	Optional<ClinicInvitation> findByTokenHash(String tokenHash);

	long countByClinicIdAndStatus(Long clinicId, InvitationStatus status);

	List<ClinicInvitation> findByClinicIdAndStatusOrderByCreatedAtDesc(Long clinicId, InvitationStatus status);

	Optional<ClinicInvitation> findByEmailIgnoreCaseAndStatus(String email, InvitationStatus status);

	Optional<ClinicInvitation> findFirstByEmailIgnoreCaseAndStatusOrderByRedeemedAtDesc(String email,
			InvitationStatus status);

	Optional<ClinicInvitation> findFirstByRedeemedByUserIdOrderByRedeemedAtDesc(String redeemedByUserId);

	Optional<ClinicInvitation> findByIdAndClinicId(Long id, Long clinicId);

}
