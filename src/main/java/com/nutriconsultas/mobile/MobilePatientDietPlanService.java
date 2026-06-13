package com.nutriconsultas.mobile;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.nutriconsultas.dieta.Dieta;
import com.nutriconsultas.dieta.DietaPdfService;
import com.nutriconsultas.dieta.Ingesta;
import com.nutriconsultas.mobile.dto.DietPlanDetailDto;
import com.nutriconsultas.mobile.dto.DietPlanPdfResult;
import com.nutriconsultas.mobile.dto.DietPlanSummaryDto;
import com.nutriconsultas.mobile.dto.PagedResponse;
import com.nutriconsultas.paciente.PacienteDieta;
import com.nutriconsultas.paciente.PacienteDietaRepository;
import com.nutriconsultas.paciente.PacienteDietaStatus;
import com.nutriconsultas.util.LogRedaction;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class MobilePatientDietPlanService {

	private static final int MAX_PAGE_SIZE = 100;

	private final PacienteDietaRepository pacienteDietaRepository;

	private final DietaPdfService dietaPdfService;

	public MobilePatientDietPlanService(final PacienteDietaRepository pacienteDietaRepository,
			final DietaPdfService dietaPdfService) {
		this.pacienteDietaRepository = pacienteDietaRepository;
		this.dietaPdfService = dietaPdfService;
	}

	@Transactional(readOnly = true)
	public PagedResponse<DietPlanSummaryDto> listDietPlans(final Long pacienteId, final int page, final int size,
			final boolean activeOnly) {
		final int safePage = Math.max(page, 0);
		final int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
		final Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "startDate"));
		final Page<PacienteDieta> assignments = activeOnly
				? pacienteDietaRepository.findByPacienteIdAndStatus(pacienteId, PacienteDietaStatus.ACTIVE, pageable)
				: pacienteDietaRepository.findByPacienteId(pacienteId, pageable);
		final Page<DietPlanSummaryDto> summaries = assignments.map(DietPlanSummaryDto::fromEntity);
		if (log.isDebugEnabled()) {
			log.debug("Listed mobile diet plans page={} size={} count={} activeOnly={} for patient {}", safePage,
					safeSize, summaries.getNumberOfElements(), activeOnly, LogRedaction.redactPaciente(pacienteId));
		}
		return PagedResponse.of(summaries);
	}

	@Transactional(readOnly = true)
	public DietPlanDetailDto getDietPlanDetail(final Long pacienteId, final Long assignmentId) {
		final PacienteDieta assignment = pacienteDietaRepository.findByIdAndPacienteId(assignmentId, pacienteId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
		initializeDietaTree(assignment.getDieta());
		if (log.isDebugEnabled()) {
			log.debug("Loaded mobile diet plan detail assignmentId={} for patient {}", assignmentId,
					LogRedaction.redactPaciente(pacienteId));
		}
		return DietPlanDetailDto.fromEntity(assignment);
	}

	@Transactional(readOnly = true)
	public DietPlanPdfResult generateDietPlanPdf(final Long pacienteId, final Long assignmentId) {
		final PacienteDieta assignment = pacienteDietaRepository.findByIdAndPacienteId(assignmentId, pacienteId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
		final byte[] pdfBytes = dietaPdfService.generatePdfForAssignment(assignment);
		final Dieta dieta = assignment.getDieta();
		final String filename = (dieta != null && dieta.getNombre() != null ? dieta.getNombre() : "dieta") + ".pdf";
		if (log.isDebugEnabled()) {
			log.debug("Generated mobile diet plan PDF assignmentId={} for patient {}", assignmentId,
					LogRedaction.redactPaciente(pacienteId));
		}
		return new DietPlanPdfResult(pdfBytes, filename);
	}

	private static void initializeDietaTree(final Dieta dieta) {
		if (dieta == null || dieta.getIngestas() == null) {
			return;
		}
		for (final Ingesta ingesta : dieta.getIngestas()) {
			if (ingesta.getPlatillos() != null) {
				ingesta.getPlatillos().size();
			}
			if (ingesta.getAlimentos() != null) {
				ingesta.getAlimentos().size();
			}
		}
	}

}
