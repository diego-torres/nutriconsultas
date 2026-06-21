package com.nutriconsultas.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.test.context.ActiveProfiles;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class NutritionistAvailabilityRestControllerTest {

	@InjectMocks
	private NutritionistAvailabilityRestController controller;

	@Mock
	private NutritionistAvailabilityService availabilityService;

	@Mock
	private OidcUser principal;

	private AvailabilityScheduleDto schedule;

	@BeforeEach
	void setup() {
		schedule = new AvailabilityScheduleDto();
		schedule.setSlotDurationMinutes(60);
		schedule.setTimezone(BookingAvailabilityConstants.DEFAULT_TIMEZONE_ID);
		schedule.setIntervals(List.of(new WorkingHoursIntervalDto(1, LocalTime.of(9, 0), LocalTime.of(17, 0))));
	}

	@Test
	void getScheduleReturnsSavedData() {
		when(principal.getSubject()).thenReturn("auth0|user1");
		when(availabilityService.getSchedule("auth0|user1")).thenReturn(schedule);

		final ResponseEntity<AvailabilityScheduleDto> response = controller.getSchedule(principal);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().getIntervals()).hasSize(1);
	}

	@Test
	void getScheduleRequiresAuthentication() {
		final ResponseEntity<AvailabilityScheduleDto> response = controller.getSchedule(null);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
	}

	@Test
	void saveScheduleReturnsValidationError() {
		when(principal.getSubject()).thenReturn("auth0|user1");
		when(availabilityService.saveSchedule("auth0|user1", schedule))
			.thenThrow(new IllegalArgumentException("Los horarios se traslapan el mismo día"));

		final ResponseEntity<?> response = controller.saveSchedule(principal, schedule,
				new org.springframework.validation.BeanPropertyBindingResult(schedule, "schedule"));

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		@SuppressWarnings("unchecked")
		final Map<String, Object> body = (Map<String, Object>) response.getBody();
		assertThat(body).containsEntry("success", false);
	}

}
