package com.nutriconsultas.mobile;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.nutriconsultas.dieta.AlimentoIngesta;
import com.nutriconsultas.dieta.Dieta;
import com.nutriconsultas.dieta.DietaPdfService;
import com.nutriconsultas.dieta.Ingesta;
import com.nutriconsultas.dieta.IngredientePlatilloIngesta;
import com.nutriconsultas.dieta.PlatilloIngesta;
import com.nutriconsultas.dieta.PlatilloIngestaRepository;
import com.nutriconsultas.mobile.dto.DietGroceryListDto;
import com.nutriconsultas.mobile.dto.DietPlanDetailDto;
import com.nutriconsultas.mobile.dto.DietPlanPdfResult;
import com.nutriconsultas.mobile.dto.DietPlanSummaryDto;
import com.nutriconsultas.mobile.dto.DietPlatilloDetailDto;
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

	private final PlatilloIngestaRepository platilloIngestaRepository;

	private final DietaPdfService dietaPdfService;

	public MobilePatientDietPlanService(final PacienteDietaRepository pacienteDietaRepository,
			final PlatilloIngestaRepository platilloIngestaRepository, final DietaPdfService dietaPdfService) {
		this.pacienteDietaRepository = pacienteDietaRepository;
		this.platilloIngestaRepository = platilloIngestaRepository;
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
			log.debug("Loaded mobile diet plan detail assignmentId={} for patient {}",
					LogRedaction.redactPacienteDieta(assignmentId), LogRedaction.redactPaciente(pacienteId));
		}
		return DietPlanDetailDto.fromEntity(assignment);
	}

	@Transactional(readOnly = true)
	public DietPlatilloDetailDto getPlatilloDetail(final Long pacienteId, final Long assignmentId,
			final Long platilloIngestaId) {
		final PlatilloIngesta platillo = platilloIngestaRepository
			.findByIdForPatientAssignment(platilloIngestaId, assignmentId, pacienteId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
		initializePlatilloDetail(platillo);
		if (log.isDebugEnabled()) {
			log.debug("Loaded mobile platillo detail platilloIngestaId={} assignmentId={} for patient {}",
					platilloIngestaId, LogRedaction.redactPacienteDieta(assignmentId),
					LogRedaction.redactPaciente(pacienteId));
		}
		return DietPlatilloDetailDto.fromEntity(platillo);
	}

	@Transactional(readOnly = true)
	public DietGroceryListDto getGroceryList(final Long pacienteId, final Long assignmentId, final String week) {
		final PacienteDieta assignment = pacienteDietaRepository.findByIdAndPacienteId(assignmentId, pacienteId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
		if (week != null && !week.isBlank() && !"current".equalsIgnoreCase(week)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
		}
		initializeGroceryTree(assignment.getDieta());
		if (log.isDebugEnabled()) {
			log.debug("Loaded mobile grocery list assignmentId={} for patient {}",
					LogRedaction.redactPacienteDieta(assignmentId), LogRedaction.redactPaciente(pacienteId));
		}
		return new DietGroceryListDto(DietGroceryListAggregator.aggregate(assignment.getDieta()));
	}

	@Transactional(readOnly = true)
	public DietPlanPdfResult generateDietPlanPdf(final Long pacienteId, final Long assignmentId) {
		final PacienteDieta assignment = pacienteDietaRepository.findByIdAndPacienteId(assignmentId, pacienteId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
		final byte[] pdfBytes = dietaPdfService.generatePdfForAssignment(assignment);
		final Dieta dieta = assignment.getDieta();
		final String filename = (dieta != null && dieta.getNombre() != null ? dieta.getNombre() : "dieta") + ".pdf";
		if (log.isDebugEnabled()) {
			log.debug("Generated mobile diet plan PDF assignmentId={} for patient {}",
					LogRedaction.redactPacienteDieta(assignmentId), LogRedaction.redactPaciente(pacienteId));
		}
		return new DietPlanPdfResult(pdfBytes, filename);
	}

	private static void initializeGroceryTree(final Dieta dieta) {
		if (dieta == null || dieta.getIngestas() == null) {
			return;
		}
		for (final Ingesta ingesta : dieta.getIngestas()) {
			if (ingesta.getPlatillos() != null) {
				Hibernate.initialize(ingesta.getPlatillos());
				for (final PlatilloIngesta platillo : ingesta.getPlatillos()) {
					initializePlatilloDetail(platillo);
				}
			}
			if (ingesta.getAlimentos() != null) {
				Hibernate.initialize(ingesta.getAlimentos());
				for (final AlimentoIngesta alimentoIngesta : ingesta.getAlimentos()) {
					if (alimentoIngesta.getAlimento() != null) {
						Hibernate.initialize(alimentoIngesta.getAlimento());
					}
				}
			}
		}
	}

	private static void initializePlatilloDetail(final PlatilloIngesta platillo) {
		if (platillo.getIngredientes() != null) {
			Hibernate.initialize(platillo.getIngredientes());
			for (final IngredientePlatilloIngesta ingrediente : platillo.getIngredientes()) {
				if (ingrediente.getAlimento() != null) {
					Hibernate.initialize(ingrediente.getAlimento());
				}
			}
		}
	}

	private static void initializeDietaTree(final Dieta dieta) {
		if (dieta == null || dieta.getIngestas() == null) {
			return;
		}
		for (final Ingesta ingesta : dieta.getIngestas()) {
			if (ingesta.getPlatillos() != null) {
				Hibernate.initialize(ingesta.getPlatillos());
			}
			if (ingesta.getAlimentos() != null) {
				Hibernate.initialize(ingesta.getAlimentos());
			}
		}
	}

}
