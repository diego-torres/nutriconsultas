package com.nutriconsultas.auth.apple;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AppleSignInNotificationAdminService {

	private final AppleSignInNotificationRepository notificationRepository;

	public AppleSignInNotificationAdminService(final AppleSignInNotificationRepository notificationRepository) {
		this.notificationRepository = notificationRepository;
	}

	@Transactional(readOnly = true)
	public List<AppleSignInNotification> findDestructiveEventsNewestFirst() {
		return notificationRepository.findByEventTypeInOrderByReceivedAtDesc(
				List.of(AppleSignInEventType.CONSENT_REVOKED, AppleSignInEventType.ACCOUNT_DELETE));
	}

}
