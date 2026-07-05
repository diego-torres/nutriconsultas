package com.nutriconsultas.ai;

import java.util.List;

public record PatientAppointmentsData(List<PatientAppointmentItem> upcoming, List<PatientAppointmentItem> past,
		int totalReturned) {

	public PatientAppointmentsData {
		upcoming = upcoming == null ? List.of() : List.copyOf(upcoming);
		past = past == null ? List.of() : List.copyOf(past);
	}

}
