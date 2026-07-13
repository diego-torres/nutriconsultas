package com.nutriconsultas.controller;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.unit.DataSize;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@Component
@ControllerAdvice
@Slf4j
public class AdminMultipartExceptionHandler {

	private static final Pattern PLATILLO_PICTURE_URI = Pattern.compile("^/admin/platillos/(\\d+)/picture$");

	private static final Pattern PACIENTE_PHOTO_URI = Pattern.compile("^/admin/pacientes/(\\d+)/photo$");

	private final DataSize maxFileSize;

	public AdminMultipartExceptionHandler(
			@Value("${spring.servlet.multipart.max-file-size:1MB}") final DataSize maxFileSize) {
		this.maxFileSize = maxFileSize;
	}

	@ExceptionHandler(MaxUploadSizeExceededException.class)
	public String handleMaxUploadSizeExceeded(final MaxUploadSizeExceededException exception,
			final HttpServletRequest request, final RedirectAttributes redirectAttributes) {
		log.warn("Multipart upload exceeded size limit for uri={}", request.getRequestURI());
		redirectAttributes.addFlashAttribute("errorMessage", buildOversizeMessage(request.getRequestURI()));
		return resolveRedirect(request.getRequestURI());
	}

	private String buildOversizeMessage(final String requestUri) {
		final String sizeLabel = formatMaxFileSize();
		if ("/admin/perfil/logo".equals(requestUri)) {
			return "El archivo supera el tamaño máximo permitido (" + sizeLabel + ").";
		}
		final Matcher patientMatcher = PACIENTE_PHOTO_URI.matcher(requestUri);
		if (patientMatcher.matches()) {
			return "La foto supera el tamaño máximo permitido (" + sizeLabel + ").";
		}
		return "La imagen supera el tamaño máximo permitido (" + sizeLabel + ").";
	}

	private String formatMaxFileSize() {
		final long megabytes = maxFileSize.toMegabytes();
		if (megabytes > 0) {
			return megabytes + " MB";
		}
		return maxFileSize.toString();
	}

	private String resolveRedirect(final String requestUri) {
		final Matcher platilloMatcher = PLATILLO_PICTURE_URI.matcher(requestUri);
		if (platilloMatcher.matches()) {
			return "redirect:/admin/platillos/" + platilloMatcher.group(1);
		}
		final Matcher patientMatcher = PACIENTE_PHOTO_URI.matcher(requestUri);
		if (patientMatcher.matches()) {
			return "redirect:/admin/pacientes/" + patientMatcher.group(1) + "/perfil";
		}
		if ("/admin/perfil/logo".equals(requestUri)) {
			return "redirect:/admin/perfil";
		}
		return "redirect:/admin";
	}

}
