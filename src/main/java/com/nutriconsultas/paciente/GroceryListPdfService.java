package com.nutriconsultas.paciente;

import java.io.ByteArrayOutputStream;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.xhtmlrenderer.pdf.ITextRenderer;

import com.nutriconsultas.mobile.dto.DietGroceryListItemDto;
import com.nutriconsultas.profile.NutritionistBrandingHelper;
import com.nutriconsultas.profile.NutritionistProfile;
import com.nutriconsultas.profile.NutritionistProfileService;

import lombok.extern.slf4j.Slf4j;

/**
 * Generates printable PDF documents for patient diet grocery lists (#532).
 */
@Service
@Slf4j
public class GroceryListPdfService {

	@Autowired
	private TemplateEngine templateEngine;

	@Autowired
	private NutritionistProfileService nutritionistProfileService;

	public byte[] generatePdf(@NonNull final Paciente paciente, @NonNull final PacienteDieta assignment,
			@NonNull final List<DietGroceryListItemDto> groceryItems, @NonNull final String nutritionistUserId,
			final String oauthDisplayName) {
		log.info("Generating grocery list PDF for assignment id: {}", assignment.getId());
		final Context context = new Context();
		context.setVariable("paciente", paciente);
		context.setVariable("pacienteDieta", assignment);
		context.setVariable("groceryItems", groceryItems);
		context.setVariable("assignmentLabel", resolveAssignmentLabel(assignment));

		final NutritionistProfile profile = nutritionistProfileService.getOrCreateProfile(nutritionistUserId);
		final String logoBase64 = nutritionistProfileService.getLogoAsBase64DataUri(nutritionistUserId);
		NutritionistBrandingHelper.addBrandingVariables(context, profile, logoBase64, oauthDisplayName);

		final String html = templateEngine.process("sbadmin/pacientes/lista-compras-pdf", context);
		return htmlToPdf(html);
	}

	public ResponseEntity<byte[]> buildPdfResponse(@NonNull final Paciente paciente,
			@NonNull final PacienteDieta assignment, @NonNull final List<DietGroceryListItemDto> groceryItems,
			@NonNull final String nutritionistUserId, final String oauthDisplayName) {
		final byte[] pdfBytes = generatePdf(paciente, assignment, groceryItems, nutritionistUserId, oauthDisplayName);
		return ResponseEntity.ok()
			.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"lista-de-compras.pdf\"")
			.contentType(MediaType.parseMediaType("application/pdf"))
			.body(pdfBytes);
	}

	private String resolveAssignmentLabel(final PacienteDieta assignment) {
		if (assignment.isWeeklyAssignment()) {
			return "Plan semanal";
		}
		if (assignment.getDieta() != null && assignment.getDieta().getNombre() != null) {
			return assignment.getDieta().getNombre();
		}
		return "Dieta";
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
			log.error("Error generating grocery list PDF", e);
			throw new IllegalStateException("Error generating grocery list PDF", e);
		}
	}

}
