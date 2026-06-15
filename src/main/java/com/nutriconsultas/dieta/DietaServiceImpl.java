package com.nutriconsultas.dieta;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
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

	private static final int PICKER_MAX_PAGE_SIZE = 50;

	@Override
	@Transactional(readOnly = true)
	public DietaPickerPageDto findPickerPage(final String search, final int page, final int size,
			final Double requerimientoKcal) {
		final String term = search == null ? "" : search.trim().toLowerCase();
		final int safeSize = Math.min(Math.max(size, 1), PICKER_MAX_PAGE_SIZE);
		final int safePage = Math.max(page, 0);

		final List<DietaPickerItemDto> filtered = dietaRepository.findAll(Sort.by("nombre"))
			.stream()
			.filter(dieta -> matchesPickerSearch(dieta, term))
			.map(this::toPickerItem)
			.sorted(pickerComparator(requerimientoKcal))
			.toList();

		final int total = filtered.size();
		final int fromIndex = safePage * safeSize;
		final List<DietaPickerItemDto> pageItems;
		if (fromIndex >= total) {
			pageItems = List.of();
		}
		else {
			final int toIndex = Math.min(fromIndex + safeSize, total);
			pageItems = filtered.subList(fromIndex, toIndex);
		}

		final DietaPickerPageDto result = new DietaPickerPageDto();
		result.setItems(new ArrayList<>(pageItems));
		result.setPage(safePage);
		result.setSize(safeSize);
		result.setTotalElements(total);
		result.setHasNext(fromIndex + safeSize < total);
		return result;
	}

	private DietaPickerItemDto toPickerItem(final Dieta dieta) {
		final Double kcal = DietaNutritionCalculator.calculateTotalKcal(dieta);
		return new DietaPickerItemDto(dieta.getId(), dieta.getNombre(), kcal != null ? kcal.intValue() : 0);
	}

	private boolean matchesPickerSearch(final Dieta dieta, final String term) {
		if (term.isEmpty()) {
			return true;
		}
		final int kcal = DietaNutritionCalculator.calculateTotalKcal(dieta).intValue();
		return (dieta.getNombre() != null && dieta.getNombre().toLowerCase().contains(term))
				|| String.valueOf(kcal).contains(term)
				|| (dieta.getIngestas() != null && dieta.getIngestas()
					.stream()
					.anyMatch(ingesta -> ingesta.getNombre() != null
							&& ingesta.getNombre().toLowerCase().contains(term)));
	}

	private Comparator<DietaPickerItemDto> pickerComparator(final Double requerimientoKcal) {
		if (requerimientoKcal == null) {
			return Comparator.comparing(DietaPickerItemDto::getNombre, String.CASE_INSENSITIVE_ORDER);
		}
		return Comparator
			.comparingInt((DietaPickerItemDto item) -> caloricFitRank(item.getEnergiaKcal(), requerimientoKcal))
			.thenComparingInt(item -> Math.abs(item.getEnergiaKcal() - requerimientoKcal.intValue()))
			.thenComparing(DietaPickerItemDto::getNombre, String.CASE_INSENSITIVE_ORDER);
	}

	private int caloricFitRank(final Integer energiaKcal, final Double requerimientoKcal) {
		if (requerimientoKcal == null || energiaKcal == null || energiaKcal <= 0) {
			return 3;
		}
		final double tolerance = Math.max(50, requerimientoKcal * 0.05);
		final double diff = energiaKcal - requerimientoKcal;
		if (Math.abs(diff) <= tolerance) {
			return 0;
		}
		if (diff < 0) {
			return 1;
		}
		return 2;
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
