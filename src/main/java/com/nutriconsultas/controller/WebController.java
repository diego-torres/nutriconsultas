package com.nutriconsultas.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class WebController {
  private static Logger logger = LoggerFactory.getLogger(WebController.class);

  @GetMapping(path = "/")
  public String index() {
    logger.debug("Resolving index");
    return "eterna/index";
  }
}
