package com.nutriconsultas.booking;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

import org.springframework.util.StringUtils;

import com.nutriconsultas.paciente.Paciente;
import com.nutriconsultas.paciente.PacienteStatus;

/**
 * Builds minimal {@link Paciente} rows for first-time public self-booking (#300). Full
 * demographics are completed later by the nutritionist in admin.
 */
public final class PublicBookingPatientFactory {

	/**
	 * Sentinel date of birth until the nutritionist completes the clinical profile.
	 */
	static final LocalDate PLACEHOLDER_DATE_OF_BIRTH = LocalDate.of(1900, 1, 1);

	static final String PLACEHOLDER_GENDER = "M";

	private PublicBookingPatientFactory() {
	}

	static Paciente buildProspect(final String userId, final PublicBookingRequestDto request) {
		final Paciente paciente = new Paciente();
		paciente.setUserId(userId);
		paciente.setName(request.getPatientName().trim());
		paciente.setEmail(request.getPatientEmail().trim());
		if (StringUtils.hasText(request.getPatientPhone())) {
			paciente.setPhone(request.getPatientPhone().trim());
		}
		paciente.setStatus(PacienteStatus.ONBOARDING);
		paciente.setDob(Date.from(PLACEHOLDER_DATE_OF_BIRTH.atStartOfDay(ZoneId.systemDefault()).toInstant()));
		paciente.setGender(PLACEHOLDER_GENDER);
		return paciente;
	}

}
