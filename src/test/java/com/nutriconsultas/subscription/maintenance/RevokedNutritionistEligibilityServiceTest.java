package com.nutriconsultas.subscription.maintenance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.nutriconsultas.subscription.Subscription;
import com.nutriconsultas.subscription.SubscriptionAuditEvent;
import com.nutriconsultas.subscription.SubscriptionAuditEventRepository;
import com.nutriconsultas.subscription.SubscriptionAuditEventType;
import com.nutriconsultas.subscription.SubscriptionStatus;

@ExtendWith(MockitoExtension.class)
class RevokedNutritionistEligibilityServiceTest {

	@Mock
	private SubscriptionAuditEventRepository auditEventRepository;

	private RevokedNutritionistEligibilityService eligibilityService;

	private MaintenanceRetentionProperties properties;

	@BeforeEach
	void setUp() {
		properties = new MaintenanceRetentionProperties();
		properties.setRetentionDays(90);
		eligibilityService = new RevokedNutritionistEligibilityService(auditEventRepository, properties);
	}

	@Test
	void findEligible_returnsDistinctUsersFromAccessRevokeEvents() {
		final Subscription subscription = new Subscription();
		subscription.setId(10L);
		subscription.setStatus(SubscriptionStatus.CANCELLED);
		final SubscriptionAuditEvent event = auditEvent(subscription, "auth0|revoked-user");
		when(auditEventRepository.findEligibleAccessRevokeEvents(any())).thenReturn(List.of(event));

		final List<RevokedNutritionistEligibilityService.EligibleRevokedNutritionist> eligible = eligibilityService
			.findEligible();

		assertThat(eligible).hasSize(1);
		assertThat(eligible.get(0).userId()).isEqualTo("auth0|revoked-user");
		assertThat(eligible.get(0).subscriptionId()).isEqualTo(10L);
	}

	@Test
	void findEligible_usesRetentionDaysForCutoff() {
		when(auditEventRepository.findEligibleAccessRevokeEvents(any())).thenReturn(List.of());
		eligibilityService.findEligible();
		final ArgumentCaptor<Instant> cutoffCaptor = ArgumentCaptor.forClass(Instant.class);
		verify(auditEventRepository).findEligibleAccessRevokeEvents(cutoffCaptor.capture());
		final Instant cutoff = cutoffCaptor.getValue();
		assertThat(cutoff).isBefore(Instant.now().minus(89, ChronoUnit.DAYS));
		assertThat(cutoff).isAfter(Instant.now().minus(91, ChronoUnit.DAYS));
	}

	private static SubscriptionAuditEvent auditEvent(final Subscription subscription, final String targetUserId) {
		final SubscriptionAuditEvent event = new SubscriptionAuditEvent();
		event.setSubscription(subscription);
		event.setEventType(SubscriptionAuditEventType.PLATFORM_ADMIN_ACTION);
		event.setNewStatus(SubscriptionStatus.CANCELLED);
		event.setDetails("action=access.revoke,targetUserId=" + targetUserId + ",invitationId=1");
		event.setCreatedAt(Instant.now().minus(100, ChronoUnit.DAYS));
		return event;
	}

}
