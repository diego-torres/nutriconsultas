package com.nutriconsultas.platillos;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
import com.nutriconsultas.dataTables.paging.Order;
import com.nutriconsultas.dataTables.paging.PageArray;
import com.nutriconsultas.dataTables.paging.PagingRequest;
import com.nutriconsultas.model.ApiResponse;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("rest/platillos")
@Slf4j
public class PlatilloRestController extends AbstractGridController<Platillo> {

	@Autowired
	private PlatilloService service;

	private static final Map<String, String> COLUMN_TO_FIELD_MAP = new HashMap<>();

	static {
		COLUMN_TO_FIELD_MAP.put("platillo", "name");
		COLUMN_TO_FIELD_MAP.put("ingestas", "ingestasSugeridas");
		COLUMN_TO_FIELD_MAP.put("kcal", "energia");
		COLUMN_TO_FIELD_MAP.put("prot", "proteina");
		COLUMN_TO_FIELD_MAP.put("lip", "lipidos");
		COLUMN_TO_FIELD_MAP.put("hc", "hidratosDeCarbono");
	}

	/**
	 * Converts a PagingRequest to Spring Data Pageable.
	 * @param pagingRequest the paging request
	 * @return Spring Data Pageable
	 */
	@NonNull
	private Pageable toPageable(final PagingRequest pagingRequest) {
		final int length = pagingRequest.getLength() > 0 ? pagingRequest.getLength() : 10;
		final int page = pagingRequest.getStart() / length;
		final int size = length;

		Sort sort = Sort.unsorted();
		if (pagingRequest.getOrder() != null && !pagingRequest.getOrder().isEmpty()) {
			final Order order = pagingRequest.getOrder().get(0);
			if (order.getColumn() != null && order.getColumn() < pagingRequest.getColumns().size()) {
				final String columnName = pagingRequest.getColumns().get(order.getColumn()).getData();
				final String fieldName = COLUMN_TO_FIELD_MAP.getOrDefault(columnName, columnName);
				final Sort.Direction direction = order.getDir() == Direction.asc ? Sort.Direction.ASC
						: Sort.Direction.DESC;
				sort = Sort.by(direction, fieldName);
			}
		}

		return PageRequest.of(page, size, sort);
	}

	/**
	 * Overrides base class method to use server-side pagination.
	 * @param pagingRequest the paging request
	 * @return paginated platillo data
	 */
	@Override
	@PostMapping("data-table")
	public PageArray getPageArray(@RequestBody final PagingRequest pagingRequest) {
		log.info("starting getPageArray with pagingRequest: {}", pagingRequest);
		pagingRequest.setColumns(getColumns());
		final com.nutriconsultas.dataTables.paging.Page<Platillo> page = getRows(pagingRequest);
		log.debug("page with records: {}", page.getRecordsTotal());
		final PageArray pageArray = new PageArray();
		pageArray.setData(page.getData().stream().map(this::toStringList).collect(Collectors.toList()));
		pageArray.setDraw(page.getDraw());
		pageArray.setRecordsFiltered(page.getRecordsFiltered());
		pageArray.setRecordsTotal(page.getRecordsTotal());
		log.info("returning data at getPageArray: {}", pageArray.getRecordsTotal());
		return pageArray;
	}

