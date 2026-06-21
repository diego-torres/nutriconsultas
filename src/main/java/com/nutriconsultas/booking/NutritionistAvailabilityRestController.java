package com.nutriconsultas.booking;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/rest/profile/availability")
@Slf4j
public class NutritionistAvailabilityRestController {

	private final NutritionistAvailabilityService availabilityService;

	public NutritionistAvailabilityRestController(final NutritionistAvailabilityService availabilityService) {
		this.availabilityService = availabilityService;
	}

	@GetMapping
	public ResponseEntity<AvailabilityScheduleDto> getSchedule(@AuthenticationPrincipal final OidcUser principal) {
		if (principal == null || principal.getSubject() == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}
		return ResponseEntity.ok(availabilityService.getSchedule(principal.getSubject()));
	}

	@PutMapping
	public ResponseEntity<?> saveSchedule(@AuthenticationPrincipal final OidcUser principal,
			@Valid @RequestBody final AvailabilityScheduleDto schedule, final BindingResult bindingResult) {
		if (principal == null || principal.getSubject() == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}
		if (bindingResult.hasErrors()) {
			return validationErrorResponse("Datos de horario no válidos");
		}
		try {
			final AvailabilityScheduleDto saved = availabilityService.saveSchedule(principal.getSubject(), schedule);
			return ResponseEntity.ok(saved);
		}
		catch (final IllegalArgumentException ex) {
			log.debug("Availability validation failed for user request");
			return validationErrorResponse(ex.getMessage());
		}
	}

	private static ResponseEntity<Map<String, Object>> validationErrorResponse(final String message) {
		final Map<String, Object> body = new HashMap<>();
		body.put("success", false);
		body.put("error", message);
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
	}

}
