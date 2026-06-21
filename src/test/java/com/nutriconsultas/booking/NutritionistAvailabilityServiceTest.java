package com.nutriconsultas.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class NutritionistAvailabilityServiceTest {

	private static final String USER_ID = "auth0|nutritionist1";

	@InjectMocks
	private NutritionistAvailabilityServiceImpl service;

	@Mock
	private NutritionistAvailabilitySettingsRepository settingsRepository;

	@Mock
	private NutritionistWorkingHoursIntervalRepository intervalRepository;

	@BeforeEach
	void setup() {
		when(settingsRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
		when(intervalRepository.findByUserIdOrderByDayOfWeekAscStartTimeAsc(USER_ID)).thenReturn(List.of());
	}

	@Test
	void getScheduleReturnsDefaultsWhenMissing() {
		final AvailabilityScheduleDto schedule = service.getSchedule(USER_ID);

		assertThat(schedule.getSlotDurationMinutes()).isEqualTo(60);
		assertThat(schedule.getTimezone()).isEqualTo(BookingAvailabilityConstants.DEFAULT_TIMEZONE_ID);
		assertThat(schedule.getIntervals()).isEmpty();
	}

	@Test
	void saveSchedulePersistsSettingsAndIntervals() {
		when(settingsRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
		when(settingsRepository.save(any(NutritionistAvailabilitySettings.class)))
			.thenAnswer(invocation -> invocation.getArgument(0));
		when(intervalRepository.save(any(NutritionistWorkingHoursInterval.class)))
			.thenAnswer(invocation -> invocation.getArgument(0));

		final AvailabilityScheduleDto request = new AvailabilityScheduleDto();
		request.setSlotDurationMinutes(30);
		request.setTimezone(BookingAvailabilityConstants.DEFAULT_TIMEZONE_ID);
		request.setIntervals(List.of(new WorkingHoursIntervalDto(1, LocalTime.of(8, 0), LocalTime.of(16, 0))));

		final AvailabilityScheduleDto saved = service.saveSchedule(USER_ID, request);

		assertThat(saved.getSlotDurationMinutes()).isEqualTo(30);
		assertThat(saved.getIntervals()).hasSize(1);
		verify(intervalRepository).deleteByUserId(USER_ID);
		final ArgumentCaptor<NutritionistWorkingHoursInterval> captor = ArgumentCaptor
			.forClass(NutritionistWorkingHoursInterval.class);
		verify(intervalRepository).save(captor.capture());
		assertThat(captor.getValue().getDayOfWeek()).isEqualTo(1);
	}

}
