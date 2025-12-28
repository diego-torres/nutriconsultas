package com.nutriconsultas.dieta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.nutriconsultas.validation.template.BaseTemplateValidator;

/**
 * Validator for dieta (diet) templates. Provides mock variables for diet forms and
 * listings.
 */
public class DietaTemplateValidator extends BaseTemplateValidator {

	@Override
	public String getTemplatePathPattern() {
		return "sbadmin/dietas/*";
	}

	@Override
	public Map<String, Object> createMockModelVariables() {
		Map<String, Object> variables = super.createMockModelVariables();

		// Create mock ingestas list
		List<Ingesta> mockIngestas = new ArrayList<>();
		Ingesta mockIngesta = new Ingesta();
		mockIngesta.setId(1L);
		mockIngesta.setNombre("Desayuno");
		mockIngesta.setEnergia(0);
		mockIngesta.setProteina(0.0);
		mockIngesta.setLipidos(0.0);
		mockIngesta.setHidratosDeCarbono(0.0);
		// Add empty platillos list to ingesta
		mockIngesta.setPlatillos(new ArrayList<>());
		mockIngestas.add(mockIngesta);

		// Create Dieta object with ingestas
		Dieta mockDieta = new Dieta();
		mockDieta.setId(0L);
		mockDieta.setNombre("");
		mockDieta.setEnergia(0);
		mockDieta.setProteina(0.0);
		mockDieta.setLipidos(0.0);
		mockDieta.setHidratosDeCarbono(0.0);
		mockDieta.setIngestas(mockIngestas);

		variables.put("dieta", mockDieta);

		return variables;
	}

}
