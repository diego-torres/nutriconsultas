package com.nutriconsultas.booking;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;

import com.nutriconsultas.recaptcha.PublicRecaptchaForm;

import lombok.extern.slf4j.Slf4j;

@Controller
@Slf4j
@PublicRecaptchaForm
public class PublicBookingController {

	private final PublicBookingService publicBookingService;

	public PublicBookingController(final PublicBookingService publicBookingService) {
		this.publicBookingService = publicBookingService;
	}

	@GetMapping("/consultas/{publicBookingId}/agendar-cita")
	public String bookingPage(@PathVariable final String publicBookingId, final Model model) {
		log.debug("Public booking page requested");
		try {
			final PublicBookingNutritionistContext context = publicBookingService.resolveContext(publicBookingId);
			model.addAttribute("nutritionistDisplayName", context.displayName());
			model.addAttribute("publicBookingId", context.publicBookingId());
			model.addAttribute("minAdvanceDays", context.minAdvanceDays());
			model.addAttribute("minBookableDate",
					PublicBookingAdvanceRules.earliestBookableDate(java.time.ZoneId.of(context.timezone())).toString());
			model.addAttribute("advanceNotice",
					"Las citas requieren al menos " + context.minAdvanceDays() + " días de anticipación.");
			return "eterna/agendar-cita";
		}
		catch (PublicBookingNotFoundException ex) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Enlace de reserva no disponible");
		}
	}

}
