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
		}
		else {
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
		Paciente _paciente = pacienteRepository.findById(id)
			.orElseThrow(() -> new IllegalArgumentException("No se ha encontrado paciente con folio " + id));

		_paciente.setName(paciente.getName());
		_paciente.setDob(paciente.getDob());
		_paciente.setEmail(paciente.getEmail());
		_paciente.setPhone(paciente.getPhone());
		_paciente.setGender(paciente.getGender());
		_paciente.setResponsibleName(paciente.getResponsibleName());
		_paciente.setParentesco(paciente.getParentesco());

		pacienteRepository.save(_paciente);
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
		Paciente _paciente = pacienteRepository.findById(id)
			.orElseThrow(() -> new IllegalArgumentException("No se ha encontrado paciente con folio " + id));

		_paciente.setTipoSanguineo(paciente.getTipoSanguineo());
		_paciente.setAntecedentesNatales(paciente.getAntecedentesNatales());
		_paciente.setAntecedentesPatologicosFamiliares(paciente.getAntecedentesPatologicosFamiliares());
		_paciente.setAntecedentesPatologicosPersonales(paciente.getAntecedentesPatologicosPersonales());
		_paciente.setAntecedentesPrenatales(paciente.getAntecedentesPrenatales());
		_paciente.setComplicaciones(paciente.getComplicaciones());

		pacienteRepository.save(_paciente);
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
		Paciente _paciente = pacienteRepository.findById(id)
			.orElseThrow(() -> new IllegalArgumentException("No se ha encontrado paciente con folio " + id));

		_paciente.setHistorialAlimenticio(paciente.getHistorialAlimenticio());
		_paciente.setDesarrolloPsicomotor(paciente.getDesarrolloPsicomotor());
		_paciente.setAlergias(paciente.getAlergias());

		_paciente.setHipertension(paciente.getHipertension());
		_paciente.setDiabetes(paciente.getDiabetes());
		_paciente.setHipotiroidismo(paciente.getHipotiroidismo());
		_paciente.setObesidad(paciente.getObesidad());
		_paciente.setAnemia(paciente.getAnemia());
		_paciente.setBulimia(paciente.getBulimia());
		_paciente.setAnorexia(paciente.getAnorexia());

		pacienteRepository.save(_paciente);
		return String.format("redirect:/admin/pacientes/%d", id);
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
		}

		Date today = new Date();
		Date eventDate = evento.getEventDateTime();
		if (eventDate != null) {
			// Comparar solo la fecha (sin hora)
			java.util.Calendar calToday = java.util.Calendar.getInstance();
			calToday.setTime(today);
			calToday.set(java.util.Calendar.HOUR_OF_DAY, 0);
			calToday.set(java.util.Calendar.MINUTE, 0);
			calToday.set(java.util.Calendar.SECOND, 0);
			calToday.set(java.util.Calendar.MILLISECOND, 0);

			java.util.Calendar calEvent = java.util.Calendar.getInstance();
			calEvent.setTime(eventDate);
			calEvent.set(java.util.Calendar.HOUR_OF_DAY, 0);
			calEvent.set(java.util.Calendar.MINUTE, 0);
			calEvent.set(java.util.Calendar.SECOND, 0);
			calEvent.set(java.util.Calendar.MILLISECOND, 0);

			if (calToday.getTime().compareTo(calEvent.getTime()) == 0) {
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
			}
			else {
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
		if (evento.getTitle() == null || evento.getTitle().trim().isEmpty()) {
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

	/**
	 * Calculates age from date of birth.
	 * @param dob Date of birth
	 * @return Age in years, or null if dob is null or in the future
	 */
	private Integer calculateAge(final Date dob) {
		if (dob == null) {
			return null;
		}
		final LocalDate birthDate = dob.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
		final LocalDate currentDate = LocalDate.now();
		if (birthDate.isAfter(currentDate)) {
			log.warn("Date of birth is in the future: {}", dob);
			return null;
		}
		return currentDate.getYear() - birthDate.getYear()
				- (currentDate.getDayOfYear() < birthDate.getDayOfYear() ? 1 : 0);
	}

}
