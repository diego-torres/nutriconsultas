package com.nutriconsultas.dieta;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nutriconsultas.alimentos.Alimento;

import lombok.extern.slf4j.Slf4j;

@Service
@Transactional
@Slf4j
public class DietaServiceImpl implements DietaService {

	@Autowired
	private DietaRepository dietaRepository;

	@Override
	public Dieta getDieta(@NonNull final Long id) {
		log.info("Getting dieta with id: " + id);
		return dietaRepository.findById(id).orElse(null);
	}

	@Override
	public Dieta getDietaByIdAndUserId(@NonNull final Long id, @NonNull final String userId) {
		log.info("Getting dieta with id: {} and userId: {}", id, userId);
		return dietaRepository.findByIdAndUserId(id, userId).orElse(null);
	}

	@Override
	public Dieta saveDieta(@NonNull final Dieta dieta) {
		log.info("Saving dieta with id: " + dieta.getId());
		return dietaRepository.save(dieta);
	}

	@Override
	public void deleteDieta(@NonNull final Long id) {
		log.info("Deleting dieta with id: " + id);
		dietaRepository.deleteById(id);
	}

	@Override
	public void deleteDietaByIdAndUserId(@NonNull final Long id, @NonNull final String userId) {
		log.info("Deleting dieta with id: {} and userId: {}", id, userId);
		dietaRepository.findByIdAndUserId(id, userId).ifPresent(dietaRepository::delete);
	}

	@Override
	public List<Dieta> getDietas() {
		log.info("Getting all dietas");
		return dietaRepository.findAll();
	}

	@Override
	public void addIngesta(@NonNull final Long id, final String nombreIngesta) {
		log.info("Adding ingesta to dieta with id: " + id);
		final Dieta dieta = dietaRepository.findById(id).orElse(null);
		if (dieta != null) {
			final Ingesta ingesta = new Ingesta();
			ingesta.setNombre(nombreIngesta);
			ingesta.setDieta(dieta);
			dieta.getIngestas().add(ingesta);
			dietaRepository.save(dieta);
		}
	}

	@Override
	public void renameIngesta(@NonNull final Long id, @NonNull final Long ingestaId, final String nombreIngesta) {
		log.info("Renaming ingesta in dieta with id: " + id);
		final Dieta dieta = dietaRepository.findById(id).orElse(null);
		if (dieta != null) {
			log.debug("dieta: {}", dieta);
			dieta.getIngestas().forEach(ingesta -> {
				if (ingesta.getId() != null && ingesta.getId().equals(ingestaId)) {
					log.debug("Renaming ingesta with id: {}", id);
					ingesta.setNombre(nombreIngesta);
				}
			});
			log.debug("Saving dieta with updated ingesta {}", dieta);
			dietaRepository.save(dieta);
		}
		else {
			log.warn("Dieta with id {} not found in an attempt to rename one of its ingestas", id);
		}
	}

	@Override
	public void recalculateAlimentoIngestaNutrients(@NonNull final AlimentoIngesta alimentoIngesta,
			@NonNull final Integer portions) {
		log.info("Recalculating nutrients for AlimentoIngesta with id {} for {} portions", alimentoIngesta.getId(),
				portions);
		if (alimentoIngesta.getAlimento() != null) {
			final Alimento alimento = alimentoIngesta.getAlimento();
			alimentoIngesta.setPortions(portions);

			// Recalculate nutritional values from original alimento
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
		}
	}

