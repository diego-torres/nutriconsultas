package com.nutriconsultas.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.nutriconsultas.dataTables.paging.Page;
import com.nutriconsultas.dataTables.paging.PageArray;
import com.nutriconsultas.dataTables.paging.PagingRequest;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractGridItemController<T> extends AbstractGridController<T> {

	protected abstract List<T> getData(@NonNull Long id);

	@PostMapping("data-table")
	public PageArray getPageArray(@RequestBody PagingRequest pagingRequest, @NonNull @PathVariable Long id) {
		log.info("starting getPageArray with pagingRequest: {} for id {}", pagingRequest, id);
		pagingRequest.setColumns(getColumns());
		Page<T> page = getRows(pagingRequest, id);
		log.debug("page with records: {}", page.getRecordsTotal());
		PageArray pageArray = new PageArray();
		pageArray.setData(page.getData().stream().map(this::toStringList).collect(Collectors.toList()));
		pageArray.setDraw(page.getDraw());
		pageArray.setRecordsFiltered(page.getRecordsFiltered());
		pageArray.setRecordsTotal(page.getRecordsTotal());
		log.info("returning data at getPageArray: {}", pageArray.getRecordsTotal());
		return pageArray;
	}

	@Override
	@PostMapping("not-implemented")
	public PageArray getPageArray(@RequestBody PagingRequest pagingRequest) {
		log.warn("getPageArray() called on AbstractGridItemController. This should not be called.");
		return null;
	}

	protected Page<T> getRows(PagingRequest pagingRequest, @NonNull Long id) {
		log.debug("starting getRows with pagingRequest: {}", pagingRequest);
		return getPage(pagingRequest, getData(id));
	}

	@Override
	protected List<T> getData() {
		log.warn("getData() called on AbstractGridItemController. This should not be called.");
		return null;
	}

	@Override
	protected Page<T> getRows(PagingRequest pagingRequest) {
		log.warn("getRows() called on AbstractGridItemController. This should not be called.");
		return null;
	}

}
