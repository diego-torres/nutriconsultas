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
	public Dieta addDieta(@RequestBody Dieta dieta) {
		log.info("starting addDieta with dieta {}.", dieta);
		List<Ingesta> ingestas = Stream.of("Desayuno", "Comida", "Cena").map(Ingesta::new).collect(Collectors.toList());
		ingestas.forEach(i -> i.setDieta(dieta));
		dieta.setIngestas(ingestas);
		Dieta _dieta = dietaService.saveDieta(dieta);
		log.info("finish addDieta with dieta {}.", dieta);
		return _dieta;
	}

	@DeleteMapping("{dietaId}/ingestas/{ingestaId}/platillos/{platilloIngestaId}")
	public ResponseEntity<ApiResponse<Dieta>> deletePlatilloIngesta(@PathVariable @NonNull Long dietaId,
			@PathVariable @NonNull Long ingestaId, @PathVariable @NonNull Long platilloIngestaId) {
		log.info("starting deletePlatilloIngesta with dietaId {}, ingestaId {}, platilloIngestaId {}.", dietaId,
				ingestaId, platilloIngestaId);
		Dieta dieta = dietaService.getDieta(dietaId);
		if (dieta != null) {
			dieta.getIngestas()
				.stream()
				.filter(ingesta -> ingesta.getId().equals(ingestaId))
				.findFirst()
				.ifPresent(ingesta -> ingesta.getPlatillos()
					.removeIf(platillo -> platillo.getId().equals(platilloIngestaId)));
			Dieta saved = dietaService.saveDieta(dieta);
			log.info("finish deletePlatilloIngesta with dietaId {}, ingestaId {}, platilloIngestaId {}.", dietaId,
					ingestaId, platilloIngestaId);
			return ResponseEntity.ok(new ApiResponse<Dieta>(saved));
		}
		log.warn("Dieta with id {} not found when trying to delete platilloIngesta", dietaId);
		return ResponseEntity.notFound().build();
	}

	@Override
	protected List<Column> getColumns() {
		return Stream.of("dieta", "ingestas", "dist", "kcal", "prot", "lip", "hc")
			.map(Column::new)
			.collect(Collectors.toList());
	}

	@Override
	protected List<String> toStringList(Dieta row) {
		log.debug("converting Dieta row {} to string list.", row);
		return Arrays.asList("<a href='/admin/dietas/" + row.getId() + "'>" + row.getNombre() + "</a>",
				getIngestas(row), getDist(row), String.format("%.1f", getKCal(row)),
				String.format("%.1f", getTotalProteina(row)), String.format("%.1f", getTotalLipidos(row)),
				String.format("%.1f", getTotalHidratosDeCarbono(row)));
	}

	private String getIngestas(Dieta row) {
		return row.getIngestas().stream().map(Ingesta::getNombre).collect(Collectors.joining(", "));
	}

	private String getDist(Dieta row) {
		// use protein, lipid, and carbohydrate values to calculate distribution
		Double kCal = getKCal(row);

		// If diet is empty (kCal is 0 or very small), return empty string
		if (kCal == null || kCal == 0 || kCal < 0.01) {
			return "";
		}

		Double distProteina = getTotalProteina(row) * 4 / kCal;
		Double distLipido = getTotalLipidos(row) * 9 / kCal;
		Double distHidratoCarbono = getTotalHidratosDeCarbono(row) * 4 / kCal;

		return String.format("%.1f", distProteina) + " / " + String.format("%.1f", distLipido) + " / "
				+ String.format("%.1f", distHidratoCarbono);
	}

	private Double getKCal(Dieta row) {
		return getTotalProteina(row) * 4 + getTotalLipidos(row) * 9 + getTotalHidratosDeCarbono(row) * 4;
	}

	private Double getTotalProteina(Dieta row) {
		return row.getIngestas()
			.stream()
			.mapToDouble(i -> i.getPlatillos().stream().mapToDouble(p -> p.getProteina()).sum())
			.sum();
	}

	private Double getTotalLipidos(Dieta row) {
		return row.getIngestas()
			.stream()
			.mapToDouble(i -> i.getPlatillos().stream().mapToDouble(p -> p.getLipidos()).sum())
			.sum();
	}

	private Double getTotalHidratosDeCarbono(Dieta row) {
		return row.getIngestas()
			.stream()
			.mapToDouble(i -> i.getPlatillos().stream().mapToDouble(p -> p.getHidratosDeCarbono()).sum())
			.sum();
	}

	@Override
	protected List<Dieta> getData() {
		log.debug("getting all Dieta records.");
		return dietaService.getDietas();
	}

	@Override
	protected Predicate<Dieta> getPredicate(String value) {
		return row -> row.getNombre().toLowerCase().contains(value) || row.getNombre().toLowerCase().startsWith(value)
				|| row.getIngestas().stream().anyMatch(i -> i.getNombre().toLowerCase().contains(value))
				|| row.getIngestas().stream().anyMatch(i -> i.getNombre().toLowerCase().startsWith(value));
	}

	@Override
	protected Comparator<Dieta> getComparator(String column, Direction dir) {
		log.debug("getting Dieta comparator with column {} and direction {}.", column, dir);
		return DietaComparators.getComparator(column, dir);
	}

}
