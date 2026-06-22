package com.nutriconsultas.subscription.maintenance;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.nutriconsultas.subscription.SubscriptionAuditEvent;
import com.nutriconsultas.subscription.SubscriptionAuditEventRepository;

@Service
public class RevokedNutritionistEligibilityService {

	private static final Pattern TARGET_USER_ID = Pattern.compile("targetUserId=([^,]+)");

	private final SubscriptionAuditEventRepository auditEventRepository;

	private final MaintenanceRetentionProperties properties;

	public RevokedNutritionistEligibilityService(final SubscriptionAuditEventRepository auditEventRepository,
			final MaintenanceRetentionProperties properties) {
		this.auditEventRepository = auditEventRepository;
		this.properties = properties;
	}

	@Transactional(readOnly = true)
	public List<EligibleRevokedNutritionist> findEligible() {
		final Instant cutoff = Instant.now().minusSeconds((long) properties.getRetentionDays() * 24 * 60 * 60);
		final List<SubscriptionAuditEvent> events = auditEventRepository.findEligibleAccessRevokeEvents(cutoff);
		final Map<String, EligibleRevokedNutritionist> byUserId = new LinkedHashMap<>();
		for (final SubscriptionAuditEvent event : events) {
			final String targetUserId = extractTargetUserId(event);
			if (!StringUtils.hasText(targetUserId) || byUserId.containsKey(targetUserId)) {
				continue;
			}
			byUserId.put(targetUserId, new EligibleRevokedNutritionist(targetUserId, event.getSubscription().getId(),
					event.getCreatedAt()));
		}
		return new ArrayList<>(byUserId.values());
	}

	private static String extractTargetUserId(final SubscriptionAuditEvent event) {
		if (!StringUtils.hasText(event.getDetails())) {
			return null;
		}
		final Matcher matcher = TARGET_USER_ID.matcher(event.getDetails());
		if (matcher.find()) {
			return matcher.group(1).trim();
		}
		return null;
	}

	public record EligibleRevokedNutritionist(String userId, Long subscriptionId, Instant revokedAt) {
	}

}
