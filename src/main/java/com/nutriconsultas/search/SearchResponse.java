package com.nutriconsultas.search;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SearchResponse {

	private String query;

	private PaginatedSearchResults pacientes;

	private PaginatedSearchResults alimentos;

	private PaginatedSearchResults platillos;

	private PaginatedSearchResults calendarEvents;

	private PaginatedSearchResults clinicalExams;

	private int totalResults;

}
