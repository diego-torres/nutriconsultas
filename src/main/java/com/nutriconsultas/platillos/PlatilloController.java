package com.nutriconsultas.platillos;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;

import com.nutriconsultas.alimentos.AlimentoService;
import com.nutriconsultas.controller.AbstractAuthorizedController;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

@Controller
@Slf4j
public class PlatilloController extends AbstractAuthorizedController {

    @Autowired
    private PlatilloService service;

    @Autowired
    private AlimentoService alimentoService;

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
   
    @GetMapping(path = "/admin/platillos/{id}")
    public String editar(@PathVariable @NonNull Long id, Model model) {
        log.debug("Starting editar with id {}", id);
        model.addAttribute("activeMenu", "platillos");
        Platillo platillo = service.findById(id);
        model.addAttribute("platillo", platillo);
        List<String> ingestas = new ArrayList<>(); 
        if(platillo.getIngestasSugeridas()!=null){
            ingestas = Arrays.asList(platillo.getIngestasSugeridas().split(","));   
        }
        model.addAttribute("ingestas", ingestas);
        log.debug("Finishing editar with platillo {}", platillo);
        model.addAttribute("alimentosList", alimentoService.findAll());
        return "sbadmin/platillos/formulario";
    }

    @PostMapping("/admin/platillos/save")
    public String save(@ModelAttribute @NonNull Platillo platillo) {
        log.debug("Starting save with platillo {}", platillo);

        @SuppressWarnings("null")
        Platillo dbPlatillo = service.findById(platillo.getId());
        if (dbPlatillo != null) {
            dbPlatillo.setName(platillo.getName());
            dbPlatillo.setDescription(platillo.getDescription());
            service.save(dbPlatillo);
        } else {
            service.save(platillo);
        }
        
        log.debug("Finishing save with platillo {}", platillo);
        return "redirect:/admin/platillos/" + platillo.getId();
    }
    
    @PostMapping("/admin/platillos/{id}/picture")
    public String uploadPicture(@PathVariable @NonNull Long id, @RequestParam("file") MultipartFile file, Model model) {
        log.debug("Starting uploadPicture with id {}", id);
        model.addAttribute("activeMenu", "platillos");

        if (file.isEmpty()) {
            log.error("Failed to upload picture because the file is empty");
            model.addAttribute("errorMessage", "The file is empty");
            return "sbadmin/platillos/formulario";
        }

        try {
            byte[] bytes = file.getBytes();
            // Assuming you have a method in PlatilloService to handle picture saving
            String fileName = file.getOriginalFilename();
            String fileExtension = "";
            if (fileName != null) {
                int dotIndex = fileName.lastIndexOf('.');
                if (dotIndex > 0 && dotIndex < fileName.length() - 1) {
                    fileExtension = fileName.substring(dotIndex + 1);
                }
                log.debug("File extension is {}", fileExtension);
            }
            service.savePicture(id, bytes, fileExtension);
            log.debug("Successfully uploaded picture for platillo with id {}", id);
        } catch (IOException e) {
            log.error("Failed to upload picture for platillo with id {}", id, e);
            model.addAttribute("errorMessage", "Failed to upload picture");
            return "sbadmin/platillos/formulario";
        }

        return "redirect:/admin/platillos/" + id;
    }
    
    
}
