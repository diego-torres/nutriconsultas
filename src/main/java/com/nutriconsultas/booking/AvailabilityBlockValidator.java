package com.nutriconsultas.booking;

import java.time.LocalDateTime;

import org.springframework.util.StringUtils;

/**
 * Validates nutritionist absence / day-off blocks (#247).
 */
public final class AvailabilityBlockValidator {

	private AvailabilityBlockValidator() {
	}

	public static void validate(final AvailabilityBlockDto block) {
		if (block == null) {
			throw new IllegalArgumentException("El bloqueo de disponibilidad es obligatorio");
		}
		if (!StringUtils.hasText(block.getTitle())) {
			throw new IllegalArgumentException("El título del bloqueo es obligatorio");
		}
		if (block.getStartDateTime() == null || block.getEndDateTime() == null) {
			throw new IllegalArgumentException("La fecha de inicio y fin son obligatorias");
		}
		if (block.isAllDay()) {
			normalizeAllDay(block);
			return;
		}
		if (!block.getEndDateTime().isAfter(block.getStartDateTime())) {
			throw new IllegalArgumentException("La fecha de fin debe ser posterior a la de inicio");
		}
	}

	private static void normalizeAllDay(final AvailabilityBlockDto block) {
		final LocalDateTime start = block.getStartDateTime().toLocalDate().atStartOfDay();
		final LocalDateTime end = block.getEndDateTime().toLocalDate().plusDays(1).atStartOfDay();
		block.setStartDateTime(start);
		block.setEndDateTime(end);
		if (!end.isAfter(start)) {
			throw new IllegalArgumentException("El bloqueo de día completo debe abarcar al menos un día");
		}
	}

}
