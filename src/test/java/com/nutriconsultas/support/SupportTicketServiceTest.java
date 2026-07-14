package com.nutriconsultas.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.nutriconsultas.subscription.PlanTier;
import com.nutriconsultas.subscription.SubscriptionEntitlementService;
import com.nutriconsultas.subscription.clinic.ClinicMemberLabelResolver;

@ExtendWith(MockitoExtension.class)
class SupportTicketServiceTest {

	private static final String USER_A = "auth0|support-user-a";

	private static final String USER_B = "auth0|support-user-b";

	@InjectMocks
	private SupportTicketServiceImpl supportTicketService;

	@Mock
	private SupportTicketRepository supportTicketRepository;

	@Mock
	private ClinicMemberLabelResolver clinicMemberLabelResolver;

	@Mock
	private SubscriptionEntitlementService subscriptionEntitlementService;

	@Test
	void create_persistsOpenTicketForUser() {
		when(supportTicketRepository.save(any(SupportTicket.class))).thenAnswer(invocation -> {
			final SupportTicket ticket = invocation.getArgument(0);
			ticket.setId(42L);
			return ticket;
		});

		final SupportTicket saved = supportTicketService.create(USER_A, "  No abre el PDF  ", " Falla en Safari ");

		final ArgumentCaptor<SupportTicket> captor = ArgumentCaptor.forClass(SupportTicket.class);
		verify(supportTicketRepository).save(captor.capture());
		assertThat(saved.getId()).isEqualTo(42L);
		assertThat(captor.getValue().getUserId()).isEqualTo(USER_A);
		assertThat(captor.getValue().getTitle()).isEqualTo("No abre el PDF");
		assertThat(captor.getValue().getDescription()).isEqualTo("Falla en Safari");
		assertThat(captor.getValue().getStatus()).isEqualTo(SupportTicketStatus.OPEN);
	}

	@Test
	void create_whenTitleBlank_throwsBadRequest() {
		assertThatThrownBy(() -> supportTicketService.create(USER_A, "  ", "Descripción"))
			.isInstanceOf(ResponseStatusException.class)
			.extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
			.isEqualTo(HttpStatus.BAD_REQUEST);
		verify(supportTicketRepository, never()).save(any());
	}

	@Test
	void findOwnTickets_delegatesToRepository() {
		final SupportTicket ticket = sampleTicket(1L, USER_A, SupportTicketStatus.OPEN);
		when(supportTicketRepository.findByUserIdOrderByCreatedAtDesc(USER_A)).thenReturn(List.of(ticket));

		assertThat(supportTicketService.findOwnTickets(USER_A)).containsExactly(ticket);
	}

	@Test
	void findOwnTicket_whenOwned_returnsTicket() {
		final SupportTicket ticket = sampleTicket(7L, USER_A, SupportTicketStatus.OPEN);
		when(supportTicketRepository.findByIdAndUserId(7L, USER_A)).thenReturn(Optional.of(ticket));

		assertThat(supportTicketService.findOwnTicket(USER_A, 7L)).isSameAs(ticket);
	}

