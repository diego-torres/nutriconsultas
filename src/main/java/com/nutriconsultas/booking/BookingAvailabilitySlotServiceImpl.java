package com.nutriconsultas.booking;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nutriconsultas.calendar.CalendarEvent;
import com.nutriconsultas.calendar.CalendarEventRepository;
import com.nutriconsultas.calendar.EventStatus;

@Service
public class BookingAvailabilitySlotServiceImpl implements BookingAvailabilitySlotService {

	private final NutritionistAvailabilityService availabilityService;

	private final NutritionistAvailabilityBlockRepository blockRepository;

	private final CalendarEventRepository calendarEventRepository;

	public BookingAvailabilitySlotServiceImpl(final NutritionistAvailabilityService availabilityService,
			final NutritionistAvailabilityBlockRepository blockRepository,
			final CalendarEventRepository calendarEventRepository) {
		this.availabilityService = availabilityService;
		this.blockRepository = blockRepository;
		this.calendarEventRepository = calendarEventRepository;
	}

	@Override
	@Transactional(readOnly = true)
	public List<LocalTime> getAvailableSlotStarts(@NonNull final String userId, @NonNull final LocalDate date) {
		final AvailabilityScheduleDto schedule = availabilityService.getSchedule(userId);
		final ZoneId zoneId = ZoneId.of(schedule.getTimezone());
		final int dayOfWeek = date.getDayOfWeek().getValue();
		final List<WorkingHoursIntervalDto> dayIntervals = schedule.getIntervals()
			.stream()
			.filter(interval -> dayOfWeek == interval.getDayOfWeek())
			.toList();
		final List<LocalTime> rawSlots = BookingSlotGenerator.generateSlotStarts(dayIntervals,
				schedule.getSlotDurationMinutes());
		final List<BusyTimeInterval> busyIntervals = loadBusyIntervals(userId, date, zoneId);
		return BookingSlotFilter.removeBusySlots(rawSlots, date, schedule.getSlotDurationMinutes(), busyIntervals);
	}

	@Override
	@Transactional(readOnly = true)
	public LocalDateTime findNextAvailableStart(@NonNull final String userId, @NonNull final LocalDate date,
			@NonNull final LocalDateTime notBefore) {
		final List<LocalTime> slots = getAvailableSlotStarts(userId, date);
		for (final LocalTime slotStart : slots) {
			final LocalDateTime candidate = date.atTime(slotStart);
			if (!candidate.isBefore(notBefore)) {
				return candidate;
			}
		}
		return null;
	}

	private List<BusyTimeInterval> loadBusyIntervals(final String userId, final LocalDate date, final ZoneId zoneId) {
		final LocalDateTime dayStart = date.atStartOfDay();
		final LocalDateTime dayEnd = date.plusDays(1).atStartOfDay();
		final List<BusyTimeInterval> busy = new ArrayList<>();

		for (final NutritionistAvailabilityBlock block : blockRepository.findOverlappingRange(userId, dayStart,
				dayEnd)) {
			busy.add(new BusyTimeInterval(block.getStartDateTime(), block.getEndDateTime()));
		}

		final Date rangeStart = Date.from(dayStart.atZone(zoneId).toInstant());
		final Date rangeEnd = Date.from(dayEnd.atZone(zoneId).toInstant());
		final List<CalendarEvent> events = calendarEventRepository.findByUserIdAndDateRange(userId, rangeStart,
				rangeEnd);
		for (final CalendarEvent event : events) {
			if (event.getStatus() != EventStatus.SCHEDULED || event.getEventDateTime() == null) {
				continue;
			}
			final LocalDateTime eventStart = event.getEventDateTime().toInstant().atZone(zoneId).toLocalDateTime();
			final int durationMinutes = event.getDurationMinutes() != null ? event.getDurationMinutes() : 60;
			busy.add(new BusyTimeInterval(eventStart, eventStart.plusMinutes(durationMinutes)));
		}
		return busy;
	}

}
