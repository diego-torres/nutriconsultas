package com.nutriconsultas.paciente;

import java.util.Date;
import java.util.List;

import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import com.nutriconsultas.consulta.Consulta;
import com.nutriconsultas.consulta.ConsultaRepository;

@Controller
public class PacienteController {
  private static Logger logger = LoggerFactory.getLogger(PacienteController.class);

  @Autowired
  private PacienteRepository pacienteRepository;

  @Autowired
  private ConsultaRepository consultaRepository;

  @GetMapping(path = "/admin/pacientes/nuevo")
  public String nuevo(Model model) {
    logger.debug("Nuevo paciente");
    model.addAttribute("activeMenu", "pacientes");
    model.addAttribute("paciente", new Paciente());
    return "sbadmin/pacientes/nuevo";
  }

  @GetMapping(path = "/admin/pacientes")
  public String listado(Model model) {
    logger.debug("Listado de pacientes");
    model.addAttribute("activeMenu", "pacientes");
    model.addAttribute("paciente", new Paciente());
    return "sbadmin/pacientes/listado";
  }

  @PostMapping(path = "/admin/pacientes/nuevo")
  public String addPaciente(@Valid Paciente paciente, BindingResult result, Model model) {
    logger.debug("Grabando nuevo paciente: " + paciente.getName());
    if (result.hasErrors()) {
      return "sbadmin/pacientes/nuevo";
    }

    pacienteRepository.save(paciente);
    return "redirect:/admin/pacientes";
  }

  @GetMapping(path = "/admin/pacientes/{id}")
  public String perfilPaciente(@PathVariable("id") Long id, Model model) {
    logger.debug("Cargando perfil de paciente {}", id);
    Paciente paciente = pacienteRepository.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("No se ha encontrado paciente con folio " + id));

    model.addAttribute("activeMenu", "perfil");
    model.addAttribute("paciente", paciente);
    return "sbadmin/pacientes/perfil";
  }

  @GetMapping(path = "/admin/pacientes/{id}/afiliacion")
  public String afiliacionPaciente(@PathVariable("id") Long id, Model model) {
    logger.debug("Cargando perfil de paciente {}", id);
    Paciente paciente = pacienteRepository.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("No se ha encontrado paciente con folio " + id));

    model.addAttribute("activeMenu", "afiliacion");
    model.addAttribute("paciente", paciente);
    return "sbadmin/pacientes/afiliacion";
  }

  @PostMapping(path = "/admin/pacientes/{id}/afiliacion")
  public String cambiaAfiliacionPaciente(@PathVariable("id") Long id, @Valid Paciente paciente, BindingResult result,
      Model model) {
    logger.debug("Cargando perfil de paciente {}", id);
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
  public String antecedentesPaciente(@PathVariable("id") Long id, Model model) {
    logger.debug("Cargando antecedentes de paciente {}", id);
    Paciente paciente = pacienteRepository.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("No se ha encontrado paciente con folio " + id));

    model.addAttribute("activeMenu", "antecedentes");
    model.addAttribute("paciente", paciente);
    return "sbadmin/pacientes/antecedentes";
  }

  @PostMapping(path = "/admin/pacientes/{id}/antecedentes")
  public String cambiaAntecedentesPaciente(@PathVariable("id") Long id, @Valid Paciente paciente, BindingResult result,
      Model model) {
    logger.debug("Cargando perfil de paciente {}", id);
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
  public String desarrolloPaciente(@PathVariable("id") Long id, Model model) {
    logger.debug("Cargando datos de desarrollo de paciente {}", id);
    Paciente paciente = pacienteRepository.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("No se ha encontrado paciente con folio " + id));

    model.addAttribute("activeMenu", "desarrollo");
    model.addAttribute("paciente", paciente);
    return "sbadmin/pacientes/desarrollo";
  }

  @PostMapping(path = "/admin/pacientes/{id}/desarrollo")
  public String cambiaDesarrolloPaciente(@PathVariable("id") Long id, @Valid Paciente paciente, BindingResult result,
      Model model) {
    logger.debug("Cargando desarrollo de paciente {}", id);
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
  public String historialPaciente(@PathVariable("id") Long id, Model model) {
    logger.debug("Cargando datos de consultas de paciente {}", id);
    Paciente paciente = pacienteRepository.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("No se ha encontrado paciente con folio " + id));

    model.addAttribute("activeMenu", "historial");
    model.addAttribute("paciente", paciente);
    return "sbadmin/pacientes/historial";
  }

  @GetMapping(path = "/admin/pacientes/{id}/consulta")
  public String consultaPaciente(@PathVariable("id") Long id, Model model) {
    logger.debug("Cargando datos de consultas de paciente {}", id);
    Paciente paciente = pacienteRepository.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("No se ha encontrado paciente con folio " + id));

    model.addAttribute("activeMenu", "historial");
    model.addAttribute("paciente", paciente);
    Consulta consulta = new Consulta();
    consulta.setFechaConsulta(new Date());
    model.addAttribute("consulta", consulta);
    return "sbadmin/pacientes/consulta";
  }

  @PostMapping(path = "/admin/pacientes/{id}/consulta")
  public String agregarConsultaPaciente(@PathVariable("id") Long id, @Valid Consulta consulta, BindingResult result,
      Model model) {
    logger.debug("Cargando desarrollo de paciente {}", id);
    Paciente paciente = pacienteRepository.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("No se ha encontrado paciente con folio " + id));

    consulta.setPaciente(paciente);

    Double imc = consulta.getPeso() / Math.pow(consulta.getEstatura(), 2);
    NivelPeso np = imc > 30.0d ? NivelPeso.SOBREPESO
        : imc > 25.0d ? NivelPeso.ALTO : imc > 18.5d ? NivelPeso.NORMAL : NivelPeso.BAJO;

    if (consulta.getFechaConsulta().compareTo(new Date()) == 0) {
      logger.debug("Working on today's appointment, setting new patient weight vars");
      paciente.setPeso(consulta.getPeso());
      paciente.setEstatura(consulta.getEstatura());
      paciente.setImc(imc);
      paciente.setNivelPeso(np);
      pacienteRepository.save(paciente);
    } else {
      List<Consulta> consultasPrevias = consultaRepository.findByPacienteId(id);
      Boolean laterExists = consultasPrevias.stream()
          .filter(c -> c.getFechaConsulta().after(consulta.getFechaConsulta())).findAny().isPresent();
      if (!laterExists) {
        logger.debug("No other entry found, setting patient weight vars as latest date appointment");
        paciente.setPeso(consulta.getPeso());
        paciente.setEstatura(consulta.getEstatura());
        paciente.setImc(imc);
        paciente.setNivelPeso(np);
        pacienteRepository.save(paciente);
      }
    }

    consulta.setImc(imc);
    consulta.setNivelPeso(np);

    consultaRepository.save(consulta);
    return String.format("redirect:/admin/pacientes/%d/historial", id);
  }

}
