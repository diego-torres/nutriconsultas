package com.nutriconsultas.paciente;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import com.nutriconsultas.calendar.CalendarEvent;
import com.nutriconsultas.calendar.CalendarEventService;
import com.nutriconsultas.calendar.EventStatus;
import com.nutriconsultas.controller.AbstractAuthorizedController;
import com.nutriconsultas.dieta.DietaRepository;
import com.nutriconsultas.dieta.DietaService;

import java.time.LocalDate;
import java.time.ZoneId;

@Controller
@Slf4j
public class PacienteController extends AbstractAuthorizedController {

	@Autowired
	private PacienteRepository pacienteRepository;

	@Autowired
	private CalendarEventService calendarEventService;

	@Autowired
	private BodyFatCalculatorService bodyFatCalculatorService;

	@Autowired
	private PacienteDietaService pacienteDietaService;

	@Autowired
	private DietaService dietaService;

	@Autowired
	private DietaRepository dietaRepository;

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

	@PostMapping(path = "/admin/pacientes/nuevo")
	public String addPaciente(@Valid final Paciente paciente, final BindingResult result, final Model model) {
		log.debug("Grabando nuevo paciente: " + paciente.getName());
		String resultView;
		if (result.hasErrors()) {
			resultView = "sbadmin/pacientes/nuevo";
		} else {
			pacienteRepository.save(paciente);
			resultView = "redirect:/admin/pacientes";
		}
		return resultView;
	}

