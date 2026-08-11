package com.nutriconsultas.device;

/**
 * Best-effort push event for patient devices. Contains no message body / PHI (#575).
 *
 * @param type event type for client routing
 * @param messageId optional related message id (may be null)
 */
public record PushEvent(PushEventType type, Long messageId) {

	public static PushEvent newMessage(final Long messageId) {
		return new PushEvent(PushEventType.NEW_MESSAGE, messageId);
	}

}
