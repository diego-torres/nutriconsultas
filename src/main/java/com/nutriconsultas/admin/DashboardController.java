package com.nutriconsultas.admin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.nutriconsultas.controller.AbstractAuthorizedController;

@Controller
public class DashboardController extends AbstractAuthorizedController {
  private static Logger logger = LoggerFactory.getLogger(DashboardController.class);

  @GetMapping(path = "/admin")
  public String index(Model model) {
    logger.debug("Resolving Admin Index");
    model.addAttribute("activeMenu", "home");
    return "sbadmin/index";
  }
}
