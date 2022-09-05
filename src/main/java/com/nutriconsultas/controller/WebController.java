package com.nutriconsultas.controller;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class WebController {
  private static Logger logger = LoggerFactory.getLogger(WebController.class);

  @GetMapping(path = "/")
  public String index() {
    logger.debug("Resolving index.html");
    return "eterna/index.html";
  }

  @GetMapping(path = "/login")
  public String login() {
    logger.debug("Resolving login.html");
    return "sbadmin/login.html";
  }

  @GetMapping(path = "/role-route")
  public String roleRoute(HttpServletRequest request) {
    logger.debug("Routing based on roles");
    if (request.isUserInRole("ROLE_ADMIN")) {
      return "redirect:/admin/";
    }
    return "redirect:/user/";
  }
}
