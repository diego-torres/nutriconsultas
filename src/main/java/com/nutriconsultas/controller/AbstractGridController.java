package com.nutriconsultas.controller;

import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.nutriconsultas.dataTables.paging.Column;
import com.nutriconsultas.dataTables.paging.Direction;
import com.nutriconsultas.dataTables.paging.Order;
import com.nutriconsultas.dataTables.paging.Page;
import com.nutriconsultas.dataTables.paging.PageArray;
import com.nutriconsultas.dataTables.paging.PagingRequest;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractGridController<T> {

	@PostMapping("data-table")
	public PageArray getPageArray(@RequestBody final PagingRequest pagingRequest) {
		log.info("starting getPageArray with pagingRequest: {}", pagingRequest);
		pagingRequest.setColumns(getColumns());
		final Page<T> page = getRows(pagingRequest);
		log.debug("page with records: {}", page.getRecordsTotal());
		final PageArray pageArray = new PageArray();
		pageArray.setData(page.getData().stream().map(this::toStringList).collect(Collectors.toList()));
		pageArray.setDraw(page.getDraw());
		pageArray.setRecordsFiltered(page.getRecordsFiltered());
		pageArray.setRecordsTotal(page.getRecordsTotal());
		log.info("returning data at getPageArray: {}", pageArray.getRecordsTotal());
		return pageArray;
	}

	protected abstract List<String> toStringList(T row);

	protected abstract List<T> getData();

	protected abstract Predicate<T> getPredicate(String value);

	protected abstract Comparator<T> getComparator(String column, Direction dir);

	protected abstract List<Column> getColumns();

	protected Page<T> getRows(final PagingRequest pagingRequest) {
		log.debug("starting getRows with pagingRequest: {}", pagingRequest);
		return getPage(pagingRequest, getData());
	}

	protected Page<T> getPage(final PagingRequest pagingRequest, final List<T> data) {
		log.debug("getPage: {}", pagingRequest);
		final List<T> filtered = data.stream()
			.filter(filterRows(pagingRequest))
			.sorted(sortRows(pagingRequest))
			.skip(pagingRequest.getStart())
			.limit(pagingRequest.getLength())
			.collect(Collectors.toList());

		log.debug("filtered records {}", filtered.size());

		final long filteredCount = data.stream().filter(filterRows(pagingRequest)).count();
		final int totalCount = data.size();

		log.debug("total records before filtering: {}", totalCount);
		log.debug("total records after filtering: {}", filteredCount);

		final Page<T> result = new Page<>(filtered);
		result.setRecordsFiltered((int) filteredCount);
		result.setRecordsTotal(totalCount);
		result.setDraw(pagingRequest.getDraw());

		log.debug("returning data at getPage: recordsTotal={}, recordsFiltered={}", result.getRecordsTotal(),
				result.getRecordsFiltered());
		return result;
	}

	private Predicate<T> filterRows(final PagingRequest pagingRequest) {
		log.debug("filterRows: {}", pagingRequest);
		Predicate<T> predicate = t -> true;
		if (pagingRequest.getSearch() != null) {
			final String value = pagingRequest.getSearch().getValue().toLowerCase();
			predicate = getPredicate(value);
		}
		log.debug("filterRows: {}", predicate);
		return predicate;
	}

	private Comparator<T> sortRows(final PagingRequest pagingRequest) {
		log.debug("start sortRows: {}", pagingRequest);
		Comparator<T> comparator = (o1, o2) -> 0;
		if (pagingRequest.getOrder() != null) {
			final Order order = pagingRequest.getOrder().get(0);
			final String column = pagingRequest.getColumns().get(order.getColumn()).getData();
			comparator = getComparator(column, order.getDir());
		}
		log.debug("finish sortRows: {}", comparator);
		return comparator;
	}

}
