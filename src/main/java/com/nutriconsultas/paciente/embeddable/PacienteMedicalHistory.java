package com.nutriconsultas.paciente.embeddable;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Medical history text fields and common pathology flags — #156 Phase B.
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PacienteMedicalHistory {

	@Column(columnDefinition = "TEXT")
	private String antecedentesPrenatales;

	@Column(columnDefinition = "TEXT")
	private String antecedentesNatales;

	@Column(columnDefinition = "TEXT")
	private String antecedentesPatologicosPersonales;

	@Column(columnDefinition = "TEXT")
	private String antecedentesPatologicosFamiliares;

	@Column(columnDefinition = "TEXT")
	private String complicaciones;

	@Column(length = 4)
	private String tipoSanguineo;

	@Column(columnDefinition = "TEXT")
	private String historialAlimenticio;

	@Column(columnDefinition = "TEXT")
	private String desarrolloPsicomotor;

	@Column(columnDefinition = "TEXT")
	private String alergias;

	private Boolean hipertension = false;

	private Boolean diabetes = false;

	private Boolean hipotiroidismo = false;

	private Boolean obesidad = false;

	private Boolean anemia = false;

	private Boolean bulimia = false;

	private Boolean anorexia = false;

	private Boolean enfermedadesHepaticas = false;

}
