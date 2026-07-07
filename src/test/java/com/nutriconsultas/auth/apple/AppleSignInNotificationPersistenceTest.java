package com.nutriconsultas.auth.apple;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class AppleSignInNotificationPersistenceTest {

	@Autowired
	private AppleSignInNotificationRepository notificationRepository;

	@Autowired
	private EntityManager entityManager;

	@Test
	void notificationPersistsWithUniqueAppleEventId() {
		final AppleSignInNotification notification = sampleNotification("apple-evt-1");
		notificationRepository.saveAndFlush(notification);

		entityManager.clear();

		assertThat(notificationRepository.findByAppleEventId("apple-evt-1")).isPresent();
	}

	private static AppleSignInNotification sampleNotification(final String appleEventId) {
		final AppleSignInNotification notification = new AppleSignInNotification();
		notification.setAppleEventId(appleEventId);
		notification.setEventType(AppleSignInEventType.EMAIL_DISABLED);
		notification.setAppleSubject("001234.abc");
		notification.setProcessingStatus(AppleSignInNotificationProcessingStatus.PROCESSED);
		notification.setReceivedAt(Instant.now());
		notification.setProcessedAt(Instant.now());
		return notification;
	}

}
