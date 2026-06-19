package com.nutriconsultas.paciente.mpx;

import lombok.Getter;
import lombok.Setter;

/**
 * Cached body metrics mirrored from {@code PacienteBodySnapshot} (#221).
 */
@Getter
@Setter
public class MpxBodySnapshot {

	private Double peso;

	private Double estatura;

	private Double imc;

	private Double bmr;

	private Double getKcal;

	private String nivelPeso;

	private Double tefKcal;

	private Double totalAdjustedKcal;

	private Double stressKcal;

	private Double finalTotalKcal;

}
