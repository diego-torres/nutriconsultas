package com.nutriconsultas.dataTables.paging;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Page<T> {

	private List<T> data;

	private int recordsFiltered;

	private int recordsTotal;

	private int draw;

	public Page(List<T> data) {
		this.data = data;
	}

}
