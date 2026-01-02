package com.nutriconsultas.alimentos;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.GetMapping;
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

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/rest/alimentos")
@Slf4j
public class AlimentoRestController extends AbstractGridController<Alimento> {

	@Autowired
	private AlimentoService service;

	private static final Map<String, String> COLUMN_TO_FIELD_MAP = new HashMap<>();

	static {
		COLUMN_TO_FIELD_MAP.put("alimento", "nombreAlimento");
		COLUMN_TO_FIELD_MAP.put("grupo", "clasificacion");
		COLUMN_TO_FIELD_MAP.put("cantidad", "cantSugerida");
		COLUMN_TO_FIELD_MAP.put("unidad", "unidad");
		COLUMN_TO_FIELD_MAP.put("bruto", "pesoBrutoRedondeado");
		COLUMN_TO_FIELD_MAP.put("neto", "pesoNeto");
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
	 * @return paginated alimento data
	 */
	@Override
	@PostMapping("data-table")
	public PageArray getPageArray(@RequestBody final PagingRequest pagingRequest) {
		log.info("starting getPageArray with pagingRequest: {}", pagingRequest);
		pagingRequest.setColumns(getColumns());
		final com.nutriconsultas.dataTables.paging.Page<Alimento> page = getRows(pagingRequest);
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
	 * @return the page of alimentos
	 */
	@Override
	protected com.nutriconsultas.dataTables.paging.Page<Alimento> getRows(final PagingRequest pagingRequest) {
		log.debug("starting getRows with pagingRequest: {}", pagingRequest);
		final Pageable pageable = toPageable(pagingRequest);
		final String searchValue = pagingRequest.getSearch() != null && pagingRequest.getSearch().getValue() != null
				? pagingRequest.getSearch().getValue().trim() : null;

		final Page<Alimento> springPage;
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

		final com.nutriconsultas.dataTables.paging.Page<Alimento> result = new com.nutriconsultas.dataTables.paging.Page<>(
				springPage.getContent());
		result.setRecordsFiltered((int) filteredCount);
		result.setRecordsTotal((int) totalCount);
		result.setDraw(pagingRequest.getDraw());

		log.debug("returning data at getRows: recordsTotal={}, recordsFiltered={}", result.getRecordsTotal(),
				result.getRecordsFiltered());
		return result;
	}

	@GetMapping("{id}")
	public Alimento get(@PathVariable @NonNull final Long id) {
		log.info("starting get with id {}.", id);
		final Alimento alimento = service.findById(id);
		log.info("finish get with alimento {}.", alimento);
		return alimento;
	}

	@Override
	protected List<String> toStringList(final Alimento row) {
		log.debug("converting Alimento row {} to string list.", row);
		return Arrays.asList("<a href='/admin/alimentos/" + row.getId() + "'>" + row.getNombreAlimento() + "</a>",
				row.getClasificacion(), //
				row.getFractionalCantSugerida(), //
				row.getUnidad(), //
				row.getPesoBrutoRedondeado().toString(), //
				row.getPesoNeto().toString(), //
				row.getEnergia().toString(), //
				String.format("%.1f", row.getProteina()), //
				String.format("%.1f", row.getLipidos()), //
				String.format("%.1f", row.getHidratosDeCarbono()));
	}

	@Override
	protected List<Alimento> getData() {
		log.warn("getData() called without pagination. This should not happen in production.");
		return service.findAll();
	}

	@Override
	protected Predicate<Alimento> getPredicate(final String value) {
		log.debug("getting Alimento predicate with value {}.", value);
		return row -> row.getNombreAlimento().toLowerCase().contains(value)
				|| row.getNombreAlimento().toLowerCase().startsWith(value)
				|| row.getClasificacion().toLowerCase().contains(value)
				|| row.getClasificacion().toLowerCase().startsWith(value);
	}

	@Override
	protected Comparator<Alimento> getComparator(final String column, final Direction dir) {
		log.debug("getting Alimento comparator with column {} and direction {}.", column, dir);
		return AlimentoComparators.getComparator(column, dir);
	}

	@Override
	protected List<Column> getColumns() {
		log.debug("getting Alimento columns.");
		return Stream.of("alimento", "grupo", "cantidad", "unidad", "bruto", "neto", "kcal", "prot", "lip", "hc")
			.map(Column::new)
			.collect(Collectors.toList());
	}

}
