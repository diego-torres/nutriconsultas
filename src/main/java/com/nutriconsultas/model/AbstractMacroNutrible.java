package com.nutriconsultas.model;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Data;

@Data
@MappedSuperclass
public class AbstractMacroNutrible {

	private Integer energia;

	@Column(precision = 5)
	private Double proteina;

	@Column(precision = 5)
	private Double lipidos;

	@Column(precision = 5)
	private Double hidratosDeCarbono;

	public Integer getEnergia() {
		if (energia == null)
			return 0;
		return energia;
	}

	public Double getProteina() {
		if (proteina == null)
			return 0.0;
		return proteina;
	}

	public Double getLipidos() {
		if (lipidos == null)
			return 0.0;
		return lipidos;
	}

	public Double getHidratosDeCarbono() {
		if (hidratosDeCarbono == null)
			return 0.0;
		return hidratosDeCarbono;
	}

}
