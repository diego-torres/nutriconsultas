package com.nutriconsultas.mobile.dto;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nutriconsultas.message.MessageSenderRole;
import com.nutriconsultas.message.PatientMessage;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PatientMessageSummaryDto(Long id, Instant sentAt, MessageSenderRole senderRole, String body, boolean read,
		String senderDisplayName) {

	public static PatientMessageSummaryDto fromEntity(final PatientMessage message,
			final String nutritionistDisplayName) {
		final String senderDisplayName = message.getSenderRole() == MessageSenderRole.NUTRITIONIST
				? nutritionistDisplayName : null;
		return new PatientMessageSummaryDto(message.getId(), message.getSentAt(), message.getSenderRole(),
				message.getBody(), message.isReadByPatient(), senderDisplayName);
	}

}
