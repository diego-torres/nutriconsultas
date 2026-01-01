package com.nutriconsultas.dieta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.nutriconsultas.validation.template.BaseTemplateValidator;

/**
 * Validator for dieta (diet) templates. Provides mock variables for diet forms and
 * listings. Includes validation for chart functionality with distribution calculations.
 *
 * <p>
 * This validator supports validation of the printable template
 * ({@code sbadmin/dietas/printable.html}) for both assigned and unassigned dietas:
 * <ul>
 * <li><b>Unassigned Dieta Scenario:</b> Sets {@code paciente} and {@code pacienteDieta}
 * to null, validating that the template correctly handles the case when no patient
 * assignment exists.</li>
 * <li><b>Assigned Dieta Scenario:</b> While the validator currently only tests the
 * unassigned scenario (for simplicity), the template's conditional rendering
 * ({@code th:if="${paciente != null}"}) ensures it works correctly when patient variables
 * are provided.</li>
 * </ul>
 *
 * <p>
 * The printable template uses conditional rendering to show/hide patient-specific
 * sections, allowing a single template to serve both use cases.
 */
public class DietaTemplateValidator extends BaseTemplateValidator {

	@Override
	public String getTemplatePathPattern() {
		return "sbadmin/dietas/*";
	}

	@Override
	public Map<String, Object> createMockModelVariables() {
		final Map<String, Object> variables = super.createMockModelVariables();

		// Create mock ingestas list
		final List<Ingesta> mockIngestas = createMockIngestas();

		// Create Dieta object with ingestas
		final Dieta mockDieta = createMockDieta(mockIngestas);

		variables.put("dieta", mockDieta);
		variables.put("ingestas", mockIngestas);

		// Add minId for formulario template (minimum ingesta id)
		variables.put("minId", mockIngestas.isEmpty() ? 0L : mockIngestas.get(0).getId());

		// Add platillos and alimentos lists for formulario template
		variables.put("platillos", createMockPlatillosList());
		variables.put("alimentos", createMockAlimentosList());

		// Calculate distribution percentages for chart validation
		addDistributionVariables(variables, mockDieta, mockIngestas);

		// Add variables for printable template
		addPrintableTemplateVariables(variables, mockDieta);

		return variables;
	}

	/**
	 * Creates a list of mock ingestas with platillos and alimentos for template validation.
	 * @return list of mock ingestas
	 */
	private List<Ingesta> createMockIngestas() {
		final List<Ingesta> mockIngestas = new ArrayList<>();
		final Ingesta mockIngesta = new Ingesta();
		mockIngesta.setId(1L);
		mockIngesta.setNombre("Desayuno");
		mockIngesta.setEnergia(0);
		mockIngesta.setProteina(0.0);
		mockIngesta.setLipidos(0.0);
		mockIngesta.setHidratosDeCarbono(0.0);
		mockIngesta.setPlatillos(createMockPlatillos());
		mockIngesta.setAlimentos(createMockAlimentos());
		mockIngestas.add(mockIngesta);
		return mockIngestas;
	}

	/**
	 * Creates a list of mock platillos for ingesta validation.
	 * @return list of mock platillos
	 */
	private List<PlatilloIngesta> createMockPlatillos() {
		final List<PlatilloIngesta> mockPlatillos = new ArrayList<>();
		final PlatilloIngesta mockPlatillo = new PlatilloIngesta();
		mockPlatillo.setId(1L);
		mockPlatillo.setName("Platillo de ejemplo");
		mockPlatillo.setPortions(1);
		mockPlatillo.setEnergia(250);
		mockPlatillo.setProteina(15.75);
		mockPlatillo.setLipidos(8.5);
		mockPlatillo.setHidratosDeCarbono(30.25);
		mockPlatillo.setIngredientes(createMockIngredientes(mockPlatillo));
		mockPlatillos.add(mockPlatillo);
		return mockPlatillos;
	}

	/**
	 * Creates a list of mock ingredientes for platillo validation.
	 * @param platillo the platillo to associate with ingredientes
	 * @return list of mock ingredientes
	 */
	private List<IngredientePlatilloIngesta> createMockIngredientes(final PlatilloIngesta platillo) {
		final List<IngredientePlatilloIngesta> mockIngredientes = new ArrayList<>();
		final IngredientePlatilloIngesta mockIngrediente = new IngredientePlatilloIngesta();
		mockIngrediente.setId(1L);
		mockIngrediente.setDescription("Ingrediente de ejemplo");
		mockIngrediente.setCantSugerida(1.5); // Test fractional value (1 1/2)
		mockIngrediente.setUnidad("taza");
		// Create mock Alimento for the ingrediente
		final com.nutriconsultas.alimentos.Alimento mockAlimentoIngrediente = new com.nutriconsultas.alimentos.Alimento();
		mockAlimentoIngrediente.setId(1L);
		mockAlimentoIngrediente.setNombreAlimento("Alimento ingrediente");
		mockIngrediente.setAlimento(mockAlimentoIngrediente);
		mockIngrediente.setPlatillo(platillo);
		mockIngredientes.add(mockIngrediente);
		return mockIngredientes;
	}

