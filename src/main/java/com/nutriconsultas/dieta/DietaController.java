package com.nutriconsultas.dieta;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
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
import com.nutriconsultas.platillos.Ingrediente;
import com.nutriconsultas.platillos.Platillo;
import com.nutriconsultas.platillos.PlatilloService;

@Controller
public class DietaController extends AbstractAuthorizedController {

	private static Logger logger = LoggerFactory.getLogger(DietaController.class);

	@Autowired
	private DietaService dietaService;

	@Autowired
	private PlatilloService platilloService;

	@GetMapping(path = "/admin/dietas")
	public String listado(Model model) {
		logger.debug("Lista de dietas");
		model.addAttribute("activeMenu", "dietas");
		return "sbadmin/dietas/listado";
	}

	@PostMapping(path = "/admin/dietas/save")
	public String saveDieta(@RequestParam @NonNull Long id, @RequestParam @NonNull String nombre, Model model) {
		logger.debug("Guardar dieta with id {}, {}", id, nombre);
		Dieta _dieta = dietaService.getDieta(id);
		_dieta.setNombre(nombre);
		dietaService.saveDieta(_dieta);
		return "redirect:/admin/dietas/" + id;
	}

	@GetMapping(path = "/admin/dietas/{id}")
	public String editar(@PathVariable @NonNull Long id, Model model) {
		logger.debug("Editar dieta con id {}", id);
		model.addAttribute("activeMenu", "dietas");

		Dieta dieta = dietaService.getDieta(id);
		logger.debug("Dieta encontrada: {}", dieta);
		model.addAttribute("dieta", dieta);

		// Sort the ingestas list by id
		List<Ingesta> sortedIngestas = dieta.getIngestas()
			.stream()
			.sorted(Comparator.comparingLong(Ingesta::getId))
			.collect(Collectors.toList());
		model.addAttribute("ingestas", sortedIngestas);

		// calculate the minimun id in ingestas
		model.addAttribute("minId", sortedIngestas.isEmpty() ? 0 : sortedIngestas.get(0).getId());

		model.addAttribute("platillos", platilloService.findAll());

		return "sbadmin/dietas/formulario";
	}

	@PostMapping(path = "/admin/dietas/{id}/ingestas/save")
	public String saveIngesta(@PathVariable @NonNull Long id, @ModelAttribute IngestaFormModel ingesta, Model model) {
		logger.debug("Agregar ingesta {} a dieta con id {}", ingesta, id);
		model.addAttribute("activeMenu", "dietas");

		if (ingesta.getIngestaId() == 0) {
			logger.debug("nueva ingesta en dieta, agregar");
			dietaService.addIngesta(id, ingesta.getIngesta());
		}
		else {
			logger.debug("Ingesta existente, cambiar nombre");
			Long ingestaId = ingesta.getIngestaId();
			if (ingestaId == null) {
				logger.error("Ingesta ID is null, cannot rename");
				return "redirect:/admin/dietas/" + id;
			}
			dietaService.renameIngesta(id, ingestaId, ingesta.getIngesta());
		}

		return "redirect:/admin/dietas/" + id;
	}

	@PostMapping(path = "/admin/dietas/{id}/ingestas/delete")
	public String deleteIngesta(@PathVariable @NonNull Long id, @ModelAttribute IngestaFormModel ingestaModel,
			Model model) {
		logger.debug("Eliminar ingesta con id {} de dieta con id {}", ingestaModel, id);
		Dieta dieta = dietaService.getDieta(id);
		dieta.getIngestas().removeIf(ingesta -> ingesta.getId().equals(ingestaModel.getIngestaId()));
		dietaService.saveDieta(dieta);
		return "redirect:/admin/dietas/" + id;
	}

	@PostMapping(path = "/admin/dietas/{id}/platillos/save")
	public String savePlatillo(@PathVariable @NonNull Long id, @ModelAttribute PlatilloFormModel platilloModel,
			Model model) {
		logger.debug("Agregar platillo {} a ingesta con id {}", platilloModel, id);
		Dieta dieta = dietaService.getDieta(id);
		Ingesta ingesta = dieta.getIngestas()
			.stream()
			.filter(i -> i.getId().equals(platilloModel.getIngestaPlatillo()))
			.findFirst()
			.orElse(null);
		if (ingesta != null && platilloModel.getPlatillo() != null) {
			logger.debug("ingresar platillo en ingesta {}, de la dieta {}", ingesta, dieta);
			Long platilloId = Objects.requireNonNull(platilloModel.getPlatillo());
			Platillo platillo = platilloService.findById(platilloId);
			if (platillo != null) {
				// map the found platillo to a PlatilloIngesta
				PlatilloIngesta platilloIngesta = mapPlatilloIngesta(platillo);
				platilloIngesta.setIngesta(ingesta);
				platilloIngesta.setPortions(platilloModel.getPorciones() != null ? platilloModel.getPorciones() : 1);

				// map ingredientes from platillo to ingredientes of platilloIngesta
				if (platillo.getIngredientes() != null) {
					for (Ingrediente ingrediente : platillo.getIngredientes()) {
						IngredientePlatilloIngesta ingredientePlatilloIngesta = mapFromIngredienteToIngredientePlatilloIngesta(
								ingrediente);
						ingredientePlatilloIngesta.setPlatillo(platilloIngesta);
						platilloIngesta.getIngredientes().add(ingredientePlatilloIngesta);
					}
				}

				ingesta.getPlatillos().add(platilloIngesta);
				dietaService.saveDieta(dieta);
			}
		}
		return "redirect:/admin/dietas/" + id;
	}

