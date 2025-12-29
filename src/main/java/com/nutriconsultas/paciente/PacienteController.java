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

import com.nutriconsultas.consulta.Consulta;
import com.nutriconsultas.consulta.ConsultaRepository;
import com.nutriconsultas.controller.AbstractAuthorizedController;

@Controller
@Slf4j
public class PacienteController extends AbstractAuthorizedController {

	@Autowired
	private PacienteRepository pacienteRepository;

	@Autowired
	private ConsultaRepository consultaRepository;

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
		final Consulta citaAnterior = getCitaAnterior(id);
		final String fechaCitaAnterior = citaAnterior != null ? dateFormat.format(citaAnterior.getFechaConsulta()) : "";
		model.addAttribute("citaAnterior", fechaCitaAnterior);
		// TODO: Calcular siguiente cita en calendario
		model.addAttribute("citaSiguiente", "");
		return "sbadmin/pacientes/perfil";
	}

	private Consulta getCitaAnterior(final Long pacienteId) {
		final List<Consulta> consultas = consultaRepository.findByPacienteId(pacienteId);
		Consulta result = null;
		if (!consultas.isEmpty()) {
			final List<Consulta> consultasByDate = consultas.stream()
				.sorted(Comparator.comparing(Consulta::getFechaConsulta).reversed())
				.collect(Collectors.toList());
			result = consultasByDate.get(0);
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
		Consulta consulta = new Consulta();
		consulta.setFechaConsulta(new Date());
		model.addAttribute("consulta", consulta);
		return "sbadmin/pacientes/consulta";
	}

	@PostMapping(path = "/admin/pacientes/{pacienteId}/consulta")
	public String agregarConsultaPaciente(@PathVariable @NonNull Long pacienteId, @Valid Consulta consulta,
			BindingResult result, Model model) {
		log.debug("Grabando consulta {}", consulta);
		Paciente paciente = pacienteRepository.findById(pacienteId)
			.orElseThrow(() -> new IllegalArgumentException("No se ha encontrado paciente con folio " + pacienteId));

		consulta.setPaciente(paciente);

		Double imc = consulta.getPeso() / Math.pow(consulta.getEstatura(), 2);
		NivelPeso np = imc > 30.0d ? NivelPeso.SOBREPESO
				: imc > 25.0d ? NivelPeso.ALTO : imc > 18.5d ? NivelPeso.NORMAL : NivelPeso.BAJO;

		if (consulta.getFechaConsulta().compareTo(new Date()) == 0) {
			log.debug("Working on today's appointment, setting new patient weight vars");
			paciente.setPeso(consulta.getPeso());
			paciente.setEstatura(consulta.getEstatura());
			paciente.setImc(imc);
			paciente.setNivelPeso(np);
			pacienteRepository.save(paciente);
		}
		else {
			List<Consulta> consultasPrevias = consultaRepository.findByPacienteId(pacienteId);
			Boolean laterExists = consultasPrevias.stream()
				.filter(c -> c.getFechaConsulta().after(consulta.getFechaConsulta()))
				.findAny()
				.isPresent();
			if (!laterExists) {
				log.debug("No later consulta exists, setting patient weight vars as latest date appointment");
				paciente.setPeso(consulta.getPeso());
				paciente.setEstatura(consulta.getEstatura());
				paciente.setImc(imc);
				paciente.setNivelPeso(np);
				pacienteRepository.save(paciente);
			}
		}

		consulta.setImc(imc);
		consulta.setNivelPeso(np);

		log.debug("Consulta lista para grabar {}", consulta);
		consultaRepository.save(consulta);
		return String.format("redirect:/admin/pacientes/%d/historial", pacienteId);
	}

}
