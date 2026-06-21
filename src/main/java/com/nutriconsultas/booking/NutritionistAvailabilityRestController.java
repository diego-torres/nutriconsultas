package com.nutriconsultas.booking;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/rest/profile/availability")
@Slf4j
public class NutritionistAvailabilityRestController {

	private static final DateTimeFormatter SLOT_TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

	private final NutritionistAvailabilityService availabilityService;

	private final BookingAvailabilitySlotService bookingAvailabilitySlotService;

	public NutritionistAvailabilityRestController(final NutritionistAvailabilityService availabilityService,
			final BookingAvailabilitySlotService bookingAvailabilitySlotService) {
		this.availabilityService = availabilityService;
		this.bookingAvailabilitySlotService = bookingAvailabilitySlotService;
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

	@GetMapping("/slots")
	public ResponseEntity<Map<String, Object>> getAvailableSlots(@AuthenticationPrincipal final OidcUser principal,
			@RequestParam final String date) {
		if (principal == null || principal.getSubject() == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}
		try {
			final LocalDate parsedDate = LocalDate.parse(date);
			final List<LocalTime> slots = bookingAvailabilitySlotService.getAvailableSlotStarts(principal.getSubject(),
					parsedDate);
			final Map<String, Object> body = new HashMap<>();
			body.put("date", parsedDate.toString());
			body.put("slots", slots.stream().map(SLOT_TIME_FORMAT::format).collect(Collectors.toList()));
			return ResponseEntity.ok(body);
		}
		catch (final Exception ex) {
			log.debug("Invalid slot query date");
			return validationErrorResponse("Fecha no válida");
		}
	}

	private static ResponseEntity<Map<String, Object>> validationErrorResponse(final String message) {
		final Map<String, Object> body = new HashMap<>();
		body.put("success", false);
		body.put("error", message);
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
	}

}