	@Override
	public void recalculatePlatilloIngestaNutrients(@NonNull final PlatilloIngesta platilloIngesta,
			@NonNull final Integer portions) {
		log.info("Recalculating nutrients for PlatilloIngesta with id {} for {} portions", platilloIngesta.getId(),
				portions);
		final Integer oldPortions = platilloIngesta.getPortions() != null ? platilloIngesta.getPortions() : 1;
		final double ratio = portions.doubleValue() / oldPortions.doubleValue();

		// Update portions
		platilloIngesta.setPortions(portions);

		// Recalculate nutritional values based on portion ratio
		if (platilloIngesta.getEnergia() != null) {
			platilloIngesta.setEnergia((int) (platilloIngesta.getEnergia() * ratio));
		}
		if (platilloIngesta.getProteina() != null) {
			platilloIngesta.setProteina(platilloIngesta.getProteina() * ratio);
		}
		if (platilloIngesta.getLipidos() != null) {
			platilloIngesta.setLipidos(platilloIngesta.getLipidos() * ratio);
		}
		if (platilloIngesta.getHidratosDeCarbono() != null) {
			platilloIngesta.setHidratosDeCarbono(platilloIngesta.getHidratosDeCarbono() * ratio);
		}
		if (platilloIngesta.getPesoBrutoRedondeado() != null) {
			platilloIngesta.setPesoBrutoRedondeado((int) (platilloIngesta.getPesoBrutoRedondeado() * ratio));
		}
		if (platilloIngesta.getPesoNeto() != null) {
			platilloIngesta.setPesoNeto((int) (platilloIngesta.getPesoNeto() * ratio));
		}
		if (platilloIngesta.getFibra() != null) {
			platilloIngesta.setFibra(platilloIngesta.getFibra() * ratio);
		}
		if (platilloIngesta.getVitA() != null) {
			platilloIngesta.setVitA(platilloIngesta.getVitA() * ratio);
		}
		if (platilloIngesta.getAcidoAscorbico() != null) {
			platilloIngesta.setAcidoAscorbico(platilloIngesta.getAcidoAscorbico() * ratio);
		}
		if (platilloIngesta.getHierroNoHem() != null) {
			platilloIngesta.setHierroNoHem(platilloIngesta.getHierroNoHem() * ratio);
		}
		if (platilloIngesta.getPotasio() != null) {
			platilloIngesta.setPotasio(platilloIngesta.getPotasio() * ratio);
		}
		if (platilloIngesta.getIndiceGlicemico() != null) {
			platilloIngesta.setIndiceGlicemico(platilloIngesta.getIndiceGlicemico() * ratio);
		}
		if (platilloIngesta.getCargaGlicemica() != null) {
			platilloIngesta.setCargaGlicemica(platilloIngesta.getCargaGlicemica() * ratio);
		}
		if (platilloIngesta.getAcidoFolico() != null) {
			platilloIngesta.setAcidoFolico(platilloIngesta.getAcidoFolico() * ratio);
		}
		if (platilloIngesta.getCalcio() != null) {
			platilloIngesta.setCalcio(platilloIngesta.getCalcio() * ratio);
		}
		if (platilloIngesta.getHierro() != null) {
			platilloIngesta.setHierro(platilloIngesta.getHierro() * ratio);
		}
		if (platilloIngesta.getSodio() != null) {
			platilloIngesta.setSodio(platilloIngesta.getSodio() * ratio);
		}
		if (platilloIngesta.getAzucarPorEquivalente() != null) {
			platilloIngesta.setAzucarPorEquivalente(platilloIngesta.getAzucarPorEquivalente() * ratio);
		}
		if (platilloIngesta.getSelenio() != null) {
			platilloIngesta.setSelenio(platilloIngesta.getSelenio() * ratio);
		}
		if (platilloIngesta.getFosforo() != null) {
			platilloIngesta.setFosforo(platilloIngesta.getFosforo() * ratio);
		}
		if (platilloIngesta.getColesterol() != null) {
			platilloIngesta.setColesterol(platilloIngesta.getColesterol() * ratio);
		}
		if (platilloIngesta.getAgSaturados() != null) {
			platilloIngesta.setAgSaturados(platilloIngesta.getAgSaturados() * ratio);
		}
		if (platilloIngesta.getAgMonoinsaturados() != null) {
			platilloIngesta.setAgMonoinsaturados(platilloIngesta.getAgMonoinsaturados() * ratio);
		}
		if (platilloIngesta.getAgPoliinsaturados() != null) {
			platilloIngesta.setAgPoliinsaturados(platilloIngesta.getAgPoliinsaturados() * ratio);
		}
		if (platilloIngesta.getEtanol() != null) {
			platilloIngesta.setEtanol(platilloIngesta.getEtanol() * ratio);
		}
	}

