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
	public PageArray getPageArray(@RequestBody PagingRequest pagingRequest) {
		log.info("starting getPageArray with pagingRequest: {}", pagingRequest);
		pagingRequest.setColumns(getColumns());
		Page<T> page = getRows(pagingRequest);
		log.debug("page with records: {}", page.getRecordsTotal());
		PageArray pageArray = new PageArray();
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

	protected Page<T> getRows(PagingRequest pagingRequest) {
		log.debug("starting getRows with pagingRequest: {}", pagingRequest);
		return getPage(pagingRequest, getData());
	}

	protected Page<T> getPage(PagingRequest pagingRequest, List<T> data) {
		log.debug("getPage: {}", pagingRequest);
		List<T> filtered = data.stream()
			.filter(filterRows(pagingRequest))
			.sorted(sortRows(pagingRequest))
			.skip(pagingRequest.getStart())
			.limit(pagingRequest.getLength())
			.collect(Collectors.toList());

		log.debug("filtered records {}", filtered.size());

		long count = data.stream().filter(filterRows(pagingRequest)).count();

		log.debug("total records {}", count);

		Page<T> result = new Page<>(filtered);
		result.setRecordsFiltered((int) count);
		result.setRecordsTotal((int) count);
		result.setDraw(pagingRequest.getDraw());

		log.debug("returning data at getPage: {}", result.getRecordsTotal());
		return result;
	}

	private Predicate<T> filterRows(PagingRequest pagingRequest) {
		log.debug("filterRows: {}", pagingRequest);
		Predicate<T> predicate = t -> true;
		if (pagingRequest.getSearch() != null) {
			String value = pagingRequest.getSearch().getValue().toLowerCase();
			predicate = getPredicate(value);
		}
		log.debug("filterRows: {}", predicate);
		return predicate;
	}

	private Comparator<T> sortRows(PagingRequest pagingRequest) {
		log.debug("start sortRows: {}", pagingRequest);
		Comparator<T> comparator = (o1, o2) -> 0;
		if (pagingRequest.getOrder() != null) {
			Order order = pagingRequest.getOrder().get(0);
			String column = pagingRequest.getColumns().get(order.getColumn()).getData();
			comparator = getComparator(column, order.getDir());
		}
		log.debug("finish sortRows: {}", comparator);
		return comparator;
	}

}
