package com.nutriconsultas.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.nutriconsultas.calendar.CalendarEvent;
import com.nutriconsultas.calendar.CalendarEventRepository;
import com.nutriconsultas.calendar.EventStatus;

@ExtendWith(MockitoExtension.class)
class BookingAvailabilitySlotServiceTest {

	private static final String USER_ID = "auth0|nutritionist";

	@InjectMocks
	private BookingAvailabilitySlotServiceImpl service;

	@Mock
	private NutritionistAvailabilityService availabilityService;

	@Mock
	private NutritionistAvailabilityBlockRepository blockRepository;

	@Mock
	private CalendarEventRepository calendarEventRepository;

	private AvailabilityScheduleDto schedule;

	@BeforeEach
	void setup() {
		schedule = new AvailabilityScheduleDto();
		schedule.setSlotDurationMinutes(60);
		schedule.setTimezone(BookingAvailabilityConstants.DEFAULT_TIMEZONE_ID);
		schedule.setIntervals(List.of(new WorkingHoursIntervalDto(1, LocalTime.of(9, 0), LocalTime.of(12, 0))));
	}

	@Test
	void subtractsAvailabilityBlocksFromSlots() {
		final LocalDate monday = LocalDate.of(2026, 6, 22);
		when(availabilityService.getSchedule(USER_ID)).thenReturn(schedule);
		when(blockRepository.findOverlappingRange(eq(USER_ID), eq(monday.atStartOfDay()),
				eq(monday.plusDays(1).atStartOfDay())))
			.thenReturn(List.of(block(LocalDateTime.of(2026, 6, 22, 9, 0), LocalDateTime.of(2026, 6, 22, 10, 0))));
		when(calendarEventRepository.findByUserIdAndDateRange(eq(USER_ID), any(Date.class), any(Date.class)))
			.thenReturn(List.of());

		final List<LocalTime> slots = service.getAvailableSlotStarts(USER_ID, monday);

		assertThat(slots).containsExactly(LocalTime.of(10, 0), LocalTime.of(11, 0));
	}

	@Test
	void subtractsScheduledAppointmentsFromSlots() {
		final LocalDate monday = LocalDate.of(2026, 6, 22);
		when(availabilityService.getSchedule(USER_ID)).thenReturn(schedule);
		when(blockRepository.findOverlappingRange(eq(USER_ID), any(), any())).thenReturn(List.of());

		final CalendarEvent event = new CalendarEvent();
		event.setStatus(EventStatus.SCHEDULED);
		event.setEventDateTime(Date.from(LocalDateTime.of(2026, 6, 22, 10, 0)
			.atZone(ZoneId.of(BookingAvailabilityConstants.DEFAULT_TIMEZONE_ID))
			.toInstant()));
		event.setDurationMinutes(60);
		when(calendarEventRepository.findByUserIdAndDateRange(eq(USER_ID), any(Date.class), any(Date.class)))
			.thenReturn(List.of(event));

		final List<LocalTime> slots = service.getAvailableSlotStarts(USER_ID, monday);

		assertThat(slots).containsExactly(LocalTime.of(9, 0), LocalTime.of(11, 0));
	}

	@Test
	void loadsBlocksOnlyForRequestedUser() {
		final LocalDate monday = LocalDate.of(2026, 6, 22);
		when(availabilityService.getSchedule(USER_ID)).thenReturn(schedule);
		when(blockRepository.findOverlappingRange(eq(USER_ID), any(), any())).thenReturn(List.of());
		when(calendarEventRepository.findByUserIdAndDateRange(eq(USER_ID), any(Date.class), any(Date.class)))
			.thenReturn(List.of());

		service.getAvailableSlotStarts(USER_ID, monday);

		verify(blockRepository).findOverlappingRange(eq(USER_ID), any(), any());
	}

	private static NutritionistAvailabilityBlock block(final LocalDateTime start, final LocalDateTime end) {
		final NutritionistAvailabilityBlock block = new NutritionistAvailabilityBlock();
		block.setId(1L);
		block.setUserId(USER_ID);
		block.setTitle("Ausencia");
		block.setAllDay(false);
		block.setStartDateTime(start);
		block.setEndDateTime(end);
		return block;
	}

}
