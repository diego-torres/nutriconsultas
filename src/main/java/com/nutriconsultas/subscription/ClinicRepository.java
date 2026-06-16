package com.nutriconsultas.subscription;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClinicRepository extends JpaRepository<Clinic, Long> {

	Optional<Clinic> findByDirectorUserId(String directorUserId);

	@Query("SELECT c FROM Clinic c JOIN FETCH c.subscription WHERE c.directorUserId = :directorUserId")
	Optional<Clinic> findByDirectorUserIdWithSubscription(@Param("directorUserId") String directorUserId);

}
