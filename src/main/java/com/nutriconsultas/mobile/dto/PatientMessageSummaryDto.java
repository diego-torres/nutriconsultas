package com.nutriconsultas.mobile.dto;

import java.time.Instant;

import com.nutriconsultas.message.MessageSenderRole;
import com.nutriconsultas.message.PatientMessage;

public record PatientMessageSummaryDto(Long id, Instant sentAt, MessageSenderRole senderRole, String body,
		boolean read) {

	public static PatientMessageSummaryDto fromEntity(final PatientMessage message) {
		return new PatientMessageSummaryDto(message.getId(), message.getSentAt(), message.getSenderRole(),
				message.getBody(), message.isReadByPatient());
	}

}
