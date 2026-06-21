package com.nutriconsultas.util;

import java.util.Objects;

import com.nutriconsultas.alimentos.Alimento;
import com.nutriconsultas.dieta.IngredientePlatilloIngesta;
import com.nutriconsultas.model.AbstractFraccionable;
import com.nutriconsultas.platillos.Ingrediente;

import lombok.extern.slf4j.Slf4j;

/**
 * Builds ingredient nutrient snapshots from catalog {@link Alimento} rows, mirroring
 * platillo catalog ingredient calculations.
 */
@Slf4j
public final class IngredienteFromAlimentoCalculator {

	private IngredienteFromAlimentoCalculator() {
	}

	public static void populateFromAlimento(final IngredientePlatilloIngesta ingrediente, final Alimento alimento,
			final String cantidad, final Integer peso) {
		ingrediente.setAlimento(alimento);
		ingrediente.setUnidad(alimento.getUnidad());
		ingrediente.setDescription(alimento.getNombreAlimento());
		populateNutrientsFromAlimento(ingrediente, alimento, cantidad, peso);
	}

	public static void populateCatalogIngredienteFromAlimento(final Ingrediente ingrediente, final Alimento alimento,
			final String cantidad, final Integer peso) {
		populateNutrientsFromAlimento(ingrediente, alimento, cantidad, peso);
	}

	private static void populateNutrientsFromAlimento(final AbstractFraccionable ingrediente, final Alimento alimento,
			final String cantidad, final Integer peso) {
		boolean calculatedFromCantidad = false;
		if (!cantidad.equals(alimento.getFractionalCantSugerida())) {
			log.debug("Calculating ingredient from cantidad change");
			calculateFromCantidadChange(cantidad, ingrediente, alimento);
			calculatedFromCantidad = true;
		}
		else {
			copyAlimentoDefaults(ingrediente, alimento);
		}

		if (!Objects.equals(peso, alimento.getPesoNeto()) && !calculatedFromCantidad) {
			log.debug("Calculating ingredient from peso change");
			calculateFromPesoChange(peso, ingrediente, alimento);
		}
	}

	private static void copyAlimentoDefaults(final AbstractFraccionable ingrediente, final Alimento alimento) {
		ingrediente.setCantSugerida(alimento.getCantSugerida());
		ingrediente.setAcidoAscorbico(alimento.getAcidoAscorbico());
		ingrediente.setAcidoFolico(alimento.getAcidoFolico());
		ingrediente.setAgMonoinsaturados(alimento.getAgMonoinsaturados());
		ingrediente.setAgPoliinsaturados(alimento.getAgPoliinsaturados());
		ingrediente.setAgSaturados(alimento.getAgSaturados());
		ingrediente.setAzucarPorEquivalente(alimento.getAzucarPorEquivalente());
		ingrediente.setCalcio(alimento.getCalcio());
		ingrediente.setCargaGlicemica(alimento.getCargaGlicemica());
		ingrediente.setColesterol(alimento.getColesterol());
		ingrediente.setEnergia(alimento.getEnergia());
		ingrediente.setFibra(alimento.getFibra());
		ingrediente.setFosforo(alimento.getFosforo());
		ingrediente.setHierro(alimento.getHierro());
		ingrediente.setHierroNoHem(alimento.getHierroNoHem());
		ingrediente.setIndiceGlicemico(alimento.getIndiceGlicemico());
		ingrediente.setEtanol(alimento.getEtanol());
		ingrediente.setHidratosDeCarbono(alimento.getHidratosDeCarbono());
		ingrediente.setLipidos(alimento.getLipidos());
		ingrediente.setPotasio(alimento.getPotasio());
		ingrediente.setProteina(alimento.getProteina());
		ingrediente.setSelenio(alimento.getSelenio());
		ingrediente.setSodio(alimento.getSodio());
		ingrediente.setVitA(alimento.getVitA());
		ingrediente.setPesoBrutoRedondeado(alimento.getPesoBrutoRedondeado());
		ingrediente.setPesoNeto(alimento.getPesoNeto());
	}

	private static void calculateFromCantidadChange(final String given, final AbstractFraccionable ingrediente,
			final Alimento alimento) {
		final double parsedQuantity = parseFractionalQuantity(given);
		final double factor = parsedQuantity / alimento.getCantSugerida();
		applyNutrientFactor(ingrediente, alimento, factor);
		ingrediente.setCantSugerida(parsedQuantity);
	}

	private static void calculateFromPesoChange(final Integer given, final AbstractFraccionable ingrediente,
			final Alimento alimento) {
		final double factor = (double) given / (double) alimento.getPesoNeto();
		applyNutrientFactor(ingrediente, alimento, factor);
		ingrediente.setCantSugerida(factor * alimento.getCantSugerida());
	}

