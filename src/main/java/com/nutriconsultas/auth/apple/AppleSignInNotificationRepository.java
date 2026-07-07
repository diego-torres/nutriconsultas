package com.nutriconsultas.auth.apple;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AppleSignInNotificationRepository extends JpaRepository<AppleSignInNotification, Long> {

	Optional<AppleSignInNotification> findByAppleEventId(String appleEventId);

	List<AppleSignInNotification> findByEventTypeInOrderByReceivedAtDesc(Collection<AppleSignInEventType> eventTypes);

}
