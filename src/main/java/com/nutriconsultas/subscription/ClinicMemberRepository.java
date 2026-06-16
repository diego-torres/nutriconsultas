package com.nutriconsultas.subscription;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClinicMemberRepository extends JpaRepository<ClinicMember, Long> {

	List<ClinicMember> findByClinicIdAndMembershipStatus(Long clinicId, MembershipStatus membershipStatus);

	Optional<ClinicMember> findByClinicIdAndUserId(Long clinicId, String userId);

	@Query("SELECT cm FROM ClinicMember cm JOIN FETCH cm.clinic c JOIN FETCH c.subscription "
			+ "WHERE cm.userId = :userId")
	Optional<ClinicMember> findByUserIdWithClinicAndSubscription(@Param("userId") String userId);

}
