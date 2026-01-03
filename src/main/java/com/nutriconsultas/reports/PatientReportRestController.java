package com.nutriconsultas.reports;

import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

/**
 * REST controller for patient progress report generation.
 *
 * <p>
 * Provides endpoints for generating patient progress reports in PDF format. All endpoints
 * require authentication and enforce multi-tenant access control.
 *
 * @see PatientReportService
 */
@RestController
@RequestMapping("/rest/reports")
@Slf4j
public class PatientReportRestController {

	@Autowired
	private PatientReportService reportService;

	/**
	 * Gets the user ID from the OAuth2 principal.
	 * @param principal the OAuth2 principal
	 * @return the user ID (sub claim) or null if not available
	 */
	private String getUserId(@AuthenticationPrincipal final OidcUser principal) {
		if (principal == null) {
			log.warn("OAuth2 principal is null, cannot get user ID");
			return null;
		}
		final String userId = principal.getSubject();
		log.debug("Retrieved user ID: {}", userId);
		return userId;
	}

	/**
	 * Generates a PDF progress report for a patient.
	 *
	 * <p>
	 * The report includes:
	 * <ul>
	 * <li>Patient demographics and medical history</li>
	 * <li>Weight/BMI trend data over time</li>
	 * <li>Consultation history with key metrics</li>
	 * <li>Current dietary plan summary</li>
	 * <li>Progress notes and recommendations</li>
	 * </ul>
	 *
	 * <p>
	 * Date range filtering is optional. If not provided, all available data is included.
	 * @param pacienteId the ID of the patient
	 * @param startDate optional start date for filtering (format: yyyy-MM-dd)
	 * @param endDate optional end date for filtering (format: yyyy-MM-dd)
	 * @param principal the authenticated OAuth2 user
	 * @return PDF document as ResponseEntity with appropriate headers
	 * @throws IllegalArgumentException if patient not found or access denied
	 * @throws IllegalStateException if PDF generation fails
	 */
	@GetMapping(value = "/patient/{id}", produces = MediaType.APPLICATION_PDF_VALUE)
	public ResponseEntity<byte[]> generatePatientReport(
			@PathVariable("id") @org.springframework.lang.NonNull final Long pacienteId,
			@RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") final Date startDate,
			@RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") final Date endDate,
			@AuthenticationPrincipal final OidcUser principal) {
		log.info("Generating patient report for id: {} (date range: {} to {})", pacienteId, startDate, endDate);

		final OidcUser userPrincipal = principal != null ? principal
				: (OidcUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		final String userId = getUserId(userPrincipal);

		if (userId == null) {
			log.error("Cannot generate report: user ID is null");
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}

		try {
			final byte[] pdfBytes = reportService.generateReport(pacienteId, userId, startDate, endDate);

			final HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_PDF);
			headers.setContentDispositionFormData("attachment", "reporte-progreso-paciente-" + pacienteId + ".pdf");
			headers.setContentLength(pdfBytes.length);

			log.info("Successfully generated patient report for id: {}", pacienteId);
			return ResponseEntity.ok().headers(headers).body(pdfBytes);
		}
		catch (final IllegalArgumentException e) {
			log.warn("Failed to generate report for patient id: {} - {}", pacienteId, e.getMessage());
			return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
		}
		catch (final Exception e) {
			log.error("Error generating patient report for id: {}", pacienteId, e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}

}
