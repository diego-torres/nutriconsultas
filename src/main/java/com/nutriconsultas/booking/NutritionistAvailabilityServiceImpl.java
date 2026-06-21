package com.nutriconsultas.booking;

import java.util.ArrayList;
import java.util.List;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nutriconsultas.util.LogRedaction;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class NutritionistAvailabilityServiceImpl implements NutritionistAvailabilityService {

	private final NutritionistAvailabilitySettingsRepository settingsRepository;

	private final NutritionistWorkingHoursIntervalRepository intervalRepository;

	public NutritionistAvailabilityServiceImpl(final NutritionistAvailabilitySettingsRepository settingsRepository,
			final NutritionistWorkingHoursIntervalRepository intervalRepository) {
		this.settingsRepository = settingsRepository;
		this.intervalRepository = intervalRepository;
	}

	@Override
	@Transactional(readOnly = true)
	public AvailabilityScheduleDto getSchedule(@NonNull final String userId) {
		final NutritionistAvailabilitySettings settings = settingsRepository.findByUserId(userId)
			.orElse(defaultSettings(userId));
		final List<NutritionistWorkingHoursInterval> stored = intervalRepository
			.findByUserIdOrderByDayOfWeekAscStartTimeAsc(userId);
		return toDto(settings, stored);
	}

	@Override
	@Transactional
	public AvailabilityScheduleDto saveSchedule(@NonNull final String userId,
			@NonNull final AvailabilityScheduleDto schedule) {
		WorkingHoursValidator.validateSchedule(schedule);
		final NutritionistAvailabilitySettings settings = settingsRepository.findByUserId(userId)
			.orElseGet(() -> defaultSettings(userId));
		settings.setSlotDurationMinutes(schedule.getSlotDurationMinutes());
		settings.setTimezone(schedule.getTimezone().trim());
		settingsRepository.save(settings);

		intervalRepository.deleteByUserId(userId);
		final List<NutritionistWorkingHoursInterval> savedIntervals = new ArrayList<>();
		for (final WorkingHoursIntervalDto intervalDto : schedule.getIntervals()) {
			final NutritionistWorkingHoursInterval row = new NutritionistWorkingHoursInterval();
			row.setUserId(userId);
			row.setDayOfWeek(intervalDto.getDayOfWeek());
			row.setStartTime(intervalDto.getStartTime());
			row.setEndTime(intervalDto.getEndTime());
			savedIntervals.add(intervalRepository.save(row));
		}
		if (log.isInfoEnabled()) {
			log.info("Saved availability for user {} with {} intervals", LogRedaction.redactUserId(userId),
					savedIntervals.size());
		}
		return toDto(settings, savedIntervals);
	}

	private static NutritionistAvailabilitySettings defaultSettings(final String userId) {
		final NutritionistAvailabilitySettings settings = new NutritionistAvailabilitySettings();
		settings.setUserId(userId);
		settings.setSlotDurationMinutes(BookingAvailabilityConstants.DEFAULT_SLOT_DURATION_MINUTES);
		settings.setTimezone(BookingAvailabilityConstants.DEFAULT_TIMEZONE_ID);
		return settings;
	}

	private static AvailabilityScheduleDto toDto(final NutritionistAvailabilitySettings settings,
			final List<NutritionistWorkingHoursInterval> intervals) {
		final AvailabilityScheduleDto dto = new AvailabilityScheduleDto();
		dto.setSlotDurationMinutes(settings.getSlotDurationMinutes());
		dto.setTimezone(settings.getTimezone());
		final List<WorkingHoursIntervalDto> intervalDtos = new ArrayList<>();
		for (final NutritionistWorkingHoursInterval interval : intervals) {
			intervalDtos.add(new WorkingHoursIntervalDto(interval.getDayOfWeek(), interval.getStartTime(),
					interval.getEndTime()));
		}
		dto.setIntervals(intervalDtos);
		return dto;
	}

}
