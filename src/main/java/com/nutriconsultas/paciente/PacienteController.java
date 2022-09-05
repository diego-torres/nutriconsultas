package com.nutriconsultas.paciente;

import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class PacienteController {
  private static Logger logger = LoggerFactory.getLogger(PacienteController.class);

  @Autowired
  private PacienteRepository pacienteRepository;

  @GetMapping(path = "/admin/pacientes/nuevo")
  public String index(Model model) {
    logger.debug("Nuevo paciente");
    model.addAttribute("paciente", new Paciente());
    return "sbadmin/pacientes/nuevo";
  }

  @PostMapping(path = "/admin/pacientes/nuevo")
  public String addPaciente(@Valid Paciente paciente, BindingResult result, Model model) {
    if (result.hasErrors()) {
      return "sbadmin/pacientes/nuevo";
    }

    pacienteRepository.save(paciente);
    return "redirect:/admin/";
  }

}
