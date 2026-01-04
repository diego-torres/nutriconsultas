package com.nutriconsultas.paciente;

import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;

import com.nutriconsultas.paciente.validation.ValidPregnancy;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Paciente {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@NotBlank(message = "El nombre es requerido")
	@Column(nullable = false, length = 100)
	private String name;

	@Column(nullable = false, length = 255)
	private String userId;

	@DateTimeFormat(pattern = "yyyy-MM-dd")
	@Temporal(TemporalType.DATE)
	@NotNull(message = "La fecha de nacimiento es requerida")
	@Column(nullable = false)
	private Date dob;

	@DateTimeFormat(iso = ISO.DATE_TIME)
	@Temporal(TemporalType.TIMESTAMP)
	private Date registro = new Date();

	@Column(length = 100)
	private String email;

	@Column(length = 25)
	private String phone;

	@NotBlank(message = "El género es requerido")
	@Column(nullable = false, length = 1)
	private String gender;

	@Column(length = 100)
	private String responsibleName;

	private String parentesco;

	@Column(precision = 5)
	private Double peso;

	@Column(precision = 3)
	private Double estatura;

	@Column(precision = 3)
	private Double imc;

	private NivelPeso nivelPeso;

	// ANTECEDENTES
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

	// NUTRICION Y DESARROLLO
	@Column(columnDefinition = "TEXT")
	private String historialAlimenticio;

	@Column(columnDefinition = "TEXT")
	private String desarrolloPsicomotor;

	@Column(columnDefinition = "TEXT")
	private String alergias;

	// BANDERAS DE PATOLOGIAS COMUNES
	private Boolean hipertension = false;

	private Boolean diabetes = false;

	private Boolean hipotiroidismo = false;

	private Boolean obesidad = false;

	private Boolean anemia = false;

	private Boolean bulimia = false;

	private Boolean anorexia = false;

	private Boolean enfermedadesHepaticas = false;

	// ESTADO DE EMBARAZO (solo para mujeres entre 12-50 años)
	@ValidPregnancy
	private Boolean pregnancy = false;

}
