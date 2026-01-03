package com.nutriconsultas.admin;

import java.util.List;

import com.nutriconsultas.calendar.CalendarEvent;
import com.nutriconsultas.paciente.Paciente;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatistics {

	private long totalPatients;

	private long activeDietaryPlans;

	private long upcomingAppointments;

	private long consultationsThisWeek;

	private long newPatientsThisMonth;

	private List<Paciente> recentPatients;

	private List<CalendarEvent> upcomingAppointmentsList;

	private List<Paciente> patientsNeedingFollowUp;

}
