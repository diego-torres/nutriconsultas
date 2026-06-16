package com.nutriconsultas.subscription;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriptionAuditEventRepository extends JpaRepository<SubscriptionAuditEvent, Long> {

}