	/**
	 * Gets rows using server-side pagination.
	 * @param pagingRequest the paging request
	 * @return the page of platillos
	 */
	@Override
	protected com.nutriconsultas.dataTables.paging.Page<Platillo> getRows(final PagingRequest pagingRequest) {
		log.debug("starting getRows with pagingRequest: {}", pagingRequest);
		final Pageable pageable = toPageable(pagingRequest);
		final String searchValue = pagingRequest.getSearch() != null && pagingRequest.getSearch().getValue() != null
				? pagingRequest.getSearch().getValue().trim() : null;

		final Page<Platillo> springPage;
		final long totalCount;
		final long filteredCount;

		if (searchValue != null && !searchValue.isEmpty()) {
			springPage = service.findBySearchTerm(searchValue, pageable);
			filteredCount = service.countBySearchTerm(searchValue);
		}
		else {
			springPage = service.findAll(pageable);
			filteredCount = service.count();
		}
		totalCount = service.count();

		final com.nutriconsultas.dataTables.paging.Page<Platillo> result = new com.nutriconsultas.dataTables.paging.Page<>(
				springPage.getContent());
		result.setRecordsFiltered((int) filteredCount);
		result.setRecordsTotal((int) totalCount);
		result.setDraw(pagingRequest.getDraw());

		log.debug("returning data at getRows: recordsTotal={}, recordsFiltered={}", result.getRecordsTotal(),
				result.getRecordsFiltered());
		return result;
	}

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
			final Ingrediente ingredienteResult = service.addIngrediente(id, alimentoId, cantidad, peso);
			log.info("finish addIngrediente with id {} and ingrediente {}.", id, ingrediente);
			result = ResponseEntity.ok(new ApiResponse<Ingrediente>(ingredienteResult));
		}
		return result;
	}

	@PostMapping("{id}/ingestas/add")
	public ResponseEntity<ApiResponse<Platillo>> addIngesta(@PathVariable @NonNull final Long id,
			@RequestBody @NonNull final IngestaFormModel ingesta) {
		log.info("starting addIngesta with id {} and ingesta {}.", id, ingesta);
		final Platillo platillo = service.findById(id);
		String ingestas = platillo.getIngestasSugeridas();

		// prevent duplicate ingestas
		ResponseEntity<ApiResponse<Platillo>> result;
		if (ingestas != null && ingestas.contains(ingesta.getIngesta())) {
			log.info("finish addIngesta with id {} and ingesta {} - DUPLICATE requested.", id, ingesta);
			result = ResponseEntity.ok(new ApiResponse<Platillo>(platillo));
		}
		else {
			if (ingestas == null || ingestas.isEmpty()) {
				ingestas = ingesta.getIngesta();
			}
			else {
				ingestas += ", " + ingesta.getIngesta();
			}
			platillo.setIngestasSugeridas(ingestas);
			final Platillo saved = service.save(platillo);
			log.info("finish addIngesta with id {} and ingesta {}.", id, ingesta);
			result = ResponseEntity.ok(new ApiResponse<Platillo>(saved));
		}
		return result;
	}

	@DeleteMapping("{id}/ingestas/{ingesta}")
	public ResponseEntity<ApiResponse<Platillo>> deleteIngesta(@PathVariable @NonNull final Long id,
			@PathVariable @NonNull final String ingesta) {
		log.info("starting deleteIngesta with id {} and ingesta {}.", id, ingesta);
		final Platillo platillo = service.findById(id);
		final String ingestas = platillo.getIngestasSugeridas();
		ResponseEntity<ApiResponse<Platillo>> result;
		if (ingestas != null) {
			String updatedIngestas = ingestas.replace(ingesta, "").replace(",,", ",").trim();
			if (updatedIngestas.startsWith(",")) {
				updatedIngestas = updatedIngestas.substring(1);
			}
			if (updatedIngestas.endsWith(",")) {
				updatedIngestas = updatedIngestas.substring(0, updatedIngestas.length() - 1);
			}
			platillo.setIngestasSugeridas(updatedIngestas);
			log.debug("Ingestas after delete: [{}]", updatedIngestas);
			final Platillo saved = service.save(platillo);
			log.info("finish deleteIngesta with id {} and ingesta {}.", id, ingesta);
			result = ResponseEntity.ok(new ApiResponse<Platillo>(saved));
		}
		else {
			log.info("finish deleteIngesta with id {} and ingesta {}.", id, ingesta);
			result = ResponseEntity.ok(new ApiResponse<Platillo>(platillo));
		}
		return result;
	}

	// "/rest/platillos/" + $("#id").val() + "/video/add"
	@PostMapping("{id}/video/add")
	public ResponseEntity<ApiResponse<Platillo>> addVideo(@PathVariable @NonNull final Long id,
			@RequestBody @NonNull final VideoFormModel video) {
		log.info("starting addVideo with id {} and video {}.", id, video);
		final Platillo platillo = service.findById(id);
		platillo.setVideoUrl(video.getVideoUrl());
		final Platillo saved = service.save(platillo);
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
		log.warn("getData() called without pagination. This should not happen in production.");
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
