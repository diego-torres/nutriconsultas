package com.nutriconsultas.support;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SupportTicketRepository extends JpaRepository<SupportTicket, Long> {

	List<SupportTicket> findByUserIdOrderByCreatedAtDesc(String userId);

	Optional<SupportTicket> findByIdAndUserId(Long id, String userId);

	List<SupportTicket> findByStatusOrderByCreatedAtDesc(SupportTicketStatus status);

	List<SupportTicket> findAllByOrderByCreatedAtDesc();

}
