package com.nutriconsultas.booking;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.nutriconsultas.recaptcha.RecaptchaVerificationService;

import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/rest/public/booking")
@Slf4j
public class PublicBookingRestController {

	private final PublicBookingService publicBookingService;

	private final PublicBookingRateLimiter publicBookingRateLimiter;

	private final RecaptchaVerificationService recaptchaVerificationService;

	public PublicBookingRestController(final PublicBookingService publicBookingService,
			final PublicBookingRateLimiter publicBookingRateLimiter,
			final RecaptchaVerificationService recaptchaVerificationService) {
		this.publicBookingService = publicBookingService;
		this.publicBookingRateLimiter = publicBookingRateLimiter;
		this.recaptchaVerificationService = recaptchaVerificationService;
	}

	@GetMapping("/{publicBookingId}/context")
	public PublicBookingNutritionistContext getContext(@PathVariable final String publicBookingId) {
		return publicBookingService.resolveContext(publicBookingId);
	}

	@GetMapping("/{publicBookingId}/slots")
	public PublicBookingSlotsResponse getSlots(@PathVariable final String publicBookingId,
			@RequestParam final String date) {
		return publicBookingService.getPublicSlots(publicBookingId, date);
	}

	@PostMapping("/{publicBookingId}/book")
	public ResponseEntity<Map<String, Object>> book(@PathVariable final String publicBookingId,
			@Valid @RequestBody final PublicBookingRequestDto request, final BindingResult bindingResult,
			final HttpServletRequest httpRequest) {
		if (bindingResult.hasErrors()) {
			return validationError("Complete todos los campos requeridos correctamente.");
		}
		if (!recaptchaVerificationService.verifyToken(request.getRecaptchaResponse())) {
			return validationError("La verificación reCAPTCHA falló. Intente nuevamente.");
		}
		final String clientKey = ClientIpResolver.resolve(httpRequest);
		try {
			final PublicBookingConfirmation confirmation = publicBookingRateLimiter.execute(clientKey,
					() -> publicBookingService.book(publicBookingId, request));
			final Map<String, Object> body = new HashMap<>();
			body.put("success", true);
			body.put("eventId", confirmation.eventId());
			body.put("date", confirmation.date());
			body.put("time", confirmation.time());
			return ResponseEntity.ok(body);
		}
		catch (RequestNotPermitted ex) {
			throw ex;
		}
		catch (IllegalArgumentException ex) {
			return validationError(ex.getMessage());
		}
	}

	private static ResponseEntity<Map<String, Object>> validationError(final String message) {
		final Map<String, Object> body = new HashMap<>();
		body.put("success", false);
		body.put("error", message);
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
	}

}
