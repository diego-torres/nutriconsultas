package com.nutriconsultas.util;

import com.nutriconsultas.model.AbstractNutrible;

/**
 * Aggregates nutrient fields across {@link AbstractNutrible} instances.
 */
public final class NutrientSummarizer {

	private NutrientSummarizer() {
	}

	public static void resetNutrients(final AbstractNutrible target) {
		target.setAcidoAscorbico(0d);
		target.setAcidoFolico(0d);
		target.setAgMonoinsaturados(0d);
		target.setAgPoliinsaturados(0d);
		target.setAgSaturados(0d);
		target.setAzucarPorEquivalente(0d);
		target.setCalcio(0d);
		target.setCargaGlicemica(0d);
		target.setColesterol(0d);
		target.setEnergia(0);
		target.setFibra(0d);
		target.setFosforo(0d);
		target.setHierro(0d);
		target.setHierroNoHem(0d);
		target.setIndiceGlicemico(0d);
		target.setEtanol(0d);
		target.setHidratosDeCarbono(0d);
		target.setLipidos(0d);
		target.setPotasio(0d);
		target.setProteina(0d);
		target.setSelenio(0d);
		target.setSodio(0d);
		target.setVitA(0d);
		target.setPesoBrutoRedondeado(0);
		target.setPesoNeto(0);
	}

	public static void addNutrients(final AbstractNutrible target, final AbstractNutrible source) {
		if (source.getAcidoAscorbico() != null) {
			target.setAcidoAscorbico(safeDouble(target.getAcidoAscorbico()) + source.getAcidoAscorbico());
		}
		if (source.getAcidoFolico() != null) {
			target.setAcidoFolico(safeDouble(target.getAcidoFolico()) + source.getAcidoFolico());
		}
		if (source.getAgMonoinsaturados() != null) {
			target.setAgMonoinsaturados(safeDouble(target.getAgMonoinsaturados()) + source.getAgMonoinsaturados());
		}
		if (source.getAgPoliinsaturados() != null) {
			target.setAgPoliinsaturados(safeDouble(target.getAgPoliinsaturados()) + source.getAgPoliinsaturados());
		}
		if (source.getAgSaturados() != null) {
			target.setAgSaturados(safeDouble(target.getAgSaturados()) + source.getAgSaturados());
		}
		if (source.getAzucarPorEquivalente() != null) {
			target.setAzucarPorEquivalente(
					safeDouble(target.getAzucarPorEquivalente()) + source.getAzucarPorEquivalente());
		}
		if (source.getCalcio() != null) {
			target.setCalcio(safeDouble(target.getCalcio()) + source.getCalcio());
		}
		if (source.getCargaGlicemica() != null) {
			target.setCargaGlicemica(safeDouble(target.getCargaGlicemica()) + source.getCargaGlicemica());
		}
		if (source.getColesterol() != null) {
			target.setColesterol(safeDouble(target.getColesterol()) + source.getColesterol());
		}
		if (source.getEnergia() != null) {
			target.setEnergia(safeInt(target.getEnergia()) + source.getEnergia());
		}
		if (source.getFibra() != null) {
			target.setFibra(safeDouble(target.getFibra()) + source.getFibra());
		}
		if (source.getFosforo() != null) {
			target.setFosforo(safeDouble(target.getFosforo()) + source.getFosforo());
		}
		if (source.getHierro() != null) {
			target.setHierro(safeDouble(target.getHierro()) + source.getHierro());
		}
		if (source.getHierroNoHem() != null) {
			target.setHierroNoHem(safeDouble(target.getHierroNoHem()) + source.getHierroNoHem());
		}
		if (source.getIndiceGlicemico() != null) {
			target.setIndiceGlicemico(safeDouble(target.getIndiceGlicemico()) + source.getIndiceGlicemico());
		}
		if (source.getEtanol() != null) {
			target.setEtanol(safeDouble(target.getEtanol()) + source.getEtanol());
		}
		if (source.getHidratosDeCarbono() != null) {
			target.setHidratosDeCarbono(safeDouble(target.getHidratosDeCarbono()) + source.getHidratosDeCarbono());
		}
		if (source.getLipidos() != null) {
			target.setLipidos(safeDouble(target.getLipidos()) + source.getLipidos());
		}
		if (source.getPotasio() != null) {
			target.setPotasio(safeDouble(target.getPotasio()) + source.getPotasio());
		}
		if (source.getProteina() != null) {
			target.setProteina(safeDouble(target.getProteina()) + source.getProteina());
		}
		if (source.getSelenio() != null) {
			target.setSelenio(safeDouble(target.getSelenio()) + source.getSelenio());
		}
		if (source.getSodio() != null) {
			target.setSodio(safeDouble(target.getSodio()) + source.getSodio());
		}
		if (source.getVitA() != null) {
			target.setVitA(safeDouble(target.getVitA()) + source.getVitA());
		}
		if (source.getPesoBrutoRedondeado() != null) {
			target.setPesoBrutoRedondeado(safeInt(target.getPesoBrutoRedondeado()) + source.getPesoBrutoRedondeado());
		}
		if (source.getPesoNeto() != null) {
			target.setPesoNeto(safeInt(target.getPesoNeto()) + source.getPesoNeto());
		}
	}

