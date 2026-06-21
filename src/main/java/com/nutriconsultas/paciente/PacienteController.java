package com.nutriconsultas.paciente;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.nutriconsultas.calendar.CalendarEvent;
import com.nutriconsultas.calendar.CalendarEventService;
import com.nutriconsultas.calendar.EventStatus;
import com.nutriconsultas.clinical.exam.AnthropometricMeasurement;
import com.nutriconsultas.clinical.exam.anthropometric.AnthropometricFieldCatalog;
import com.nutriconsultas.clinical.exam.ClinicalExam;
import com.nutriconsultas.clinical.exam.ClinicalExamService;
import com.nutriconsultas.auth0.Auth0UserLookup;
import com.nutriconsultas.controller.AbstractAuthorizedController;
import com.nutriconsultas.mobile.PatientMobileAuthStatus;
import com.nutriconsultas.dieta.DietaNutritionCalculator;
import com.nutriconsultas.dieta.DietaPdfService;
import com.nutriconsultas.dieta.DietaRepository;
import com.nutriconsultas.dieta.DietaService;
import com.nutriconsultas.paciente.calculation.BmrCalculationService;
import com.nutriconsultas.subscription.SubscriptionErrorResponses;
import com.nutriconsultas.subscription.SubscriptionLimitExceededException;
import com.nutriconsultas.paciente.mpx.MpxExportResult;
import com.nutriconsultas.paciente.mpx.MpxImportException;
import com.nutriconsultas.paciente.mpx.MpxImportResult;
import com.nutriconsultas.paciente.mpx.PacienteMpxExportService;
import com.nutriconsultas.paciente.mpx.PacienteMpxImportService;
import com.nutriconsultas.util.LogRedaction;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;

@Controller
@Slf4j
public class PacienteController extends AbstractAuthorizedController {

	@Autowired
	private PacienteRepository pacienteRepository;

	@Autowired
	private PacienteService pacienteService;

	@Autowired
	private SubscriptionErrorResponses subscriptionErrorResponses;

	@Autowired
	private CalendarEventService calendarEventService;

	@Autowired
	private ClinicalExamService clinicalExamService;

	@Autowired
	private com.nutriconsultas.clinical.exam.AnthropometricMeasurementService anthropometricMeasurementService;

	@Autowired
	private com.nutriconsultas.paciente.metrics.BodyMetricRecordService bodyMetricRecordService;

	@Autowired
	private BodyFatCalculatorService bodyFatCalculatorService;

	@Autowired
	private com.nutriconsultas.clinical.exam.anthropometric.BodyCompositionService bodyCompositionService;

	@Autowired
	private com.nutriconsultas.clinical.exam.anthropometric.SomatotypeService somatotypeService;

	@Autowired
	private PacienteDietaService pacienteDietaService;

	@Autowired
	private DietaService dietaService;

	@Autowired
	private DietaRepository dietaRepository;

	@Autowired
	private DietaPdfService dietaPdfService;

	@Autowired
	private PacienteDietaRepository pacienteDietaRepository;

	@Autowired
	private Auth0UserLookup auth0UserLookup;

	@Autowired
	private PacienteMpxExportService pacienteMpxExportService;

	@Autowired
	private PacienteMpxImportService pacienteMpxImportService;

	/**
	 * Gets the user ID from the OAuth2 principal.
	 * @param principal the OAuth2 principal
	 * @return the user ID (sub claim) or null if not available
	 */
	private String getUserId(@AuthenticationPrincipal final OidcUser principal) {
		if (principal == null) {
			log.warn("OAuth2 principal is null, cannot get user ID");
			return null;
		}
		final String userId = principal.getSubject();
		log.debug("Retrieved user ID: {}", userId);
		return userId;
	}

	/**
	 * Verifies that a patient belongs to the current user.
	 * @param paciente the patient to verify
	 * @param userId the current user's ID
	 * @throws IllegalArgumentException if the patient does not belong to the user
	 */
	private Double resolveRequerimientoKcal(final Paciente paciente) {
		if (paciente.getTotalAdjustedKcal() != null) {
			return paciente.getTotalAdjustedKcal();
		}
		return paciente.getGetKcal();
	}

	private void verifyPatientOwnership(final Paciente paciente, final String userId) {
		if (paciente == null) {
			throw new IllegalArgumentException("Paciente no encontrado");
		}
		if (paciente.getUserId() == null || !paciente.getUserId().equals(userId)) {
			log.warn("User {} attempted to access patient {} owned by {}", userId, paciente.getId(),
					paciente.getUserId());
			throw new IllegalArgumentException("No tiene permiso para acceder a este paciente");
		}
	}

	@GetMapping(path = "/admin/pacientes/nuevo")
	public String nuevo(final Model model) {
		log.debug("Starting nuevo method");
		model.addAttribute("activeMenu", "pacientes");
		model.addAttribute("paciente", new Paciente());
		log.debug("Finished nuevo method with model {}", model);
		return "sbadmin/pacientes/nuevo";
	}

	@GetMapping(path = "/admin/pacientes")
	public String listado(final Model model) {
		log.debug("Listado de pacientes");
		model.addAttribute("activeMenu", "pacientes");
		return "sbadmin/pacientes/listado";
	}

