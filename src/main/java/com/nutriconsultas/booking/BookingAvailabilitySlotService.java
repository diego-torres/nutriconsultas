package com.nutriconsultas.booking;

import org.springframework.lang.NonNull;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public interface BookingAvailabilitySlotService {

	List<LocalTime> getAvailableSlotStarts(@NonNull String userId, @NonNull LocalDate date);

	LocalDateTime findNextAvailableStart(@NonNull String userId, @NonNull LocalDate date,
			@NonNull LocalDateTime notBefore);

}
