package com.nutriconsultas.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class PublicBookingWebControllerTest {

	private static final String PUBLIC_ID = "11111111-2222-4333-8444-555555555555";

	@InjectMocks
	private PublicBookingController controller;

	@Mock
	private PublicBookingService publicBookingService;

	@Test
	void bookingPageRendersView() {
		when(publicBookingService.resolveContext(PUBLIC_ID)).thenReturn(new PublicBookingNutritionistContext(PUBLIC_ID,
				"Dra. Ejemplo", BookingAvailabilityConstants.DEFAULT_TIMEZONE_ID, 2));

		final String view = controller.bookingPage(PUBLIC_ID, new ExtendedModelMap());

		assertThat(view).isEqualTo("eterna/agendar-cita");
	}

	@Test
	void bookingPageReturns404WhenLinkInvalid() {
		when(publicBookingService.resolveContext(PUBLIC_ID)).thenThrow(new PublicBookingNotFoundException());

		try {
			controller.bookingPage(PUBLIC_ID, new ExtendedModelMap());
		}
		catch (ResponseStatusException ex) {
			assertThat(ex.getStatusCode().value()).isEqualTo(404);
			return;
		}
		throw new AssertionError("Expected ResponseStatusException");
	}

}
