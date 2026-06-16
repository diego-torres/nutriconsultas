package com.nutriconsultas.platform;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.nutriconsultas.subscription.SubscriptionAuditEvent;
import com.nutriconsultas.subscription.SubscriptionAuditEventRepository;
import com.nutriconsultas.subscription.SubscriptionAuditEventType;

@ExtendWith(MockitoExtension.class)
class PlatformAdminAuditServiceTest {

	@InjectMocks
	private PlatformAdminAuditService platformAdminAuditService;

	@Mock
	private SubscriptionAuditEventRepository subscriptionAuditEventRepository;

	@Test
	void recordAction_persistsAuditEventWithActorUserIdOnly() {
		when(subscriptionAuditEventRepository.save(any(SubscriptionAuditEvent.class)))
			.thenAnswer(invocation -> invocation.getArgument(0));

		platformAdminAuditService.recordAction("auth0|admin-one", "contact-inquiries.list");

		final ArgumentCaptor<SubscriptionAuditEvent> captor = ArgumentCaptor.forClass(SubscriptionAuditEvent.class);
		verify(subscriptionAuditEventRepository).save(captor.capture());
		final SubscriptionAuditEvent saved = captor.getValue();
		assertThat(saved.getEventType()).isEqualTo(SubscriptionAuditEventType.PLATFORM_ADMIN_ACTION);
		assertThat(saved.getActorUserId()).isEqualTo("auth0|admin-one");
		assertThat(saved.getDetails()).isEqualTo("contact-inquiries.list");
		assertThat(saved.getSubscription()).isNull();
	}

	@Test
	void recordAction_skipsBlankActorOrAction() {
		platformAdminAuditService.recordAction("", "platform.index");
		platformAdminAuditService.recordAction("auth0|admin-one", "");

		verify(subscriptionAuditEventRepository, org.mockito.Mockito.never()).save(any());
	}

}
