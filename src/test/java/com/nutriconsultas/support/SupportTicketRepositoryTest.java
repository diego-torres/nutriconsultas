package com.nutriconsultas.support;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@DataJpaTest
@ActiveProfiles("test")
@Transactional
class SupportTicketRepositoryTest {

	private static final String USER_A = "auth0|support-user-a";

	private static final String USER_B = "auth0|support-user-b";

	@Autowired
	private SupportTicketRepository supportTicketRepository;

	@Autowired
	private EntityManager entityManager;

	@Test
	void saveAndLoadRoundTripDefaultsToOpen() {
		final SupportTicket ticket = sampleTicket(USER_A, "No puedo exportar dieta", "El PDF falla en Safari.");
		final SupportTicket saved = supportTicketRepository.saveAndFlush(ticket);

		entityManager.clear();

		final SupportTicket loaded = supportTicketRepository.findById(saved.getId()).orElseThrow();
		assertThat(loaded.getUserId()).isEqualTo(USER_A);
		assertThat(loaded.getTitle()).isEqualTo("No puedo exportar dieta");
		assertThat(loaded.getDescription()).isEqualTo("El PDF falla en Safari.");
		assertThat(loaded.getStatus()).isEqualTo(SupportTicketStatus.OPEN);
		assertThat(loaded.getCreatedAt()).isNotNull();
		assertThat(loaded.getUpdatedAt()).isNotNull();
		assertThat(loaded.getClosedAt()).isNull();
		assertThat(loaded.getAdminNotes()).isNull();
	}

	@Test
	void findByUserIdOrderByCreatedAtDesc_returnsOnlyOwnTicketsNewestFirst() {
		final SupportTicket older = sampleTicket(USER_A, "Primero", "Detalle 1");
		older.setCreatedAt(java.time.Instant.parse("2026-01-01T10:00:00Z"));
		older.setUpdatedAt(older.getCreatedAt());
		supportTicketRepository.saveAndFlush(older);

		final SupportTicket newer = sampleTicket(USER_A, "Segundo", "Detalle 2");
		newer.setCreatedAt(java.time.Instant.parse("2026-01-02T10:00:00Z"));
		newer.setUpdatedAt(newer.getCreatedAt());
		supportTicketRepository.saveAndFlush(newer);

		supportTicketRepository.saveAndFlush(sampleTicket(USER_B, "Ajeno", "No listar"));

		final List<SupportTicket> tickets = supportTicketRepository.findByUserIdOrderByCreatedAtDesc(USER_A);

		assertThat(tickets).hasSize(2);
		assertThat(tickets).extracting(SupportTicket::getTitle).containsExactly("Segundo", "Primero");
		assertThat(tickets).allMatch(ticket -> USER_A.equals(ticket.getUserId()));
	}

	@Test
	void findByIdAndUserId_blocksCrossTenantAccess() {
		final SupportTicket ticket = supportTicketRepository
			.saveAndFlush(sampleTicket(USER_A, "Solo mío", "Descripción"));

		assertThat(supportTicketRepository.findByIdAndUserId(ticket.getId(), USER_A)).isPresent();
		assertThat(supportTicketRepository.findByIdAndUserId(ticket.getId(), USER_B)).isEmpty();
	}

	@Test
	void findByStatusOrderByCreatedAtDesc_filtersOpenAndClosed() {
		final SupportTicket openTicket = supportTicketRepository
			.saveAndFlush(sampleTicket(USER_A, "Abierto", "Activo"));
		final SupportTicket closedTicket = sampleTicket(USER_B, "Cerrado", "Resuelto");
		closedTicket.setStatus(SupportTicketStatus.CLOSED);
		closedTicket.setClosedAt(java.time.Instant.parse("2026-01-03T12:00:00Z"));
		supportTicketRepository.saveAndFlush(closedTicket);

		assertThat(supportTicketRepository.findByStatusOrderByCreatedAtDesc(SupportTicketStatus.OPEN))
			.extracting(SupportTicket::getId)
			.contains(openTicket.getId())
			.doesNotContain(closedTicket.getId());
		assertThat(supportTicketRepository.findByStatusOrderByCreatedAtDesc(SupportTicketStatus.CLOSED))
			.extracting(SupportTicket::getId)
			.contains(closedTicket.getId())
			.doesNotContain(openTicket.getId());
	}

	@Test
	void findAllByOrderByCreatedAtDesc_listsAllUsers() {
		supportTicketRepository.saveAndFlush(sampleTicket(USER_A, "A", "a"));
		supportTicketRepository.saveAndFlush(sampleTicket(USER_B, "B", "b"));

		final List<SupportTicket> all = supportTicketRepository.findAllByOrderByCreatedAtDesc();

		assertThat(all).extracting(SupportTicket::getUserId).contains(USER_A, USER_B);
	}

	@Test
	void updateSetsUpdatedAtAndPersistsAdminNotes() {
		final SupportTicket ticket = supportTicketRepository
			.saveAndFlush(sampleTicket(USER_A, "Actualizar", "Inicial"));
		final java.time.Instant createdAt = ticket.getCreatedAt();
		final java.time.Instant updatedAtBefore = ticket.getUpdatedAt();

		ticket.setAdminNotes("Revisado por plataforma");
		ticket.setStatus(SupportTicketStatus.CLOSED);
		ticket.setClosedAt(java.time.Instant.parse("2026-01-04T15:00:00Z"));
		supportTicketRepository.saveAndFlush(ticket);
		entityManager.clear();

		final SupportTicket loaded = supportTicketRepository.findById(ticket.getId()).orElseThrow();
		assertThat(loaded.getAdminNotes()).isEqualTo("Revisado por plataforma");
		assertThat(loaded.getStatus()).isEqualTo(SupportTicketStatus.CLOSED);
		assertThat(loaded.getClosedAt()).isNotNull();
		// H2 TIMESTAMP may truncate Instant nanos; compare at millisecond precision.
		assertThat(loaded.getCreatedAt().toEpochMilli()).isEqualTo(createdAt.toEpochMilli());
		assertThat(loaded.getUpdatedAt())
			.isAfterOrEqualTo(updatedAtBefore.truncatedTo(java.time.temporal.ChronoUnit.MILLIS));
	}

	private static SupportTicket sampleTicket(final String userId, final String title, final String description) {
		final SupportTicket ticket = new SupportTicket();
		ticket.setUserId(userId);
		ticket.setTitle(title);
		ticket.setDescription(description);
		return ticket;
	}

}
