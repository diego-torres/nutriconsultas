package com.nutriconsultas.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Test;

class AvailabilityBlockValidatorTest {

	@Test
	void validatesTimeRangeBlock() {
		final AvailabilityBlockDto block = new AvailabilityBlockDto();
		block.setTitle("Conferencia");
		block.setAllDay(false);
		block.setStartDateTime(LocalDateTime.of(2026, 6, 21, 9, 0));
		block.setEndDateTime(LocalDateTime.of(2026, 6, 21, 12, 0));

		AvailabilityBlockValidator.validate(block);

		assertThat(block.getStartDateTime()).isEqualTo(LocalDateTime.of(2026, 6, 21, 9, 0));
	}

	@Test
	void normalizesAllDayBlock() {
		final AvailabilityBlockDto block = new AvailabilityBlockDto();
		block.setTitle("Vacaciones");
		block.setAllDay(true);
		block.setStartDateTime(LocalDateTime.of(2026, 6, 21, 14, 30));
		block.setEndDateTime(LocalDateTime.of(2026, 6, 21, 18, 0));

		AvailabilityBlockValidator.validate(block);

		assertThat(block.getStartDateTime()).isEqualTo(LocalDate.of(2026, 6, 21).atStartOfDay());
		assertThat(block.getEndDateTime()).isEqualTo(LocalDate.of(2026, 6, 22).atStartOfDay());
	}

	@Test
	void rejectsEndBeforeStart() {
		final AvailabilityBlockDto block = new AvailabilityBlockDto();
		block.setTitle("Error");
		block.setStartDateTime(LocalDateTime.of(2026, 6, 21, 12, 0));
		block.setEndDateTime(LocalDateTime.of(2026, 6, 21, 10, 0));

		assertThatThrownBy(() -> AvailabilityBlockValidator.validate(block))
			.isInstanceOf(IllegalArgumentException.class);
	}

}
