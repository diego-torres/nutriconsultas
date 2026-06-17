package com.nutriconsultas.subscription;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

	Optional<Subscription> findByExternalSubscriptionId(String externalSubscriptionId);

	@Query("SELECT s FROM Subscription s WHERE s.status = :status AND s.periodEnd IS NOT NULL AND s.periodEnd < :cutoff")
	List<Subscription> findByStatusAndPeriodEndBefore(@Param("status") SubscriptionStatus status,
			@Param("cutoff") Instant cutoff);

	@Query("SELECT s FROM Subscription s WHERE s.status = :status AND s.periodEnd IS NOT NULL "
			+ "AND s.periodEnd >= :windowStart AND s.periodEnd < :windowEnd")
	List<Subscription> findActiveExpiringBetween(@Param("status") SubscriptionStatus status,
			@Param("windowStart") Instant windowStart, @Param("windowEnd") Instant windowEnd);

	List<Subscription> findByStatus(SubscriptionStatus status);

}
