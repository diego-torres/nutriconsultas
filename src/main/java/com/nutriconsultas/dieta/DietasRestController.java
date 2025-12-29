package com.nutriconsultas.dieta;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nutriconsultas.controller.AbstractGridController;
import com.nutriconsultas.dataTables.paging.Column;
import com.nutriconsultas.dataTables.paging.Direction;
import com.nutriconsultas.model.ApiResponse;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/rest/dietas")
@Slf4j
public class DietasRestController extends AbstractGridController<Dieta> {

	@Autowired
	private DietaService dietaService;

	@PostMapping("add")
	public Dieta addDieta(@RequestBody final Dieta dieta) {
		log.info("starting addDieta with dieta {}.", dieta);
		final List<Ingesta> ingestas = Stream.of("Desayuno", "Comida", "Cena")
			.map(Ingesta::new)
			.collect(Collectors.toList());
		ingestas.forEach(i -> i.setDieta(dieta));
		dieta.setIngestas(ingestas);
		final Dieta _dieta = dietaService.saveDieta(dieta);
		log.info("finish addDieta with dieta {}.", dieta);
		return _dieta;
	}

	@DeleteMapping("{dietaId}/ingestas/{ingestaId}/platillos/{platilloIngestaId}")
	public ResponseEntity<ApiResponse<Dieta>> deletePlatilloIngesta(@PathVariable @NonNull final Long dietaId,
			@PathVariable @NonNull final Long ingestaId, @PathVariable @NonNull final Long platilloIngestaId) {
		log.info("starting deletePlatilloIngesta with dietaId {}, ingestaId {}, platilloIngestaId {}.", dietaId,
				ingestaId, platilloIngestaId);
		final Dieta dieta = dietaService.getDieta(dietaId);
		ResponseEntity<ApiResponse<Dieta>> result;
		if (dieta != null) {
			dieta.getIngestas()
				.stream()
				.filter(ingesta -> ingesta.getId().equals(ingestaId))
				.findFirst()
				.ifPresent(ingesta -> ingesta.getPlatillos()
					.removeIf(platillo -> platillo.getId().equals(platilloIngestaId)));
			final Dieta saved = dietaService.saveDieta(dieta);
			log.info("finish deletePlatilloIngesta with dietaId {}, ingestaId {}, platilloIngestaId {}.", dietaId,
					ingestaId, platilloIngestaId);
			result = ResponseEntity.ok(new ApiResponse<Dieta>(saved));
		}
		else {
			log.warn("Dieta with id {} not found when trying to delete platilloIngesta", dietaId);
			result = ResponseEntity.notFound().build();
		}
		return result;
	}

	@DeleteMapping("{dietaId}/ingestas/{ingestaId}/alimentos/{alimentoIngestaId}")
	public ResponseEntity<ApiResponse<Dieta>> deleteAlimentoIngesta(@PathVariable @NonNull final Long dietaId,
			@PathVariable @NonNull final Long ingestaId, @PathVariable @NonNull final Long alimentoIngestaId) {
		log.info("starting deleteAlimentoIngesta with dietaId {}, ingestaId {}, alimentoIngestaId {}.", dietaId,
				ingestaId, alimentoIngestaId);
		final Dieta dieta = dietaService.getDieta(dietaId);
		ResponseEntity<ApiResponse<Dieta>> result;
		if (dieta != null) {
			dieta.getIngestas()
				.stream()
				.filter(ingesta -> ingesta.getId().equals(ingestaId))
				.findFirst()
				.ifPresent(ingesta -> ingesta.getAlimentos()
					.removeIf(alimento -> alimento.getId().equals(alimentoIngestaId)));
			final Dieta saved = dietaService.saveDieta(dieta);
			log.info("finish deleteAlimentoIngesta with dietaId {}, ingestaId {}, alimentoIngestaId {}.", dietaId,
					ingestaId, alimentoIngestaId);
			result = ResponseEntity.ok(new ApiResponse<Dieta>(saved));
		}
		else {
			log.warn("Dieta with id {} not found when trying to delete alimentoIngesta", dietaId);
			result = ResponseEntity.notFound().build();
		}
		return result;
	}

