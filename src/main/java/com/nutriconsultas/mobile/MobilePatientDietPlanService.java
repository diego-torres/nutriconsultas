package com.nutriconsultas.mobile;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

	public MobilePatientDietPlanService(final PacienteDietaRepository pacienteDietaRepository) {
		this.pacienteDietaRepository = pacienteDietaRepository;
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

}