	@Override
	public Dieta duplicateDieta(@NonNull final Long id, @NonNull final String userId) {
		log.info("Duplicating dieta with id: {} for user: {}", id, userId);
		final Dieta originalDieta = dietaRepository.findById(id).orElse(null);
		if (originalDieta == null) {
			log.warn("Dieta with id {} not found for duplication", id);
			return null;
		}

		// Create new dieta with "Copy of [Original Name]"
		final Dieta newDieta = new Dieta();
		final String originalNombre = originalDieta.getNombre() != null ? originalDieta.getNombre() : "Dieta";
		newDieta.setNombre("Copia de " + originalNombre);
		newDieta.setUserId(userId);

		// Copy nutritional values from AbstractMacroNutrible
		newDieta.setEnergia(originalDieta.getEnergia());
		newDieta.setProteina(originalDieta.getProteina());
		newDieta.setLipidos(originalDieta.getLipidos());
		newDieta.setHidratosDeCarbono(originalDieta.getHidratosDeCarbono());

		// Copy all ingestas
		final List<Ingesta> newIngestas = new ArrayList<>();
		for (final Ingesta originalIngesta : originalDieta.getIngestas()) {
			final Ingesta newIngesta = new Ingesta();
			newIngesta.setNombre(originalIngesta.getNombre());
			newIngesta.setDieta(newDieta);

			// Copy nutritional values from AbstractMacroNutrible
			newIngesta.setEnergia(originalIngesta.getEnergia());
			newIngesta.setProteina(originalIngesta.getProteina());
			newIngesta.setLipidos(originalIngesta.getLipidos());
			newIngesta.setHidratosDeCarbono(originalIngesta.getHidratosDeCarbono());

			// Copy platillos
			final List<PlatilloIngesta> newPlatillos = new ArrayList<>();
			for (final PlatilloIngesta originalPlatillo : originalIngesta.getPlatillos()) {
				final PlatilloIngesta newPlatillo = copyPlatilloIngesta(originalPlatillo);
				newPlatillo.setIngesta(newIngesta);
				newPlatillos.add(newPlatillo);
			}
			newIngesta.setPlatillos(newPlatillos);

			// Copy alimentos
			final List<AlimentoIngesta> newAlimentos = new ArrayList<>();
			for (final AlimentoIngesta originalAlimento : originalIngesta.getAlimentos()) {
				final AlimentoIngesta newAlimento = copyAlimentoIngesta(originalAlimento);
				newAlimento.setIngesta(newIngesta);
				newAlimentos.add(newAlimento);
			}
			newIngesta.setAlimentos(newAlimentos);

			newIngestas.add(newIngesta);
		}
		newDieta.setIngestas(newIngestas);

		// Save and return the duplicated dieta
		final Dieta savedDieta = dietaRepository.save(newDieta);
		log.info("Successfully duplicated dieta with id {} to new dieta with id {}", id, savedDieta.getId());
		return savedDieta;
	}

	private PlatilloIngesta copyPlatilloIngesta(final PlatilloIngesta original) {
		final PlatilloIngesta copy = new PlatilloIngesta();
		copy.setName(original.getName());
		copy.setPortions(original.getPortions());
		copy.setRecommendations(original.getRecommendations());
		copy.setImageUrl(original.getImageUrl());
		copy.setVideoUrl(original.getVideoUrl());
		copy.setPdfUrl(original.getPdfUrl());

		// Copy nutritional values from AbstractNutrible
		copy.setEnergia(original.getEnergia());
		copy.setProteina(original.getProteina());
		copy.setLipidos(original.getLipidos());
		copy.setHidratosDeCarbono(original.getHidratosDeCarbono());
		copy.setPesoBrutoRedondeado(original.getPesoBrutoRedondeado());
		copy.setPesoNeto(original.getPesoNeto());
		copy.setFibra(original.getFibra());
		copy.setVitA(original.getVitA());
		copy.setAcidoAscorbico(original.getAcidoAscorbico());
		copy.setHierroNoHem(original.getHierroNoHem());
		copy.setPotasio(original.getPotasio());
		copy.setIndiceGlicemico(original.getIndiceGlicemico());
		copy.setCargaGlicemica(original.getCargaGlicemica());
		copy.setAcidoFolico(original.getAcidoFolico());
		copy.setCalcio(original.getCalcio());
		copy.setHierro(original.getHierro());
		copy.setSodio(original.getSodio());
		copy.setAzucarPorEquivalente(original.getAzucarPorEquivalente());
		copy.setSelenio(original.getSelenio());
		copy.setFosforo(original.getFosforo());
		copy.setColesterol(original.getColesterol());
		copy.setAgSaturados(original.getAgSaturados());
		copy.setAgMonoinsaturados(original.getAgMonoinsaturados());
		copy.setAgPoliinsaturados(original.getAgPoliinsaturados());
		copy.setEtanol(original.getEtanol());

		// Copy ingredientes
		final List<IngredientePlatilloIngesta> newIngredientes = new ArrayList<>();
		for (final IngredientePlatilloIngesta originalIngrediente : original.getIngredientes()) {
			final IngredientePlatilloIngesta newIngrediente = copyIngredientePlatilloIngesta(originalIngrediente);
			newIngrediente.setPlatillo(copy);
			newIngredientes.add(newIngrediente);
		}
		copy.setIngredientes(newIngredientes);

		return copy;
	}

