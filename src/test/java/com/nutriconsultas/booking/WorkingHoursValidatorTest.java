package com.nutriconsultas.booking;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Test;

class WorkingHoursValidatorTest {

	@Test
	void acceptsNonOverlappingIntervals() {
		final AvailabilityScheduleDto schedule = validSchedule();
		schedule.setIntervals(List.of(new WorkingHoursIntervalDto(1, LocalTime.of(9, 0), LocalTime.of(12, 0)),
				new WorkingHoursIntervalDto(1, LocalTime.of(14, 0), LocalTime.of(17, 0))));

		WorkingHoursValidator.validateSchedule(schedule);
	}

	@Test
	void rejectsOverlappingIntervalsSameDay() {
		final AvailabilityScheduleDto schedule = validSchedule();
		schedule.setIntervals(List.of(new WorkingHoursIntervalDto(1, LocalTime.of(9, 0), LocalTime.of(12, 0)),
				new WorkingHoursIntervalDto(1, LocalTime.of(11, 0), LocalTime.of(13, 0))));

		assertThatThrownBy(() -> WorkingHoursValidator.validateSchedule(schedule))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("traslap");
	}

	@Test
	void rejectsEndBeforeStart() {
		final AvailabilityScheduleDto schedule = validSchedule();
		schedule.setIntervals(List.of(new WorkingHoursIntervalDto(2, LocalTime.of(17, 0), LocalTime.of(9, 0))));

		assertThatThrownBy(() -> WorkingHoursValidator.validateSchedule(schedule))
			.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void rejectsSlotDurationOutsideBounds() {
		final AvailabilityScheduleDto schedule = validSchedule();
		schedule.setSlotDurationMinutes(10);

		assertThatThrownBy(() -> WorkingHoursValidator.validateSchedule(schedule))
			.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void rejectsInvalidTimezone() {
		final AvailabilityScheduleDto schedule = validSchedule();
		schedule.setTimezone("Not/AZone");

		assertThatThrownBy(() -> WorkingHoursValidator.validateSchedule(schedule))
			.isInstanceOf(IllegalArgumentException.class);
	}

	private static AvailabilityScheduleDto validSchedule() {
		final AvailabilityScheduleDto schedule = new AvailabilityScheduleDto();
		schedule.setSlotDurationMinutes(60);
		schedule.setTimezone(BookingAvailabilityConstants.DEFAULT_TIMEZONE_ID);
		schedule.setIntervals(List.of(new WorkingHoursIntervalDto(1, LocalTime.of(9, 0), LocalTime.of(17, 0))));
		return schedule;
	}

}
