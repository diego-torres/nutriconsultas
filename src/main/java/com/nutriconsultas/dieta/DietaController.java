package com.nutriconsultas.dieta;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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

    // Sort the ingestas list by id
    List<Ingesta> sortedIngestas = dieta.getIngestas().stream()
        .sorted(Comparator.comparingLong(Ingesta::getId))
        .collect(Collectors.toList());
    model.addAttribute("ingestas", sortedIngestas);

    // calculate the minimun id in ingestas
    model.addAttribute("minId", sortedIngestas.isEmpty() ? 0 : sortedIngestas.get(0).getId());

    model.addAttribute("platillos", new ArrayList<PlatilloIngesta>());

    return "sbadmin/dietas/formulario";
  }
  
  @PostMapping(path = "/admin/dietas/{id}/ingestas/save")
  public String addIngesta(@PathVariable @NonNull Long id, @ModelAttribute IngestaFormModel ingesta, Model model) {
    logger.debug("Agregar ingesta {} a dieta con id {}", ingesta, id);
    model.addAttribute("activeMenu", "dietas");

    if(ingesta.getIngestaId() == 0) {
      logger.debug("nueva ingesta en dieta, agregar");
      dietaService.addIngesta(id, ingesta.getIngesta());
    } else {
      logger.debug("Ingesta existente, cambiar nombre");
      dietaService.renameIngesta(id, ingesta.getIngestaId(), ingesta.getIngesta());
    }

    Dieta dieta = dietaService.getDieta(id);
    logger.debug("Dieta encontrada: {}", dieta);
    model.addAttribute("dieta", dieta);
    // calculate the minimun id in ingestas
    Long minId = dieta.getIngestas().stream().mapToLong(Ingesta::getId).min().orElse(0);
    model.addAttribute("minId", minId);

    model.addAttribute("platillos", new ArrayList<PlatilloIngesta>());

    return "redirect:/admin/dietas/" + id;
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
