package com.nutriconsultas.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;

import com.nutriconsultas.recaptcha.RecaptchaVerificationService;

@ExtendWith(MockitoExtension.class)
class PublicBookingRestControllerTest {

	private static final String PUBLIC_ID = "11111111-2222-4333-8444-555555555555";

	@InjectMocks
	private PublicBookingRestController controller;

	@Mock
	private PublicBookingService publicBookingService;

	@Mock
	private PublicBookingRateLimiter publicBookingRateLimiter;

	@Mock
	private RecaptchaVerificationService recaptchaVerificationService;

	@Test
	void getSlotsReturnsServiceResponse() {
		final LocalDate minDate = PublicBookingAdvanceRules
			.earliestBookableDate(ZoneId.of(BookingAvailabilityConstants.DEFAULT_TIMEZONE_ID));
		when(publicBookingService.getPublicSlots(PUBLIC_ID, minDate.toString()))
			.thenReturn(new PublicBookingSlotsResponse(minDate.toString(), minDate, 2, List.of("09:00"), null));

		final PublicBookingSlotsResponse response = controller.getSlots(PUBLIC_ID, minDate.toString());

		assertThat(response.slots()).containsExactly("09:00");
	}

	@Test
	void bookReturnsSuccessWhenRecaptchaValid() throws Exception {
		when(publicBookingRateLimiter.execute(any(), any()))
			.thenAnswer(invocation -> ((java.util.concurrent.Callable<?>) invocation.getArgument(1)).call());
		when(recaptchaVerificationService.verifyToken("good")).thenReturn(true);
		when(publicBookingService.book(eq(PUBLIC_ID), any()))
			.thenReturn(new PublicBookingConfirmation(1L, "2026-06-22", "09:00"));

		final PublicBookingRequestDto request = bookingRequest("good");
		final BindingResult bindingResult = new BeanPropertyBindingResult(request, "request");

		final ResponseEntity<Map<String, Object>> response = controller.book(PUBLIC_ID, request, bindingResult,
				new MockHttpServletRequest());

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).containsEntry("success", true);
	}

	@Test
	void bookRejectsInvalidRecaptcha() {
		when(recaptchaVerificationService.verifyToken("bad")).thenReturn(false);
		final PublicBookingRequestDto request = bookingRequest("bad");
		final BindingResult bindingResult = new BeanPropertyBindingResult(request, "request");

		final ResponseEntity<Map<String, Object>> response = controller.book(PUBLIC_ID, request, bindingResult,
				new MockHttpServletRequest());

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(response.getBody()).containsEntry("success", false);
	}

	private static PublicBookingRequestDto bookingRequest(final String recaptcha) {
		final PublicBookingRequestDto request = new PublicBookingRequestDto();
		request.setPatientName("Ana");
		request.setPatientEmail("ana@example.com");
		request.setDate("2026-06-22");
		request.setTime("09:00");
		request.setRecaptchaResponse(recaptcha);
		return request;
	}

}