	@GetMapping(path = "/admin/pacientes/{id}")
	public String perfilPaciente(@PathVariable @NonNull final Long id, final Model model) {
		log.debug("Cargando perfil de paciente {}", id);
		final Paciente paciente = pacienteRepository.findById(id)
			.orElseThrow(() -> new IllegalArgumentException("No se ha encontrado paciente con folio " + id));

		model.addAttribute("activeMenu", "perfil");
		model.addAttribute("paciente", paciente);
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
		// obtener último registro médico (peso, estatura, IMC)
		final CalendarEvent ultimoRegistro = getUltimoRegistroMedico(id);
		if (ultimoRegistro != null) {
			model.addAttribute("ultimoPeso", ultimoRegistro.getPeso());
			model.addAttribute("ultimaEstatura", ultimoRegistro.getEstatura());
			model.addAttribute("ultimoImc", ultimoRegistro.getImc());
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

	private CalendarEvent getUltimoRegistroMedico(@NonNull final Long pacienteId) {
		final List<CalendarEvent> eventos = calendarEventService.findByPacienteId(pacienteId);
		CalendarEvent result = null;
		if (!eventos.isEmpty()) {
			final List<CalendarEvent> eventosConDatos = eventos.stream()
				.filter(e -> e.getEventDateTime() != null)
				.filter(e -> e.getPeso() != null || e.getEstatura() != null || e.getImc() != null)
				.sorted(Comparator.comparing(CalendarEvent::getEventDateTime).reversed())
				.collect(Collectors.toList());
			if (!eventosConDatos.isEmpty()) {
				result = eventosConDatos.get(0);
			}
		}
		return result;
	}

	@GetMapping(path = "/admin/pacientes/{id}/afiliacion")
	public String afiliacionPaciente(@PathVariable @NonNull Long id, Model model) {
		log.debug("Cargando perfil de paciente {}", id);
		Paciente paciente = pacienteRepository.findById(id)
			.orElseThrow(() -> new IllegalArgumentException("No se ha encontrado paciente con folio " + id));

		model.addAttribute("activeMenu", "afiliacion");
		model.addAttribute("paciente", paciente);
		return "sbadmin/pacientes/afiliacion";
	}

	@PostMapping(path = "/admin/pacientes/{id}/afiliacion")
	public String cambiaAfiliacionPaciente(@PathVariable @NonNull Long id, @Valid Paciente paciente,
			BindingResult result, Model model) {
		log.debug("Cargando perfil de paciente {}", id);
		Paciente pacienteEntity = pacienteRepository.findById(id)
			.orElseThrow(() -> new IllegalArgumentException("No se ha encontrado paciente con folio " + id));

		pacienteEntity.setName(paciente.getName());
		pacienteEntity.setDob(paciente.getDob());
		pacienteEntity.setEmail(paciente.getEmail());
		pacienteEntity.setPhone(paciente.getPhone());
		pacienteEntity.setGender(paciente.getGender());
		pacienteEntity.setResponsibleName(paciente.getResponsibleName());
		pacienteEntity.setParentesco(paciente.getParentesco());

		pacienteRepository.save(pacienteEntity);
		return String.format("redirect:/admin/pacientes/%d", id);
	}

	@GetMapping(path = "/admin/pacientes/{id}/antecedentes")
	public String antecedentesPaciente(@PathVariable @NonNull Long id, Model model) {
		log.debug("Cargando antecedentes de paciente {}", id);
		Paciente paciente = pacienteRepository.findById(id)
			.orElseThrow(() -> new IllegalArgumentException("No se ha encontrado paciente con folio " + id));

		model.addAttribute("activeMenu", "antecedentes");
		model.addAttribute("paciente", paciente);
		return "sbadmin/pacientes/antecedentes";
	}

	@PostMapping(path = "/admin/pacientes/{id}/antecedentes")
	public String cambiaAntecedentesPaciente(@PathVariable @NonNull Long id, @Valid Paciente paciente,
			BindingResult result, Model model) {
		log.debug("Cargando perfil de paciente {}", id);
		Paciente pacienteEntity = pacienteRepository.findById(id)
			.orElseThrow(() -> new IllegalArgumentException("No se ha encontrado paciente con folio " + id));

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
	public String desarrolloPaciente(@PathVariable @NonNull Long id, Model model) {
		log.debug("Cargando datos de desarrollo de paciente {}", id);
		Paciente paciente = pacienteRepository.findById(id)
			.orElseThrow(() -> new IllegalArgumentException("No se ha encontrado paciente con folio " + id));

		model.addAttribute("activeMenu", "desarrollo");
		model.addAttribute("paciente", paciente);
		return "sbadmin/pacientes/desarrollo";
	}

	@PostMapping(path = "/admin/pacientes/{id}/desarrollo")
	public String cambiaDesarrolloPaciente(@PathVariable @NonNull Long id, @Valid Paciente paciente,
			BindingResult result, Model model) {
		log.debug("Cargando desarrollo de paciente {}", id);
		Paciente pacienteEntity = pacienteRepository.findById(id)
			.orElseThrow(() -> new IllegalArgumentException("No se ha encontrado paciente con folio " + id));

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

		pacienteRepository.save(pacienteEntity);
		return String.format("redirect:/admin/pacientes/%d", id);
	}

	@GetMapping(path = "/admin/pacientes/{id}/dietas")
	public String dietasPaciente(@PathVariable @NonNull final Long id, final Model model) {
		log.debug("Cargando dietas asignadas de paciente {}", id);
		final Paciente paciente = pacienteRepository.findById(id)
			.orElseThrow(() -> new IllegalArgumentException("No se ha encontrado paciente con folio " + id));

		model.addAttribute("activeMenu", "plan-alimentario");
		model.addAttribute("paciente", paciente);
		// obtener dietas asignadas
		final List<PacienteDieta> dietasAsignadas = pacienteDietaService.findByPacienteId(id);
		// Calcular macronutrientes para cada dieta asignada
		// Load dieta with ingestas to calculate macronutrientes
		for (final PacienteDieta pacienteDieta : dietasAsignadas) {
			if (pacienteDieta.getDieta() != null) {
				final com.nutriconsultas.dieta.Dieta dieta = dietaService.getDieta(pacienteDieta.getDieta().getId());
				if (dieta != null) {
					// Calculate and set macronutrientes
					dieta.setProteina(getTotalProteina(dieta));
					dieta.setLipidos(getTotalLipidos(dieta));
					dieta.setHidratosDeCarbono(getTotalHidratosDeCarbono(dieta));
					// Calculate and set kilocalorías
					final Double kCal = getTotalKCal(dieta);
					dieta.setEnergia(kCal != null ? kCal.intValue() : 0);
					// Update the dieta in pacienteDieta
					pacienteDieta.setDieta(dieta);
				}
			}
		}
		model.addAttribute("dietasAsignadas", dietasAsignadas);
		final List<PacienteDieta> dietasActivas = pacienteDietaService.findActiveByPacienteId(id);
		// Calcular macronutrientes para dietas activas
		for (final PacienteDieta pacienteDieta : dietasActivas) {
			if (pacienteDieta.getDieta() != null) {
				final com.nutriconsultas.dieta.Dieta dieta = dietaService.getDieta(pacienteDieta.getDieta().getId());
				if (dieta != null) {
					// Calculate and set macronutrientes
					dieta.setProteina(getTotalProteina(dieta));
					dieta.setLipidos(getTotalLipidos(dieta));
					dieta.setHidratosDeCarbono(getTotalHidratosDeCarbono(dieta));
					// Calculate and set kilocalorías
					final Double kCal = getTotalKCal(dieta);
					dieta.setEnergia(kCal != null ? kCal.intValue() : 0);
					// Update the dieta in pacienteDieta
					pacienteDieta.setDieta(dieta);
				}
			}
		}
		model.addAttribute("dietasActivas", dietasActivas);
		// obtener todas las dietas disponibles para asignar
		model.addAttribute("dietasDisponibles", dietaService.getDietas());
		return "sbadmin/pacientes/dietas";
	}

	@GetMapping(path = "/admin/pacientes/{id}/historial")
	public String historialPaciente(@PathVariable @NonNull Long id, Model model) {
		log.debug("Cargando datos de consultas de paciente {}", id);
		Paciente paciente = pacienteRepository.findById(id)
			.orElseThrow(() -> new IllegalArgumentException("No se ha encontrado paciente con folio " + id));

		model.addAttribute("activeMenu", "historial");
		model.addAttribute("paciente", paciente);
		return "sbadmin/pacientes/historial";
	}

	@GetMapping(path = "/admin/pacientes/{id}/consulta")
	public String consultaPaciente(@PathVariable @NonNull Long id, Model model) {
		log.debug("Cargando datos de consultas de paciente {}", id);
		Paciente paciente = pacienteRepository.findById(id)
			.orElseThrow(() -> new IllegalArgumentException("No se ha encontrado paciente con folio " + id));

		model.addAttribute("activeMenu", "historial");
		model.addAttribute("paciente", paciente);
		CalendarEvent evento = new CalendarEvent();
		evento.setEventDateTime(new Date());
		evento.setPaciente(paciente);
		evento.setTitle("Consulta");
		evento.setDurationMinutes(60);
		evento.setStatus(EventStatus.SCHEDULED);
		model.addAttribute("consulta", evento);
		return "sbadmin/pacientes/consulta";
	}

	@PostMapping(path = "/admin/pacientes/{pacienteId}/consulta")
	public String agregarConsultaPaciente(@PathVariable @NonNull Long pacienteId, @Valid CalendarEvent evento,
			BindingResult result, Model model) {
		log.debug("Grabando consulta {}", evento);
		Paciente paciente = pacienteRepository.findById(pacienteId)
			.orElseThrow(() -> new IllegalArgumentException("No se ha encontrado paciente con folio " + pacienteId));

		evento.setPaciente(paciente);

		Double imc = null;
		NivelPeso np = null;
		if (evento.getPeso() != null && evento.getEstatura() != null) {
			imc = evento.getPeso() / Math.pow(evento.getEstatura(), 2);
			np = imc > 30.0d ? NivelPeso.SOBREPESO
					: imc > 25.0d ? NivelPeso.ALTO : imc > 18.5d ? NivelPeso.NORMAL : NivelPeso.BAJO;

			// Calcular índice de grasa corporal si hay datos del paciente
			if (paciente.getDob() != null && paciente.getGender() != null) {
				Integer age = calculateAge(paciente.getDob());
				if (age != null) {
					Double bodyFatPercentage = bodyFatCalculatorService.calculateBodyFatPercentage(imc, age,
							paciente.getGender());
					evento.setIndiceGrasaCorporal(bodyFatPercentage);
				}
			}
		} final LocalDate today = LocalDate.now();
		final Date eventDate = evento.getEventDateTime();
		if (eventDate != null) {
			// Comparar solo la fecha (sin hora)
			final LocalDate eventLocalDate = eventDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();

			if (today.equals(eventLocalDate)) {
				log.debug("Working on today's appointment, setting new patient weight vars");
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
				if (paciente != null) {
					pacienteRepository.save(paciente);
				}
			} else {
				List<CalendarEvent> eventosPrevios = calendarEventService.findByPacienteId(pacienteId);
				Boolean laterExists = eventosPrevios.stream()
					.filter(e -> e.getEventDateTime() != null && e.getEventDateTime().after(eventDate))
					.findAny()
					.isPresent();
				if (!laterExists) {
					log.debug("No later evento exists, setting patient weight vars as latest date appointment");
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
					if (paciente != null) {
						pacienteRepository.save(paciente);
					}
				}
			}
		}

		if (imc != null) {
			evento.setImc(imc);
		}
		if (np != null) {
			evento.setNivelPeso(np);
		}

		// Asegurar que el evento tenga título y estado si no los tiene
		if (evento.getTitle() == null || evento.getTitle().isBlank()) {
			evento.setTitle("Consulta");
		}
		if (evento.getStatus() == null) {
			evento.setStatus(EventStatus.COMPLETED);
		}
		if (evento.getDurationMinutes() == null) {
			evento.setDurationMinutes(60);
		}

		log.debug("Evento lista para grabar {}", evento);
		calendarEventService.save(evento);
		return String.format("redirect:/admin/pacientes/%d/historial", pacienteId);
	}

	@GetMapping(path = "/admin/pacientes/{id}/dietas/asignar")
	public String asignarDieta(@PathVariable @NonNull final Long id, final Model model) {
		log.debug("Cargando formulario para asignar dieta a paciente {}", id);
		final Paciente paciente = pacienteRepository.findById(id)
			.orElseThrow(() -> new IllegalArgumentException("No se ha encontrado paciente con folio " + id));

		model.addAttribute("activeMenu", "perfil");
		model.addAttribute("paciente", paciente);
		model.addAttribute("dietasDisponibles", dietaService.getDietas());
		model.addAttribute("pacienteDieta", new PacienteDieta());
		return "sbadmin/pacientes/asignar-dieta";
	}

	@PostMapping(path = "/admin/pacientes/{id}/dietas/asignar")
	public String guardarAsignacionDieta(@PathVariable @NonNull final Long id, final PacienteDieta pacienteDieta,
			final BindingResult result, final Model model,
			@org.springframework.web.bind.annotation.RequestParam(required = true) @NonNull final Long dietaId) {
		log.debug("Guardando asignación de dieta {} para paciente {}", dietaId, id);

		// Check if pacienteDieta is null before using it
		if (pacienteDieta == null) {
			throw new IllegalArgumentException("PacienteDieta cannot be null");
		}

		// Set paciente and dieta from parameters before validation
		// This is necessary because paciente and dieta are validated as @NotNull but come
		// from
		// path variable and request parameter, not from form binding
		final Paciente paciente = pacienteRepository.findById(id)
			.orElseThrow(() -> new IllegalArgumentException("No se ha encontrado paciente con folio " + id));
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
			model.addAttribute("dietasDisponibles", dietaService.getDietas());
			return "sbadmin/pacientes/asignar-dieta";
		} final PacienteDieta saved = pacienteDietaService.assignDieta(id, dietaId, pacienteDieta);
		log.debug("Dieta asignada exitosamente: {}", saved);
		return String.format("redirect:/admin/pacientes/%d/dietas", id);
	}

	@GetMapping(path = "/admin/pacientes/{pacienteId}/dietas/{id}/editar")
	public String editarAsignacionDieta(@PathVariable @NonNull final Long pacienteId,
			@PathVariable @NonNull final Long id, final Model model) {
		log.debug("Cargando formulario para editar asignación de dieta {}", id);
		final Paciente paciente = pacienteRepository.findById(pacienteId)
			.orElseThrow(() -> new IllegalArgumentException("No se ha encontrado paciente con folio " + pacienteId));
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
			final Model model) {
		log.debug("Actualizando asignación de dieta {}", id);

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
			final Paciente paciente = pacienteRepository.findById(pacienteId)
				.orElseThrow(
						() -> new IllegalArgumentException("No se ha encontrado paciente con folio " + pacienteId));
			model.addAttribute("activeMenu", "perfil");
			model.addAttribute("paciente", paciente);
			model.addAttribute("pacienteDieta", pacienteDieta);
			return "sbadmin/pacientes/editar-dieta";
		} final PacienteDieta updated = pacienteDietaService.updateAssignment(id, pacienteDieta);
		log.info("Asignación de dieta actualizada exitosamente: {}", updated);
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
	 * Calculates age from date of birth.
	 * @param dob Date of birth
	 * @return Age in years, or null if dob is null or in the future
	 */
	private Integer calculateAge(final Date dob) {
		if (dob == null) {
			return null;
		} final LocalDate birthDate = dob.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
		final LocalDate currentDate = LocalDate.now();
		if (birthDate.isAfter(currentDate)) {
			log.warn("Date of birth is in the future: {}", dob);
			return null;
		}
		return currentDate.getYear() - birthDate.getYear()
				- (currentDate.getDayOfYear() < birthDate.getDayOfYear() ? 1 : 0);
	}

	/**
	 * Calculates total protein from a dieta.
	 * @param dieta the dieta to calculate protein from
	 * @return total protein in grams
	 */
	private Double getTotalProteina(final com.nutriconsultas.dieta.Dieta dieta) {
		if (dieta == null || dieta.getIngestas() == null) {
			return 0.0;
		}
		return dieta.getIngestas().stream().mapToDouble(i -> {
			double platillosProteina = i.getPlatillos() != null
					? i.getPlatillos().stream().mapToDouble(p -> p.getProteina() != null ? p.getProteina() : 0.0).sum()
					: 0.0;
			double alimentosProteina = i.getAlimentos() != null
					? i.getAlimentos().stream().mapToDouble(a -> a.getProteina() != null ? a.getProteina() : 0.0).sum()
					: 0.0;
			return platillosProteina + alimentosProteina;
		}).sum();
	}

	/**
	 * Calculates total lipids from a dieta.
	 * @param dieta the dieta to calculate lipids from
	 * @return total lipids in grams
	 */
	private Double getTotalLipidos(final com.nutriconsultas.dieta.Dieta dieta) {
		if (dieta == null || dieta.getIngestas() == null) {
			return 0.0;
		}
		return dieta.getIngestas().stream().mapToDouble(i -> {
			double platillosLipidos = i.getPlatillos() != null
					? i.getPlatillos().stream().mapToDouble(p -> p.getLipidos() != null ? p.getLipidos() : 0.0).sum()
					: 0.0;
			double alimentosLipidos = i.getAlimentos() != null
					? i.getAlimentos().stream().mapToDouble(a -> a.getLipidos() != null ? a.getLipidos() : 0.0).sum()
					: 0.0;
			return platillosLipidos + alimentosLipidos;
		}).sum();
	}

	/**
	 * Calculates total carbohydrates from a dieta.
	 * @param dieta the dieta to calculate carbohydrates from
	 * @return total carbohydrates in grams
	 */
	private Double getTotalHidratosDeCarbono(final com.nutriconsultas.dieta.Dieta dieta) {
		if (dieta == null || dieta.getIngestas() == null) {
			return 0.0;
		}
		return dieta.getIngestas().stream().mapToDouble(i -> {
			double platillosHidratos = i.getPlatillos() != null ? i.getPlatillos()
				.stream()
				.mapToDouble(p -> p.getHidratosDeCarbono() != null ? p.getHidratosDeCarbono() : 0.0)
				.sum() : 0.0;
			double alimentosHidratos = i.getAlimentos() != null ? i.getAlimentos()
				.stream()
				.mapToDouble(a -> a.getHidratosDeCarbono() != null ? a.getHidratosDeCarbono() : 0.0)
				.sum() : 0.0;
			return platillosHidratos + alimentosHidratos;
		}).sum();
	}

	/**
	 * Calculates total kilocalories from a dieta. Formula: protein * 4 + lipids * 9 +
	 * carbohydrates * 4
	 * @param dieta the dieta to calculate kilocalories from
	 * @return total kilocalories
	 */
	private Double getTotalKCal(final com.nutriconsultas.dieta.Dieta dieta) {
		if (dieta == null) {
			return 0.0;
		} final Double proteina = getTotalProteina(dieta);
		final Double lipidos = getTotalLipidos(dieta);
		final Double hidratosDeCarbono = getTotalHidratosDeCarbono(dieta);
		return proteina * 4 + lipidos * 9 + hidratosDeCarbono * 4;
	}

}
