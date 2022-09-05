package com.nutriconsultas.admin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {
  private static Logger logger = LoggerFactory.getLogger(DashboardController.class);

  @GetMapping(path = "/admin")
  public String index() {
    logger.debug("Resolving Admin Index");
    return "sbadmin/index";
  }
}
