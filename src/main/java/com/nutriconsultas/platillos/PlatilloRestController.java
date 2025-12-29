package com.nutriconsultas.platillos;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

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
import com.nutriconsultas.model.ApiResponse;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("rest/platillos")
@Slf4j
public class PlatilloRestController extends AbstractGridController<Platillo> {

	@Autowired
	private PlatilloService service;

	@PostMapping("{id}/ingredientes/add")
	public ResponseEntity<ApiResponse<Ingrediente>> addIngrediente(@PathVariable @NonNull final Long id,
			@RequestBody @NonNull final IngredienteFormModel ingrediente) {
		log.info("starting addIngrediente with id {} and ingrediente {}.", id, ingrediente);
		final Long alimentoId = ingrediente.getAlimentoId();
		final String cantidad = ingrediente.getCantidad();
		final Integer peso = ingrediente.getPeso();
		ResponseEntity<ApiResponse<Ingrediente>> result;
		if (alimentoId == null || cantidad == null || peso == null) {
			log.error("Missing required fields: alimentoId={}, cantidad={}, peso={}", alimentoId, cantidad, peso);
			result = ResponseEntity.badRequest().build();
		}
		else {
			final Ingrediente _ingrediente = service.addIngrediente(id, alimentoId, cantidad, peso);
			log.info("finish addIngrediente with id {} and ingrediente {}.", id, ingrediente);
			result = ResponseEntity.ok(new ApiResponse<Ingrediente>(_ingrediente));
		}
		return result;
	}

	@PostMapping("{id}/ingestas/add")
	public ResponseEntity<ApiResponse<Platillo>> addIngesta(@PathVariable @NonNull final Long id,
			@RequestBody @NonNull final IngestaFormModel ingesta) {
		log.info("starting addIngesta with id {} and ingesta {}.", id, ingesta);
		final Platillo _platillo = service.findById(id);
		String ingestas = _platillo.getIngestasSugeridas();

		// prevent duplicate ingestas
		ResponseEntity<ApiResponse<Platillo>> result;
		if (ingestas != null && ingestas.contains(ingesta.getIngesta())) {
			log.info("finish addIngesta with id {} and ingesta {} - DUPLICATE requested.", id, ingesta);
			result = ResponseEntity.ok(new ApiResponse<Platillo>(_platillo));
		}
		else {
			if (ingestas == null || ingestas.isEmpty()) {
				ingestas = ingesta.getIngesta();
			}
			else {
				ingestas += ", " + ingesta.getIngesta();
			}
			_platillo.setIngestasSugeridas(ingestas);
			final Platillo saved = service.save(_platillo);
			log.info("finish addIngesta with id {} and ingesta {}.", id, ingesta);
			result = ResponseEntity.ok(new ApiResponse<Platillo>(saved));
		}
		return result;
	}

	@DeleteMapping("{id}/ingestas/{ingesta}")
	public ResponseEntity<ApiResponse<Platillo>> deleteIngesta(@PathVariable @NonNull final Long id,
			@PathVariable @NonNull final String ingesta) {
		log.info("starting deleteIngesta with id {} and ingesta {}.", id, ingesta);
		final Platillo _platillo = service.findById(id);
		final String ingestas = _platillo.getIngestasSugeridas();
		ResponseEntity<ApiResponse<Platillo>> result;
		if (ingestas != null) {
			String updatedIngestas = ingestas.replace(ingesta, "").replace(",,", ",").trim();
			if (updatedIngestas.startsWith(",")) {
				updatedIngestas = updatedIngestas.substring(1);
			}
			if (updatedIngestas.endsWith(",")) {
				updatedIngestas = updatedIngestas.substring(0, updatedIngestas.length() - 1);
			}
			_platillo.setIngestasSugeridas(updatedIngestas);
			log.debug("Ingestas after delete: [{}]", updatedIngestas);
			final Platillo saved = service.save(_platillo);
			log.info("finish deleteIngesta with id {} and ingesta {}.", id, ingesta);
			result = ResponseEntity.ok(new ApiResponse<Platillo>(saved));
		}
		else {
			log.info("finish deleteIngesta with id {} and ingesta {}.", id, ingesta);
			result = ResponseEntity.ok(new ApiResponse<Platillo>(_platillo));
		}
		return result;
	}

	// "/rest/platillos/" + $("#id").val() + "/video/add"
	@PostMapping("{id}/video/add")
	public ResponseEntity<ApiResponse<Platillo>> addVideo(@PathVariable @NonNull final Long id,
			@RequestBody @NonNull final VideoFormModel video) {
		log.info("starting addVideo with id {} and video {}.", id, video);
		final Platillo _platillo = service.findById(id);
		_platillo.setVideoUrl(video.getVideoUrl());
		final Platillo saved = service.save(_platillo);
		log.info("finish addVideo with id {} and video {}.", id, video);
		return ResponseEntity.ok(new ApiResponse<Platillo>(saved));
	}

	@PostMapping("add")
	public Platillo add(@RequestBody @NonNull final Platillo platillo) {
		log.info("starting add with platillo {}.", platillo);
		final Platillo saved = service.save(platillo);
		log.info("finish add with platillo {}.", saved);
		return saved;
	}

	@Override
	protected List<Column> getColumns() {
		log.debug("getting Platillo columns.");
		return Arrays.asList(new Column("platillo"), //
				new Column("ingestas"), //
				new Column("kcal"), //
				new Column("prot"), //
				new Column("lip"), //
				new Column("hc"));
	}

	@Override
	protected List<String> toStringList(final Platillo row) {
		log.debug("converting Platillo row {} to string list.", row);
		return Arrays.asList("<a href='/admin/platillos/" + row.getId() + "'>" + row.getName() + "</a>",
				row.getIngestasSugeridas() == null ? "" : row.getIngestasSugeridas(),
				row.getEnergia() == null ? "" : row.getEnergia().toString(), String.format("%.1f", row.getProteina()), //
				String.format("%.1f", row.getLipidos()), //
				String.format("%.1f", row.getHidratosDeCarbono()));
	}

	@Override
	protected List<Platillo> getData() {
		log.debug("getting all Platillos.");
		return service.findAll();
	}

	@Override
	protected Predicate<Platillo> getPredicate(final String value) {
		log.debug("getting Platillo predicate with value {}.", value);
		return platillo -> platillo.getName().toLowerCase().contains(value.toLowerCase())
				|| (platillo.getIngestasSugeridas() != null
						&& platillo.getIngestasSugeridas().toLowerCase().contains(value.toLowerCase()));
	}

	@Override
	protected Comparator<Platillo> getComparator(final String column,
			final com.nutriconsultas.dataTables.paging.Direction dir) {
		log.debug("getting Platillo comparator with column {} and direction {}.", column, dir);
		return PlatilloComparators.getComparator(column, dir);
	}

}
