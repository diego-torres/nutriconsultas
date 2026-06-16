package com.nutriconsultas.subscription;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ClinicInvitationRepository extends JpaRepository<ClinicInvitation, Long> {

	Optional<ClinicInvitation> findByTokenHash(String tokenHash);

}
