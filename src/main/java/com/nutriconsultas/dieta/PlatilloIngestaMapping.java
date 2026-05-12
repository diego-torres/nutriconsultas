package com.nutriconsultas.dieta;

import com.nutriconsultas.platillos.Ingrediente;
import com.nutriconsultas.platillos.Platillo;

/**
 * Maps catalog {@link Platillo} / {@link Ingrediente} rows into persisted ingesta
 * structures, mirroring {@link DietaController#savePlatillo}.
 */
public final class PlatilloIngestaMapping {

	private PlatilloIngestaMapping() {
	}

	public static PlatilloIngesta mapPlatilloIngesta(final Platillo platillo) {
		final PlatilloIngesta platilloIngesta = new PlatilloIngesta();
		platilloIngesta.setName(platillo.getName());
		platilloIngesta.setRecommendations(platillo.getDescription());
		platilloIngesta.setVideoUrl(platillo.getVideoUrl());
		platilloIngesta.setPdfUrl(platillo.getPdfUrl());
		platilloIngesta.setImageUrl(platillo.getImageUrl());

		platilloIngesta.setAcidoAscorbico(platillo.getAcidoAscorbico());
		platilloIngesta.setAcidoFolico(platillo.getAcidoFolico());
		platilloIngesta.setAgMonoinsaturados(platillo.getAgMonoinsaturados());
		platilloIngesta.setAgPoliinsaturados(platillo.getAgPoliinsaturados());
		platilloIngesta.setAgSaturados(platillo.getAgSaturados());
		platilloIngesta.setCalcio(platillo.getCalcio());
		platilloIngesta.setCargaGlicemica(platillo.getCargaGlicemica());
		platilloIngesta.setColesterol(platillo.getColesterol());
		platilloIngesta.setEnergia(platillo.getEnergia());
		platilloIngesta.setFibra(platillo.getFibra());
		platilloIngesta.setHierro(platillo.getHierro());
		platilloIngesta.setHierroNoHem(platillo.getHierroNoHem());
		platilloIngesta.setHidratosDeCarbono(platillo.getHidratosDeCarbono());
		platilloIngesta.setIndiceGlicemico(platillo.getIndiceGlicemico());
		platilloIngesta.setLipidos(platillo.getLipidos());
		platilloIngesta.setPesoBrutoRedondeado(platillo.getPesoBrutoRedondeado());
		platilloIngesta.setPesoNeto(platillo.getPesoNeto());
		platilloIngesta.setPotasio(platillo.getPotasio());
		platilloIngesta.setProteina(platillo.getProteina());
		platilloIngesta.setSodio(platillo.getSodio());
		platilloIngesta.setSelenio(platillo.getSelenio());
		platilloIngesta.setVitA(platillo.getVitA());
		platilloIngesta.setAzucarPorEquivalente(platillo.getAzucarPorEquivalente());
		platilloIngesta.setEtanol(platillo.getEtanol());
		platilloIngesta.setFosforo(platillo.getFosforo());

		return platilloIngesta;
	}

	public static IngredientePlatilloIngesta mapFromIngredienteToIngredientePlatilloIngesta(
			final Ingrediente ingrediente) {
		final IngredientePlatilloIngesta result = new IngredientePlatilloIngesta();
		result.setDescription(ingrediente.getDescription());
		result.setCantSugerida(ingrediente.getCantSugerida());
		result.setAlimento(ingrediente.getAlimento());
		result.setUnidad(ingrediente.getUnidad());

		result.setEnergia(ingrediente.getEnergia());
		result.setProteina(ingrediente.getProteina());
		result.setLipidos(ingrediente.getLipidos());
		result.setHidratosDeCarbono(ingrediente.getHidratosDeCarbono());

		result.setPesoBrutoRedondeado(ingrediente.getPesoBrutoRedondeado());
		result.setPesoNeto(ingrediente.getPesoNeto());
		result.setFibra(ingrediente.getFibra());
		result.setVitA(ingrediente.getVitA());
		result.setAcidoAscorbico(ingrediente.getAcidoAscorbico());
		result.setHierroNoHem(ingrediente.getHierroNoHem());
		result.setPotasio(ingrediente.getPotasio());
		result.setIndiceGlicemico(ingrediente.getIndiceGlicemico());
		result.setCargaGlicemica(ingrediente.getCargaGlicemica());
		result.setAcidoFolico(ingrediente.getAcidoFolico());
		result.setCalcio(ingrediente.getCalcio());
		result.setHierro(ingrediente.getHierro());
		result.setSodio(ingrediente.getSodio());
		result.setAzucarPorEquivalente(ingrediente.getAzucarPorEquivalente());
		result.setSelenio(ingrediente.getSelenio());
		result.setFosforo(ingrediente.getFosforo());
		result.setColesterol(ingrediente.getColesterol());
		result.setAgSaturados(ingrediente.getAgSaturados());
		result.setAgMonoinsaturados(ingrediente.getAgMonoinsaturados());
		result.setAgPoliinsaturados(ingrediente.getAgPoliinsaturados());
		result.setEtanol(ingrediente.getEtanol());

		return result;
	}

}
