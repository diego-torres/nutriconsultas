package com.nutriconsultas.ai;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.nutriconsultas.dieta.Dieta;
import com.nutriconsultas.dieta.DietaCatalogConstants;
import com.nutriconsultas.dieta.DietaService;
import com.nutriconsultas.dieta.Ingesta;
import com.nutriconsultas.dieta.IngestaComparators;
import com.nutriconsultas.paciente.PacienteRepository;

@Service
public class AiDietaPromptContextResolverImpl implements AiDietaPromptContextResolver {

	private static final int MAX_INGESTA_NAMES = 12;

	private final DietaService dietaService;

	private final PacienteRepository pacienteRepository;

	public AiDietaPromptContextResolverImpl(final DietaService dietaService,
			final PacienteRepository pacienteRepository) {
		this.dietaService = dietaService;
		this.pacienteRepository = pacienteRepository;
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<AiDietaPromptContext> resolve(@Nullable final Long dietaId, final String nutritionistId) {
		if (dietaId == null || !StringUtils.hasText(nutritionistId)) {
			return Optional.empty();
		}
		final Dieta dieta = dietaService.getDieta(dietaId);
		if (dieta == null || !canAccess(dieta, nutritionistId)) {
			throw new AiChatException(org.springframework.http.HttpStatus.NOT_FOUND, AiToolErrorCode.NOT_FOUND,
					"No se encontró la dieta.");
		}
		return Optional.of(toPromptContext(dieta));
	}

	private boolean canAccess(final Dieta dieta, final String nutritionistId) {
		return !DietaCatalogConstants.isPatientAssignment(dieta) || (dieta.getPacienteId() != null
				&& pacienteRepository.findByIdAndUserId(dieta.getPacienteId(), nutritionistId).isPresent());
	}

	private AiDietaPromptContext toPromptContext(final Dieta dieta) {
		final List<String> ingestaNames = new ArrayList<>();
		if (dieta.getIngestas() != null) {
			dieta.getIngestas()
				.stream()
				.sorted(IngestaComparators.BY_DISPLAY_ORDER)
				.limit(MAX_INGESTA_NAMES)
				.map(Ingesta::getNombre)
				.filter(StringUtils::hasText)
				.forEach(ingestaNames::add);
		}
		final boolean patientAssignment = DietaCatalogConstants.isPatientAssignment(dieta);
		return new AiDietaPromptContext(dieta.getId(), dieta.getNombre(), dieta.getEnergia(), dieta.getProteina(),
				dieta.getLipidos(), dieta.getHidratosDeCarbono(),
				dieta.getIngestas() != null ? dieta.getIngestas().size() : 0, List.copyOf(ingestaNames),
				patientAssignment, patientAssignment ? dieta.getPacienteId() : null);
	}

}