	private IngredientePlatilloIngesta copyIngredientePlatilloIngesta(final IngredientePlatilloIngesta original) {
		final IngredientePlatilloIngesta copy = new IngredientePlatilloIngesta();
		copy.setDescription(original.getDescription());
		copy.setCantSugerida(original.getCantSugerida());
		copy.setAlimento(original.getAlimento());
		copy.setUnidad(original.getUnidad());

		// Copy nutritional values from AbstractFraccionable (extends AbstractNutrible)
		copy.setEnergia(original.getEnergia());
		copy.setProteina(original.getProteina());
		copy.setLipidos(original.getLipidos());
		copy.setHidratosDeCarbono(original.getHidratosDeCarbono());
		copy.setPesoBrutoRedondeado(original.getPesoBrutoRedondeado());
		copy.setPesoNeto(original.getPesoNeto());
		copy.setFibra(original.getFibra());
		copy.setVitA(original.getVitA());
		copy.setAcidoAscorbico(original.getAcidoAscorbico());
		copy.setHierroNoHem(original.getHierroNoHem());
		copy.setPotasio(original.getPotasio());
		copy.setIndiceGlicemico(original.getIndiceGlicemico());
		copy.setCargaGlicemica(original.getCargaGlicemica());
		copy.setAcidoFolico(original.getAcidoFolico());
		copy.setCalcio(original.getCalcio());
		copy.setHierro(original.getHierro());
		copy.setSodio(original.getSodio());
		copy.setAzucarPorEquivalente(original.getAzucarPorEquivalente());
		copy.setSelenio(original.getSelenio());
		copy.setFosforo(original.getFosforo());
		copy.setColesterol(original.getColesterol());
		copy.setAgSaturados(original.getAgSaturados());
		copy.setAgMonoinsaturados(original.getAgMonoinsaturados());
		copy.setAgPoliinsaturados(original.getAgPoliinsaturados());
		copy.setEtanol(original.getEtanol());

		return copy;
	}

	private AlimentoIngesta copyAlimentoIngesta(final AlimentoIngesta original) {
		final AlimentoIngesta copy = new AlimentoIngesta();
		copy.setName(original.getName());
		copy.setPortions(original.getPortions());
		copy.setAlimento(original.getAlimento());
		copy.setUnidad(original.getUnidad());

		// Copy all nutritional values
		copy.setPesoBrutoRedondeado(original.getPesoBrutoRedondeado());
		copy.setPesoNeto(original.getPesoNeto());
		copy.setEnergia(original.getEnergia());
		copy.setProteina(original.getProteina());
		copy.setLipidos(original.getLipidos());
		copy.setHidratosDeCarbono(original.getHidratosDeCarbono());
		copy.setFibra(original.getFibra());
		copy.setVitA(original.getVitA());
		copy.setAcidoAscorbico(original.getAcidoAscorbico());
		copy.setHierroNoHem(original.getHierroNoHem());
		copy.setPotasio(original.getPotasio());
		copy.setIndiceGlicemico(original.getIndiceGlicemico());
		copy.setCargaGlicemica(original.getCargaGlicemica());
		copy.setAcidoFolico(original.getAcidoFolico());
		copy.setCalcio(original.getCalcio());
		copy.setHierro(original.getHierro());
		copy.setSodio(original.getSodio());
		copy.setAzucarPorEquivalente(original.getAzucarPorEquivalente());
		copy.setSelenio(original.getSelenio());
		copy.setFosforo(original.getFosforo());
		copy.setColesterol(original.getColesterol());
		copy.setAgSaturados(original.getAgSaturados());
		copy.setAgMonoinsaturados(original.getAgMonoinsaturados());
		copy.setAgPoliinsaturados(original.getAgPoliinsaturados());
		copy.setEtanol(original.getEtanol());

		return copy;
	}

}