	public static void copyScaled(final AbstractNutrible source, final AbstractNutrible target,
			final double multiplier) {
		target.setAcidoAscorbico(scaleNullable(source.getAcidoAscorbico(), multiplier));
		target.setAcidoFolico(scaleNullable(source.getAcidoFolico(), multiplier));
		target.setAgMonoinsaturados(scaleNullable(source.getAgMonoinsaturados(), multiplier));
		target.setAgPoliinsaturados(scaleNullable(source.getAgPoliinsaturados(), multiplier));
		target.setAgSaturados(scaleNullable(source.getAgSaturados(), multiplier));
		target.setAzucarPorEquivalente(scaleNullable(source.getAzucarPorEquivalente(), multiplier));
		target.setCalcio(scaleNullable(source.getCalcio(), multiplier));
		target.setCargaGlicemica(scaleNullable(source.getCargaGlicemica(), multiplier));
		target.setColesterol(scaleNullable(source.getColesterol(), multiplier));
		target.setEnergia(source.getEnergia() != null ? (int) (source.getEnergia() * multiplier) : null);
		target.setFibra(scaleNullable(source.getFibra(), multiplier));
		target.setFosforo(scaleNullable(source.getFosforo(), multiplier));
		target.setHierro(scaleNullable(source.getHierro(), multiplier));
		target.setHierroNoHem(scaleNullable(source.getHierroNoHem(), multiplier));
		target.setIndiceGlicemico(scaleNullable(source.getIndiceGlicemico(), multiplier));
		target.setEtanol(scaleNullable(source.getEtanol(), multiplier));
		target.setHidratosDeCarbono(scaleNullable(source.getHidratosDeCarbono(), multiplier));
		target.setLipidos(scaleNullable(source.getLipidos(), multiplier));
		target.setPotasio(scaleNullable(source.getPotasio(), multiplier));
		target.setProteina(scaleNullable(source.getProteina(), multiplier));
		target.setSelenio(scaleNullable(source.getSelenio(), multiplier));
		target.setSodio(scaleNullable(source.getSodio(), multiplier));
		target.setVitA(scaleNullable(source.getVitA(), multiplier));
		target.setPesoBrutoRedondeado(source.getPesoBrutoRedondeado() != null
				? (int) Math.round(source.getPesoBrutoRedondeado() * multiplier) : null);
		target.setPesoNeto(source.getPesoNeto() != null ? (int) Math.round(source.getPesoNeto() * multiplier) : null);
	}

	private static Double scaleNullable(final Double value, final double multiplier) {
		return value != null ? value * multiplier : null;
	}

	private static double safeDouble(final Double value) {
		return value != null ? value : 0d;
	}

	private static int safeInt(final Integer value) {
		return value != null ? value : 0;
	}

}
