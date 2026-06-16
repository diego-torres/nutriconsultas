package com.nutriconsultas.subscription;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ClinicMemberRepository extends JpaRepository<ClinicMember, Long> {

	List<ClinicMember> findByClinicIdAndMembershipStatus(Long clinicId, MembershipStatus membershipStatus);

	Optional<ClinicMember> findByClinicIdAndUserId(Long clinicId, String userId);

}
