package com.nutriconsultas.booking;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AvailabilityScheduleDto {

	@Min(BookingAvailabilityConstants.MIN_SLOT_DURATION_MINUTES)
	@Max(BookingAvailabilityConstants.MAX_SLOT_DURATION_MINUTES)
	private int slotDurationMinutes = BookingAvailabilityConstants.DEFAULT_SLOT_DURATION_MINUTES;

	@NotBlank
	private String timezone = BookingAvailabilityConstants.DEFAULT_TIMEZONE_ID;

	@NotNull
	@Valid
	private List<WorkingHoursIntervalDto> intervals = new ArrayList<>();

}
