package com.nutriconsultas.platillos;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;

import com.nutriconsultas.controller.AbstractAuthorizedController;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;



@Controller
@Slf4j
public class PlatilloController extends AbstractAuthorizedController {
    @GetMapping(path = "/admin/platillos/nuevo")
    public String nuevo(Model model) {
        log.debug("Starting nuevo");
        model.addAttribute("activeMenu", "platillos");
        Platillo platillo = new Platillo();
        platillo.setId(0L);
        model.addAttribute("platillo", platillo);
        log.debug("Finishing nuevo platillo con valores predeterminados: {}", platillo);
        return "sbadmin/platillos/formulario";
    }

    @GetMapping(path = "/admin/platillos")
    public String listado(Model model) {
        log.debug("Starting listado");
        model.addAttribute("activeMenu", "platillos");
        log.debug("Finishing listado");
        return "sbadmin/platillos/listado";
    }
    
    
}
