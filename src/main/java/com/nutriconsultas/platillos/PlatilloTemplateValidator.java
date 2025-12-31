package com.nutriconsultas.platillos;

import java.util.Map;

import com.nutriconsultas.validation.template.BaseTemplateValidator;

/**
 * Validator for platillo (dish/meal) templates. Provides mock variables for dish forms
 * and listings.
 */
public class PlatilloTemplateValidator extends BaseTemplateValidator {

	@Override
	public String getTemplatePathPattern() {
		return "sbadmin/platillos/*";
	}

	@Override
	public Map<String, Object> createMockModelVariables() {
		Map<String, Object> variables = super.createMockModelVariables();

		// Create a new Platillo object with default values
		Platillo platillo = new Platillo();
		platillo.setId(0L);
		platillo.setName("");
		platillo.setDescription("");
		platillo.setEnergia(0);
		platillo.setProteina(0.0);
		platillo.setLipidos(0.0);
		platillo.setHidratosDeCarbono(0.0);
		platillo.setAzucarPorEquivalente(0.0);
		// Set imageUrl to null to test default image path behavior
		// Template will use /sbadmin/img/plato-vacio.jpg when imageUrl is null or empty
		platillo.setImageUrl(null);
		platillo.setPdfUrl("");
		platillo.setVideoUrl("");

		variables.put("platillo", platillo);

		// Mock list of alimentos for selection
		variables.put("alimentosList", java.util.Collections.emptyList());

		// Mock list of ingestas
		variables.put("ingestas", java.util.Collections.emptyList());

		return variables;
	}

}