	@Override
	protected List<Column> getColumns() {
		return Stream.of("dieta", "ingestas", "dist", "kcal", "prot", "lip", "hc")
			.map(Column::new)
			.collect(Collectors.toList());
	}

	@Override
	protected List<String> toStringList(final Dieta row) {
		log.debug("converting Dieta row {} to string list.", row);
		return Arrays.asList("<a href='/admin/dietas/" + row.getId() + "'>" + row.getNombre() + "</a>",
				getIngestas(row), getDist(row), String.format("%.1f", getKCal(row)),
				String.format("%.1f", getTotalProteina(row)), String.format("%.1f", getTotalLipidos(row)),
				String.format("%.1f", getTotalHidratosDeCarbono(row)));
	}

	private String getIngestas(final Dieta row) {
		return row.getIngestas().stream().map(Ingesta::getNombre).collect(Collectors.joining(", "));
	}

	private String getDist(final Dieta row) {
		// use protein, lipid, and carbohydrate values to calculate distribution
		final Double kCal = getKCal(row);

		// If diet is empty (kCal is 0 or very small), return empty string
		String result;
		if (kCal == null || kCal == 0 || kCal < 0.01) {
			result = "";
		}
		else {
			final Double distProteina = getTotalProteina(row) * 4 / kCal;
			final Double distLipido = getTotalLipidos(row) * 9 / kCal;
			final Double distHidratoCarbono = getTotalHidratosDeCarbono(row) * 4 / kCal;

			result = String.format("%.1f", distProteina) + " / " + String.format("%.1f", distLipido) + " / "
					+ String.format("%.1f", distHidratoCarbono);
		}
		return result;
	}

	private Double getKCal(final Dieta row) {
		return getTotalProteina(row) * 4 + getTotalLipidos(row) * 9 + getTotalHidratosDeCarbono(row) * 4;
	}

	private Double getTotalProteina(final Dieta row) {
		return row.getIngestas()
			.stream()
			.mapToDouble(i -> i.getPlatillos()
				.stream()
				.mapToDouble(p -> p.getProteina() != null ? p.getProteina() : 0.0)
				.sum()
					+ i.getAlimentos().stream().mapToDouble(a -> a.getProteina() != null ? a.getProteina() : 0.0).sum())
			.sum();
	}

	private Double getTotalLipidos(final Dieta row) {
		return row.getIngestas()
			.stream()
			.mapToDouble(i -> i.getPlatillos()
				.stream()
				.mapToDouble(p -> p.getLipidos() != null ? p.getLipidos() : 0.0)
				.sum()
					+ i.getAlimentos().stream().mapToDouble(a -> a.getLipidos() != null ? a.getLipidos() : 0.0).sum())
			.sum();
	}

	private Double getTotalHidratosDeCarbono(final Dieta row) {
		return row.getIngestas()
			.stream()
			.mapToDouble(i -> i.getPlatillos()
				.stream()
				.mapToDouble(p -> p.getHidratosDeCarbono() != null ? p.getHidratosDeCarbono() : 0.0)
				.sum()
					+ i.getAlimentos()
						.stream()
						.mapToDouble(a -> a.getHidratosDeCarbono() != null ? a.getHidratosDeCarbono() : 0.0)
						.sum())
			.sum();
	}

	@Override
	protected List<Dieta> getData() {
		log.debug("getting all Dieta records.");
		return dietaService.getDietas();
	}

	@Override
	protected Predicate<Dieta> getPredicate(final String value) {
		return row -> row.getNombre().toLowerCase().contains(value) || row.getNombre().toLowerCase().startsWith(value)
				|| row.getIngestas().stream().anyMatch(i -> i.getNombre().toLowerCase().contains(value))
				|| row.getIngestas().stream().anyMatch(i -> i.getNombre().toLowerCase().startsWith(value));
	}

	@Override
	protected Comparator<Dieta> getComparator(final String column, final Direction dir) {
		log.debug("getting Dieta comparator with column {} and direction {}.", column, dir);
		return DietaComparators.getComparator(column, dir);
	}

}
