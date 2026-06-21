package com.nutriconsultas.booking;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/rest/calendario/blocks")
@Slf4j
public class NutritionistAvailabilityBlockRestController {

	private static final String BLOCK_COLOR = "#858796";

	private final NutritionistAvailabilityBlockService blockService;

	public NutritionistAvailabilityBlockRestController(final NutritionistAvailabilityBlockService blockService) {
		this.blockService = blockService;
	}

	@GetMapping
	public List<Map<String, Object>> listBlocks(
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final Date start,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final Date end,
			@AuthenticationPrincipal final OidcUser principal) {
		final String userId = getUserId(principal);
		if (userId == null || start == null || end == null) {
			return List.of();
		}
		final ZoneId zoneId = ZoneId.systemDefault();
		final LocalDateTime rangeStart = start.toInstant().atZone(zoneId).toLocalDateTime();
		final LocalDateTime rangeEnd = end.toInstant().atZone(zoneId).toLocalDateTime();
		return blockService.findBlocksInRange(userId, rangeStart, rangeEnd)
			.stream()
			.map(this::toCalendarBlockMap)
			.collect(Collectors.toList());
	}

	@PostMapping
	public ResponseEntity<Map<String, Object>> createBlock(@Valid @RequestBody final AvailabilityBlockDto block,
			final BindingResult bindingResult, @AuthenticationPrincipal final OidcUser principal) {
		final String userId = getUserId(principal);
		if (userId == null) {
			return unauthorized();
		}
		if (bindingResult.hasErrors()) {
			return badRequest("Datos del bloqueo no válidos");
		}
		try {
			final AvailabilityBlockDto saved = blockService.createBlock(userId, block);
			final Map<String, Object> response = new HashMap<>();
			response.put("success", true);
			response.put("block", toCalendarBlockMapFromDto(saved));
			return ResponseEntity.ok(response);
		}
		catch (final IllegalArgumentException ex) {
			log.debug("Block validation failed");
			return badRequest(ex.getMessage());
		}
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Map<String, Object>> deleteBlock(@PathVariable final Long id,
			@AuthenticationPrincipal final OidcUser principal) {
		final String userId = getUserId(principal);
		if (userId == null) {
			return unauthorized();
		}
		try {
			blockService.deleteBlock(userId, id);
			final Map<String, Object> response = new HashMap<>();
			response.put("success", true);
			return ResponseEntity.ok(response);
		}
		catch (final IllegalArgumentException ex) {
			return badRequest(ex.getMessage());
		}
	}

	private Map<String, Object> toCalendarBlockMap(final NutritionistAvailabilityBlock block) {
		return toCalendarBlockMapFromDto(NutritionistAvailabilityBlockServiceImpl.toDto(block));
	}

	private Map<String, Object> toCalendarBlockMapFromDto(final AvailabilityBlockDto block) {
		final Map<String, Object> eventMap = new HashMap<>();
		eventMap.put("id", "block-" + block.getId());
		eventMap.put("title", block.getTitle());
		eventMap.put("start", formatDateTime(block.getStartDateTime()));
		eventMap.put("end", formatDateTime(block.getEndDateTime()));
		eventMap.put("allDay", block.isAllDay());
		eventMap.put("backgroundColor", BLOCK_COLOR);
		eventMap.put("borderColor", BLOCK_COLOR);
		eventMap.put("classNames", List.of("availability-block-event"));
		final Map<String, Object> extendedProps = new HashMap<>();
		extendedProps.put("type", "BLOCK");
		extendedProps.put("blockId", block.getId());
		extendedProps.put("title", block.getTitle());
		extendedProps.put("allDay", block.isAllDay());
		eventMap.put("extendedProps", extendedProps);
		return eventMap;
	}

	private static String formatDateTime(final LocalDateTime dateTime) {
		if (dateTime == null) {
			return null;
		}
		final Date date = Date.from(dateTime.atZone(ZoneId.systemDefault()).toInstant());
		final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
		return dateFormat.format(date);
	}

	private static String getUserId(final OidcUser principal) {
		if (principal == null) {
			return null;
		}
		return principal.getSubject();
	}

	private static ResponseEntity<Map<String, Object>> unauthorized() {
		final Map<String, Object> body = new HashMap<>();
		body.put("success", false);
		body.put("error", "User not authenticated");
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
	}

	private static ResponseEntity<Map<String, Object>> badRequest(final String message) {
		final Map<String, Object> body = new HashMap<>();
		body.put("success", false);
		body.put("error", message);
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
	}

}
