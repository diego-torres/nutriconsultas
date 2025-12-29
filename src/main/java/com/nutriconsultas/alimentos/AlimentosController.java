package com.nutriconsultas.alimentos;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import com.nutriconsultas.controller.AbstractAuthorizedController;

@Controller
@Slf4j
public class AlimentosController extends AbstractAuthorizedController {

	@Autowired
	private AlimentoService alimentoService;

	@GetMapping(path = "/admin/alimentos")
	public String listado(final Model model) {
		log.info("Start Listado de alimentos");
		model.addAttribute("activeMenu", "alimentos");
		log.debug("set alimentos as the active menu");
		log.info("finish listado de alimentos with model {} to view sbadmin/alimentos/listado", model);
		return "sbadmin/alimentos/listado";
	}

	@GetMapping(path = "/admin/alimentos/nuevo")
	public String nuevoAlimento(final Model model) {
		log.info("Start nuevo alimento");
		model.addAttribute("activeMenu", "alimentos");
		log.debug("set alimentos as the active menu");
		// asignar valores por defecto
		final Alimento alimento = new Alimento();
		alimento.setClasificacion("ACEITES Y GRASAS");
		alimento.setUnidad("pieza");
		model.addAttribute("alimento", alimento);
		log.info("finish nuevo alimento with model {} to view sbadmin/alimentos/formulario and default values {}",
				model, alimento);
		return "sbadmin/alimentos/formulario";
	}

	@GetMapping(path = "/admin/alimentos/{id}")
	public String verAlimento(@PathVariable @NonNull final Long id, final Model model) {
		log.info("Start verAlimento with id {}", id);
		model.addAttribute("activeMenu", "alimentos");
		log.debug("set alimentos as the active menu");

		final Alimento alimento = alimentoService.findById(id);
		model.addAttribute("alimento", alimento);
		log.info("finish ver alimento with model {} to view sbadmin/alimentos/formulario and form falues {}", model,
				alimento);
		return "sbadmin/alimentos/formulario";
	}

	@PostMapping(path = "/admin/alimentos")
	public String agregarNuevoAlimento(@Valid @NonNull final Alimento alimento, final BindingResult result,
			final Model model) {
		log.info("Start agregar nuevo alimento with values {}, and binding result {}", alimento, result);
		String resultView;
		if (result.hasErrors()) {
			log.warn("Found {} errors on binding result", result.getErrorCount());
			resultView = "sbadmin/alimentos/formulario";
		}
		else {
			final Alimento _alimento = alimentoService.save(alimento);
			log.info(
					"finish agregar nuevo alimento post method, after saving with values {}, redirecting to /admin/alimentos",
					_alimento);
			resultView = "redirect:/admin/alimentos";
		}
		return resultView;
	}

}
