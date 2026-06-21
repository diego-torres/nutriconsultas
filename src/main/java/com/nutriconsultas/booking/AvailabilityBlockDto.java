package com.nutriconsultas.booking;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AvailabilityBlockDto {

	private Long id;

	@NotBlank
	@Size(max = 200)
	private String title;

	private boolean allDay;

	@NotNull
	private LocalDateTime startDateTime;

	@NotNull
	private LocalDateTime endDateTime;

}
