package com.nutriconsultas.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.nutriconsultas.subscription.Entitlement;
import com.nutriconsultas.subscription.SubscriptionEntitlementService;
import com.nutriconsultas.subscription.SubscriptionErrorResponses;
import com.nutriconsultas.subscription.SubscriptionLimitExceededException;

@ExtendWith(MockitoExtension.class)
class AiEntitlementGuardTest {

	private static final String NUTRITIONIST_ID = "auth0|nutritionist-a";

	@InjectMocks
	private AiEntitlementGuard guard;

	@Mock
	private SubscriptionEntitlementService subscriptionEntitlementService;

	@Mock
	private AiAuditLogger auditLogger;

	@Test
	void canUseAiAssistantDelegatesToSubscriptionService() {
		when(subscriptionEntitlementService.hasEntitlement(NUTRITIONIST_ID, Entitlement.AI_ASSISTANT)).thenReturn(true);

		assertThat(guard.canUseAiAssistant(NUTRITIONIST_ID)).isTrue();
	}

	@Test
	void canUseAiAssistantFalseWhenUserIdMissing() {
		assertThat(guard.canUseAiAssistant(null)).isFalse();
		assertThat(guard.canUseAiAssistant("")).isFalse();
	}

	@Test
	void assertCanUseAiAssistantPropagatesSubscriptionDenial() {
		when(subscriptionEntitlementService.hasEntitlement(NUTRITIONIST_ID, Entitlement.AI_ASSISTANT))
			.thenReturn(false);
		final SubscriptionLimitExceededException denied = new SubscriptionLimitExceededException(
				SubscriptionErrorResponses.KEY_AI_ASSISTANT_DENIED);
		org.mockito.Mockito.doThrow(denied)
			.when(subscriptionEntitlementService)
			.assertCanUseAiAssistant(NUTRITIONIST_ID);

		assertThatThrownBy(() -> guard.assertCanUseAiAssistant(NUTRITIONIST_ID)).isSameAs(denied);
		verify(auditLogger).logAccessDenied(NUTRITIONIST_ID, "missing_entitlement");
	}

	@Test
	void assertCanUseAiAssistantChecksEmptyUserId() {
		org.mockito.Mockito
			.doThrow(new SubscriptionLimitExceededException(SubscriptionErrorResponses.KEY_AI_ASSISTANT_DENIED))
			.when(subscriptionEntitlementService)
			.assertCanUseAiAssistant("");

		assertThatThrownBy(() -> guard.assertCanUseAiAssistant(null))
			.isInstanceOf(SubscriptionLimitExceededException.class);
		verify(subscriptionEntitlementService).assertCanUseAiAssistant("");
	}

}
