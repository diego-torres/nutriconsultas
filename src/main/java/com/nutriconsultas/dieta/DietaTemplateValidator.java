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
		// Add mock platillo with numeric values for validation
		List<PlatilloIngesta> mockPlatillos = new ArrayList<>();
		PlatilloIngesta mockPlatillo = new PlatilloIngesta();
		mockPlatillo.setId(1L);
		mockPlatillo.setName("Platillo de ejemplo");
		mockPlatillo.setPortions(1);
		mockPlatillo.setEnergia(250);
		mockPlatillo.setProteina(15.75);
		mockPlatillo.setLipidos(8.5);
		mockPlatillo.setHidratosDeCarbono(30.25);
		mockPlatillos.add(mockPlatillo);
		mockIngesta.setPlatillos(mockPlatillos);
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