	private PlatilloIngesta mapPlatilloIngesta(Platillo platillo) {
		PlatilloIngesta platilloIngesta = new PlatilloIngesta();
		// map each field from platillo into platillo ingesta
		platilloIngesta.setName(platillo.getName());
		platilloIngesta.setRecommendations(platillo.getDescription());
		platilloIngesta.setVideoUrl(platillo.getVideoUrl());
		platilloIngesta.setPdfUrl(platillo.getPdfUrl());
		platilloIngesta.setImageUrl(platillo.getImageUrl());

		platilloIngesta.setAcidoAscorbico(platillo.getAcidoAscorbico());
		platilloIngesta.setAcidoFolico(platillo.getAcidoFolico());
		platilloIngesta.setAgMonoinsaturados(platillo.getAgMonoinsaturados());
		platilloIngesta.setAgPoliinsaturados(platillo.getAgPoliinsaturados());
		platilloIngesta.setAgSaturados(platillo.getAgSaturados());
		platilloIngesta.setCalcio(platillo.getCalcio());
		platilloIngesta.setCargaGlicemica(platillo.getCargaGlicemica());
		platilloIngesta.setColesterol(platillo.getColesterol());
		platilloIngesta.setEnergia(platillo.getEnergia());
		platilloIngesta.setFibra(platillo.getFibra());
		platilloIngesta.setHierro(platillo.getHierro());
		platilloIngesta.setHierroNoHem(platillo.getHierroNoHem());
		platilloIngesta.setHidratosDeCarbono(platillo.getHidratosDeCarbono());
		platilloIngesta.setIndiceGlicemico(platillo.getIndiceGlicemico());
		platilloIngesta.setLipidos(platillo.getLipidos());
		platilloIngesta.setPesoBrutoRedondeado(platillo.getPesoBrutoRedondeado());
		platilloIngesta.setPesoNeto(platillo.getPesoNeto());
		platilloIngesta.setPotasio(platillo.getPotasio());
		platilloIngesta.setProteina(platillo.getProteina());
		platilloIngesta.setSodio(platillo.getSodio());
		platilloIngesta.setSelenio(platillo.getSelenio());
		platilloIngesta.setVitA(platillo.getVitA());
		platilloIngesta.setAzucarPorEquivalente(platillo.getAzucarPorEquivalente());
		platilloIngesta.setEtanol(platillo.getEtanol());
		platilloIngesta.setFosforo(platillo.getFosforo());

		return platilloIngesta;
	}

	private IngredientePlatilloIngesta mapFromIngredienteToIngredientePlatilloIngesta(Ingrediente ingrediente) {
		IngredientePlatilloIngesta result = new IngredientePlatilloIngesta();
		// map each field from ingrediente into ingredientePlatilloIngesta
		result.setDescription(ingrediente.getDescription());
		result.setCantSugerida(ingrediente.getCantSugerida());
		result.setAlimento(ingrediente.getAlimento());
		result.setUnidad(ingrediente.getUnidad());

		// map macro nutrients from AbstractMacroNutrible
		result.setEnergia(ingrediente.getEnergia());
		result.setProteina(ingrediente.getProteina());
		result.setLipidos(ingrediente.getLipidos());
		result.setHidratosDeCarbono(ingrediente.getHidratosDeCarbono());

		// map nutrients from AbstractNutrible
		result.setPesoBrutoRedondeado(ingrediente.getPesoBrutoRedondeado());
		result.setPesoNeto(ingrediente.getPesoNeto());
		result.setFibra(ingrediente.getFibra());
		result.setVitA(ingrediente.getVitA());
		result.setAcidoAscorbico(ingrediente.getAcidoAscorbico());
		result.setHierroNoHem(ingrediente.getHierroNoHem());
		result.setPotasio(ingrediente.getPotasio());
		result.setIndiceGlicemico(ingrediente.getIndiceGlicemico());
		result.setCargaGlicemica(ingrediente.getCargaGlicemica());
		result.setAcidoFolico(ingrediente.getAcidoFolico());
		result.setCalcio(ingrediente.getCalcio());
		result.setHierro(ingrediente.getHierro());
		result.setSodio(ingrediente.getSodio());
		result.setAzucarPorEquivalente(ingrediente.getAzucarPorEquivalente());
		result.setSelenio(ingrediente.getSelenio());
		result.setFosforo(ingrediente.getFosforo());
		result.setColesterol(ingrediente.getColesterol());
		result.setAgSaturados(ingrediente.getAgSaturados());
		result.setAgMonoinsaturados(ingrediente.getAgMonoinsaturados());
		result.setAgPoliinsaturados(ingrediente.getAgPoliinsaturados());
		result.setEtanol(ingrediente.getEtanol());

		return result;
	}

}
