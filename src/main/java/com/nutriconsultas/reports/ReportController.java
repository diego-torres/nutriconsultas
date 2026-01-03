package com.nutriconsultas.reports;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

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

}