	/**
	 * Creates a list of mock alimentos for ingesta validation.
	 * @return list of mock alimentos
	 */
	private List<AlimentoIngesta> createMockAlimentos() {
		final List<AlimentoIngesta> mockAlimentos = new ArrayList<>();
		final AlimentoIngesta mockAlimento = new AlimentoIngesta();
		mockAlimento.setId(1L);
		mockAlimento.setName("Alimento de ejemplo");
		mockAlimento.setPortions(1);
		mockAlimento.setEnergia(100);
		mockAlimento.setProteina(5.0);
		mockAlimento.setLipidos(3.0);
		mockAlimento.setHidratosDeCarbono(12.0);
		mockAlimento.setUnidad("taza");
		// Create mock Alimento with cantidad sugerida for template validation
		final com.nutriconsultas.alimentos.Alimento mockAlimentoEntity = new com.nutriconsultas.alimentos.Alimento();
		mockAlimentoEntity.setId(1L);
		mockAlimentoEntity.setNombreAlimento("Alimento de ejemplo");
		mockAlimentoEntity.setCantSugerida(1.5); // Test fractional value (1 1/2)
		mockAlimentoEntity.setUnidad("taza");
		mockAlimento.setAlimento(mockAlimentoEntity);
		mockAlimentos.add(mockAlimento);
		return mockAlimentos;
	}

	/**
	 * Creates a mock Dieta object with the provided ingestas.
	 * @param mockIngestas the ingestas to add to the dieta
	 * @return mock Dieta object
	 */
	private Dieta createMockDieta(final List<Ingesta> mockIngestas) {
		final Dieta mockDieta = new Dieta();
		mockDieta.setId(0L);
		mockDieta.setNombre("");
		mockDieta.setEnergia(0);
		mockDieta.setProteina(0.0);
		mockDieta.setLipidos(0.0);
		mockDieta.setHidratosDeCarbono(0.0);
		mockDieta.setIngestas(mockIngestas);
		return mockDieta;
	}

	/**
	 * Creates a list of mock platillos for the formulario template dropdown.
	 * @return list of mock platillos
	 */
	private List<com.nutriconsultas.platillos.Platillo> createMockPlatillosList() {
		final List<com.nutriconsultas.platillos.Platillo> mockPlatillosList = new ArrayList<>();
		final com.nutriconsultas.platillos.Platillo mockPlatilloEntity = new com.nutriconsultas.platillos.Platillo();
		mockPlatilloEntity.setId(1L);
		mockPlatilloEntity.setName("Platillo disponible");
		mockPlatillosList.add(mockPlatilloEntity);
		return mockPlatillosList;
	}

	/**
	 * Creates a list of mock alimentos for the formulario template dropdown.
	 * @return list of mock alimentos
	 */
	private List<com.nutriconsultas.alimentos.Alimento> createMockAlimentosList() {
		final List<com.nutriconsultas.alimentos.Alimento> mockAlimentosList = new ArrayList<>();
		final com.nutriconsultas.alimentos.Alimento mockAlimentoEntityForList = new com.nutriconsultas.alimentos.Alimento();
		mockAlimentoEntityForList.setId(1L);
		mockAlimentoEntityForList.setNombreAlimento("Alimento disponible");
		mockAlimentosList.add(mockAlimentoEntityForList);
		return mockAlimentosList;
	}

	/**
	 * Adds distribution variables for chart validation to the variables map.
	 * @param variables the variables map to add to
	 * @param mockDieta the mock dieta to calculate from
	 * @param mockIngestas the mock ingestas for totals calculation
	 */
	private void addDistributionVariables(final Map<String, Object> variables, final Dieta mockDieta,
			final List<Ingesta> mockIngestas) {
		// Calculate distribution percentages for chart validation
		// Using the same calculation logic as DietaController
		final double totalProteina = calculateTotalProteina(mockDieta);
		final double totalLipidos = calculateTotalLipidos(mockDieta);
		final double totalHidratosDeCarbono = calculateTotalHidratosDeCarbono(mockDieta);

		// Add ingesta totals map for printable template
		final java.util.Map<Long, DietaPdfService.IngestaNutritionalTotals> ingestaTotals = new java.util.HashMap<>();
		for (final Ingesta ingesta : mockIngestas) {
			final DietaPdfService.IngestaNutritionalTotals totals = new DietaPdfService.IngestaNutritionalTotals();
			totals.setTotalEnergia(calculateTotalEnergia(ingesta));
			totals.setTotalProteina(calculateTotalProteina(ingesta));
			totals.setTotalLipidos(calculateTotalLipidos(ingesta));
			totals.setTotalHidratosDeCarbono(calculateTotalHidratosDeCarbono(ingesta));
			ingestaTotals.put(ingesta.getId(), totals);
		}
		variables.put("ingestaTotals", ingestaTotals);

		final double kCal = totalProteina * 4 + totalLipidos * 9 + totalHidratosDeCarbono * 4;

		if (kCal > 0.01) {
			final double distProteina = totalProteina * 4 / kCal * 100;
			final double distLipido = totalLipidos * 9 / kCal * 100;
			final double distHidratoCarbono = totalHidratosDeCarbono * 4 / kCal * 100;
			variables.put("distribucionProteina", distProteina);
			variables.put("distribucionLipido", distLipido);
			variables.put("distribucionHidratoCarbono", distHidratoCarbono);
			variables.put("hasDistribucion", true);
		}
		else {
			variables.put("hasDistribucion", false);
		}
	}

