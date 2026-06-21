package com.nutriconsultas.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.nutriconsultas.calendar.CalendarEvent;
import com.nutriconsultas.calendar.CalendarEventService;
import com.nutriconsultas.calendar.EventStatus;
import com.nutriconsultas.paciente.Paciente;
import com.nutriconsultas.paciente.PacienteRepository;
import com.nutriconsultas.paciente.PacienteService;
import com.nutriconsultas.paciente.PacienteStatus;
import com.nutriconsultas.profile.NutritionistProfile;
import com.nutriconsultas.profile.NutritionistProfileRepository;
import com.nutriconsultas.subscription.Subscription;
import com.nutriconsultas.subscription.lifecycle.SubscriptionAccessService;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PublicBookingServiceTest {

	private static final String PUBLIC_ID = "11111111-2222-4333-8444-555555555555";

	private static final String USER_ID = "auth0|nutritionist";

	@InjectMocks
	private PublicBookingServiceImpl service;

	@Mock
	private NutritionistProfileRepository profileRepository;

	@Mock
	private SubscriptionAccessService subscriptionAccessService;

	@Mock
	private NutritionistAvailabilityService availabilityService;

	@Mock
	private BookingAvailabilitySlotService bookingAvailabilitySlotService;

	@Mock
	private PacienteRepository pacienteRepository;

	@Mock
	private PacienteService pacienteService;

	@Mock
	private CalendarEventService calendarEventService;

	private NutritionistProfile profile;

	private AvailabilityScheduleDto schedule;

	@BeforeEach
	void setup() {
		profile = new NutritionistProfile();
		profile.setUserId(USER_ID);
		profile.setPublicBookingId(PUBLIC_ID);
		profile.setDisplayName("Dra. Ejemplo");
		schedule = new AvailabilityScheduleDto();
		schedule.setSlotDurationMinutes(60);
		schedule.setTimezone(BookingAvailabilityConstants.DEFAULT_TIMEZONE_ID);
		when(profileRepository.findByPublicBookingId(PUBLIC_ID)).thenReturn(Optional.of(profile));
		when(subscriptionAccessService.findGrantingSubscriptionForUser(USER_ID))
			.thenReturn(Optional.of(new Subscription()));
		when(availabilityService.getSchedule(USER_ID)).thenReturn(schedule);
	}

	@Test
	void getPublicSlotsReturnsEmptyForDatesInsideAdvanceWindow() {
		final LocalDate tomorrow = LocalDate.now(ZoneId.of(schedule.getTimezone())).plusDays(1);

		final PublicBookingSlotsResponse response = service.getPublicSlots(PUBLIC_ID, tomorrow.toString());

		assertThat(response.slots()).isEmpty();
		assertThat(response.notice()).contains("anticipación");
		verify(bookingAvailabilitySlotService, never()).getAvailableSlotStarts(any(), any());
	}

	@Test
	void getPublicSlotsDelegatesWhenDateEligible() {
		final LocalDate eligible = LocalDate.now(ZoneId.of(schedule.getTimezone())).plusDays(2);
		when(bookingAvailabilitySlotService.getAvailableSlotStarts(USER_ID, eligible))
			.thenReturn(List.of(LocalTime.of(9, 0)));

		final PublicBookingSlotsResponse response = service.getPublicSlots(PUBLIC_ID, eligible.toString());

		assertThat(response.slots()).containsExactly("09:00");
		assertThat(response.notice()).isNull();
	}

	@Test
	void bookCreatesCalendarEventForEligibleSlot() {
		final LocalDate eligible = LocalDate.now(ZoneId.of(schedule.getTimezone())).plusDays(2);
		when(bookingAvailabilitySlotService.getAvailableSlotStarts(USER_ID, eligible))
			.thenReturn(List.of(LocalTime.of(10, 0)));
		when(pacienteRepository.findFirstByUserIdAndEmailIgnoreCase(eq(USER_ID), eq("paciente@example.com")))
			.thenReturn(Optional.empty());
		final Paciente savedPatient = new Paciente();
		savedPatient.setId(5L);
		when(pacienteService.save(any(Paciente.class))).thenReturn(savedPatient);
		final CalendarEvent savedEvent = new CalendarEvent();
		savedEvent.setId(99L);
		when(calendarEventService.save(any(CalendarEvent.class))).thenReturn(savedEvent);

		final PublicBookingRequestDto request = new PublicBookingRequestDto();
		request.setPatientName("Paciente Test");
		request.setPatientEmail("paciente@example.com");
		request.setDate(eligible.toString());
		request.setTime("10:00");
		request.setRecaptchaResponse("token");

		final PublicBookingConfirmation confirmation = service.book(PUBLIC_ID, request);

		assertThat(confirmation.eventId()).isEqualTo(99L);
		final ArgumentCaptor<Paciente> patientCaptor = ArgumentCaptor.forClass(Paciente.class);
		verify(pacienteService).save(patientCaptor.capture());
		assertThat(patientCaptor.getValue().getStatus()).isEqualTo(PacienteStatus.ONBOARDING);
		final ArgumentCaptor<CalendarEvent> captor = ArgumentCaptor.forClass(CalendarEvent.class);
		verify(calendarEventService).save(captor.capture());
		assertThat(captor.getValue().getStatus()).isEqualTo(EventStatus.SCHEDULED);
		assertThat(captor.getValue().getPaciente().getId()).isEqualTo(5L);
	}

	@Test
	void bookRejectsSlotInsideAdvanceWindow() {
		final LocalDate tomorrow = LocalDate.now(ZoneId.of(schedule.getTimezone())).plusDays(1);
		final PublicBookingRequestDto request = new PublicBookingRequestDto();
		request.setPatientName("Paciente Test");
		request.setPatientEmail("paciente@example.com");
		request.setDate(tomorrow.toString());
		request.setTime("10:00");

		assertThatThrownBy(() -> service.book(PUBLIC_ID, request)).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void resolveContextThrowsWhenSubscriptionInactive() {
		when(subscriptionAccessService.findGrantingSubscriptionForUser(USER_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.resolveContext(PUBLIC_ID)).isInstanceOf(PublicBookingNotFoundException.class);
	}

}
