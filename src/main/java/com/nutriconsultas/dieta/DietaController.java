package com.nutriconsultas.dieta;

import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import com.nutriconsultas.controller.AbstractAuthorizedController;
import com.nutriconsultas.platillos.IngestaFormModel;



@Controller
public class DietaController extends AbstractAuthorizedController {
  private static Logger logger = LoggerFactory.getLogger(DietaController.class);
  
  @Autowired
  private DietaService dietaService;

  @GetMapping(path = "/admin/dietas")
  public String listado(Model model) {
    logger.debug("Lista de dietas");
    model.addAttribute("activeMenu", "dietas");
    return "sbadmin/dietas/listado";
  }

  @GetMapping(path = "/admin/dietas/{id}")
  public String editar(@PathVariable @NonNull Long id, Model model) {
    logger.debug("Editar dieta con id {}", id);
    model.addAttribute("activeMenu", "dietas");

    Dieta dieta = dietaService.getDieta(id);
    logger.debug("Dieta encontrada: {}", dieta);
    model.addAttribute("dieta", dieta);
    // calculate the minimun id in ingestas
    Long minId = dieta.getIngestas().stream().mapToLong(Ingesta::getId).min().orElse(0);
    model.addAttribute("minId", minId);

    model.addAttribute("platillos", new ArrayList<PlatilloIngesta>());

    return "sbadmin/dietas/formulario";
  }
  
  @PostMapping(path = "/admin/dietas/{id}/ingestas/add")
  public String addIngesta(@PathVariable @NonNull Long id, @RequestParam IngestaFormModel ingesta, Model model) {
    logger.debug("Agregar ingesta a dieta con id {}", id);
    model.addAttribute("activeMenu", "dietas");

    dietaService.addIngesta(id, ingesta.getIngesta());

    Dieta dieta = dietaService.getDieta(id);
    logger.debug("Dieta encontrada: {}", dieta);
    model.addAttribute("dieta", dieta);
    // calculate the minimun id in ingestas
    Long minId = dieta.getIngestas().stream().mapToLong(Ingesta::getId).min().orElse(0);
    model.addAttribute("minId", minId);

    model.addAttribute("platillos", new ArrayList<PlatilloIngesta>());

    return "sbadmin/dietas/formulario";
  }

  @PostMapping(path = "/admin/dietas/save")
  public String saveDieta(@RequestParam @NonNull Long id, @RequestParam @NonNull String nombre, Model model) {
    logger.debug("Guardar dieta with id {}, {}", id, nombre);
    Dieta _dieta = dietaService.getDieta(id);
    _dieta.setNombre(nombre);
    dietaService.saveDieta(_dieta);
    return "redirect:/admin/dietas/" + id;
  }
  
}
