package com.nutriconsultas.model;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@MappedSuperclass
@EqualsAndHashCode(callSuper = false)
public class AbstractNutrible extends AbstractMacroNutrible {

	private Integer pesoBrutoRedondeado;

	private Integer pesoNeto;

	@Column(precision = 5)
	private Double fibra;

	@Column(precision = 5)
	private Double vitA;

	@Column(precision = 5)
	private Double acidoAscorbico;

	@Column(precision = 5)
	private Double hierroNoHem;

	@Column(precision = 5)
	private Double potasio;

	@Column(precision = 5)
	private Double indiceGlicemico;

	@Column(precision = 5)
	private Double cargaGlicemica;

	@Column(precision = 5)
	private Double acidoFolico;

	@Column(precision = 5)
	private Double calcio;

	@Column(precision = 5)
	private Double hierro;

	@Column(precision = 5)
	private Double sodio;

	@Column(precision = 5)
	private Double azucarPorEquivalente;

	@Column(precision = 5)
	private Double selenio;

	@Column(precision = 5)
	private Double fosforo;

	@Column(precision = 5)
	private Double colesterol;

	@Column(precision = 5)
	private Double agSaturados;

	@Column(precision = 5)
	private Double agMonoinsaturados;

	@Column(precision = 5)
	private Double agPoliinsaturados;

	@Column(precision = 5)
	private Double etanol;

	public Double getFibra() {
		Double result = 0.0;
		if (fibra != null) {
			result = fibra;
		}
		return result;
	}

	public Double getVitA() {
		Double result = 0.0;
		if (vitA != null) {
			result = vitA;
		}
		return result;
	}

	public Double getAcidoAscorbico() {
		Double result = 0.0;
		if (acidoAscorbico != null) {
			result = acidoAscorbico;
		}
		return result;
	}

	public Double getHierroNoHem() {
		Double result = 0.0;
		if (hierroNoHem != null) {
			result = hierroNoHem;
		}
		return result;
	}

	public Double getPotasio() {
		Double result = 0.0;
		if (potasio != null) {
			result = potasio;
		}
		return result;
	}

	public Double getIndiceGlicemico() {
		Double result = 0.0;
		if (indiceGlicemico != null) {
			result = indiceGlicemico;
		}
		return result;
	}

	public Double getCargaGlicemica() {
		Double result = 0.0;
		if (cargaGlicemica != null) {
			result = cargaGlicemica;
		}
		return result;
	}

	public Double getAcidoFolico() {
		Double result = 0.0;
		if (acidoFolico != null) {
			result = acidoFolico;
		}
		return result;
	}

	public Double getCalcio() {
		Double result = 0.0;
		if (calcio != null) {
			result = calcio;
		}
		return result;
	}

	public Double getHierro() {
		Double result = 0.0;
		if (hierro != null) {
			result = hierro;
		}
		return result;
	}

	public Double getSodio() {
		Double result = 0.0;
		if (sodio != null) {
			result = sodio;
		}
		return result;
	}

	public Double getAzucarPorEquivalente() {
		Double result = 0.0;
		if (azucarPorEquivalente != null) {
			result = azucarPorEquivalente;
		}
		return result;
	}

}
