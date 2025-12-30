package com.nutriconsultas.admin;

import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.nutriconsultas.calendar.CalendarEvent;
import com.nutriconsultas.calendar.CalendarEventService;
import com.nutriconsultas.controller.AbstractAuthorizedController;

@Controller
public class DashboardController extends AbstractAuthorizedController {

	private static final Logger LOGGER = LoggerFactory.getLogger(DashboardController.class);

	@Autowired
	private CalendarEventService calendarEventService;

	@GetMapping(path = "/admin")
	public String index(final Model model) {
		LOGGER.debug("Resolving Admin Index");
		model.addAttribute("activeMenu", "home");

		// Get upcoming appointments (next 7 days)
		final Date now = new Date();
		final List<CalendarEvent> upcomingEvents = calendarEventService.findUpcomingEvents(now);
		// Limit to next 5 appointments for dashboard display
		final List<CalendarEvent> nextAppointments = upcomingEvents.stream().limit(5).toList();
		model.addAttribute("upcomingAppointments", nextAppointments);

		return "sbadmin/index";
	}

}
