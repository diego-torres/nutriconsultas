package com.nutriconsultas.booking;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Test;

class BookingSlotFilterTest {

	@Test
	void removesSlotsOverlappingBusyInterval() {
		final List<LocalTime> slots = List.of(LocalTime.of(9, 0), LocalTime.of(10, 0), LocalTime.of(11, 0));
		final List<BusyTimeInterval> busy = List
			.of(new BusyTimeInterval(LocalDateTime.of(2026, 6, 21, 9, 30), LocalDateTime.of(2026, 6, 21, 10, 30)));

		final List<LocalTime> available = BookingSlotFilter.removeBusySlots(slots, LocalDate.of(2026, 6, 21), 60, busy);

		assertThat(available).containsExactly(LocalTime.of(11, 0));
	}

	@Test
	void keepsAllSlotsWhenNoBusyIntervals() {
		final List<LocalTime> slots = List.of(LocalTime.of(9, 0), LocalTime.of(10, 0));

		final List<LocalTime> available = BookingSlotFilter.removeBusySlots(slots, LocalDate.of(2026, 6, 21), 60,
				List.of());

		assertThat(available).containsExactlyElementsOf(slots);
	}

	@Test
	void removesSlotFullyInsideBlock() {
		final List<LocalTime> slots = List.of(LocalTime.of(10, 0));
		final List<BusyTimeInterval> busy = List
			.of(new BusyTimeInterval(LocalDateTime.of(2026, 6, 21, 8, 0), LocalDateTime.of(2026, 6, 21, 18, 0)));

		assertThat(BookingSlotFilter.removeBusySlots(slots, LocalDate.of(2026, 6, 21), 60, busy)).isEmpty();
	}

}
