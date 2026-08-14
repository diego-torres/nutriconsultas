package com.nutriconsultas.ai;

import java.sql.Date;
import java.time.LocalDate;

import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.nutriconsultas.dieta.Dieta;
import com.nutriconsultas.paciente.PacienteDieta;
import com.nutriconsultas.paciente.PacienteDietaService;
import com.nutriconsultas.paciente.PacienteDietaStatus;
import com.nutriconsultas.platillos.Platillo;

@Service
public class AiDraftAcceptanceServiceImpl implements AiDraftAcceptanceService {

	static final int PATIENT_ASSIGNMENT_MONTHS = 1;

	private final AiGeneratedDraftRepository draftRepository;

	private final AiDraftMaterializationService materializationService;

	private final AiDraftLifecycleService draftLifecycleService;

	private final AiEntitlementGuard aiEntitlementGuard;

	private final AiAuditLogger auditLogger;

	private final PacienteDietaService pacienteDietaService;

	public AiDraftAcceptanceServiceImpl(final AiGeneratedDraftRepository draftRepository,
			final AiDraftMaterializationService materializationService,
			final AiDraftLifecycleService draftLifecycleService, final AiEntitlementGuard aiEntitlementGuard,
			final AiAuditLogger auditLogger, final PacienteDietaService pacienteDietaService) {
		this.draftRepository = draftRepository;
		this.materializationService = materializationService;
		this.draftLifecycleService = draftLifecycleService;
		this.aiEntitlementGuard = aiEntitlementGuard;
		this.auditLogger = auditLogger;
		this.pacienteDietaService = pacienteDietaService;
	}

	@Override
	@Transactional
	public AiDraftAcceptanceResult accept(@NonNull final Long draftId, @NonNull final String nutritionistId,
			@NonNull final OidcUser principal) {
		if (!StringUtils.hasText(nutritionistId)) {
			throw new AiDraftLifecycleException("Sesión de nutriólogo no válida.");
		}
		aiEntitlementGuard.assertCanUseAiAssistant(nutritionistId);
		final AiGeneratedDraft draft = loadMutableDraft(draftId, nutritionistId);
		final MaterializedEntity entity = materialize(draft, nutritionistId, principal);
		final PatientAssignment assignment = assignToPatientIfNeeded(draft, entity, nutritionistId);
		final AiGeneratedDraft accepted = draftLifecycleService.acceptDraft(draftId, nutritionistId,
				entity.entityType(), entity.entityId(), entity.entityName());
		final String createdEntityPath = AiDraftCreatedEntityLinks.path(entity.entityType(), entity.entityId());
		final String summary = buildSummary(draft.getDraftType(), entity, assignment);
		auditLogger.logDraftMaterialized(accepted.getId(), accepted.getThread().getId(), entity.entityType(),
				entity.entityId());
		return new AiDraftAcceptanceResult(accepted.getId(), accepted.getDraftType(), accepted.getStatus(),
				entity.entityType(), entity.entityId(), entity.entityName(), createdEntityPath, summary,
				assignment.pacienteId(), assignment.assignmentId(), assignment.assignmentPath());
	}

	private AiGeneratedDraft loadMutableDraft(final Long draftId, final String nutritionistId) {
		final AiGeneratedDraft draft = draftRepository.findByIdAndThreadNutritionistId(draftId, nutritionistId)
			.orElseThrow(() -> new AiDraftLifecycleException("Borrador no encontrado."));
		if (draft.getStatus() != AiDraftStatus.DRAFT) {
			throw new AiDraftLifecycleException("El borrador ya no se puede modificar.");
		}
		return draft;
	}

	private MaterializedEntity materialize(final AiGeneratedDraft draft, final String nutritionistId,
			final OidcUser principal) {
		return switch (draft.getDraftType()) {
			case DISH -> materializeDish(draft, nutritionistId, principal);
			case MENU -> materializeMenu(draft, nutritionistId, principal);
			case DIET_PLAN -> materializeDietPlan(draft, nutritionistId, principal);
		};
	}

	private MaterializedEntity materializeDish(final AiGeneratedDraft draft, final String nutritionistId,
			final OidcUser principal) {
		final DishDraftPayload payload = AiDraftPayloadDeserializer.dish(draft.getJsonPayload());
		final Platillo platillo = materializationService.materializeDish(payload, nutritionistId, principal);
		return new MaterializedEntity(AiDraftCreatedEntityType.PLATILLO, platillo.getId(),
				resolveName(platillo.getName(), "Platillo", platillo.getId()));
	}

