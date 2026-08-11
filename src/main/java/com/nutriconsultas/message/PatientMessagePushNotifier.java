package com.nutriconsultas.message;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.nutriconsultas.device.PatientPushSender;
import com.nutriconsultas.device.PushEvent;
import com.nutriconsultas.util.LogRedaction;

import lombok.extern.slf4j.Slf4j;

/**
 * Async bridge from nutritionist message save to patient push (#576).
 */
@Service
@Slf4j
public class PatientMessagePushNotifier {

	private final PatientPushSender patientPushSender;

	public PatientMessagePushNotifier(final PatientPushSender patientPushSender) {
		this.patientPushSender = patientPushSender;
	}

	@Async("patientPushExecutor")
	public void notifyNewNutritionistMessage(final Long pacienteId, final Long messageId) {
		if (pacienteId == null || messageId == null) {
			return;
		}
		if (log.isDebugEnabled()) {
			log.debug("Notifying patient push for patient {} message {}", LogRedaction.redactPaciente(pacienteId),
					LogRedaction.redactPatientMessage(messageId));
		}
		patientPushSender.send(pacienteId, PushEvent.newMessage(messageId));
	}

}
