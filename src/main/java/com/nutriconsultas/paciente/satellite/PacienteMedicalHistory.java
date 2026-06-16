package com.nutriconsultas.paciente.satellite;

import com.nutriconsultas.paciente.Paciente;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Medical history and pathology flags for a patient — #156 Phase C satellite table.
 */
@Entity
@Table(name = "paciente_medical_history")
@Getter
@Setter
@NoArgsConstructor
public class PacienteMedicalHistory {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@OneToOne(optional = false)
	@JoinColumn(name = "paciente_id", nullable = false, unique = true)
	private Paciente paciente;

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
