package com.nutriconsultas.dieta;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.nutriconsultas.controller.AbstractAuthorizedController;

@Controller
public class DietaController extends AbstractAuthorizedController {
  private static Logger logger = LoggerFactory.getLogger(DietaController.class);
  
  @GetMapping(path = "/admin/dietas")
  public String listado(Model model) {
    logger.debug("Lista de dietas");
    model.addAttribute("activeMenu", "dietas");
    return "sbadmin/dietas/listado";
  }
}
