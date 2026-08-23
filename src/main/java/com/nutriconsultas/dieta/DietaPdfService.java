package com.nutriconsultas.dieta;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.xhtmlrenderer.pdf.ITextRenderer;

import com.nutriconsultas.paciente.Paciente;
import com.nutriconsultas.paciente.PacienteDieta;
import com.nutriconsultas.paciente.PacienteDietaRepository;
import com.nutriconsultas.paciente.PacienteDietaStatus;
import com.nutriconsultas.paciente.PacienteDietaWeekday;
import com.nutriconsultas.paciente.PacienteDietaWeekdayLabels;
import com.nutriconsultas.paciente.PacienteDietaWeekdayRepository;
import com.nutriconsultas.paciente.PacienteRepository;
import com.nutriconsultas.profile.NutritionistBrandingHelper;
import com.nutriconsultas.profile.NutritionistProfile;
import com.nutriconsultas.profile.NutritionistProfileService;

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
 * alimentary plan), patient information is excluded even if an assignment exists,
 * resulting in a generic diet template.</li>
 * </ul>
 *
 * <p>
 * The service provides two methods:
 * <ul>
 * <li>{@link #generatePdf(Long)}: Automatically includes patient information if an active
 * assignment exists (for backward compatibility).</li>
 * <li>{@link #generatePdf(Long, boolean)}: Allows explicit control over whether to
 * include patient information.</li>
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

	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	public static class WeeklyDietPdfDay {

		private Integer dayOfWeek;

		private String dayLabel;

		private Dieta dieta;

		private List<Ingesta> ingestas;

		private Map<Long, IngestaNutritionalTotals> ingestaTotals;

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
	private PacienteDietaWeekdayRepository pacienteDietaWeekdayRepository;

	@Autowired
	private PacienteRepository pacienteRepository;

	@Autowired
	private DietaService dietaService;

	@Autowired
	private NutritionistProfileService nutritionistProfileService;

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
	 * @throws IllegalStateException if PDF generation fails
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
	 * <li>If {@code includePatientInfo} is {@code true} and the dieta has an active
	 * patient assignment, patient information (including notes from PacienteDieta) will
	 * be included in the PDF.</li>
	 * <li>If {@code includePatientInfo} is {@code false}, only the dieta content will be
	 * included, regardless of whether an assignment exists.</li>
	 * </ul>
	 *
	 * <p>
	 * The template {@code sbadmin/dietas/printable.html} uses conditional rendering to
	 * handle both cases automatically.
	 * @param dietaId the ID of the dieta to generate PDF for
	 * @param includePatientInfo if {@code true}, includes patient information when an
	 * active assignment exists; if {@code false}, generates a generic PDF without patient
	 * information
	 * @return PDF document as byte array
	 * @throws IllegalArgumentException if dieta with the given ID is not found
	 * @throws IllegalStateException if PDF generation fails
	 */
	public byte[] generatePdf(@NonNull final Long dietaId, final boolean includePatientInfo) {
		log.info("Generating PDF for dieta with id: {} (includePatientInfo: {})", dietaId, includePatientInfo);
		final Dieta dieta = dietaService.getDieta(dietaId);
		if (dieta == null) {
			throw new IllegalArgumentException("Dieta with id " + dietaId + " not found");
		}

		// Find patient assignment if exists and if patient info should be included
		final PacienteDieta activeAssignment = includePatientInfo ? resolveAssignmentForPdf(dieta, dietaId) : null;

		return buildPdf(dieta, activeAssignment);
	}

	private PacienteDieta resolveAssignmentForPdf(final Dieta dieta, final Long dietaId) {
		final List<PacienteDieta> assignments = pacienteDietaRepository.findByDietaId(dietaId);
		final PacienteDieta activeAssignment = assignments.stream()
			.filter(a -> PacienteDietaStatus.ACTIVE.equals(a.getStatus()))
			.findFirst()
			.orElse(null);
		if (activeAssignment != null) {
			return activeAssignment;
		}
		if (!assignments.isEmpty()) {
			return assignments.get(0);
		}
		final PacienteDieta weekdayAssignment = pacienteDietaWeekdayRepository.findFirstByDietaId(dietaId)
			.map(PacienteDietaWeekday::getPacienteDieta)
			.orElse(null);
		if (weekdayAssignment != null) {
			return weekdayAssignment;
		}
		if (DietaCatalogConstants.isPatientAssignment(dieta) && dieta.getPacienteId() != null) {
			return pacienteDietaRepository.findByPacienteId(dieta.getPacienteId())
				.stream()
				.filter(a -> a.getDieta() != null && dietaId.equals(a.getDieta().getId()))
				.findFirst()
				.orElse(null);
		}
		return null;
	}

	/**
	 * Generates a patient-specific PDF for an explicit {@link PacienteDieta} assignment.
	 *
	 * <p>
	 * Used by the mobile API so the PDF reflects the requested assignment (notes, dates,
	 * patient context) rather than the first active assignment on the dieta.
	 * @param assignment the patient diet assignment; must reference a persisted dieta
	 * @return PDF document as byte array
	 * @throws IllegalArgumentException if the assignment has no dieta or the dieta is
	 * missing
	 */
	public byte[] generatePdfForAssignment(@NonNull final PacienteDieta assignment) {
		if (assignment.getDieta() == null || assignment.getDieta().getId() == null) {
			throw new IllegalArgumentException("Assignment has no dieta");
		}
		return generatePdfForAssignment(assignment, assignment.getDieta().getId());
	}

	public byte[] generatePdfForAssignment(@NonNull final PacienteDieta assignment, @NonNull final Long dietaId) {
		log.info("Generating PDF for assignment id: {} dieta id: {}", assignment.getId(), dietaId);
		final Dieta dieta = dietaService.getDieta(dietaId);
		if (dieta == null) {
			throw new IllegalArgumentException("Dieta with id " + dietaId + " not found");
		}
		return buildPdf(dieta, assignment);
	}

	public ResponseEntity<byte[]> buildAssignmentPdfResponse(@NonNull final PacienteDieta assignment) {
		if (assignment.getDieta() == null || assignment.getDieta().getId() == null) {
			throw new IllegalArgumentException("Assignment has no dieta");
		}
		return buildAssignmentPdfResponse(assignment, assignment.getDieta().getId());
	}

	public ResponseEntity<byte[]> buildAssignmentPdfResponse(@NonNull final PacienteDieta assignment,
			@NonNull final Long dietaId) {
		final byte[] pdfBytes = generatePdfForAssignment(assignment, dietaId);
		final Dieta dieta = dietaService.getDieta(dietaId);
		final String fileName = (dieta != null && dieta.getNombre() != null ? dieta.getNombre() : "dieta") + ".pdf";
		return ResponseEntity.ok()
			.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
			.contentType(MediaType.parseMediaType("application/pdf"))
			.body(pdfBytes);
	}

	/**
	 * Generates a multi-day PDF for a weekly {@link PacienteDieta} assignment.
	 *
	 * <p>
	 * The document includes a cover page (patient, dates, day overview) followed by each
	 * weekday dieta with ingestas and nutritional totals.
	 * @param assignment weekly patient diet assignment
	 * @return PDF document as byte array
	 * @throws IllegalArgumentException if the assignment is not weekly or has no days
	 */
	public byte[] generateWeeklyPdf(@NonNull final PacienteDieta assignment) {
		log.info("Generating weekly PDF for assignment id: {}", assignment.getId());
		if (!assignment.isWeeklyAssignment()) {
			throw new IllegalArgumentException("Assignment is not a weekly plan");
		}
		if (assignment.getId() == null) {
			throw new IllegalArgumentException("Assignment has no id");
		}
		final List<PacienteDietaWeekday> slots = pacienteDietaWeekdayRepository
			.findByPacienteDietaIdOrderByDayOfWeekAsc(assignment.getId());
		final List<WeeklyDietPdfDay> weeklyDays = new ArrayList<>();
		for (final PacienteDietaWeekday slot : slots) {
			final WeeklyDietPdfDay day = buildWeeklyDay(slot);
			if (day != null) {
				weeklyDays.add(day);
			}
		}
		if (weeklyDays.isEmpty()) {
			throw new IllegalArgumentException("Weekly assignment has no days");
		}
		return buildWeeklyPdf(assignment, weeklyDays);
	}

	public ResponseEntity<byte[]> buildWeeklyAssignmentPdfResponse(@NonNull final PacienteDieta assignment) {
		final byte[] pdfBytes = generateWeeklyPdf(assignment);
		return ResponseEntity.ok()
			.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"plan-semanal.pdf\"")
			.contentType(MediaType.parseMediaType("application/pdf"))
			.body(pdfBytes);
	}

	private byte[] buildPdf(final Dieta dieta, final PacienteDieta assignment) {
		final Context context = new Context();
		context.setVariable("dieta", dieta);
		context.setVariable("pacienteDieta", assignment);
		context.setVariable("paciente", resolvePaciente(dieta, assignment));
		final List<Ingesta> sortedIngestas = sortedIngestas(dieta);
		context.setVariable("ingestas", sortedIngestas);
		context.setVariable("ingestaTotals", buildIngestaTotals(sortedIngestas));
		context.setVariable("totalEnergia", calculateTotalEnergia(dieta));
		context.setVariable("totalProteina", calculateTotalProteina(dieta));
		context.setVariable("totalLipidos", calculateTotalLipidos(dieta));
		context.setVariable("totalHidratosDeCarbono", calculateTotalHidratosDeCarbono(dieta));
		applyBranding(context, dieta.getUserId());
		final String html = templateEngine.process("sbadmin/dietas/printable", context);
		return htmlToPdf(html);
	}

	private WeeklyDietPdfDay buildWeeklyDay(final PacienteDietaWeekday slot) {
		WeeklyDietPdfDay result = null;
		if (slot != null && slot.getDieta() != null && slot.getDieta().getId() != null) {
			final Dieta dieta = dietaService.getDieta(slot.getDieta().getId());
			if (dieta != null) {
				final int dayOfWeek = slot.getDayOfWeek() != null ? slot.getDayOfWeek() : 0;
				final List<Ingesta> ingestas = sortedIngestas(dieta);
				final WeeklyDietPdfDay day = new WeeklyDietPdfDay();
				day.setDayOfWeek(slot.getDayOfWeek());
				day.setDayLabel(PacienteDietaWeekdayLabels.labelForDay(dayOfWeek));
				day.setDieta(dieta);
				day.setIngestas(ingestas);
				day.setIngestaTotals(buildIngestaTotals(ingestas));
				day.setTotalEnergia(calculateTotalEnergia(dieta));
				day.setTotalProteina(calculateTotalProteina(dieta));
				day.setTotalLipidos(calculateTotalLipidos(dieta));
				day.setTotalHidratosDeCarbono(calculateTotalHidratosDeCarbono(dieta));
				result = day;
			}
		}
		return result;
	}

	private byte[] buildWeeklyPdf(final PacienteDieta assignment, final List<WeeklyDietPdfDay> weeklyDays) {
		final Context context = new Context();
		context.setVariable("weeklyDays", weeklyDays);
		context.setVariable("pacienteDieta", assignment);
		context.setVariable("paciente", assignment.getPaciente());
		applyBranding(context, resolveWeeklyBrandingUserId(assignment, weeklyDays));
		final String html = templateEngine.process("sbadmin/dietas/printable-semanal", context);
		return htmlToPdf(html);
	}

	private String resolveWeeklyBrandingUserId(final PacienteDieta assignment,
			final List<WeeklyDietPdfDay> weeklyDays) {
		String userId = null;
		if (assignment.getPaciente() != null && assignment.getPaciente().getUserId() != null) {
			userId = assignment.getPaciente().getUserId();
		}
		else {
			for (final WeeklyDietPdfDay day : weeklyDays) {
				if (day.getDieta() != null && day.getDieta().getUserId() != null) {
					userId = day.getDieta().getUserId();
					break;
				}
			}
		}
		return userId;
	}

	private List<Ingesta> sortedIngestas(final Dieta dieta) {
		List<Ingesta> result = List.of();
		if (dieta.getIngestas() != null) {
			result = dieta.getIngestas()
				.stream()
				.sorted(IngestaComparators.BY_DISPLAY_ORDER)
				.collect(Collectors.toList());
			result.forEach(ingesta -> {
				if (ingesta.getAlimentos() != null) {
					ingesta.getAlimentos().sort(AlimentoIngestaComparators.BY_DISPLAY_ORDER);
				}
			});
		}
		return result;
	}

	private Map<Long, IngestaNutritionalTotals> buildIngestaTotals(final List<Ingesta> ingestas) {
		final Map<Long, IngestaNutritionalTotals> ingestaTotals = new HashMap<>();
		for (final Ingesta ingesta : ingestas) {
			final IngestaNutritionalTotals totals = new IngestaNutritionalTotals();
			totals.setTotalEnergia(calculateTotalEnergia(ingesta));
			totals.setTotalProteina(calculateTotalProteina(ingesta));
			totals.setTotalLipidos(calculateTotalLipidos(ingesta));
			totals.setTotalHidratosDeCarbono(calculateTotalHidratosDeCarbono(ingesta));
			ingestaTotals.put(ingesta.getId(), totals);
		}
		return ingestaTotals;
	}

	private void applyBranding(final Context context, final String userId) {
		if (userId != null) {
			final NutritionistProfile profile = nutritionistProfileService.getOrCreateProfile(userId);
			final String logoBase64 = nutritionistProfileService.getLogoAsBase64DataUri(userId);
			NutritionistBrandingHelper.addBrandingVariables(context, profile, logoBase64);
		}
		else {
			NutritionistBrandingHelper.addBrandingVariables(context, null, null);
		}
	}

	private Paciente resolvePaciente(final Dieta dieta, final PacienteDieta assignment) {
		if (assignment != null && assignment.getPaciente() != null) {
			return assignment.getPaciente();
		}
		if (DietaCatalogConstants.isPatientAssignment(dieta) && dieta.getPacienteId() != null) {
			return pacienteRepository.findById(dieta.getPacienteId()).orElse(null);
		}
		return null;
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
			throw new IllegalStateException("Error generating PDF", e);
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
