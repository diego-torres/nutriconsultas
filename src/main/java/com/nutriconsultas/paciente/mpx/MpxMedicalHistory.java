package com.nutriconsultas.paciente.mpx;

import lombok.Getter;
import lombok.Setter;

/**
 * Medical history and pathology flags for MPX v1 (#221).
 */
@Getter
@Setter
public class MpxMedicalHistory {

	private String antecedentesPrenatales;

	private String antecedentesNatales;

	private String antecedentesPatologicosPersonales;

	private String antecedentesPatologicosFamiliares;

	private String complicaciones;

	private String tipoSanguineo;

	private String historialAlimenticio;

	private String desarrolloPsicomotor;

	private String alergias;

	private Boolean hipertension;

	private Boolean diabetes;

	private Boolean hipotiroidismo;

	private Boolean obesidad;

	private Boolean anemia;

	private Boolean bulimia;

	private Boolean anorexia;

	private Boolean enfermedadesHepaticas;

}
