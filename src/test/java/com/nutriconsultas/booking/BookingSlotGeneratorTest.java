package com.nutriconsultas.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Test;

class BookingSlotGeneratorTest {

	@Test
	void generateSlotStartsWithinSingleInterval() {
		final List<WorkingHoursIntervalDto> intervals = List
			.of(new WorkingHoursIntervalDto(1, LocalTime.of(9, 0), LocalTime.of(12, 0)));

		final List<LocalTime> slots = BookingSlotGenerator.generateSlotStarts(intervals, 60);

		assertThat(slots).containsExactly(LocalTime.of(9, 0), LocalTime.of(10, 0), LocalTime.of(11, 0));
	}

	@Test
	void generateSlotStartsRespectsSlotDurationBoundary() {
		final List<WorkingHoursIntervalDto> intervals = List
			.of(new WorkingHoursIntervalDto(2, LocalTime.of(9, 0), LocalTime.of(10, 30)));

		final List<LocalTime> slots = BookingSlotGenerator.generateSlotStarts(intervals, 60);

		assertThat(slots).containsExactly(LocalTime.of(9, 0));
	}

	@Test
	void generateSlotStartsAcrossMultipleIntervalsSameDay() {
		final List<WorkingHoursIntervalDto> intervals = List.of(
				new WorkingHoursIntervalDto(3, LocalTime.of(9, 0), LocalTime.of(11, 0)),
				new WorkingHoursIntervalDto(3, LocalTime.of(14, 0), LocalTime.of(16, 0)));

		final List<LocalTime> slots = BookingSlotGenerator.generateSlotStarts(intervals, 30);

		assertThat(slots).containsExactly(LocalTime.of(9, 0), LocalTime.of(9, 30), LocalTime.of(10, 0),
				LocalTime.of(10, 30), LocalTime.of(14, 0), LocalTime.of(14, 30), LocalTime.of(15, 0),
				LocalTime.of(15, 30));
	}

	@Test
	void generateSlotStartsReturnsEmptyWhenNoIntervals() {
		assertThat(BookingSlotGenerator.generateSlotStarts(List.of(), 30)).isEmpty();
	}

	@Test
	void generateSlotStartsRejectsInvalidDuration() {
		assertThatThrownBy(() -> BookingSlotGenerator.generateSlotStarts(List.of(), 5))
			.isInstanceOf(IllegalArgumentException.class);
	}

}
