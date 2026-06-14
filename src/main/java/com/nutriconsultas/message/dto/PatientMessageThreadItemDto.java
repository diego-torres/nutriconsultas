package com.nutriconsultas.message.dto;

import java.time.Instant;

import com.nutriconsultas.message.MessageSenderRole;
import com.nutriconsultas.message.PatientMessage;

public record PatientMessageThreadItemDto(Long id, Instant sentAt, MessageSenderRole senderRole, String body) {

	public static PatientMessageThreadItemDto fromEntity(final PatientMessage message) {
		return new PatientMessageThreadItemDto(message.getId(), message.getSentAt(), message.getSenderRole(),
				message.getBody());
	}

}
