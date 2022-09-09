package com.nutriconsultas.alimentos;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AlimentosController {
  private static Logger logger = LoggerFactory.getLogger(AlimentosController.class);

  @GetMapping(path = "/admin/alimentos")
  public String listado(Model model) {
    logger.debug("Lista de alimentos");
    model.addAttribute("activeMenu", "alimentos");
    return "sbadmin/alimentos/listado";
  }
}
