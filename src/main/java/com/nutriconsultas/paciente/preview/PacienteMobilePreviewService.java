package com.nutriconsultas.paciente.preview;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.nutriconsultas.calendar.EventStatus;
import com.nutriconsultas.mobile.MobilePatientDietPlanService;
import com.nutriconsultas.mobile.MobilePatientProgressService;
import com.nutriconsultas.mobile.MobilePatientVisitService;
import com.nutriconsultas.mobile.dto.DietPlanDetailDto;
import com.nutriconsultas.mobile.dto.DietPlanSummaryDto;
import com.nutriconsultas.mobile.dto.PagedResponse;
import com.nutriconsultas.mobile.dto.PatientProgressSnapshotDto;
import com.nutriconsultas.mobile.dto.VisitSummaryDto;
import com.nutriconsultas.paciente.Paciente;
import com.nutriconsultas.paciente.PacientePictureSupport;
import com.nutriconsultas.paciente.PacienteService;
import com.nutriconsultas.profile.NutritionistBrandingHelper;
import com.nutriconsultas.profile.NutritionistProfileRepository;
import com.nutriconsultas.util.LogRedaction;

import lombok.extern.slf4j.Slf4j;

/**
 * Builds a read-only mobile-app preview for a nutritionist's patient.
 */
@Service
@Slf4j
public class PacienteMobilePreviewService {

	private static final int NEXT_VISIT_CANDIDATES = 20;

	private final PacienteService pacienteService;

	private final MobilePatientProgressService mobilePatientProgressService;

	private final MobilePatientDietPlanService mobilePatientDietPlanService;

	private final MobilePatientVisitService mobilePatientVisitService;

	private final NutritionistProfileRepository nutritionistProfileRepository;

	public PacienteMobilePreviewService(final PacienteService pacienteService,
			final MobilePatientProgressService mobilePatientProgressService,
			final MobilePatientDietPlanService mobilePatientDietPlanService,
			final MobilePatientVisitService mobilePatientVisitService,
			final NutritionistProfileRepository nutritionistProfileRepository) {
		this.pacienteService = pacienteService;
		this.mobilePatientProgressService = mobilePatientProgressService;
		this.mobilePatientDietPlanService = mobilePatientDietPlanService;
		this.mobilePatientVisitService = mobilePatientVisitService;
		this.nutritionistProfileRepository = nutritionistProfileRepository;
	}

	@Transactional(readOnly = true)
	public PatientMobilePreviewDto buildPreview(final Long pacienteId, final String nutritionistUserId) {
		final Paciente paciente = pacienteService.findByIdAndUserId(pacienteId, nutritionistUserId);
		if (paciente == null) {
			throw new IllegalArgumentException("Paciente no encontrado");
		}

		final PatientProgressSnapshotDto progress = mobilePatientProgressService.getSnapshot(pacienteId);
		final DietPlanSummaryDto activePlan = resolveActivePlan(pacienteId);
		final DietPlanDetailDto activePlanDetail = activePlan != null
				? mobilePatientDietPlanService.getDietPlanDetail(pacienteId, activePlan.assignmentId()) : null;
		final VisitSummaryDto nextVisit = resolveNextVisit(pacienteId);
		final String nutritionistDisplayName = resolveNutritionistDisplayName(nutritionistUserId);
		final String firstName = resolveFirstName(paciente);
		final String avatarUrl = PacientePictureSupport.resolveDisplayUrlForAdmin(paciente);

		if (log.isDebugEnabled()) {
			log.debug("Built mobile preview for patient {} nutritionist {}", LogRedaction.redactPaciente(pacienteId),
					nutritionistUserId);
		}

		return new PatientMobilePreviewDto(firstName, nutritionistDisplayName, avatarUrl, progress, activePlan,
				activePlanDetail, nextVisit);
	}

	private DietPlanSummaryDto resolveActivePlan(final Long pacienteId) {
		final PagedResponse<DietPlanSummaryDto> page = mobilePatientDietPlanService.listDietPlans(pacienteId, 0, 1,
				true);
		if (page == null || page.content() == null || page.content().isEmpty()) {
			return null;
		}
		return page.content().get(0);
	}

	private VisitSummaryDto resolveNextVisit(final Long pacienteId) {
		final Instant now = Instant.now();
		final PagedResponse<VisitSummaryDto> page = mobilePatientVisitService.listVisits(pacienteId, 0,
				NEXT_VISIT_CANDIDATES, EventStatus.SCHEDULED, now, null);
		if (page == null || page.content() == null || page.content().isEmpty()) {
			return null;
		}
		return pickSoonestUpcomingVisit(page.content(), now);
	}

	static VisitSummaryDto pickSoonestUpcomingVisit(final List<VisitSummaryDto> visits, final Instant now) {
		return visits.stream()
			.filter(visit -> visit != null && visit.status() == EventStatus.SCHEDULED)
			.filter(visit -> visit.eventDateTime() != null && !visit.eventDateTime().isBefore(now))
			.min(Comparator.comparing(VisitSummaryDto::eventDateTime))
			.orElse(null);
	}

	private String resolveNutritionistDisplayName(final String nutritionistUserId) {
		if (!StringUtils.hasText(nutritionistUserId)) {
			return null;
		}
		return nutritionistProfileRepository.findByUserId(nutritionistUserId)
			.map(profile -> NutritionistBrandingHelper.resolveDisplayName(profile, null))
			.orElse(null);
	}

	static String resolveFirstName(final Paciente paciente) {
		final String source = StringUtils.hasText(paciente.getDisplayName()) ? paciente.getDisplayName()
				: paciente.getName();
		if (!StringUtils.hasText(source)) {
			return "Paciente";
		}
		final String trimmed = source.trim();
		final int space = trimmed.indexOf(' ');
		return space > 0 ? trimmed.substring(0, space) : trimmed;
	}

}
