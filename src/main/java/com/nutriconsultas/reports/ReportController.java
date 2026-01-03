package com.nutriconsultas.reports;

import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.nutriconsultas.controller.AbstractAuthorizedController;
import com.nutriconsultas.dieta.DietaRepository;
import com.nutriconsultas.paciente.PacienteService;

import lombok.extern.slf4j.Slf4j;

/**
 * Web controller for patient progress reports.
 *
 * <p>
 * Provides web pages for accessing and generating patient progress reports. All endpoints
 * require authentication and enforce multi-tenant access control.
 *
 * @see PatientReportService
 * @see PatientReportRestController
 */
@Controller
@Slf4j
public class ReportController extends AbstractAuthorizedController {

	@Autowired
	private PacienteService pacienteService;

	@Autowired
	private DietaRepository dietaRepository;

	@Autowired
	private ClinicStatisticsService clinicStatisticsService;

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
	 * Displays the reports page where users can select a patient and generate progress
	 * reports.
	 * @param model the model to add attributes to
	 * @param principal the authenticated OAuth2 user
	 * @return the view name for the reports page
	 */
	@GetMapping(path = "/admin/reportes")
	public String listado(final Model model, @AuthenticationPrincipal final OidcUser principal) {
		log.debug("Displaying reports page");
		model.addAttribute("activeMenu", "reportes");

		final String userId = getUserId(principal);
		if (userId == null) {
			log.error("Cannot display reports: user ID is null");
			model.addAttribute("error", "No se pudo identificar al usuario");
			return "sbadmin/reports/listado";
		}

		// Get all patients for the current user
		model.addAttribute("pacientes", pacienteService.findAllByUserId(userId));

		// Get all diets for the current user
		model.addAttribute("dietas", dietaRepository.findByUserId(userId));

		return "sbadmin/reports/listado";
	}

	/**
	 * Displays the clinic statistics page with aggregated statistics across all patients.
	 * @param model the model to add attributes to
	 * @param principal the authenticated OAuth2 user
	 * @param startDate optional start date for filtering
	 * @param endDate optional end date for filtering
	 * @return the view name for the clinic statistics page
	 */
	@GetMapping(path = "/admin/reportes/estadisticas")
	public String estadisticas(final Model model, @AuthenticationPrincipal final OidcUser principal,
			@RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") final Date startDate,
			@RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") final Date endDate) {
		log.debug("Displaying clinic statistics page");
		model.addAttribute("activeMenu", "reportes");

		final String userId = getUserId(principal);
		if (userId == null) {
			log.error("Cannot display clinic statistics: user ID is null");
			model.addAttribute("error", "No se pudo identificar al usuario");
			return "sbadmin/reports/estadisticas";
		}

		try {
			final ClinicStatistics statistics = clinicStatisticsService.generateStatistics(userId, startDate, endDate);
			model.addAttribute("statistics", statistics);
			model.addAttribute("startDate", startDate);
			model.addAttribute("endDate", endDate);
		}
		catch (final Exception e) {
			log.error("Error generating clinic statistics", e);
			model.addAttribute("error", "Error al generar las estadísticas: " + e.getMessage());
		}

		return "sbadmin/reports/estadisticas";
	}

}
