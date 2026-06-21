package com.nutriconsultas.dieta;

import com.nutriconsultas.model.AbstractNutrible;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Rolled-up nutrient totals for a dieta or ingesta, aggregated from
 * {@link PlatilloIngesta} and {@link AlimentoIngesta} snapshot rows.
 */
@Data
@NoArgsConstructor
public class DietaNutrientTotals {

	private Integer energia;

	private Double proteina;

	private Double lipidos;

	private Double hidratosDeCarbono;

	private Double fibra;

	private Double vitA;

	private Double acidoAscorbico;

	private Double acidoFolico;

	private Double calcio;

	private Double hierro;

	private Double hierroNoHem;

	private Double sodio;

	private Double potasio;

	private Double fosforo;

	private Double selenio;

	private Double colesterol;

	private Double agSaturados;

	private Double agMonoinsaturados;

	private Double agPoliinsaturados;

	private Double azucarPorEquivalente;

	private Double indiceGlicemico;

	private Double cargaGlicemica;

	private Double etanol;

	public void addFrom(final AbstractNutrible source) {
		if (source == null) {
			return;
		}
		energia = addInteger(energia, source.getEnergia());
		proteina = addDouble(proteina, source.getProteina());
		lipidos = addDouble(lipidos, source.getLipidos());
		hidratosDeCarbono = addDouble(hidratosDeCarbono, source.getHidratosDeCarbono());
		fibra = addDouble(fibra, source.getFibra());
		vitA = addDouble(vitA, source.getVitA());
		acidoAscorbico = addDouble(acidoAscorbico, source.getAcidoAscorbico());
		acidoFolico = addDouble(acidoFolico, source.getAcidoFolico());
		calcio = addDouble(calcio, source.getCalcio());
		hierro = addDouble(hierro, source.getHierro());
		hierroNoHem = addDouble(hierroNoHem, source.getHierroNoHem());
		sodio = addDouble(sodio, source.getSodio());
		potasio = addDouble(potasio, source.getPotasio());
		fosforo = addDouble(fosforo, source.getFosforo());
		selenio = addDouble(selenio, source.getSelenio());
		colesterol = addDouble(colesterol, source.getColesterol());
		agSaturados = addDouble(agSaturados, source.getAgSaturados());
		agMonoinsaturados = addDouble(agMonoinsaturados, source.getAgMonoinsaturados());
		agPoliinsaturados = addDouble(agPoliinsaturados, source.getAgPoliinsaturados());
		azucarPorEquivalente = addDouble(azucarPorEquivalente, source.getAzucarPorEquivalente());
		indiceGlicemico = addDouble(indiceGlicemico, source.getIndiceGlicemico());
		cargaGlicemica = addDouble(cargaGlicemica, source.getCargaGlicemica());
		etanol = addDouble(etanol, source.getEtanol());
	}

	public void addFromAlimento(final AlimentoIngesta alimento) {
		if (alimento == null) {
			return;
		}
		energia = addInteger(energia, alimento.getEnergia());
		proteina = addDouble(proteina, alimento.getProteina());
		lipidos = addDouble(lipidos, alimento.getLipidos());
		hidratosDeCarbono = addDouble(hidratosDeCarbono, alimento.getHidratosDeCarbono());
		fibra = addDouble(fibra, alimento.getFibra());
		vitA = addDouble(vitA, alimento.getVitA());
		acidoAscorbico = addDouble(acidoAscorbico, alimento.getAcidoAscorbico());
		acidoFolico = addDouble(acidoFolico, alimento.getAcidoFolico());
		calcio = addDouble(calcio, alimento.getCalcio());
		hierro = addDouble(hierro, alimento.getHierro());
		hierroNoHem = addDouble(hierroNoHem, alimento.getHierroNoHem());
		sodio = addDouble(sodio, alimento.getSodio());
		potasio = addDouble(potasio, alimento.getPotasio());
		fosforo = addDouble(fosforo, alimento.getFosforo());
		selenio = addDouble(selenio, alimento.getSelenio());
		colesterol = addDouble(colesterol, alimento.getColesterol());
		agSaturados = addDouble(agSaturados, alimento.getAgSaturados());
		agMonoinsaturados = addDouble(agMonoinsaturados, alimento.getAgMonoinsaturados());
		agPoliinsaturados = addDouble(agPoliinsaturados, alimento.getAgPoliinsaturados());
		azucarPorEquivalente = addDouble(azucarPorEquivalente, alimento.getAzucarPorEquivalente());
		indiceGlicemico = addDouble(indiceGlicemico, alimento.getIndiceGlicemico());
		cargaGlicemica = addDouble(cargaGlicemica, alimento.getCargaGlicemica());
		etanol = addDouble(etanol, alimento.getEtanol());
	}

	private static Integer addInteger(final Integer current, final Integer toAdd) {
		if (toAdd == null) {
			return current;
		}
		return (current != null ? current : 0) + toAdd;
	}

	private static Double addDouble(final Double current, final Double toAdd) {
		if (toAdd == null) {
			return current;
		}
		return (current != null ? current : 0.0) + toAdd;
	}

	public void addTotals(final DietaNutrientTotals other) {
		if (other == null) {
			return;
		}
		energia = addInteger(energia, other.energia);
		proteina = addDouble(proteina, other.proteina);
		lipidos = addDouble(lipidos, other.lipidos);
		hidratosDeCarbono = addDouble(hidratosDeCarbono, other.hidratosDeCarbono);
		fibra = addDouble(fibra, other.fibra);
		vitA = addDouble(vitA, other.vitA);
		acidoAscorbico = addDouble(acidoAscorbico, other.acidoAscorbico);
		acidoFolico = addDouble(acidoFolico, other.acidoFolico);
		calcio = addDouble(calcio, other.calcio);
		hierro = addDouble(hierro, other.hierro);
		hierroNoHem = addDouble(hierroNoHem, other.hierroNoHem);
		sodio = addDouble(sodio, other.sodio);
		potasio = addDouble(potasio, other.potasio);
		fosforo = addDouble(fosforo, other.fosforo);
		selenio = addDouble(selenio, other.selenio);
		colesterol = addDouble(colesterol, other.colesterol);
		agSaturados = addDouble(agSaturados, other.agSaturados);
		agMonoinsaturados = addDouble(agMonoinsaturados, other.agMonoinsaturados);
		agPoliinsaturados = addDouble(agPoliinsaturados, other.agPoliinsaturados);
		azucarPorEquivalente = addDouble(azucarPorEquivalente, other.azucarPorEquivalente);
		indiceGlicemico = addDouble(indiceGlicemico, other.indiceGlicemico);
		cargaGlicemica = addDouble(cargaGlicemica, other.cargaGlicemica);
		etanol = addDouble(etanol, other.etanol);
	}

}
