package com.nutriconsultas.dieta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.nutriconsultas.validation.template.BaseTemplateValidator;

/**
 * Validator for dieta (diet) templates. Provides mock variables for diet forms and
 * listings. Includes validation for chart functionality with distribution calculations.
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

		// Add mock alimentos for more complete validation
		List<AlimentoIngesta> mockAlimentos = new ArrayList<>();
		AlimentoIngesta mockAlimento = new AlimentoIngesta();
		mockAlimento.setId(1L);
		mockAlimento.setName("Alimento de ejemplo");
		mockAlimento.setPortions(1);
		mockAlimento.setEnergia(100);
		mockAlimento.setProteina(5.0);
		mockAlimento.setLipidos(3.0);
		mockAlimento.setHidratosDeCarbono(12.0);
		mockAlimentos.add(mockAlimento);
		mockIngesta.setAlimentos(mockAlimentos);

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

		// Calculate distribution percentages for chart validation
		// Using the same calculation logic as DietaController
		double totalProteina = calculateTotalProteina(mockDieta);
		double totalLipidos = calculateTotalLipidos(mockDieta);
		double totalHidratosDeCarbono = calculateTotalHidratosDeCarbono(mockDieta);
		double kCal = totalProteina * 4 + totalLipidos * 9 + totalHidratosDeCarbono * 4;

		if (kCal > 0.01) {
			double distProteina = totalProteina * 4 / kCal * 100;
			double distLipido = totalLipidos * 9 / kCal * 100;
			double distHidratoCarbono = totalHidratosDeCarbono * 4 / kCal * 100;
			variables.put("distribucionProteina", distProteina);
			variables.put("distribucionLipido", distLipido);
			variables.put("distribucionHidratoCarbono", distHidratoCarbono);
			variables.put("hasDistribucion", true);
		}
		else {
			variables.put("hasDistribucion", false);
		}

		return variables;
	}

	/**
	 * Calculates total protein from all ingestas (platillos and alimentos). Uses the same
	 * logic as DietaController.getTotalProteina().
	 * @param dieta the dieta to calculate from
	 * @return total protein in grams
	 */
	private double calculateTotalProteina(Dieta dieta) {
		return dieta.getIngestas()
			.stream()
			.mapToDouble(i -> i.getPlatillos()
				.stream()
				.mapToDouble(p -> p.getProteina() != null ? p.getProteina() : 0.0)
				.sum()
					+ i.getAlimentos().stream().mapToDouble(a -> a.getProteina() != null ? a.getProteina() : 0.0).sum())
			.sum();
	}

	/**
	 * Calculates total lipids from all ingestas (platillos and alimentos). Uses the same
	 * logic as DietaController.getTotalLipidos().
	 * @param dieta the dieta to calculate from
	 * @return total lipids in grams
	 */
	private double calculateTotalLipidos(Dieta dieta) {
		return dieta.getIngestas()
			.stream()
			.mapToDouble(i -> i.getPlatillos()
				.stream()
				.mapToDouble(p -> p.getLipidos() != null ? p.getLipidos() : 0.0)
				.sum()
					+ i.getAlimentos().stream().mapToDouble(a -> a.getLipidos() != null ? a.getLipidos() : 0.0).sum())
			.sum();
	}

	/**
	 * Calculates total carbohydrates from all ingestas (platillos and alimentos). Uses
	 * the same logic as DietaController.getTotalHidratosDeCarbono().
	 * @param dieta the dieta to calculate from
	 * @return total carbohydrates in grams
	 */
	private double calculateTotalHidratosDeCarbono(Dieta dieta) {
		return dieta.getIngestas()
			.stream()
			.mapToDouble(i -> i.getPlatillos()
				.stream()
				.mapToDouble(p -> p.getHidratosDeCarbono() != null ? p.getHidratosDeCarbono() : 0.0)
				.sum()
					+ i.getAlimentos()
						.stream()
						.mapToDouble(a -> a.getHidratosDeCarbono() != null ? a.getHidratosDeCarbono() : 0.0)
						.sum())
			.sum();
	}

}
