package com.nutriconsultas.alimentos;

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
public class AlimentosController {
  private static Logger logger = LoggerFactory.getLogger(AlimentosController.class);

  @Autowired
  private AlimentosRepository alimentosRepository;

  @GetMapping(path = "/admin/alimentos")
  public String listado(Model model) {
    logger.debug("Lista de alimentos");
    model.addAttribute("activeMenu", "alimentos");
    return "sbadmin/alimentos/listado";
  }

  @GetMapping(path = "/admin/alimentos/nuevo")
  public String nuevoAlimento(Model model) {
    logger.debug("formulario de alta de alimentos");
    model.addAttribute("activeMenu", "alimentos");
    // asignar valores por defecto
    Alimento alimento = new Alimento();
    alimento.setClasificacion("ACEITES Y GRASAS");
    alimento.setUnidad("pieza");
    model.addAttribute("alimento", alimento);
    return "sbadmin/alimentos/formulario";
  }

  @GetMapping(path = "/admin/alimentos/{id}")
  public String verAlimento(@PathVariable("id") Long id, Model model) {
    logger.debug("formulario de alta de alimentos");
    model.addAttribute("activeMenu", "alimentos");

    Alimento alimento = alimentosRepository.getById(id);
    model.addAttribute("alimento", alimento);
    return "sbadmin/alimentos/formulario";
  }

  @PostMapping(path = "/admin/alimentos")
  public String agregarNuevoAlimento(@Valid Alimento alimento, BindingResult result, Model model) {
    logger.debug("Alta de nuevo alimento {}", alimento.getNombreAlimento());
    if (result.hasErrors()) {
      return "sbadmin/alimentos/formulario";
    }
    
    alimentosRepository.save(alimento);
    return "redirect:/admin/alimentos";
  }
}
