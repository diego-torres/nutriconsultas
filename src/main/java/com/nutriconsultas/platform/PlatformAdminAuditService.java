package com.nutriconsultas.platform;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.nutriconsultas.subscription.SubscriptionAuditEvent;
import com.nutriconsultas.subscription.SubscriptionAuditEventRepository;
import com.nutriconsultas.subscription.SubscriptionAuditEventType;

import lombok.extern.slf4j.Slf4j;

/**
 * Persists platform admin actions for audit. Stores actor user IDs only — never PHI.
 */
@Service
@Slf4j
public class PlatformAdminAuditService {

	private static final int MAX_DETAILS_LENGTH = 500;

	private final SubscriptionAuditEventRepository subscriptionAuditEventRepository;

	public PlatformAdminAuditService(final SubscriptionAuditEventRepository subscriptionAuditEventRepository) {
		this.subscriptionAuditEventRepository = subscriptionAuditEventRepository;
	}

	@Transactional
	public void recordAction(final String actorUserId, final String action) {
		if (!StringUtils.hasText(actorUserId) || !StringUtils.hasText(action)) {
			return;
		}
		final SubscriptionAuditEvent event = new SubscriptionAuditEvent();
		event.setEventType(SubscriptionAuditEventType.PLATFORM_ADMIN_ACTION);
		event.setActorUserId(actorUserId);
		event.setDetails(truncate(action));
		subscriptionAuditEventRepository.save(event);
		if (log.isInfoEnabled()) {
			log.info("Platform admin action: actorUserId={}, action={}", actorUserId, action);
		}
	}

	private static String truncate(final String action) {
		if (action.length() <= MAX_DETAILS_LENGTH) {
			return action;
		}
		return action.substring(0, MAX_DETAILS_LENGTH);
	}

}
