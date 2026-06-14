package com.nutriconsultas.message.dto;

import java.time.Instant;

import com.nutriconsultas.message.PatientMessage;

public record PatientUnreadMessageDto(Long pacienteId, String pacienteName, String preview, Instant sentAt,
		long unreadCount) {

	public static PatientUnreadMessageDto fromLatest(final PatientMessage message, final long unreadCount) {
		return new PatientUnreadMessageDto(message.getPaciente().getId(), message.getPaciente().getName(),
				message.getBody(), message.getSentAt(), unreadCount);
	}

	public PatientUnreadMessageDto withIncrementedCount() {
		return new PatientUnreadMessageDto(pacienteId, pacienteName, preview, sentAt, unreadCount + 1);
	}

}
