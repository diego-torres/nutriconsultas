package com.nutriconsultas.search;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaginatedSearchResults {

	private List<SearchResult> results;

	private int totalCount;

	private int currentPage;

	private int pageSize;

	private int totalPages;

}
