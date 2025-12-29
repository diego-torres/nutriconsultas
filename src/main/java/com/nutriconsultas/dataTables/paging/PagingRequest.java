package com.nutriconsultas.dataTables.paging;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PagingRequest {

	private int start;

	private int length;

	private int draw;

	private List<Order> order;

	private List<Column> columns;

	private Search search;

}
