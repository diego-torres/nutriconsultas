package com.nutriconsultas.alimentos;

import java.util.Map;

import com.nutriconsultas.validation.template.BaseTemplateValidator;

/**
 * Validator for alimento (food) templates. Provides mock variables for food forms and
 * listings.
 */
public class AlimentoTemplateValidator extends BaseTemplateValidator {

	@Override
	public String getTemplatePathPattern() {
		return "sbadmin/alimentos/*";
	}

	@Override
	public Map<String, Object> createMockModelVariables() {
		Map<String, Object> variables = super.createMockModelVariables();

		// Create a new Alimento object with default values
		Alimento alimento = new Alimento();
		alimento.setId(0L);
		alimento.setNombreAlimento("");
		alimento.setClasificacion("");
		alimento.setUnidad("");
		alimento.setCantSugerida(0.0);
		alimento.setEnergia(0);
		alimento.setProteina(0.0);
		alimento.setLipidos(0.0);
		alimento.setHidratosDeCarbono(0.0);
		alimento.setPesoBrutoRedondeado(0);
		alimento.setPesoNeto(0);
		alimento.setFibra(0.0);
		alimento.setVitA(0.0);
		alimento.setAcidoAscorbico(0.0);
		alimento.setHierroNoHem(0.0);
		alimento.setPotasio(0.0);
		alimento.setIndiceGlicemico(0.0);
		alimento.setCargaGlicemica(0.0);
		alimento.setAcidoFolico(0.0);
		alimento.setCalcio(0.0);
		alimento.setHierro(0.0);
		alimento.setSodio(0.0);
		alimento.setAzucarPorEquivalente(0.0);
		alimento.setSelenio(0.0);
		alimento.setFosforo(0.0);
		alimento.setColesterol(0.0);
		alimento.setAgSaturados(0.0);
		alimento.setAgMonoinsaturados(0.0);
		alimento.setAgPoliinsaturados(0.0);
		alimento.setEtanol(0.0);

		variables.put("alimento", alimento);

		// Mock list of alimentos
		variables.put("alimentos", java.util.Collections.emptyList());
		variables.put("alimentosList", java.util.Collections.emptyList());

		return variables;
	}

}
