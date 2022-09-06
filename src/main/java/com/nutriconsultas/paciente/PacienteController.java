package com.nutriconsultas.paciente;

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

@Controller
public class PacienteController {
  private static Logger logger = LoggerFactory.getLogger(PacienteController.class);

  @Autowired
  private PacienteRepository pacienteRepository;

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

}
