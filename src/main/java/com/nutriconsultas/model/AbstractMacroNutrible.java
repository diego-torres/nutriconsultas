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
		Integer result = 0;
		if (energia != null) {
			result = energia;
		}
		return result;
	}

	public Double getProteina() {
		Double result = 0.0;
		if (proteina != null) {
			result = proteina;
		}
		return result;
	}

	public Double getLipidos() {
		Double result = 0.0;
		if (lipidos != null) {
			result = lipidos;
		}
		return result;
	}

	public Double getHidratosDeCarbono() {
		Double result = 0.0;
		if (hidratosDeCarbono != null) {
			result = hidratosDeCarbono;
		}
		return result;
	}

}