	private static void applyNutrientFactor(final AbstractFraccionable ingrediente, final Alimento alimento,
			final double factor) {
		if (alimento.getAcidoAscorbico() != null) {
			ingrediente.setAcidoAscorbico(alimento.getAcidoAscorbico() * factor);
		}
		if (alimento.getAcidoFolico() != null) {
			ingrediente.setAcidoFolico(alimento.getAcidoFolico() * factor);
		}
		if (alimento.getAgMonoinsaturados() != null) {
			ingrediente.setAgMonoinsaturados(alimento.getAgMonoinsaturados() * factor);
		}
		if (alimento.getAgPoliinsaturados() != null) {
			ingrediente.setAgPoliinsaturados(alimento.getAgPoliinsaturados() * factor);
		}
		if (alimento.getAgSaturados() != null) {
			ingrediente.setAgSaturados(alimento.getAgSaturados() * factor);
		}
		if (alimento.getAzucarPorEquivalente() != null) {
			ingrediente.setAzucarPorEquivalente(alimento.getAzucarPorEquivalente() * factor);
		}
		if (alimento.getCalcio() != null) {
			ingrediente.setCalcio(alimento.getCalcio() * factor);
		}
		if (alimento.getCargaGlicemica() != null) {
			ingrediente.setCargaGlicemica(alimento.getCargaGlicemica() * factor);
		}
		if (alimento.getColesterol() != null) {
			ingrediente.setColesterol(alimento.getColesterol() * factor);
		}
		if (alimento.getEnergia() != null) {
			ingrediente.setEnergia((int) Math.round(alimento.getEnergia() * factor));
		}
		if (alimento.getFibra() != null) {
			ingrediente.setFibra(alimento.getFibra() * factor);
		}
		if (alimento.getFosforo() != null) {
			ingrediente.setFosforo(alimento.getFosforo() * factor);
		}
		if (alimento.getHierro() != null) {
			ingrediente.setHierro(alimento.getHierro() * factor);
		}
		if (alimento.getHierroNoHem() != null) {
			ingrediente.setHierroNoHem(alimento.getHierroNoHem() * factor);
		}
		if (alimento.getIndiceGlicemico() != null) {
			ingrediente.setIndiceGlicemico(alimento.getIndiceGlicemico() * factor);
		}
		if (alimento.getEtanol() != null) {
			ingrediente.setEtanol(alimento.getEtanol() * factor);
		}
		if (alimento.getHidratosDeCarbono() != null) {
			ingrediente.setHidratosDeCarbono(alimento.getHidratosDeCarbono() * factor);
		}
		if (alimento.getLipidos() != null) {
			ingrediente.setLipidos(alimento.getLipidos() * factor);
		}
		if (alimento.getPotasio() != null) {
			ingrediente.setPotasio(alimento.getPotasio() * factor);
		}
		if (alimento.getProteina() != null) {
			ingrediente.setProteina(alimento.getProteina() * factor);
		}
		if (alimento.getSelenio() != null) {
			ingrediente.setSelenio(alimento.getSelenio() * factor);
		}
		if (alimento.getSodio() != null) {
			ingrediente.setSodio(alimento.getSodio() * factor);
		}
		if (alimento.getVitA() != null) {
			ingrediente.setVitA(alimento.getVitA() * factor);
		}
		if (alimento.getPesoBrutoRedondeado() != null) {
			ingrediente.setPesoBrutoRedondeado((int) Math.round(alimento.getPesoBrutoRedondeado() * factor));
		}
		if (alimento.getPesoNeto() != null) {
			ingrediente.setPesoNeto((int) Math.round(alimento.getPesoNeto() * factor));
		}
	}

	private static double parseFractionalQuantity(final String given) {
		final String trimmedGiven = given.trim();
		final boolean hasInteger = trimmedGiven.contains(" ") || !trimmedGiven.contains("/");
		final boolean hasFraction = trimmedGiven.contains("/");
		final int givenIntPart = hasInteger ? Integer.parseInt(trimmedGiven.split(" ")[0]) : 0;
		final int givenNumeratorPart = hasInteger
				? hasFraction ? Integer.parseInt(trimmedGiven.split(" ")[1].split("/")[0]) : 0
				: Integer.parseInt(trimmedGiven.split("/")[0]);
		final int givenDenominatorPart = hasInteger
				? hasFraction ? Integer.parseInt(trimmedGiven.split(" ")[1].split("/")[1]) : 0
				: Integer.parseInt(trimmedGiven.split("/")[1]);
		return givenIntPart + (hasFraction ? (givenNumeratorPart / (double) givenDenominatorPart) : 0d);
	}

}