	@Test
	void findOwnTicket_whenOtherUser_throwsNotFound() {
		when(supportTicketRepository.findByIdAndUserId(7L, USER_B)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> supportTicketService.findOwnTicket(USER_B, 7L))
			.isInstanceOf(ResponseStatusException.class)
			.extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
			.isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	void findByStatusForAdmin_enrichesUserAndSubscription() {
		final SupportTicket ticket = sampleTicket(3L, USER_A, SupportTicketStatus.OPEN);
		when(supportTicketRepository.findByStatusOrderByCreatedAtDesc(SupportTicketStatus.OPEN))
			.thenReturn(List.of(ticket));
		when(clinicMemberLabelResolver.resolveLabel(USER_A)).thenReturn("Dra. Ana");
		when(subscriptionEntitlementService.getEffectivePlanTier(USER_A)).thenReturn(Optional.of(PlanTier.PLUS));

		final List<SupportTicketAdminView> views = supportTicketService.findByStatusForAdmin(SupportTicketStatus.OPEN);

		assertThat(views).hasSize(1);
		assertThat(views.getFirst().title()).isEqualTo("Asunto");
		assertThat(views.getFirst().userDisplayLabel()).isEqualTo("Dra. Ana");
		assertThat(views.getFirst().planTier()).isEqualTo(PlanTier.PLUS);
		assertThat(views.getFirst().subscriptionLabel()).isEqualTo("Plus");
	}

	@Test
	void findAllForAdmin_usesSinSuscripcionWhenNoPlan() {
		final SupportTicket ticket = sampleTicket(4L, USER_B, SupportTicketStatus.CLOSED);
		when(supportTicketRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(ticket));
		when(clinicMemberLabelResolver.resolveLabel(USER_B)).thenReturn("Correo no disponible");
		when(subscriptionEntitlementService.getEffectivePlanTier(USER_B)).thenReturn(Optional.empty());

		final SupportTicketAdminView view = supportTicketService.findAllForAdmin().getFirst();

		assertThat(view.subscriptionLabel()).isEqualTo("Sin suscripción");
		assertThat(view.planTier()).isNull();
	}

	@Test
	void updateAdminNotes_persistsNotes() {
		final SupportTicket ticket = sampleTicket(9L, USER_A, SupportTicketStatus.OPEN);
		when(supportTicketRepository.findById(9L)).thenReturn(Optional.of(ticket));
		when(supportTicketRepository.save(any(SupportTicket.class)))
			.thenAnswer(invocation -> invocation.getArgument(0));

		final SupportTicket saved = supportTicketService.updateAdminNotes(9L, "  Revisado  ");

		assertThat(saved.getAdminNotes()).isEqualTo("Revisado");
		verify(supportTicketRepository).save(ticket);
	}

	@Test
	void close_setsClosedStatusAndTimestamp() {
		final SupportTicket ticket = sampleTicket(11L, USER_A, SupportTicketStatus.OPEN);
		when(supportTicketRepository.findById(11L)).thenReturn(Optional.of(ticket));
		when(supportTicketRepository.save(any(SupportTicket.class)))
			.thenAnswer(invocation -> invocation.getArgument(0));

		final SupportTicket saved = supportTicketService.close(11L);

		assertThat(saved.getStatus()).isEqualTo(SupportTicketStatus.CLOSED);
		assertThat(saved.getClosedAt()).isNotNull();
	}

	@Test
	void close_whenAlreadyClosed_isIdempotent() {
		final SupportTicket ticket = sampleTicket(12L, USER_A, SupportTicketStatus.CLOSED);
		ticket.setClosedAt(java.time.Instant.parse("2026-01-01T00:00:00Z"));
		when(supportTicketRepository.findById(12L)).thenReturn(Optional.of(ticket));

		final SupportTicket saved = supportTicketService.close(12L);

		assertThat(saved.getStatus()).isEqualTo(SupportTicketStatus.CLOSED);
		verify(supportTicketRepository, never()).save(any());
	}

	@Test
	void reopen_clearsClosedAt() {
		final SupportTicket ticket = sampleTicket(13L, USER_A, SupportTicketStatus.CLOSED);
		ticket.setClosedAt(java.time.Instant.parse("2026-01-01T00:00:00Z"));
		when(supportTicketRepository.findById(13L)).thenReturn(Optional.of(ticket));
		when(supportTicketRepository.save(any(SupportTicket.class)))
			.thenAnswer(invocation -> invocation.getArgument(0));

		final SupportTicket saved = supportTicketService.reopen(13L);

		assertThat(saved.getStatus()).isEqualTo(SupportTicketStatus.OPEN);
		assertThat(saved.getClosedAt()).isNull();
	}

	@Test
	void findByIdForAdmin_whenMissing_throwsNotFound() {
		when(supportTicketRepository.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> supportTicketService.findByIdForAdmin(99L)).isInstanceOf(ResponseStatusException.class)
			.extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
			.isEqualTo(HttpStatus.NOT_FOUND);
	}

	private static SupportTicket sampleTicket(final Long id, final String userId, final SupportTicketStatus status) {
		final SupportTicket ticket = new SupportTicket();
		ticket.setId(id);
		ticket.setUserId(userId);
		ticket.setTitle("Asunto");
		ticket.setDescription("Detalle");
		ticket.setStatus(status);
		return ticket;
	}

}
