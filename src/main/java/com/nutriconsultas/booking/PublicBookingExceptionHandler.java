package com.nutriconsultas.booking;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import io.github.resilience4j.ratelimiter.RequestNotPermitted;

@RestControllerAdvice(basePackages = "com.nutriconsultas.booking")
public class PublicBookingExceptionHandler {

	@ExceptionHandler(PublicBookingNotFoundException.class)
	public ResponseEntity<Map<String, Object>> handleNotFound(final PublicBookingNotFoundException ex) {
		return error(HttpStatus.NOT_FOUND, "Enlace de reserva no disponible");
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<Map<String, Object>> handleBadRequest(final IllegalArgumentException ex) {
		return error(HttpStatus.BAD_REQUEST, ex.getMessage());
	}

	@ExceptionHandler(RequestNotPermitted.class)
	public ResponseEntity<Map<String, Object>> handleRateLimit(final RequestNotPermitted ex) {
		final Map<String, Object> body = errorBody("Demasiadas solicitudes. Intente más tarde.");
		body.put("retryAfterSeconds", 3600);
		return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).header("Retry-After", "3600").body(body);
	}

	private static ResponseEntity<Map<String, Object>> error(final HttpStatus status, final String message) {
		return ResponseEntity.status(status).body(errorBody(message));
	}

	private static Map<String, Object> errorBody(final String message) {
		final Map<String, Object> body = new HashMap<>();
		body.put("success", false);
		body.put("error", message);
		return body;
	}

}
