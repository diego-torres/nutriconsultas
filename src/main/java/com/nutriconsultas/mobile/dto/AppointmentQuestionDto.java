package com.nutriconsultas.mobile.dto;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nutriconsultas.appointmentquestion.AppointmentQuestion;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AppointmentQuestionDto(Long id, String body, boolean answered, Instant answeredAt, Instant createdAt,
		Instant updatedAt) {

	public static AppointmentQuestionDto fromEntity(final AppointmentQuestion question) {
		return new AppointmentQuestionDto(question.getId(), question.getBody(), question.isAnswered(),
				question.getAnsweredAt(), question.getCreatedAt(), question.getUpdatedAt());
	}

}
