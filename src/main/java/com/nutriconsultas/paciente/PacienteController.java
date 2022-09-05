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
    return String.format("redirect:/admin/pacientes/%d", id) ;
  }

}
