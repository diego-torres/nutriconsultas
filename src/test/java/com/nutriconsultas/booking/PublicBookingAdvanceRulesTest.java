package com.nutriconsultas.booking;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;

class PublicBookingAdvanceRulesTest {

	private static final ZoneId ZONE = ZoneId.of("America/Mexico_City");

	@Test
	void earliestBookableDateIsTodayPlusTwoDays() {
		final LocalDate expected = LocalDate.now(ZONE).plusDays(2);
		assertThat(PublicBookingAdvanceRules.earliestBookableDate(ZONE)).isEqualTo(expected);
	}

	@Test
	void blocksTodayAndTomorrow() {
		final LocalDate today = LocalDate.now(ZONE);
		assertThat(PublicBookingAdvanceRules.isDateBookable(today, ZONE)).isFalse();
		assertThat(PublicBookingAdvanceRules.isDateBookable(today.plusDays(1), ZONE)).isFalse();
	}

	@Test
	void allowsDayAfterAdvanceWindow() {
		final LocalDate eligible = LocalDate.now(ZONE).plusDays(2);
		assertThat(PublicBookingAdvanceRules.isDateBookable(eligible, ZONE)).isTrue();
	}

}
