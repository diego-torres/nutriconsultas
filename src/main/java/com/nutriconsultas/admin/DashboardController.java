package com.nutriconsultas.admin;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.nutriconsultas.controller.AbstractAuthorizedController;

import lombok.extern.slf4j.Slf4j;

@Controller
@Slf4j
public class DashboardController extends AbstractAuthorizedController {

	@Autowired
	private DashboardService dashboardService;

	@GetMapping(path = "/admin")
	public String index(@AuthenticationPrincipal final OidcUser principal, final Model model) {
		log.debug("Resolving Admin Index");
		model.addAttribute("activeMenu", "home");

		final String userId = getUserId(principal);
		if (userId == null) {
			log.warn("User ID is null, cannot load dashboard data");
			return "sbadmin/index";
		}

		// Get dashboard statistics
		final DashboardStatistics stats = dashboardService.getDashboardStatistics(userId);
		model.addAttribute("stats", stats);
		model.addAttribute("upcomingAppointments", stats.getUpcomingAppointmentsList());

		// Get chart data
		final List<Map<String, Object>> patientGrowthTrend = dashboardService.getPatientGrowthTrend(userId, 6);
		model.addAttribute("patientGrowthTrend", patientGrowthTrend);

		final List<Map<String, Object>> consultationFrequency = dashboardService.getConsultationFrequency(userId, 6);
		model.addAttribute("consultationFrequency", consultationFrequency);

		final List<Map<String, Object>> mostCommonConditions = dashboardService.getMostCommonConditions(userId);
		model.addAttribute("mostCommonConditions", mostCommonConditions);

		return "sbadmin/index";
	}

	/**
	 * Gets the user ID from the OAuth2 principal.
	 * @param principal the OAuth2 principal
	 * @return the user ID (sub claim) or null if not available
	 */
	private String getUserId(final OidcUser principal) {
		if (principal == null) {
			log.warn("OAuth2 principal is null, cannot get user ID");
			return null;
		}
		final String userId = principal.getSubject();
		log.debug("Retrieved user ID: {}", userId);
		return userId;
	}

}
