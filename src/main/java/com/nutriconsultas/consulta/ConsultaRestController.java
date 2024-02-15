package com.nutriconsultas.consulta;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest")
public class ConsultaRestController {
  private static final Logger logger = LoggerFactory.getLogger(ConsultaRestController.class);

  @Autowired
  private ConsultaRepository repo;

  @DeleteMapping("consultas/{id}")
  public String deleteConsulta(@PathVariable Long id) {
    repo.deleteById(id);
    logger.info("La consulta {} ha sido borrada", id);
    return "OK";
  }
}
