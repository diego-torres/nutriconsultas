package com.nutriconsultas.dieta;

import java.io.ByteArrayOutputStream;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.xhtmlrenderer.pdf.ITextRenderer;

import com.nutriconsultas.paciente.PacienteDieta;
import com.nutriconsultas.paciente.PacienteDietaRepository;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for generating PDF documents from Dieta entities.
 *
 * <p>
 * This service supports generating PDFs for both assigned and unassigned dietas:
 *
 * <ul>
 * <li><b>Unassigned Dieta:</b> When a dieta has not been assigned to any patient, the PDF
 * will show only the dieta information (name, ingestas, platillos, alimentos, nutritional
 * information) without patient-specific details.</li>
 * <li><b>Assigned Dieta:</b> When a dieta has been assigned to a patient (active
 * assignment), the PDF will include patient information (name, DOB, gender, weight,
 * height), assignment dates, notes, plus all dieta content.</li>
 * <li><b>Generic PDF:</b> When generating a PDF from the diet list (not from a patient's
 * alimentary plan), patient information is excluded even if an assignment exists, resulting
 * in a generic diet template.</li>
 * </ul>
 *
 * <p>
 * The service provides two methods:
 * <ul>
 * <li>{@link #generatePdf(Long)}: Automatically includes patient information if an active
 * assignment exists (for backward compatibility).</li>
 * <li>{@link #generatePdf(Long, boolean)}: Allows explicit control over whether to include
 * patient information.</li>
 * </ul>
 *
 * <p>
 * The template used is {@code sbadmin/dietas/printable.html}, which uses conditional
 * rendering ({@code th:if="${paciente != null}"}) to show/hide patient-specific sections
 * based on whether patient information is provided.
 *
 * @see Dieta
 * @see PacienteDieta
 * @see #generatePdf(Long)
 * @see #generatePdf(Long, boolean)
 */
@Service
@Slf4j
public class DietaPdfService {

	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	public static class IngestaNutritionalTotals {

		private Integer totalEnergia;

		private Double totalProteina;

		private Double totalLipidos;

		private Double totalHidratosDeCarbono;

	}

	@Autowired
	private TemplateEngine templateEngine;

	@Autowired
	private PacienteDietaRepository pacienteDietaRepository;

	@Autowired
	private DietaService dietaService;

	/**
	 * Generates a PDF document for a dieta.
	 *
	 * <p>
	 * This method supports both assigned and unassigned dietas:
	 * <ul>
	 * <li>If the dieta has an active patient assignment, patient information (including
	 * notes from PacienteDieta) will be included in the PDF.</li>
	 * <li>If the dieta is not assigned, only the dieta content will be included.</li>
	 * </ul>
	 *
	 * <p>
	 * The template {@code sbadmin/dietas/printable.html} uses conditional rendering to
	 * handle both cases automatically.
	 * @param dietaId the ID of the dieta to generate PDF for
	 * @return PDF document as byte array
	 * @throws IllegalArgumentException if dieta with the given ID is not found
	 * @throws RuntimeException if PDF generation fails
	 */
	public byte[] generatePdf(@NonNull final Long dietaId) {
		return generatePdf(dietaId, true);
	}

	/**
	 * Generates a PDF document for a dieta with optional patient information.
	 *
	 * <p>
	 * This method allows controlling whether patient information is included in the PDF:
	 * <ul>
	 * <li>If {@code includePatientInfo} is {@code true} and the dieta has an active patient
	 * assignment, patient information (including notes from PacienteDieta) will be included
	 * in the PDF.</li>
	 * <li>If {@code includePatientInfo} is {@code false}, only the dieta content will be
	 * included, regardless of whether an assignment exists.</li>
	 * </ul>
	 *
	 * <p>
	 * The template {@code sbadmin/dietas/printable.html} uses conditional rendering to
	 * handle both cases automatically.
	 * @param dietaId the ID of the dieta to generate PDF for
	 * @param includePatientInfo if {@code true}, includes patient information when an active
	 * assignment exists; if {@code false}, generates a generic PDF without patient
	 * information
	 * @return PDF document as byte array
	 * @throws IllegalArgumentException if dieta with the given ID is not found
	 * @throws RuntimeException if PDF generation fails
	 */
	public byte[] generatePdf(@NonNull final Long dietaId, final boolean includePatientInfo) {
		log.info("Generating PDF for dieta with id: {} (includePatientInfo: {})", dietaId, includePatientInfo);
		final Dieta dieta = dietaService.getDieta(dietaId);
		if (dieta == null) {
			throw new IllegalArgumentException("Dieta with id " + dietaId + " not found");
		}

		// Find patient assignment if exists and if patient info should be included
		// This determines if we're generating a patient-specific or generic PDF
		PacienteDieta activeAssignment = null;
		if (includePatientInfo) {
			final List<PacienteDieta> assignments = pacienteDietaRepository.findByDietaId(dietaId);
			activeAssignment = assignments.stream()
				.filter(a -> a.getStatus() != null && a.getStatus().name().equals("ACTIVE"))
				.findFirst()
				.orElse(null);
		}

		// Prepare context for Thymeleaf template
		// Template will conditionally render patient info based on these variables
		final Context context = new Context();
		context.setVariable("dieta", dieta);
		context.setVariable("pacienteDieta", activeAssignment); // null if unassigned or if
																	// includePatientInfo is false
		context.setVariable("paciente", activeAssignment != null ? activeAssignment.getPaciente() : null);

		// Sort ingestas by id
		final List<Ingesta> sortedIngestas = dieta.getIngestas()
			.stream()
			.sorted(Comparator.comparingLong(Ingesta::getId))
			.collect(Collectors.toList());
		context.setVariable("ingestas", sortedIngestas);

		// Calculate nutritional totals per ingesta and store in a map
		final java.util.Map<Long, IngestaNutritionalTotals> ingestaTotals = new java.util.HashMap<>();
		for (final Ingesta ingesta : sortedIngestas) {
			final IngestaNutritionalTotals totals = new IngestaNutritionalTotals();
			totals.setTotalEnergia(calculateTotalEnergia(ingesta));
			totals.setTotalProteina(calculateTotalProteina(ingesta));
			totals.setTotalLipidos(calculateTotalLipidos(ingesta));
			totals.setTotalHidratosDeCarbono(calculateTotalHidratosDeCarbono(ingesta));
			ingestaTotals.put(ingesta.getId(), totals);
		}
		context.setVariable("ingestaTotals", ingestaTotals);

		// Calculate total nutritional values for the dieta
		context.setVariable("totalEnergia", calculateTotalEnergia(dieta));
		context.setVariable("totalProteina", calculateTotalProteina(dieta));
		context.setVariable("totalLipidos", calculateTotalLipidos(dieta));
		context.setVariable("totalHidratosDeCarbono", calculateTotalHidratosDeCarbono(dieta));

		// Render Thymeleaf template to HTML
		final String html = templateEngine.process("sbadmin/dietas/printable", context);

		// Convert HTML to PDF using Flying Saucer
		return htmlToPdf(html);
	}

	private byte[] htmlToPdf(final String html) {
		try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
			final ITextRenderer renderer = new ITextRenderer();
			renderer.setDocumentFromString(html);
			renderer.layout();
			renderer.createPDF(outputStream);
			return outputStream.toByteArray();
		}
		catch (final Exception e) {
			log.error("Error generating PDF", e);
			throw new RuntimeException("Error generating PDF", e);
		}
	}

	private Integer calculateTotalEnergia(final Ingesta ingesta) {
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

	private Double calculateTotalProteina(final Ingesta ingesta) {
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

	private Double calculateTotalLipidos(final Ingesta ingesta) {
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

	private Double calculateTotalHidratosDeCarbono(final Ingesta ingesta) {
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

	private Integer calculateTotalEnergia(final Dieta dieta) {
		int total = 0;
		if (dieta.getIngestas() != null) {
			for (final Ingesta ingesta : dieta.getIngestas()) {
				total += calculateTotalEnergia(ingesta);
			}
		}
		return total;
	}

	private Double calculateTotalProteina(final Dieta dieta) {
		double total = 0.0;
		if (dieta.getIngestas() != null) {
			for (final Ingesta ingesta : dieta.getIngestas()) {
				total += calculateTotalProteina(ingesta);
			}
		}
		return total;
	}

	private Double calculateTotalLipidos(final Dieta dieta) {
		double total = 0.0;
		if (dieta.getIngestas() != null) {
			for (final Ingesta ingesta : dieta.getIngestas()) {
				total += calculateTotalLipidos(ingesta);
			}
		}
		return total;
	}

	private Double calculateTotalHidratosDeCarbono(final Dieta dieta) {
		double total = 0.0;
		if (dieta.getIngestas() != null) {
			for (final Ingesta ingesta : dieta.getIngestas()) {
				total += calculateTotalHidratosDeCarbono(ingesta);
			}
		}
		return total;
	}

}
