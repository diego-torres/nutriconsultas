package com.nutriconsultas.dataTables.paging;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Column {

	private String data;

	private String name;

	private Boolean searchable;

	private Boolean orderable;

	private Search search;

	public Column(String data) {
		this.data = data;
	}

}
