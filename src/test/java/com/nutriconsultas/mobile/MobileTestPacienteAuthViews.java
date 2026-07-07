package com.nutriconsultas.mobile;

import com.nutriconsultas.paciente.ApplePacienteLifecycleStatus;
import com.nutriconsultas.paciente.PacienteStatus;
import com.nutriconsultas.paciente.projection.PacienteAuthView;

public final class MobileTestPacienteAuthViews {

	private MobileTestPacienteAuthViews() {
	}

	public static PacienteAuthView authView(final Long id, final String patientAuthSub, final String userId) {
		return authView(id, patientAuthSub, userId, PacienteStatus.ACTIVE);
	}

	public static PacienteAuthView authView(final Long id, final String patientAuthSub, final String userId,
			final PacienteStatus status) {
		return authView(id, patientAuthSub, userId, status, ApplePacienteLifecycleStatus.NONE);
	}

	public static PacienteAuthView authView(final Long id, final String patientAuthSub, final String userId,
			final PacienteStatus status, final ApplePacienteLifecycleStatus appleLifecycleStatus) {
		return new PacienteAuthView() {
			@Override
			public Long getId() {
				return id;
			}

			@Override
			public String getPatientAuthSub() {
				return patientAuthSub;
			}

			@Override
			public String getUserId() {
				return userId;
			}

			@Override
			public PacienteStatus getStatus() {
				return status;
			}

			@Override
			public ApplePacienteLifecycleStatus getAppleLifecycleStatus() {
				return appleLifecycleStatus;
			}
		};
	}

}
