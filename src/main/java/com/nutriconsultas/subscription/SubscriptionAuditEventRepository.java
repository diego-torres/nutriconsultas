package com.nutriconsultas.subscription;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SubscriptionAuditEventRepository extends JpaRepository<SubscriptionAuditEvent, Long> {

	@Query("SELECT e FROM SubscriptionAuditEvent e JOIN FETCH e.subscription s "
			+ "WHERE e.eventType = com.nutriconsultas.subscription.SubscriptionAuditEventType.PLATFORM_ADMIN_ACTION "
			+ "AND e.newStatus = com.nutriconsultas.subscription.SubscriptionStatus.CANCELLED "
			+ "AND e.details LIKE '%action=access.revoke%' AND s.tenantPurgedAt IS NULL "
			+ "AND e.createdAt <= :cutoff ORDER BY e.createdAt ASC")
	List<SubscriptionAuditEvent> findEligibleAccessRevokeEvents(@Param("cutoff") Instant cutoff);

}
