package com.nutriconsultas.dieta;

import com.nutriconsultas.alimentos.Alimento;

/**
 * Scales catalog {@link Alimento} nutrients onto a standalone {@link AlimentoIngesta}.
 * {@code AlimentoIngesta} does not extend {@code AbstractNutrible}, so
 * {@link com.nutriconsultas.util.NutrientSummarizer} cannot be used directly.
 */
public final class AlimentoIngestaNutrientScaler {

	private AlimentoIngestaNutrientScaler() {
	}

	public static void copyScaled(final Alimento source, final AlimentoIngesta target, final double multiplier) {
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

}