	/**
	 * Adds variables for printable template validation.
	 * @param variables the variables map to add to
	 * @param mockDieta the mock dieta to calculate from
	 */
	private void addPrintableTemplateVariables(final Map<String, Object> variables, final Dieta mockDieta) {
		// Note: Setting these to null validates the "unassigned dieta" scenario.
		// The template uses conditional rendering (th:if="${paciente != null}") to
		// handle both assigned and unassigned cases. When paciente is null, the
		// patient information section is hidden, which is the expected behavior
		// for unassigned dietas.
		variables.put("pacienteDieta", null); // null = unassigned dieta scenario
		variables.put("paciente", null); // null = unassigned dieta scenario
		variables.put("totalEnergia", calculateTotalEnergia(mockDieta));
		final double totalProteina = calculateTotalProteina(mockDieta);
		final double totalLipidos = calculateTotalLipidos(mockDieta);
		final double totalHidratosDeCarbono = calculateTotalHidratosDeCarbono(mockDieta);
		variables.put("totalProteina", totalProteina);
		variables.put("totalLipidos", totalLipidos);
		variables.put("totalHidratosDeCarbono", totalHidratosDeCarbono);
	}

	/**
	 * Calculates total protein from all ingestas (platillos and alimentos). Uses the same
	 * logic as DietaController.getTotalProteina().
	 * @param dieta the dieta to calculate from
	 * @return total protein in grams
	 */
	private double calculateTotalProteina(final Dieta dieta) {
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
	private double calculateTotalLipidos(final Dieta dieta) {
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
	private double calculateTotalHidratosDeCarbono(final Dieta dieta) {
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

	private int calculateTotalEnergia(final Dieta dieta) {
		return dieta.getIngestas()
			.stream()
			.mapToInt(i -> i.getPlatillos().stream().mapToInt(p -> p.getEnergia() != null ? p.getEnergia() : 0).sum()
					+ i.getAlimentos().stream().mapToInt(a -> a.getEnergia() != null ? a.getEnergia() : 0).sum())
			.sum();
	}

	private int calculateTotalEnergia(final Ingesta ingesta) {
		int total = 0;
		if (ingesta.getPlatillos() != null) {
			for (final PlatilloIngesta platillo : ingesta.getPlatillos()) {
				if (platillo.getEnergia() != null) {
					total += platillo.getEnergia();
				}
			}
		}
		if (ingesta.getAlimentos() != null) {
			for (final AlimentoIngesta alimento : ingesta.getAlimentos()) {
				if (alimento.getEnergia() != null) {
					total += alimento.getEnergia();
				}
			}
		}
		return total;
	}

	private double calculateTotalProteina(final Ingesta ingesta) {
		double total = 0.0;
		if (ingesta.getPlatillos() != null) {
			for (final PlatilloIngesta platillo : ingesta.getPlatillos()) {
				if (platillo.getProteina() != null) {
					total += platillo.getProteina();
				}
			}
		}
		if (ingesta.getAlimentos() != null) {
			for (final AlimentoIngesta alimento : ingesta.getAlimentos()) {
				if (alimento.getProteina() != null) {
					total += alimento.getProteina();
				}
			}
		}
		return total;
	}

	private double calculateTotalLipidos(final Ingesta ingesta) {
		double total = 0.0;
		if (ingesta.getPlatillos() != null) {
			for (final PlatilloIngesta platillo : ingesta.getPlatillos()) {
				if (platillo.getLipidos() != null) {
					total += platillo.getLipidos();
				}
			}
		}
		if (ingesta.getAlimentos() != null) {
			for (final AlimentoIngesta alimento : ingesta.getAlimentos()) {
				if (alimento.getLipidos() != null) {
					total += alimento.getLipidos();
				}
			}
		}
		return total;
	}

	private double calculateTotalHidratosDeCarbono(final Ingesta ingesta) {
		double total = 0.0;
		if (ingesta.getPlatillos() != null) {
			for (final PlatilloIngesta platillo : ingesta.getPlatillos()) {
				if (platillo.getHidratosDeCarbono() != null) {
					total += platillo.getHidratosDeCarbono();
				}
			}
		}
		if (ingesta.getAlimentos() != null) {
			for (final AlimentoIngesta alimento : ingesta.getAlimentos()) {
				if (alimento.getHidratosDeCarbono() != null) {
					total += alimento.getHidratosDeCarbono();
				}
			}
		}
		return total;
	}

}
