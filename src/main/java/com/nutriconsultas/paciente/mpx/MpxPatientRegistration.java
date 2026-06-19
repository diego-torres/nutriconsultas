package com.nutriconsultas.paciente.mpx;

import lombok.Getter;
import lombok.Setter;

/**
 * Registration-only patient payload for MPX v1 (#221).
 */
@Getter
@Setter
public class MpxPatientRegistration {

	private String name;

	private String dob;

	private String email;

	private String phone;

	private String gender;

	private String responsibleName;

	private String parentesco;

	private Boolean pregnancy;

	private MpxBodySnapshot bodySnapshot;

	private MpxEnergyPreferences energyPreferences;

	private MpxMedicalHistory medicalHistory;

}
