package com.nutriconsultas.paciente.mpx;

import lombok.Getter;
import lombok.Setter;

/**
 * Root MPX v1 document envelope (#221).
 */
@Getter
@Setter
public class MpxDocument {

	private int mpxVersion;

	private String exportedAt;

	private String sourceApp;

	private MpxPatientRegistration patient;

}
