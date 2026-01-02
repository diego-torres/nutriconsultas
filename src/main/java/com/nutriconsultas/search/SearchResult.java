package com.nutriconsultas.search;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SearchResult {

	private SearchResultType type;

	private Long id;

	private String title;

	private String description;

	private String link;

}
