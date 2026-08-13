package com.nutriconsultas.mobile.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

/**
 * Partial update for an appointment question reminder. Omitted fields are left unchanged;
 * send {@code body} and/or {@code answered}.
 */
@Schema(description = "Patch appointment question reminder")
public record PatchAppointmentQuestionRequest(
		@Size(max = 2000) @Schema(maxLength = 2000, description = "Updated question text") String body,
		@Schema(description = "Mark as asked/answered (true) or reopen (false)") Boolean answered) {
}
