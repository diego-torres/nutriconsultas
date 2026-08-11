package com.nutriconsultas.device;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nutriconsultas.util.LogRedaction;

import lombok.extern.slf4j.Slf4j;

/**
 * Fan-out push notifications to registered patient devices (#575). Best-effort: transport
 * failures are logged and never thrown to callers.
 */
@Service
@Slf4j
public class PatientPushSenderImpl implements PatientPushSender {

	private final PatientPushProperties properties;

	private final PatientDeviceRepository patientDeviceRepository;

	private final ApnsPushClient apnsPushClient;

	private final FcmHttpV1Client fcmHttpV1Client;

	public PatientPushSenderImpl(final PatientPushProperties properties,
			final PatientDeviceRepository patientDeviceRepository, final ApnsPushClient apnsPushClient,
			final FcmHttpV1Client fcmHttpV1Client) {
		this.properties = properties;
		this.patientDeviceRepository = patientDeviceRepository;
		this.apnsPushClient = apnsPushClient;
		this.fcmHttpV1Client = fcmHttpV1Client;
	}

	@Override
	@Transactional
	public void send(final Long pacienteId, final PushEvent event) {
		if (pacienteId == null || event == null || event.type() == null) {
			return;
		}
		if (!properties.isEnabled()) {
			return;
		}
		if (!properties.isOperational()) {
			if (log.isDebugEnabled()) {
				log.debug("Skipping push for patient {}: push enabled but no platform credentials",
						LogRedaction.redactPaciente(pacienteId));
			}
			return;
		}
		try {
			final List<PatientDevice> devices = patientDeviceRepository.findByPacienteId(pacienteId);
			if (devices.isEmpty()) {
				if (log.isDebugEnabled()) {
					log.debug("No registered devices for patient {}", LogRedaction.redactPaciente(pacienteId));
				}
				return;
			}
			for (final PatientDevice device : devices) {
				deliverToDevice(device, event);
			}
		}
		catch (final RuntimeException ex) {
			log.warn("Patient push fan-out failed for patient {} eventType={}: {}",
					LogRedaction.redactPaciente(pacienteId), event.type(), ex.getMessage());
		}
	}

	private void deliverToDevice(final PatientDevice device, final PushEvent event) {
		try {
			final PushDeliveryResult result = switch (device.getPlatform()) {
				case IOS -> apnsPushClient.send(device, event);
				case ANDROID -> fcmHttpV1Client.send(device, event);
			};
			if (result == PushDeliveryResult.INVALID_TOKEN) {
				patientDeviceRepository.delete(device);
				if (log.isInfoEnabled()) {
					log.info("Removed invalid push token deviceId={} platform={} token={}", device.getId(),
							device.getPlatform(), LogRedaction.redactDeviceToken(device.getToken()));
				}
			}
			else if (log.isDebugEnabled()) {
				log.debug("Push result={} deviceId={} platform={} eventType={}", result, device.getId(),
						device.getPlatform(), event.type());
			}
		}
		catch (final RuntimeException ex) {
			log.warn("Push delivery failed for deviceId={} platform={}: {}", device.getId(), device.getPlatform(),
					ex.getMessage());
		}
	}

}