	private MaterializedEntity materializeMenu(final AiGeneratedDraft draft, final String nutritionistId,
			final OidcUser principal) {
		final MenuDraftPayload payload = AiDraftPayloadDeserializer.menu(draft.getJsonPayload());
		final Dieta dieta = materializationService.materializeMenu(payload, nutritionistId, principal);
		return new MaterializedEntity(AiDraftCreatedEntityType.DIETA, dieta.getId(),
				resolveName(dieta.getNombre(), "Dieta", dieta.getId()));
	}

	private MaterializedEntity materializeDietPlan(final AiGeneratedDraft draft, final String nutritionistId,
			final OidcUser principal) {
		final DietPlanDraftPayload payload = AiDraftPayloadDeserializer.dietPlan(draft.getJsonPayload());
		final Dieta dieta = materializationService.materializeDietPlan(payload, nutritionistId, principal);
		return new MaterializedEntity(AiDraftCreatedEntityType.DIETA, dieta.getId(),
				resolveName(dieta.getNombre(), "Plan alimenticio", dieta.getId()));
	}

	private PatientAssignment assignToPatientIfNeeded(final AiGeneratedDraft draft, final MaterializedEntity entity,
			final String nutritionistId) {
		final Long pacienteId = resolvePacienteId(draft);
		if (pacienteId == null || entity.entityType() != AiDraftCreatedEntityType.DIETA) {
			return PatientAssignment.none();
		}
		final PacienteDieta shell = new PacienteDieta();
		final LocalDate start = LocalDate.now();
		shell.setStartDate(Date.valueOf(start));
		shell.setEndDate(Date.valueOf(start.plusMonths(PATIENT_ASSIGNMENT_MONTHS)));
		shell.setStatus(PacienteDietaStatus.ACTIVE);
		shell.setNotes("Asignación automática al aceptar borrador IA (vigencia "
				+ PATIENT_ASSIGNMENT_MONTHS + " mes).");
		try {
			final PacienteDieta saved = pacienteDietaService.assignDieta(pacienteId, entity.entityId(), shell,
					nutritionistId);
			return new PatientAssignment(pacienteId, saved.getId(), patientAssignmentPath(pacienteId));
		}
		catch (IllegalArgumentException ex) {
			throw new AiDraftLifecycleException(
					ex.getMessage() != null ? ex.getMessage() : "No se pudo asignar la dieta al paciente.", ex);
		}
	}

	@Nullable
	private static Long resolvePacienteId(final AiGeneratedDraft draft) {
		if (draft.getPacienteId() != null) {
			return draft.getPacienteId();
		}
		if (draft.getThread() != null && draft.getThread().getPatient() != null) {
			return draft.getThread().getPatient().getId();
		}
		return null;
	}

	private static String patientAssignmentPath(final long pacienteId) {
		return "/admin/pacientes/" + pacienteId + "/dietas";
	}

	private static String resolveName(final String name, final String fallbackLabel, final long entityId) {
		if (StringUtils.hasText(name)) {
			return name.trim();
		}
		return fallbackLabel + " #" + entityId;
	}

	private static String buildSummary(final AiDraftType draftType, final MaterializedEntity entity,
			final PatientAssignment assignment) {
		final String catalogSummary = switch (draftType) {
			case DISH -> "Se creó el platillo «" + entity.entityName() + "» en tu catálogo.";
			case MENU -> "Se creó la dieta «" + entity.entityName() + "» en tu catálogo.";
			case DIET_PLAN -> "Se creó el plan «" + entity.entityName() + "» en tu catálogo.";
		};
		if (assignment.pacienteId() == null) {
			return catalogSummary;
		}
		return catalogSummary + " Se asignó al paciente por " + PATIENT_ASSIGNMENT_MONTHS + " mes.";
	}

	private record MaterializedEntity(AiDraftCreatedEntityType entityType, long entityId, String entityName) {
	}

	private record PatientAssignment(@Nullable Long pacienteId, @Nullable Long assignmentId,
			@Nullable String assignmentPath) {

		private static PatientAssignment none() {
			return new PatientAssignment(null, null, null);
		}

	}

}
