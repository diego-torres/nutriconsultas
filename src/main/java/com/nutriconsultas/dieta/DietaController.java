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

import com.nutriconsultas.alimentos.Alimento;
import com.nutriconsultas.alimentos.AlimentoService;
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

	@Autowired
	private AlimentoService alimentoService;

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

		model.addAttribute("alimentos", alimentoService.findAll());

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

	@PostMapping(path = "/admin/dietas/{id}/alimentos/save")
	public String saveAlimento(@PathVariable @NonNull Long id, @ModelAttribute AlimentoFormModel alimentoModel,
			Model model) {
		logger.debug("Agregar alimento {} a ingesta con id {}", alimentoModel, id);
		Dieta dieta = dietaService.getDieta(id);
		Ingesta ingesta = dieta.getIngestas()
			.stream()
			.filter(i -> i.getId().equals(alimentoModel.getIngestaAlimento()))
			.findFirst()
			.orElse(null);
		if (ingesta != null && alimentoModel.getAlimento() != null) {
			logger.debug("ingresar alimento en ingesta {}, de la dieta {}", ingesta, dieta);
			Long alimentoId = Objects.requireNonNull(alimentoModel.getAlimento());
			Alimento alimento = alimentoService.findById(alimentoId);
			if (alimento != null) {
				// map the found alimento to a AlimentoIngesta
				AlimentoIngesta alimentoIngesta = mapAlimentoIngesta(alimento, alimentoModel);
				alimentoIngesta.setIngesta(ingesta);

				ingesta.getAlimentos().add(alimentoIngesta);
				dietaService.saveDieta(dieta);
			}
		}
		return "redirect:/admin/dietas/" + id;
	}

	private AlimentoIngesta mapAlimentoIngesta(Alimento alimento, AlimentoFormModel alimentoModel) {
		AlimentoIngesta alimentoIngesta = new AlimentoIngesta();
		// map each field from alimento into alimento ingesta
		alimentoIngesta.setName(alimento.getNombreAlimento());
		alimentoIngesta.setAlimento(alimento);
		alimentoIngesta.setPortions(alimentoModel.getPorciones() != null ? alimentoModel.getPorciones() : 1);
		alimentoIngesta.setUnidad(alimento.getUnidad());

		// Calculate nutritional values based on portions
		Integer portions = alimentoIngesta.getPortions();
		if (portions == null) {
			portions = 1;
		}

		// Map nutritional values from alimento (which extends AbstractFraccionable)
		// These values are already per portion, so multiply by portions
		if (alimento.getEnergia() != null) {
			alimentoIngesta.setEnergia((int) (alimento.getEnergia() * portions));
		}
		if (alimento.getProteina() != null) {
			alimentoIngesta.setProteina(alimento.getProteina() * portions);
		}
		if (alimento.getLipidos() != null) {
			alimentoIngesta.setLipidos(alimento.getLipidos() * portions);
		}
		if (alimento.getHidratosDeCarbono() != null) {
			alimentoIngesta.setHidratosDeCarbono(alimento.getHidratosDeCarbono() * portions);
		}
		if (alimento.getPesoBrutoRedondeado() != null) {
			alimentoIngesta.setPesoBrutoRedondeado(alimento.getPesoBrutoRedondeado() * portions);
		}
		if (alimento.getPesoNeto() != null) {
			alimentoIngesta.setPesoNeto(alimento.getPesoNeto() * portions);
		}
		if (alimento.getFibra() != null) {
			alimentoIngesta.setFibra(alimento.getFibra() * portions);
		}
		if (alimento.getVitA() != null) {
			alimentoIngesta.setVitA(alimento.getVitA() * portions);
		}
		if (alimento.getAcidoAscorbico() != null) {
			alimentoIngesta.setAcidoAscorbico(alimento.getAcidoAscorbico() * portions);
		}
		if (alimento.getHierroNoHem() != null) {
			alimentoIngesta.setHierroNoHem(alimento.getHierroNoHem() * portions);
		}
		if (alimento.getPotasio() != null) {
			alimentoIngesta.setPotasio(alimento.getPotasio() * portions);
		}
		if (alimento.getIndiceGlicemico() != null) {
			alimentoIngesta.setIndiceGlicemico(alimento.getIndiceGlicemico() * portions);
		}
		if (alimento.getCargaGlicemica() != null) {
			alimentoIngesta.setCargaGlicemica(alimento.getCargaGlicemica() * portions);
		}
		if (alimento.getAcidoFolico() != null) {
			alimentoIngesta.setAcidoFolico(alimento.getAcidoFolico() * portions);
		}
		if (alimento.getCalcio() != null) {
			alimentoIngesta.setCalcio(alimento.getCalcio() * portions);
		}
		if (alimento.getHierro() != null) {
			alimentoIngesta.setHierro(alimento.getHierro() * portions);
		}
		if (alimento.getSodio() != null) {
			alimentoIngesta.setSodio(alimento.getSodio() * portions);
		}
		if (alimento.getAzucarPorEquivalente() != null) {
			alimentoIngesta.setAzucarPorEquivalente(alimento.getAzucarPorEquivalente() * portions);
		}
		if (alimento.getSelenio() != null) {
			alimentoIngesta.setSelenio(alimento.getSelenio() * portions);
		}
		if (alimento.getFosforo() != null) {
			alimentoIngesta.setFosforo(alimento.getFosforo() * portions);
		}
		if (alimento.getColesterol() != null) {
			alimentoIngesta.setColesterol(alimento.getColesterol() * portions);
		}
		if (alimento.getAgSaturados() != null) {
			alimentoIngesta.setAgSaturados(alimento.getAgSaturados() * portions);
		}
		if (alimento.getAgMonoinsaturados() != null) {
			alimentoIngesta.setAgMonoinsaturados(alimento.getAgMonoinsaturados() * portions);
		}
		if (alimento.getAgPoliinsaturados() != null) {
			alimentoIngesta.setAgPoliinsaturados(alimento.getAgPoliinsaturados() * portions);
		}
		if (alimento.getEtanol() != null) {
			alimentoIngesta.setEtanol(alimento.getEtanol() * portions);
		}

		return alimentoIngesta;
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
