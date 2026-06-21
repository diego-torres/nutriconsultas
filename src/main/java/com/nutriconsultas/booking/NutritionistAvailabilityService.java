package com.nutriconsultas.booking;

import org.springframework.lang.NonNull;

public interface NutritionistAvailabilityService {

	AvailabilityScheduleDto getSchedule(@NonNull String userId);

	AvailabilityScheduleDto saveSchedule(@NonNull String userId, @NonNull AvailabilityScheduleDto schedule);

}