	/**
	 * Imports a patient registration profile from an uploaded {@code .mpx} file (#222).
	 * Creates a new patient for the authenticated nutritionist; clinical history is not
	 * restored.
	 */
	@PostMapping(path = "/admin/pacientes/importar.mpx", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public String importPacienteMpx(@RequestParam("mpxFile") final MultipartFile mpxFile,
			@AuthenticationPrincipal final OidcUser principal, final RedirectAttributes redirectAttributes) {
		log.debug("Importing MPX registration");
		final String userId = getUserId(principal);
		if (userId == null) {
			redirectAttributes.addFlashAttribute("importError", "No se pudo identificar al usuario");
			return "redirect:/admin/pacientes";
		}
		try {
			final MpxImportResult result = pacienteMpxImportService.importRegistration(mpxFile, userId);
			redirectAttributes.addFlashAttribute("importSuccess", "Paciente importado correctamente");
			if (result.duplicateWarning()) {
				redirectAttributes.addFlashAttribute("importDuplicateWarning",
						"Ya existe un paciente con el mismo nombre y fecha de nacimiento en su lista");
			}
			return "redirect:/admin/pacientes/" + result.pacienteId();
		}
		catch (final MpxImportException ex) {
			log.warn("MPX import rejected: {}", ex.getMessage());
			redirectAttributes.addFlashAttribute("importError", ex.getMessage());
			return "redirect:/admin/pacientes";
		}
		catch (final SubscriptionLimitExceededException ex) {
			redirectAttributes.addFlashAttribute("importError", subscriptionErrorResponses.resolve(ex));
			return "redirect:/admin/pacientes";
		}
	}

	@PostMapping(path = "/admin/pacientes/nuevo")
	public String addPaciente(@Valid final Paciente paciente, final BindingResult result, final Model model,
			@AuthenticationPrincipal final OidcUser principal) {
		log.debug("Grabando nuevo paciente: {}", LogRedaction.redactPaciente(paciente));
		final String userId = getUserId(principal);
		if (userId == null) {
			log.error("Cannot create patient: user ID is null");
			model.addAttribute("error", "No se pudo identificar al usuario");
			return "sbadmin/pacientes/nuevo";
		}
		String resultView;
		if (result.hasErrors()) {
			resultView = "sbadmin/pacientes/nuevo";
		}
		else {
			paciente.setUserId(userId);
			try {
				pacienteService.save(paciente);
				resultView = "redirect:/admin/pacientes";
			}
			catch (SubscriptionLimitExceededException ex) {
				model.addAttribute("error", subscriptionErrorResponses.resolve(ex));
				resultView = "sbadmin/pacientes/nuevo";
			}
		}
		return resultView;
	}

	@GetMapping(path = "/admin/pacientes/{id}")
	public String perfilPaciente(@PathVariable @NonNull final Long id, final Model model,
			@AuthenticationPrincipal final OidcUser principal) {
		log.debug("Cargando perfil de paciente {}", id);
		final String userId = getUserId(principal);
		if (userId == null) {
			throw new IllegalArgumentException("No se pudo identificar al usuario");
		}
		final Paciente paciente = pacienteRepository.findByIdAndUserId(id, userId)
			.orElseThrow(() -> new IllegalArgumentException("No se ha encontrado paciente con folio " + id));
		verifyPatientOwnership(paciente, userId);

		model.addAttribute("activeMenu", "perfil");
		model.addAttribute("paciente", paciente);
		PacienteProfileViewSupport.addAvatarAttributes(model, paciente);
		// calcular ultima consulta
		final DateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy");
		final CalendarEvent citaAnterior = getCitaAnterior(id);
		final String fechaCitaAnterior = citaAnterior != null ? dateFormat.format(citaAnterior.getEventDateTime()) : "";
		model.addAttribute("citaAnterior", fechaCitaAnterior);
		// calcular siguiente cita en calendario
		final CalendarEvent citaSiguiente = getCitaSiguiente(id);
		final String fechaCitaSiguiente = citaSiguiente != null ? dateFormat.format(citaSiguiente.getEventDateTime())
				: "";
		model.addAttribute("citaSiguiente", fechaCitaSiguiente);
		// obtener último registro de métricas corporales (consultas, antropométricos,
		// exámenes)
		bodyMetricRecordService.findLatestByPacienteId(id).ifPresent(latest -> {
			model.addAttribute("ultimoPeso", latest.getWeight());
			model.addAttribute("ultimaEstatura", latest.getHeight());
			model.addAttribute("ultimoImc", latest.getImc());
			model.addAttribute("ultimoNivelPeso", latest.getNivelPeso());
			final Double ultimoPorcentajeGrasa = latest.getBodyFatPercentage() != null ? latest.getBodyFatPercentage()
					: latest.getBodyFatIndex();
			model.addAttribute("ultimoPorcentajeGrasaCorporal", ultimoPorcentajeGrasa);
			model.addAttribute("ultimoIndiceGrasaCorporal", ultimoPorcentajeGrasa);
			model.addAttribute("ultimoMetodoObtencionComposicion", latest.getMetodoObtencionComposicion());
		});
		addLatestCompositionMetricsToModel(id, model);
		// Calculate age and check if patient is under 18 for growth table display
		final Integer age = calculateAge(paciente.getDob());
		final boolean isUnder18 = age != null && age < 18;
		model.addAttribute("isUnder18", isUnder18);
		// Fetch anthropometric measurements for growth table (only if under 18)
		if (isUnder18) {
			final List<AnthropometricMeasurement> measurements = anthropometricMeasurementService.findByPacienteId(id);
			// Sort by measurement date descending (most recent first)
			final List<AnthropometricMeasurement> sortedMeasurements = measurements.stream()
				.sorted(Comparator.comparing(AnthropometricMeasurement::getMeasurementDateTime).reversed())
				.collect(Collectors.toList());
			model.addAttribute("growthMeasurements", sortedMeasurements);
		}
		return "sbadmin/pacientes/perfil";
	}

	private CalendarEvent getCitaAnterior(@NonNull final Long pacienteId) {
		final Date now = new Date();
		final List<CalendarEvent> eventos = calendarEventService.findByPacienteId(pacienteId);
		CalendarEvent result = null;
		if (!eventos.isEmpty()) {
			final List<CalendarEvent> eventosPasados = eventos.stream()
				.filter(e -> e.getEventDateTime() != null && e.getEventDateTime().before(now))
				.filter(e -> e.getStatus() == EventStatus.COMPLETED)
				.sorted(Comparator.comparing(CalendarEvent::getEventDateTime).reversed())
				.collect(Collectors.toList());
			if (!eventosPasados.isEmpty()) {
				result = eventosPasados.get(0);
			}
		}
		return result;
	}

	private CalendarEvent getCitaSiguiente(@NonNull final Long pacienteId) {
		final Date now = new Date();
		final List<CalendarEvent> eventos = calendarEventService.findByPacienteId(pacienteId);
		CalendarEvent result = null;
		if (!eventos.isEmpty()) {
			final List<CalendarEvent> eventosFuturos = eventos.stream()
				.filter(e -> e.getEventDateTime() != null && e.getEventDateTime().after(now))
				.filter(e -> e.getStatus() == EventStatus.SCHEDULED)
				.sorted(Comparator.comparing(CalendarEvent::getEventDateTime))
				.collect(Collectors.toList());
			if (!eventosFuturos.isEmpty()) {
				result = eventosFuturos.get(0);
			}
		}
		return result;
	}

	@GetMapping(path = "/admin/pacientes/{id}/afiliacion")
	public String afiliacionPaciente(@PathVariable @NonNull Long id, Model model,
			@AuthenticationPrincipal final OidcUser principal) {
		log.debug("Cargando perfil de paciente {}", id);
		final String userId = getUserId(principal);
		if (userId == null) {
			throw new IllegalArgumentException("No se pudo identificar al usuario");
		}
		Paciente paciente = pacienteRepository.findByIdAndUserId(id, userId)
			.orElseThrow(() -> new IllegalArgumentException("No se ha encontrado paciente con folio " + id));
		verifyPatientOwnership(paciente, userId);

		model.addAttribute("activeMenu", "afiliacion");
		model.addAttribute("paciente", paciente);
		model.addAttribute("mobileAuth",
				PatientMobileAuthStatus.of(paciente.getPatientAuthSub(), auth0UserLookup.isConfigured()));
		return "sbadmin/pacientes/afiliacion";
	}

	@PostMapping(path = "/admin/pacientes/{id}/afiliacion")
	public String cambiaAfiliacionPaciente(@PathVariable @NonNull Long id, @Valid Paciente paciente,
			BindingResult result, Model model, @AuthenticationPrincipal final OidcUser principal) {
		log.debug("Cargando perfil de paciente {}", id);
		final String userId = getUserId(principal);
		if (userId == null) {
			throw new IllegalArgumentException("No se pudo identificar al usuario");
		}
		Paciente pacienteEntity = pacienteRepository.findByIdAndUserId(id, userId)
			.orElseThrow(() -> new IllegalArgumentException("No se ha encontrado paciente con folio " + id));
		verifyPatientOwnership(pacienteEntity, userId);

		pacienteEntity.setName(paciente.getName());
		pacienteEntity.setDob(paciente.getDob());
		pacienteEntity.setEmail(paciente.getEmail());
		pacienteEntity.setPhone(paciente.getPhone());
		pacienteEntity.setGender(paciente.getGender());
		pacienteEntity.setResponsibleName(paciente.getResponsibleName());
		pacienteEntity.setParentesco(paciente.getParentesco());
		if (paciente.getActivityFactorScale() != null) {
			pacienteEntity.setActivityFactorScale(paciente.getActivityFactorScale());
		}
		if (paciente.getPreferredBmrFormula() != null) {
			pacienteEntity.setPreferredBmrFormula(paciente.getPreferredBmrFormula());
		}
		pacienteEntity.setCustomFactorSedentary(paciente.getCustomFactorSedentary());
		pacienteEntity.setCustomFactorLight(paciente.getCustomFactorLight());
		pacienteEntity.setCustomFactorModerate(paciente.getCustomFactorModerate());
		pacienteEntity.setCustomFactorIntense(paciente.getCustomFactorIntense());
		pacienteEntity.setCustomFactorVeryIntense(paciente.getCustomFactorVeryIntense());

		pacienteRepository.save(pacienteEntity);
		return String.format("redirect:/admin/pacientes/%d/afiliacion", id);
	}

	@GetMapping(path = "/admin/pacientes/{id}/antecedentes")
	public String antecedentesPaciente(@PathVariable @NonNull Long id, Model model,
			@AuthenticationPrincipal final OidcUser principal) {
		log.debug("Cargando antecedentes de paciente {}", id);
		final String userId = getUserId(principal);
		if (userId == null) {
			throw new IllegalArgumentException("No se pudo identificar al usuario");
		}
		Paciente paciente = pacienteRepository.findByIdAndUserId(id, userId)
			.orElseThrow(() -> new IllegalArgumentException("No se ha encontrado paciente con folio " + id));
		verifyPatientOwnership(paciente, userId);

		model.addAttribute("activeMenu", "antecedentes");
		model.addAttribute("paciente", paciente);
		return "sbadmin/pacientes/antecedentes";
	}

	@PostMapping(path = "/admin/pacientes/{id}/antecedentes")
	public String cambiaAntecedentesPaciente(@PathVariable @NonNull Long id, @Valid Paciente paciente,
			BindingResult result, Model model, @AuthenticationPrincipal final OidcUser principal) {
		log.debug("Cargando perfil de paciente {}", id);
		final String userId = getUserId(principal);
		if (userId == null) {
			throw new IllegalArgumentException("No se pudo identificar al usuario");
		}
		Paciente pacienteEntity = pacienteRepository.findByIdAndUserId(id, userId)
			.orElseThrow(() -> new IllegalArgumentException("No se ha encontrado paciente con folio " + id));
		verifyPatientOwnership(pacienteEntity, userId);

		pacienteEntity.setTipoSanguineo(paciente.getTipoSanguineo());
		pacienteEntity.setAntecedentesNatales(paciente.getAntecedentesNatales());
		pacienteEntity.setAntecedentesPatologicosFamiliares(paciente.getAntecedentesPatologicosFamiliares());
		pacienteEntity.setAntecedentesPatologicosPersonales(paciente.getAntecedentesPatologicosPersonales());
		pacienteEntity.setAntecedentesPrenatales(paciente.getAntecedentesPrenatales());
		pacienteEntity.setComplicaciones(paciente.getComplicaciones());

		pacienteRepository.save(pacienteEntity);
		return String.format("redirect:/admin/pacientes/%d", id);
	}

	@GetMapping(path = "/admin/pacientes/{id}/desarrollo")
	public String desarrolloPaciente(@PathVariable @NonNull Long id, Model model,
			@AuthenticationPrincipal final OidcUser principal) {
		log.debug("Cargando datos de desarrollo de paciente {}", id);
		final String userId = getUserId(principal);
		if (userId == null) {
			throw new IllegalArgumentException("No se pudo identificar al usuario");
		}
		Paciente paciente = pacienteRepository.findByIdAndUserId(id, userId)
			.orElseThrow(() -> new IllegalArgumentException("No se ha encontrado paciente con folio " + id));
		verifyPatientOwnership(paciente, userId);

		model.addAttribute("activeMenu", "desarrollo");
		model.addAttribute("paciente", paciente);
		model.addAttribute("isEligibleForPregnancy", isEligibleForPregnancy(paciente));
		model.addAttribute("suggestedStressTypes",
				com.nutriconsultas.paciente.calculation.PhysiologicalStressCatalog.suggestFromPathologies(paciente));
		return "sbadmin/pacientes/desarrollo";
	}

	@PostMapping(path = "/admin/pacientes/{id}/desarrollo")
	public String cambiaDesarrolloPaciente(@PathVariable @NonNull Long id, @Valid Paciente paciente,
			BindingResult result, Model model, @AuthenticationPrincipal final OidcUser principal) {
		log.debug("Cargando desarrollo de paciente {}", id);
		final String userId = getUserId(principal);
		if (userId == null) {
			throw new IllegalArgumentException("No se pudo identificar al usuario");
		}
		Paciente pacienteEntity = pacienteRepository.findByIdAndUserId(id, userId)
			.orElseThrow(() -> new IllegalArgumentException("No se ha encontrado paciente con folio " + id));
		verifyPatientOwnership(pacienteEntity, userId);

		pacienteEntity.setHistorialAlimenticio(paciente.getHistorialAlimenticio());
		pacienteEntity.setDesarrolloPsicomotor(paciente.getDesarrolloPsicomotor());
		pacienteEntity.setAlergias(paciente.getAlergias());

		pacienteEntity.setHipertension(paciente.getHipertension());
		pacienteEntity.setDiabetes(paciente.getDiabetes());
		pacienteEntity.setHipotiroidismo(paciente.getHipotiroidismo());
		pacienteEntity.setObesidad(paciente.getObesidad());
		pacienteEntity.setAnemia(paciente.getAnemia());
		pacienteEntity.setBulimia(paciente.getBulimia());
		pacienteEntity.setAnorexia(paciente.getAnorexia());

		// Validate and set pregnancy state
		if (paciente.getPregnancy() != null && paciente.getPregnancy()) {
			if (!isEligibleForPregnancy(pacienteEntity)) {
				result.rejectValue("pregnancy", "ValidPregnancy",
						"El estado de embarazo solo puede ser asignado a pacientes femeninas entre 12 y 50 años");
				model.addAttribute("activeMenu", "desarrollo");
				model.addAttribute("paciente", pacienteEntity);
				return "sbadmin/pacientes/desarrollo";
			}
		}
		pacienteEntity.setPregnancy(paciente.getPregnancy() != null ? paciente.getPregnancy() : false);

		pacienteEntity.setPhysiologicalStressActive(
				paciente.getPhysiologicalStressActive() != null ? paciente.getPhysiologicalStressActive() : false);
		pacienteEntity.setPhysiologicalStressType(paciente.getPhysiologicalStressType());
		if (paciente.getStressFormulaTable() != null) {
			pacienteEntity.setStressFormulaTable(paciente.getStressFormulaTable());
		}
		if (paciente.getStressIncrementMode() != null) {
			pacienteEntity.setStressIncrementMode(paciente.getStressIncrementMode());
		}
		pacienteEntity.setStressFactorValue(paciente.getStressFactorValue());
		pacienteEntity.setStressValidFrom(paciente.getStressValidFrom());
		pacienteEntity.setStressValidUntil(paciente.getStressValidUntil());
		pacienteEntity.setStressFeverTemperature(paciente.getStressFeverTemperature());

		pacienteRepository.save(pacienteEntity);
		return String.format("redirect:/admin/pacientes/%d", id);
	}

	@GetMapping(path = "/admin/pacientes/{id}/dietas")
	public String dietasPaciente(@PathVariable @NonNull final Long id, final Model model,
			@AuthenticationPrincipal final OidcUser principal) {
		log.debug("Cargando dietas asignadas de paciente {}", id);
		final String userId = getUserId(principal);
		if (userId == null) {
			throw new IllegalArgumentException("No se pudo identificar al usuario");
		}
		final Paciente paciente = pacienteRepository.findByIdAndUserId(id, userId)
			.orElseThrow(() -> new IllegalArgumentException("No se ha encontrado paciente con folio " + id));
		verifyPatientOwnership(paciente, userId);

		model.addAttribute("activeMenu", "plan-alimentario");
		model.addAttribute("paciente", paciente);
		// obtener dietas asignadas
		final List<PacienteDieta> dietasAsignadas = pacienteDietaService.findByPacienteId(id);
		// Calcular macronutrientes para cada dieta asignada
		// Load dieta with ingestas to calculate macronutrientes
		for (final PacienteDieta pacienteDieta : dietasAsignadas) {
			if (pacienteDieta.getDieta() != null) {
				final Long dietaId = pacienteDieta.getDieta().getId();
				if (dietaId != null) {
					final com.nutriconsultas.dieta.Dieta dieta = dietaService.getDieta(dietaId);
					if (dieta != null) {
						DietaNutritionCalculator.applyCalculatedNutrients(dieta);
						pacienteDieta.setDieta(dieta);
					}
				}
			}
		}
		model.addAttribute("dietasAsignadas", dietasAsignadas);
		final List<PacienteDieta> dietasActivas = pacienteDietaService.findActiveByPacienteId(id);
		// Calcular macronutrientes para dietas activas
		for (final PacienteDieta pacienteDieta : dietasActivas) {
			if (pacienteDieta.getDieta() != null) {
				final Long dietaId = pacienteDieta.getDieta().getId();
				if (dietaId != null) {
					final com.nutriconsultas.dieta.Dieta dieta = dietaService.getDieta(dietaId);
					if (dieta != null) {
						DietaNutritionCalculator.applyCalculatedNutrients(dieta);
						pacienteDieta.setDieta(dieta);
					}
				}
			}
		}
		model.addAttribute("dietasActivas", dietasActivas);
		// obtener todas las dietas disponibles para asignar (legacy attribute for dietas
		// template)
		model.addAttribute("dietasDisponibles", getDietasDisponiblesWithCalculatedNutrients());
		return "sbadmin/pacientes/dietas";
	}

	@GetMapping(path = "/admin/pacientes/{id}/historial")
	public String historialPaciente(@PathVariable @NonNull Long id, Model model,
			@AuthenticationPrincipal final OidcUser principal) {
		log.debug("Cargando datos de consultas de paciente {}", id);
		final String userId = getUserId(principal);
		if (userId == null) {
			throw new IllegalArgumentException("No se pudo identificar al usuario");
		}
		Paciente paciente = pacienteRepository.findByIdAndUserId(id, userId)
			.orElseThrow(() -> new IllegalArgumentException("No se ha encontrado paciente con folio " + id));
		verifyPatientOwnership(paciente, userId);

		model.addAttribute("activeMenu", "historial");
		model.addAttribute("paciente", paciente);
		return "sbadmin/pacientes/historial";
	}

	@GetMapping(path = "/admin/pacientes/{id}/consulta")
	public String consultaPaciente(@PathVariable @NonNull Long id, Model model,
			@AuthenticationPrincipal final OidcUser principal) {
		log.debug("Cargando datos de consultas de paciente {}", id);
		final String userId = getUserId(principal);
		if (userId == null) {
			throw new IllegalArgumentException("No se pudo identificar al usuario");
		}
		Paciente paciente = pacienteRepository.findByIdAndUserId(id, userId)
			.orElseThrow(() -> new IllegalArgumentException("No se ha encontrado paciente con folio " + id));
		verifyPatientOwnership(paciente, userId);

		model.addAttribute("activeMenu", "historial");
		model.addAttribute("paciente", paciente);
		CalendarEvent evento = new CalendarEvent();
		evento.setEventDateTime(new Date());
		evento.setPaciente(paciente);
		evento.setTitle("Consulta");
		evento.setDurationMinutes(60);
		evento.setStatus(EventStatus.SCHEDULED);
		if (Boolean.TRUE.equals(paciente.getPhysiologicalStressActive())) {
			evento.setPhysiologicalStressActive(true);
			evento.setPhysiologicalStressType(paciente.getPhysiologicalStressType());
			evento.setStressFormulaTable(paciente.getStressFormulaTable());
			evento.setStressIncrementMode(paciente.getStressIncrementMode());
			evento.setStressFactorValue(paciente.getStressFactorValue());
			evento.setStressValidFrom(paciente.getStressValidFrom());
			evento.setStressValidUntil(paciente.getStressValidUntil());
			evento.setStressFeverTemperature(paciente.getStressFeverTemperature());
		}
		model.addAttribute("consulta", evento);
		model.addAttribute("suggestedStressTypes",
				com.nutriconsultas.paciente.calculation.PhysiologicalStressCatalog.suggestFromPathologies(paciente));
		return "sbadmin/pacientes/consulta";
	}

	@PostMapping(path = "/admin/pacientes/{pacienteId}/consulta")
	public String agregarConsultaPaciente(@PathVariable @NonNull Long pacienteId, @Valid CalendarEvent evento,
			BindingResult result, Model model, @AuthenticationPrincipal final OidcUser principal) {
		log.debug("Grabando consulta {}", LogRedaction.redactCalendarEvent(evento));
		final String userId = getUserId(principal);
		if (userId == null) {
			throw new IllegalArgumentException("No se pudo identificar al usuario");
		}
		final Paciente paciente = Objects.requireNonNull(pacienteRepository.findByIdAndUserId(pacienteId, userId)
			.orElseThrow(() -> new IllegalArgumentException("No se ha encontrado paciente con folio " + pacienteId)),
				"Paciente must not be null");
		verifyPatientOwnership(paciente, userId);

		evento.setPaciente(paciente);

		final BmiCalculationResult bmiResult = calculateBmiAndBodyFat(evento, paciente);
		updatePatientWeightIfNeeded(paciente, evento, bmiResult.getImc(), bmiResult.getNivelPeso(), pacienteId,
				evento.getEventDateTime());
		setEventCalculatedValues(evento, bmiResult);
		setEventDefaultValues(evento);

		log.debug("Evento lista para grabar {}", LogRedaction.redactCalendarEvent(evento));
		final CalendarEvent savedEvent = calendarEventService.save(evento);
		if (Boolean.TRUE.equals(savedEvent.getPhysiologicalStressActive())) {
			paciente.setPhysiologicalStressActive(savedEvent.getPhysiologicalStressActive());
			paciente.setPhysiologicalStressType(savedEvent.getPhysiologicalStressType());
			paciente.setStressFormulaTable(savedEvent.getStressFormulaTable());
			paciente.setStressIncrementMode(savedEvent.getStressIncrementMode());
			paciente.setStressFactorValue(savedEvent.getStressFactorValue());
			paciente.setStressValidFrom(savedEvent.getStressValidFrom());
			paciente.setStressValidUntil(savedEvent.getStressValidUntil());
			paciente.setStressFeverTemperature(savedEvent.getStressFeverTemperature());
			pacienteRepository.save(paciente);
		}
		return String.format("redirect:/admin/pacientes/%d/historial", pacienteId);
	}

	@GetMapping(path = "/admin/pacientes/{id}/clinicos")
	public String clinicosPaciente(@PathVariable @NonNull Long id, Model model,
			@AuthenticationPrincipal final OidcUser principal) {
		log.debug("Cargando formulario de examen clínico para paciente {}", id);
		final String userId = getUserId(principal);
		if (userId == null) {
			throw new IllegalArgumentException("No se pudo identificar al usuario");
		}
		Paciente paciente = pacienteRepository.findByIdAndUserId(id, userId)
			.orElseThrow(() -> new IllegalArgumentException("No se ha encontrado paciente con folio " + id));
		verifyPatientOwnership(paciente, userId);

		model.addAttribute("activeMenu", "historial");
		model.addAttribute("paciente", paciente);
		ClinicalExam exam = new ClinicalExam();
		exam.setExamDateTime(new Date());
		exam.setPaciente(paciente);
		exam.setTitle("Examen Clínico");
		model.addAttribute("clinicos", exam);
		return "sbadmin/pacientes/clinicos";
	}

	@PostMapping(path = "/admin/pacientes/{pacienteId}/clinicos")
	public String agregarClinicosPaciente(@PathVariable @NonNull Long pacienteId, @Valid ClinicalExam exam,
			BindingResult result, Model model, @AuthenticationPrincipal final OidcUser principal) {
		log.debug("Grabando examen clínico {}", LogRedaction.redactClinicalExam(exam));
		final String userId = getUserId(principal);
		if (userId == null) {
			throw new IllegalArgumentException("No se pudo identificar al usuario");
		}
		final Paciente paciente = Objects.requireNonNull(pacienteRepository.findByIdAndUserId(pacienteId, userId)
			.orElseThrow(() -> new IllegalArgumentException("No se ha encontrado paciente con folio " + pacienteId)),
				"Paciente must not be null");
		verifyPatientOwnership(paciente, userId);

		exam.setPaciente(paciente);

		final BmiCalculationResult bmiResult = calculateBmiAndBodyFatForExam(exam, paciente);
		updatePatientWeightIfNeededForExam(paciente, exam, bmiResult.getImc(), bmiResult.getNivelPeso(), pacienteId,
				exam.getExamDateTime());
		setExamCalculatedValues(exam, bmiResult);
		setExamDefaultValues(exam);

		log.debug("Examen clínico lista para grabar {}", LogRedaction.redactClinicalExam(exam));
		clinicalExamService.save(exam);
		return String.format("redirect:/admin/pacientes/%d/historial", pacienteId);
	}

	@GetMapping(path = "/admin/pacientes/{pacienteId}/examen-clinico/{examId}")
	public String verExamenClinico(@PathVariable @NonNull final Long pacienteId,
			@PathVariable @NonNull final Long examId, final Model model,
			@AuthenticationPrincipal final OidcUser principal) {
		log.debug("Cargando examen clínico {} para paciente {}", examId, pacienteId);
		final String userId = getUserId(principal);
		if (userId == null) {
			throw new IllegalArgumentException("No se pudo identificar al usuario");
		}
		final ClinicalExam exam = clinicalExamService.findById(examId);
		if (exam == null) {
			throw new IllegalArgumentException("No se ha encontrado examen clínico con id " + examId);
		}
		if (!exam.getPaciente().getId().equals(pacienteId)) {
			throw new IllegalArgumentException("El examen clínico no pertenece al paciente especificado");
		}
		verifyPatientOwnership(exam.getPaciente(), userId);

		model.addAttribute("activeMenu", "historial");
		model.addAttribute("exam", exam);
		model.addAttribute("paciente", exam.getPaciente());
		return "sbadmin/pacientes/ver-examen-clinico";
	}

	@GetMapping(path = "/admin/pacientes/{id}/antropometricos")
	public String antropometricosPaciente(@PathVariable @NonNull Long id, Model model,
			@AuthenticationPrincipal final OidcUser principal) {
		log.debug("Cargando formulario de medición antropométrica para paciente {}", id);
		final String userId = getUserId(principal);
		if (userId == null) {
			throw new IllegalArgumentException("No se pudo identificar al usuario");
		}
		Paciente paciente = pacienteRepository.findByIdAndUserId(id, userId)
			.orElseThrow(() -> new IllegalArgumentException("No se ha encontrado paciente con folio " + id));
		verifyPatientOwnership(paciente, userId);

		model.addAttribute("activeMenu", "historial");
		model.addAttribute("paciente", paciente);
		// Calculate age for BMR calculations
		final Integer age = calculateAge(paciente.getDob());
		model.addAttribute("patientAge", age);
		// Determine if patient is male for BMR calculations
		final Boolean isMale = paciente.getGender() != null && "M".equals(paciente.getGender());
		model.addAttribute("patientIsMale", isMale);
		AnthropometricMeasurement measurement = new AnthropometricMeasurement();
		measurement.setMeasurementDateTime(new Date());
		measurement.setPaciente(paciente);
		measurement.setTitle("Medición Antropométrica");
		// Initialize category objects so form can bind to nested properties
		measurement.setBodyMass(new com.nutriconsultas.clinical.exam.anthropometric.BodyMass());
		measurement.setBioimpedance(new com.nutriconsultas.clinical.exam.anthropometric.Bioimpedance());
		measurement.setSkinfolds(new com.nutriconsultas.clinical.exam.anthropometric.Skinfolds());
		measurement.setCircumferences(new com.nutriconsultas.clinical.exam.anthropometric.Circumferences());
		measurement.setDiameters(new com.nutriconsultas.clinical.exam.anthropometric.Diameters());
		measurement.setBodyComposition(new com.nutriconsultas.clinical.exam.anthropometric.BodyComposition());
		model.addAttribute("antropometrico", measurement);
		return "sbadmin/pacientes/antropometricos";
	}

	@PostMapping(path = "/admin/pacientes/{pacienteId}/antropometricos")
	public String agregarAntropometricosPaciente(@PathVariable @NonNull Long pacienteId,
			@Valid com.nutriconsultas.clinical.exam.AnthropometricMeasurement measurement, BindingResult result,
			Model model, @AuthenticationPrincipal final OidcUser principal) {
		log.debug("Grabando medición antropométrica {}", LogRedaction.redactAnthropometricMeasurement(measurement));
		final String userId = getUserId(principal);
		if (userId == null) {
			throw new IllegalArgumentException("No se pudo identificar al usuario");
		}
		final Paciente paciente = Objects.requireNonNull(pacienteRepository.findByIdAndUserId(pacienteId, userId)
			.orElseThrow(() -> new IllegalArgumentException("No se ha encontrado paciente con folio " + pacienteId)),
				"Paciente must not be null");
		verifyPatientOwnership(paciente, userId);

		measurement.setPaciente(paciente);
		ensureAnthropometricBodyMass(measurement);

		final BmiCalculationResult bmiResult = calculateBmiAndBodyFatForMeasurement(measurement, paciente);
		updatePatientWeightIfNeededForMeasurement(paciente, measurement, bmiResult.getImc(), bmiResult.getNivelPeso(),
				pacienteId, measurement.getMeasurementDateTime());
		setMeasurementCalculatedValues(measurement, bmiResult);
		somatotypeService.applyToMeasurement(measurement, paciente);
		setMeasurementDefaultValues(measurement);

		log.debug("Medición antropométrica lista para grabar {}",
				LogRedaction.redactAnthropometricMeasurement(measurement));
		anthropometricMeasurementService.save(measurement);
		return String.format("redirect:/admin/pacientes/%d/historial", pacienteId);
	}

	@GetMapping(path = "/admin/pacientes/{pacienteId}/antropometrico/{measurementId}")
	public String verAntropometrico(@PathVariable @NonNull final Long pacienteId,
			@PathVariable @NonNull final Long measurementId, final Model model,
			@AuthenticationPrincipal final OidcUser principal) {
		log.debug("Cargando medición antropométrica {} para paciente {}", measurementId, pacienteId);
		final String userId = getUserId(principal);
		if (userId == null) {
			throw new IllegalArgumentException("No se pudo identificar al usuario");
		}
		final com.nutriconsultas.clinical.exam.AnthropometricMeasurement measurement = anthropometricMeasurementService
			.findById(measurementId);
		if (measurement == null) {
			throw new IllegalArgumentException("No se ha encontrado medición antropométrica con id " + measurementId);
		}
		if (!measurement.getPaciente().getId().equals(pacienteId)) {
			throw new IllegalArgumentException("La medición antropométrica no pertenece al paciente especificado");
		}
		verifyPatientOwnership(measurement.getPaciente(), userId);

		model.addAttribute("activeMenu", "historial");
		model.addAttribute("measurement", measurement);
		model.addAttribute("paciente", measurement.getPaciente());
		// Calculate age for BMR calculations
		final Integer age = calculateAge(measurement.getPaciente().getDob());
		model.addAttribute("patientAge", age);
		// Determine if patient is male for BMR calculations
		final Boolean isMale = measurement.getPaciente().getGender() != null
				&& "M".equals(measurement.getPaciente().getGender());
		model.addAttribute("patientIsMale", isMale);
		model.addAttribute("editableFields", AnthropometricFieldCatalog.allDefinitions());
		return "sbadmin/pacientes/ver-antropometrico";
	}

	@GetMapping(path = "/admin/pacientes/{id}/dietas/asignar")
	public String asignarDieta(@PathVariable @NonNull final Long id, final Model model,
			@AuthenticationPrincipal final OidcUser principal) {
		log.debug("Cargando formulario para asignar dieta a paciente {}", id);
		final String userId = getUserId(principal);
		if (userId == null) {
			throw new IllegalArgumentException("No se pudo identificar al usuario");
		}
		final Paciente paciente = pacienteRepository.findByIdAndUserId(id, userId)
			.orElseThrow(() -> new IllegalArgumentException("No se ha encontrado paciente con folio " + id));
		verifyPatientOwnership(paciente, userId);

		model.addAttribute("activeMenu", "perfil");
		model.addAttribute("paciente", paciente);
		model.addAttribute("requerimientoKcal", resolveRequerimientoKcal(paciente));
		model.addAttribute("pacienteDieta", new PacienteDieta());
		return "sbadmin/pacientes/asignar-dieta";
	}

	@PostMapping(path = "/admin/pacientes/{id}/dietas/asignar")
	public String guardarAsignacionDieta(@PathVariable @NonNull final Long id, final PacienteDieta pacienteDieta,
			final BindingResult result, final Model model,
			@org.springframework.web.bind.annotation.RequestParam(required = true) @NonNull final Long dietaId,
			@AuthenticationPrincipal final OidcUser principal) {
		log.debug("Guardando asignación de dieta {} para paciente {}", dietaId, id);
		final String userId = getUserId(principal);
		if (userId == null) {
			throw new IllegalArgumentException("No se pudo identificar al usuario");
		}

		// Check if pacienteDieta is null before using it
		if (pacienteDieta == null) {
			throw new IllegalArgumentException("PacienteDieta cannot be null");
		}

		// Set paciente and dieta from parameters before validation
		// This is necessary because paciente and dieta are validated as @NotNull but come
		// from
		// path variable and request parameter, not from form binding
		final Paciente paciente = pacienteRepository.findByIdAndUserId(id, userId)
			.orElseThrow(() -> new IllegalArgumentException("No se ha encontrado paciente con folio " + id));
		verifyPatientOwnership(paciente, userId);
		final com.nutriconsultas.dieta.Dieta dieta = dietaRepository.findById(dietaId)
			.orElseThrow(() -> new IllegalArgumentException("No se ha encontrado dieta con id " + dietaId));

		pacienteDieta.setPaciente(paciente);
		pacienteDieta.setDieta(dieta);

		// Manual validation since paciente and dieta come from parameters, not form
		// binding
		boolean hasErrors = false;
		if (pacienteDieta.getStartDate() == null) {
			result.rejectValue("startDate", "NotNull", "La fecha de inicio es requerida");
			hasErrors = true;
		}
		if (pacienteDieta.getStatus() == null) {
			result.rejectValue("status", "NotNull", "El estado es requerido");
			hasErrors = true;
		}
		if (pacienteDieta.getPaciente() == null) {
			result.rejectValue("paciente", "NotNull", "El paciente es requerido");
			hasErrors = true;
		}
		if (pacienteDieta.getDieta() == null) {
			result.rejectValue("dieta", "NotNull", "La dieta es requerida");
			hasErrors = true;
		}

		if (hasErrors) {
			log.error("Validation errors found: {}", result.getAllErrors());
			result.getAllErrors()
				.forEach(error -> log.error("Error: {} - {}", error.getObjectName(), error.getDefaultMessage()));
			model.addAttribute("activeMenu", "perfil");
			model.addAttribute("paciente", paciente);
			model.addAttribute("requerimientoKcal", resolveRequerimientoKcal(paciente));
			return "sbadmin/pacientes/asignar-dieta";
		}
		final PacienteDieta saved = pacienteDietaService.assignDieta(id, dietaId, pacienteDieta, userId);
		log.debug("Dieta asignada exitosamente: {}", LogRedaction.redactPacienteDieta(saved));
		return String.format("redirect:/admin/pacientes/%d/dietas", id);
	}

	@GetMapping(path = "/admin/pacientes/{pacienteId}/dietas/{id}/editar")
	public String editarAsignacionDieta(@PathVariable @NonNull final Long pacienteId,
			@PathVariable @NonNull final Long id, final Model model,
			@AuthenticationPrincipal final OidcUser principal) {
		log.debug("Cargando formulario para editar asignación de dieta {}", id);
		final String userId = getUserId(principal);
		if (userId == null) {
			throw new IllegalArgumentException("No se pudo identificar al usuario");
		}
		final Paciente paciente = pacienteRepository.findByIdAndUserId(pacienteId, userId)
			.orElseThrow(() -> new IllegalArgumentException("No se ha encontrado paciente con folio " + pacienteId));
		verifyPatientOwnership(paciente, userId);
		final PacienteDieta pacienteDieta = pacienteDietaService.findById(id);
		if (pacienteDieta == null) {
			throw new IllegalArgumentException("No se ha encontrado asignación de dieta con id " + id);
		}

		model.addAttribute("activeMenu", "perfil");
		model.addAttribute("paciente", paciente);
		model.addAttribute("pacienteDieta", pacienteDieta);
		return "sbadmin/pacientes/editar-dieta";
	}

	@PostMapping(path = "/admin/pacientes/{pacienteId}/dietas/{id}/editar")
	public String actualizarAsignacionDieta(@PathVariable @NonNull final Long pacienteId,
			@PathVariable @NonNull final Long id, final PacienteDieta pacienteDieta, final BindingResult result,
			final Model model, @AuthenticationPrincipal final OidcUser principal) {
		log.debug("Actualizando asignación de dieta {}", id);
		final String userId = getUserId(principal);
		if (userId == null) {
			throw new IllegalArgumentException("No se pudo identificar al usuario");
		}

		// Load existing assignment to preserve paciente and dieta relationships
		// These are not in the form, so they need to be set from the existing entity
		final PacienteDieta existing = pacienteDietaService.findById(id);
		if (existing == null) {
			throw new IllegalArgumentException("No se ha encontrado asignación de dieta con id " + id);
		}

		// Set paciente and dieta from existing entity
		// This prevents validation errors since these fields are @NotNull but not in the
		// form
		pacienteDieta.setPaciente(existing.getPaciente());
		pacienteDieta.setDieta(existing.getDieta());
		pacienteDieta.setId(existing.getId());

		// Manual validation for fields that come from the form
		boolean hasErrors = false;
		if (pacienteDieta.getStartDate() == null) {
			result.rejectValue("startDate", "NotNull", "La fecha de inicio es requerida");
			hasErrors = true;
		}
		if (pacienteDieta.getStatus() == null) {
			result.rejectValue("status", "NotNull", "El estado es requerido");
			hasErrors = true;
		}

		if (hasErrors) {
			log.error("Validation errors found: {}", result.getAllErrors());
			result.getAllErrors()
				.forEach(error -> log.error("Error: {} - {}", error.getObjectName(), error.getDefaultMessage()));
			final Paciente paciente = pacienteRepository.findByIdAndUserId(pacienteId, userId)
				.orElseThrow(
						() -> new IllegalArgumentException("No se ha encontrado paciente con folio " + pacienteId));
			verifyPatientOwnership(paciente, userId);
			model.addAttribute("activeMenu", "perfil");
			model.addAttribute("paciente", paciente);
			model.addAttribute("pacienteDieta", pacienteDieta);
			return "sbadmin/pacientes/editar-dieta";
		}
		final PacienteDieta updated = pacienteDietaService.updateAssignment(id, pacienteDieta);
		log.info("Asignación de dieta actualizada exitosamente: {}", LogRedaction.redactPacienteDieta(updated));
		return String.format("redirect:/admin/pacientes/%d/dietas", pacienteId);
	}

	@PostMapping(path = "/admin/pacientes/{pacienteId}/dietas/{id}/cancelar")
	public String cancelarAsignacionDieta(@PathVariable @NonNull final Long pacienteId,
			@PathVariable @NonNull final Long id) {
		log.debug("Cancelando asignación de dieta {}", id);
		pacienteDietaService.cancelAssignment(id);
		return String.format("redirect:/admin/pacientes/%d/dietas", pacienteId);
	}

	/**
	 * Exports patient registration profile to a portable {@code .mpx} YAML file (#221).
	 * Clinical history and tenant identifiers are excluded from the payload.
	 * @param id the patient ID
	 * @param principal the authenticated nutritionist
	 * @return YAML attachment or 404 when the patient is not owned by the user
	 */
	@GetMapping(path = "/admin/pacientes/{id}/export.mpx")
	public ResponseEntity<byte[]> exportPacienteMpx(@PathVariable("id") @NonNull final Long id,
			@AuthenticationPrincipal final OidcUser principal) {
		log.debug("Exporting MPX registration for paciente {}", id);
		final String userId = getUserId(principal);
		if (userId == null) {
			return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED).build();
		}
		try {
			final MpxExportResult export = pacienteMpxExportService.exportRegistration(id, userId);
			return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + export.filename() + "\"")
				.contentType(MediaType.parseMediaType("application/x-yaml"))
				.contentLength(export.content().length)
				.body(export.content());
		}
		catch (final IllegalArgumentException ex) {
			log.warn("MPX export denied for paciente {}: {}", id, ex.getMessage());
			return ResponseEntity.notFound().build();
		}
	}

	/**
	 * Generates and returns a PDF document for a dieta assigned to a patient.
	 *
	 * <p>
	 * This endpoint generates a patient-specific PDF with patient information included.
	 * It verifies that the dieta is assigned to the specified patient before generating
	 * the PDF.
	 *
	 * <p>
	 * The PDF includes patient information (name, DOB, gender, weight, height),
	 * assignment dates, notes (if any), plus all dieta content.
	 * @param pacienteId the ID of the patient
	 * @param dietaId the ID of the dieta to generate PDF for
	 * @return ResponseEntity with PDF document and appropriate headers
	 */
	@GetMapping(path = "/admin/pacientes/{pacienteId}/dietas/{dietaId}/print")
	public ResponseEntity<byte[]> printDietaFromPatient(@PathVariable @NonNull final Long pacienteId,
			@PathVariable @NonNull final Long dietaId, @AuthenticationPrincipal final OidcUser principal) {
		log.debug("Generating PDF for dieta {} from patient {} (with patient info)", dietaId, pacienteId);
		final String userId = getUserId(principal);
		if (userId == null) {
			return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED).build();
		}
		// Verify patient exists and belongs to user
		final Paciente paciente = pacienteRepository.findByIdAndUserId(pacienteId, userId).orElse(null);
		if (paciente == null) {
			log.error("Patient with id {} not found for user {}", pacienteId, userId);
			return ResponseEntity.notFound().build();
		}
		verifyPatientOwnership(paciente, userId);
		// Verify dieta exists and is assigned to this patient
		final List<PacienteDieta> assignments = pacienteDietaRepository.findByPacienteId(pacienteId);
		final PacienteDieta pacienteDieta = assignments.stream()
			.filter(a -> a.getDieta() != null && a.getDieta().getId().equals(dietaId))
			.findFirst()
			.orElse(null);
		if (pacienteDieta == null) {
			log.error("Dieta {} is not assigned to patient {}", dietaId, pacienteId);
			return ResponseEntity.notFound().build();
		}
		// Generate PDF with patient information included
		final byte[] pdfBytes = dietaPdfService.generatePdf(dietaId, true);
		final com.nutriconsultas.dieta.Dieta dieta = dietaService.getDieta(dietaId);
		final String fileName = (dieta != null && dieta.getNombre() != null ? dieta.getNombre() : "dieta") + ".pdf";
		return ResponseEntity.ok()
			.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
			.contentType(MediaType.parseMediaType("application/pdf"))
			.body(pdfBytes);
	}

	/**
	 * Calculates age from date of birth.
	 * @param dob Date of birth
	 * @return Age in years, or null if dob is null or in the future
	 */
	private Integer calculateAge(final Date dob) {
		if (dob == null) {
			return null;
		}
		LocalDate birthDate;
		if (dob instanceof java.sql.Date) {
			// java.sql.Date doesn't support toInstant(), convert directly
			birthDate = ((java.sql.Date) dob).toLocalDate();
		}
		else {
			birthDate = dob.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
		}
		final LocalDate currentDate = LocalDate.now();
		if (birthDate.isAfter(currentDate)) {
			log.warn("Date of birth is in the future: {}", dob);
			return null;
		}
		return Period.between(birthDate, currentDate).getYears();
	}

	/**
	 * Validates if a patient is eligible for pregnancy state (female, age 12-50).
	 * @param paciente the patient to validate
	 * @return true if eligible, false otherwise
	 */
	private boolean isEligibleForPregnancy(final Paciente paciente) {
		if (paciente == null) {
			return false;
		}

		// Check gender - must be female
		if (paciente.getGender() == null || !"F".equals(paciente.getGender())) {
			log.debug("Patient is not female, cannot set pregnancy");
			return false;
		}

		// Check age - must be between 12 and 50
		if (paciente.getDob() == null) {
			log.debug("Patient has no date of birth, cannot validate pregnancy eligibility");
			return false;
		}

		final Integer age = calculateAge(paciente.getDob());
		if (age == null) {
			log.debug("Could not calculate age for patient, cannot validate pregnancy eligibility");
			return false;
		}

		final boolean eligible = age >= 12 && age <= 50;
		if (!eligible) {
			log.debug("Patient age {} is not between 12 and 50, cannot set pregnancy", age);
		}
		return eligible;
	}

	/**
	 * Loads available dietas with kcal/macros calculated from ingestas (same formula as
	 * {@code /admin/dietas} grid), not the persisted {@code energia} column.
	 */
	private List<com.nutriconsultas.dieta.Dieta> getDietasDisponiblesWithCalculatedNutrients() {
		return dietaService.getDietas()
			.stream()
			.map(this::enrichDietaWithCalculatedNutrients)
			.collect(Collectors.toList());
	}

	private com.nutriconsultas.dieta.Dieta enrichDietaWithCalculatedNutrients(
			final com.nutriconsultas.dieta.Dieta dieta) {
		if (dieta == null || dieta.getId() == null) {
			return dieta;
		}
		final com.nutriconsultas.dieta.Dieta fullDieta = dietaService.getDieta(dieta.getId());
		if (fullDieta == null) {
			return dieta;
		}
		DietaNutritionCalculator.applyCalculatedNutrients(fullDieta);
		return fullDieta;
	}

	/**
	 * Record to hold BMI calculation results.
	 */
	private static final class BmiCalculationResult {

		private final Double imc;

		private final NivelPeso nivelPeso;

		private final Double bodyFatPercentage;

		BmiCalculationResult(final Double imc, final NivelPeso nivelPeso, final Double bodyFatPercentage) {
			this.imc = imc;
			this.nivelPeso = nivelPeso;
			this.bodyFatPercentage = bodyFatPercentage;
		}

		Double getImc() {
			return imc;
		}

		NivelPeso getNivelPeso() {
			return nivelPeso;
		}

		@SuppressWarnings("unused")
		Double getBodyFatPercentage() {
			return bodyFatPercentage;
		}

	}

	/**
	 * Calculates BMI, weight level, and body fat percentage for a CalendarEvent.
	 */
	private BmiCalculationResult calculateBmiAndBodyFat(final CalendarEvent evento, final Paciente paciente) {
		Double imc = null;
		NivelPeso np = null;
		Double bodyFatPercentage = null;

		if (evento.getPeso() != null && evento.getEstatura() != null) {
			imc = evento.getPeso() / Math.pow(evento.getEstatura(), 2);
			np = calculateNivelPeso(imc);
			bodyFatPercentage = calculateBodyFatPercentage(imc, paciente);
			if (bodyFatPercentage != null) {
				evento.setIndiceGrasaCorporal(bodyFatPercentage);
			}
		}

		return new BmiCalculationResult(imc, np, bodyFatPercentage);
	}

	/**
	 * Calculates BMI, weight level, and body fat percentage for a ClinicalExam.
	 */
	private BmiCalculationResult calculateBmiAndBodyFatForExam(final ClinicalExam exam, final Paciente paciente) {
		Double imc = null;
		NivelPeso np = null;
		Double bodyFatPercentage = null;

		if (exam.getPeso() != null && exam.getEstatura() != null) {
			imc = exam.getPeso() / Math.pow(exam.getEstatura(), 2);
			np = calculateNivelPeso(imc);
			bodyFatPercentage = calculateBodyFatPercentage(imc, paciente);
			if (bodyFatPercentage != null) {
				exam.setIndiceGrasaCorporal(bodyFatPercentage);
			}
		}

		return new BmiCalculationResult(imc, np, bodyFatPercentage);
	}

	/**
	 * Calculates weight level (NivelPeso) based on BMI.
	 */
	private NivelPeso calculateNivelPeso(final Double imc) {
		if (imc == null) {
			return null;
		}
		if (imc > 30.0d) {
			return NivelPeso.SOBREPESO;
		}
		if (imc > 25.0d) {
			return NivelPeso.ALTO;
		}
		if (imc > 18.5d) {
			return NivelPeso.NORMAL;
		}
		return NivelPeso.BAJO;
	}

	/**
	 * Calculates body fat percentage if patient has DOB and gender.
	 */
	private Double calculateBodyFatPercentage(final Double imc, final Paciente paciente) {
		if (paciente.getDob() == null || paciente.getGender() == null) {
			if (paciente.getDob() == null) {
				log.debug(
						"El paciente no tiene fecha de nacimiento registrada, no se calculará el índice de grasa corporal");
			}
			if (paciente.getGender() == null) {
				log.debug("El paciente no tiene género registrado, no se calculará el índice de grasa corporal");
			}
			return null;
		}

		final Integer age = calculateAge(paciente.getDob());
		if (age == null) {
			log.debug("No se pudo calcular la edad del paciente, no se calculará el índice de grasa corporal");
			return null;
		}

		return bodyFatCalculatorService.calculateBodyFatPercentage(imc, age, paciente.getGender());
	}

	/**
	 * Updates patient weight if the event date is today or is the latest event.
	 */
	private void updatePatientWeightIfNeeded(@NonNull final Paciente paciente, final CalendarEvent evento,
			final Double imc, final NivelPeso np, @NonNull final Long pacienteId, final Date eventDate) {
		if (eventDate == null) {
			return;
		}

		final LocalDate today = LocalDate.now();
		final LocalDate eventLocalDate = eventDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();

		if (today.equals(eventLocalDate)) {
			log.debug("Working on today's appointment, setting new patient weight vars");
			updatePatientWeightFromEvent(paciente, evento, imc, np);
		}
		else {
			updatePatientWeightIfLatestEvent(paciente, evento, imc, np, pacienteId, eventDate);
		}
	}

	/**
	 * Updates patient weight if the exam date is today or is the latest event.
	 */
	private void updatePatientWeightIfNeededForExam(@NonNull final Paciente paciente, final ClinicalExam exam,
			final Double imc, final NivelPeso np, @NonNull final Long pacienteId, final Date examDate) {
		if (examDate == null) {
			return;
		}

		final LocalDate today = LocalDate.now();
		final LocalDate examLocalDate = convertDateToLocalDate(examDate);

		if (today.equals(examLocalDate)) {
			log.debug("Working on today's clinical exam, setting new patient weight vars");
			updatePatientWeightFromExam(paciente, exam, imc, np);
		}
		else {
			updatePatientWeightIfLatestExam(paciente, exam, imc, np, pacienteId, examDate);
		}
	}

	/**
	 * Converts a Date to LocalDate, handling both java.util.Date and java.sql.Date.
	 */
	private LocalDate convertDateToLocalDate(final Date date) {
		if (date instanceof java.sql.Date) {
			return ((java.sql.Date) date).toLocalDate();
		}
		return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
	}

	/**
	 * Updates patient weight from a CalendarEvent.
	 */
	private void updatePatientWeightFromEvent(@NonNull final Paciente paciente, final CalendarEvent evento,
			final Double imc, final NivelPeso np) {
		if (evento.getPeso() != null) {
			paciente.setPeso(evento.getPeso());
		}
		if (evento.getEstatura() != null) {
			paciente.setEstatura(evento.getEstatura());
		}
		if (imc != null) {
			paciente.setImc(imc);
		}
		if (np != null) {
			paciente.setNivelPeso(np);
		}
		refreshPatientBmrIfPossible(paciente);
		pacienteRepository.save(paciente);
	}

	/**
	 * Updates patient weight from a ClinicalExam.
	 */
	private void updatePatientWeightFromExam(@NonNull final Paciente paciente, final ClinicalExam exam,
			final Double imc, final NivelPeso np) {
		if (exam.getPeso() != null) {
			paciente.setPeso(exam.getPeso());
		}
		if (exam.getEstatura() != null) {
			paciente.setEstatura(exam.getEstatura());
		}
		if (imc != null) {
			paciente.setImc(imc);
		}
		if (np != null) {
			paciente.setNivelPeso(np);
		}
		refreshPatientBmrIfPossible(paciente);
		pacienteRepository.save(paciente);
	}

	/**
	 * Updates patient weight if this is the latest CalendarEvent (no later events exist).
	 */
	private void updatePatientWeightIfLatestEvent(@NonNull final Paciente paciente, final CalendarEvent evento,
			final Double imc, final NivelPeso np, @NonNull final Long pacienteId, final Date eventDate) {
		final List<CalendarEvent> eventosPrevios = calendarEventService.findByPacienteId(pacienteId);
		final boolean laterExists = eventosPrevios.stream()
			.anyMatch(e -> e.getEventDateTime() != null && e.getEventDateTime().after(eventDate));

		if (!laterExists) {
			log.debug("No later evento exists, setting patient weight vars as latest date appointment");
			updatePatientWeightFromEvent(paciente, evento, imc, np);
		}
	}

	/**
	 * Updates patient weight if this is the latest ClinicalExam (no later events exist).
	 */
	private void updatePatientWeightIfLatestExam(@NonNull final Paciente paciente, final ClinicalExam exam,
			final Double imc, final NivelPeso np, @NonNull final Long pacienteId, final Date examDate) {
		final List<CalendarEvent> eventosPrevios = calendarEventService.findByPacienteId(pacienteId);
		final List<ClinicalExam> examenesPrevios = clinicalExamService.findByPacienteId(pacienteId);
		final boolean laterExists = eventosPrevios.stream()
			.anyMatch(e -> e.getEventDateTime() != null && e.getEventDateTime().after(examDate))
				|| examenesPrevios.stream()
					.anyMatch(e -> e.getExamDateTime() != null && e.getExamDateTime().after(examDate));

		if (!laterExists) {
			log.debug("No later event exists, setting patient weight vars as latest date clinical exam");
			updatePatientWeightFromExam(paciente, exam, imc, np);
		}
	}

	/**
	 * Sets calculated values (IMC, NivelPeso) on a CalendarEvent.
	 */
	private void setEventCalculatedValues(final CalendarEvent evento, final BmiCalculationResult bmiResult) {
		if (bmiResult.getImc() != null) {
			evento.setImc(bmiResult.getImc());
		}
		if (bmiResult.getNivelPeso() != null) {
			evento.setNivelPeso(bmiResult.getNivelPeso());
		}
	}

	/**
	 * Sets calculated values (IMC, NivelPeso) on a ClinicalExam.
	 */
	private void setExamCalculatedValues(final ClinicalExam exam, final BmiCalculationResult bmiResult) {
		if (bmiResult.getImc() != null) {
			exam.setImc(bmiResult.getImc());
		}
		if (bmiResult.getNivelPeso() != null) {
			exam.setNivelPeso(bmiResult.getNivelPeso());
		}
	}

	/**
	 * Sets default values on a CalendarEvent if not already set.
	 */
	private void setEventDefaultValues(final CalendarEvent evento) {
		if (evento.getTitle() == null || evento.getTitle().isBlank()) {
			evento.setTitle("Consulta");
		}
		if (evento.getStatus() == null) {
			evento.setStatus(EventStatus.COMPLETED);
		}
		if (evento.getDurationMinutes() == null) {
			evento.setDurationMinutes(60);
		}
	}

	/**
	 * Sets default values on a ClinicalExam if not already set.
	 */
	private void setExamDefaultValues(final ClinicalExam exam) {
		if (exam.getTitle() == null || exam.getTitle().isBlank()) {
			exam.setTitle("Examen Clínico");
		}
	}

	/**
	 * Calculates BMI, weight level, and body fat percentage for an
	 * AnthropometricMeasurement.
	 */
	private BmiCalculationResult calculateBmiAndBodyFatForMeasurement(
			final com.nutriconsultas.clinical.exam.AnthropometricMeasurement measurement, final Paciente paciente) {
		Double imc = null;
		NivelPeso np = null;

		if (measurement.getPeso() != null && measurement.getEstatura() != null) {
			imc = measurement.getPeso() / Math.pow(measurement.getEstatura(), 2);
			np = calculateNivelPeso(imc);
		}

		bodyCompositionService.applyToMeasurement(measurement, paciente, imc);
		final Double bodyFatPercentage = measurement.getPorcentajeGrasaCorporal();

		return new BmiCalculationResult(imc, np, bodyFatPercentage);
	}

	private void addLatestCompositionMetricsToModel(final Long pacienteId, final Model model) {
		final List<AnthropometricMeasurement> measurements = anthropometricMeasurementService
			.findByPacienteId(pacienteId);
		final Comparator<AnthropometricMeasurement> byRecency = Comparator
			.comparing(AnthropometricMeasurement::getMeasurementDateTime)
			.thenComparing(AnthropometricMeasurement::getId, Comparator.nullsLast(Comparator.naturalOrder()));

		measurements.stream()
			.filter(measurement -> measurement.getPorcentajeMasaMuscular() != null)
			.max(byRecency)
			.ifPresent(
					latest -> model.addAttribute("ultimoPorcentajeMasaMuscular", latest.getPorcentajeMasaMuscular()));

		measurements.stream()
			.filter(measurement -> measurement.getMasaOseaKg() != null || measurement.getPorcentajeMasaOsea() != null)
			.max(byRecency)
			.ifPresent(latest -> {
				model.addAttribute("ultimaMasaOseaKg", latest.getMasaOseaKg());
				model.addAttribute("ultimoPorcentajeMasaOsea", latest.getPorcentajeMasaOsea());
			});
	}

	/**
	 * Updates patient weight if the measurement date is today or is the latest event.
	 */
	private void updatePatientWeightIfNeededForMeasurement(@NonNull final Paciente paciente,
			final com.nutriconsultas.clinical.exam.AnthropometricMeasurement measurement, final Double imc,
			final NivelPeso np, @NonNull final Long pacienteId, final Date measurementDate) {
		if (measurementDate == null) {
			return;
		}

		final LocalDate today = LocalDate.now();
		final LocalDate measurementLocalDate = convertDateToLocalDate(measurementDate);

		if (today.equals(measurementLocalDate)) {
			log.debug("Working on today's anthropometric measurement, setting new patient weight vars");
			updatePatientWeightFromMeasurement(paciente, measurement, imc, np);
		}
		else {
			updatePatientWeightIfLatestMeasurement(paciente, measurement, imc, np, pacienteId, measurementDate);
		}
	}

	/**
	 * Updates patient weight from an AnthropometricMeasurement.
	 */
	private void updatePatientWeightFromMeasurement(@NonNull final Paciente paciente,
			final com.nutriconsultas.clinical.exam.AnthropometricMeasurement measurement, final Double imc,
			final NivelPeso np) {
		if (measurement.getPeso() != null) {
			paciente.setPeso(measurement.getPeso());
		}
		if (measurement.getEstatura() != null) {
			paciente.setEstatura(measurement.getEstatura());
		}
		if (imc != null) {
			paciente.setImc(imc);
		}
		if (np != null) {
			paciente.setNivelPeso(np);
		}
		refreshPatientBmrIfPossible(paciente);
		pacienteRepository.save(paciente);
	}

	/**
	 * Sets {@link Paciente#getBmr()} using the same average-BMR formulas as calendar REST
	 * updates when weight, height, age, and sex support it.
	 */
	private void refreshPatientBmrIfPossible(@NonNull final Paciente paciente) {
		if (paciente.getPeso() == null || paciente.getEstatura() == null) {
			return;
		}
		final Integer age = calculateAge(paciente.getDob());
		final Boolean isMale = "M".equalsIgnoreCase(paciente.getGender());
		final Double bmr = BmrCalculationService.calculatePromedioBmr(paciente.getPeso(), paciente.getEstatura(), age,
				isMale);
		if (bmr != null) {
			paciente.setBmr(bmr);
		}
	}

	/**
	 * Updates patient weight if this is the latest AnthropometricMeasurement (no later
	 * events exist).
	 */
	private void updatePatientWeightIfLatestMeasurement(@NonNull final Paciente paciente,
			final com.nutriconsultas.clinical.exam.AnthropometricMeasurement measurement, final Double imc,
			final NivelPeso np, @NonNull final Long pacienteId, final Date measurementDate) {
		final List<CalendarEvent> eventosPrevios = calendarEventService.findByPacienteId(pacienteId);
		final List<ClinicalExam> examenesPrevios = clinicalExamService.findByPacienteId(pacienteId);
		final List<com.nutriconsultas.clinical.exam.AnthropometricMeasurement> medicionesPrevias = anthropometricMeasurementService
			.findByPacienteId(pacienteId);
		final boolean laterExists = eventosPrevios.stream()
			.anyMatch(e -> e.getEventDateTime() != null && e.getEventDateTime().after(measurementDate))
				|| examenesPrevios.stream()
					.anyMatch(e -> e.getExamDateTime() != null && e.getExamDateTime().after(measurementDate))
				|| medicionesPrevias.stream()
					.anyMatch(m -> m.getMeasurementDateTime() != null
							&& m.getMeasurementDateTime().after(measurementDate));

		if (!laterExists) {
			log.debug("No later event exists, setting patient weight vars as latest date anthropometric measurement");
			updatePatientWeightFromMeasurement(paciente, measurement, imc, np);
		}
	}

	/**
	 * Sets calculated values (IMC, NivelPeso) on an AnthropometricMeasurement.
	 */
	private void setMeasurementCalculatedValues(
			final com.nutriconsultas.clinical.exam.AnthropometricMeasurement measurement,
			final BmiCalculationResult bmiResult) {
		if (bmiResult.getImc() != null) {
			measurement.setImc(bmiResult.getImc());
		}
		if (bmiResult.getNivelPeso() != null) {
			measurement.setNivelPeso(bmiResult.getNivelPeso());
		}
	}

	/**
	 * Sets default values on an AnthropometricMeasurement if not already set.
	 */
	private void setMeasurementDefaultValues(
			final com.nutriconsultas.clinical.exam.AnthropometricMeasurement measurement) {
		if (measurement.getTitle() == null || measurement.getTitle().isBlank()) {
			measurement.setTitle("Medición Antropométrica");
		}
	}

	private void ensureAnthropometricBodyMass(
			final com.nutriconsultas.clinical.exam.AnthropometricMeasurement measurement) {
		if (measurement.getBodyMass() == null) {
			measurement.setBodyMass(new com.nutriconsultas.clinical.exam.anthropometric.BodyMass());
		}
	}

}
