package com.nutriconsultas.device;

/**
 * Sends best-effort push notifications to a patient's registered devices (#575).
 * Implementations must never throw into the caller for transport failures.
 */
public interface PatientPushSender {

	/**
	 * Fan-out {@code event} to all registered devices for {@code pacienteId}. No-op when
	 * push is disabled, misconfigured, or the patient has no devices.
	 * @param pacienteId patient primary key
	 * @param event push event without PHI
	 */
	void send(Long pacienteId, PushEvent event);

}
