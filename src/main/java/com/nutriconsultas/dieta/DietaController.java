package com.nutriconsultas.dieta;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.nutriconsultas.paciente.PacienteController;

@Controller
public class DietaController {
  private static Logger logger = LoggerFactory.getLogger(PacienteController.class);
  
  @GetMapping(path = "/admin/dietas")
  public String listado(Model model) {
    logger.debug("Lista de dietas");
    model.addAttribute("activeMenu", "dietas");
    return "sbadmin/dietas/listado";
  }
}
