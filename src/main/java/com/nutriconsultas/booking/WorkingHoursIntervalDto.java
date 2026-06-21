package com.nutriconsultas.booking;

import java.time.LocalTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkingHoursIntervalDto {

	@NotNull
	@Min(1)
	@Max(7)
	private Integer dayOfWeek;

	@NotNull
	@JsonFormat(pattern = "HH:mm")
	private LocalTime startTime;

	@NotNull
	@JsonFormat(pattern = "HH:mm")
	private LocalTime endTime;

}
