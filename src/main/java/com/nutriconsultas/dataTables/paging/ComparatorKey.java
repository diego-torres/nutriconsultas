package com.nutriconsultas.dataTables.paging;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ComparatorKey {

	@SuppressWarnings("checkstyle:VisibilityModifier")
	private String name;

	@SuppressWarnings("checkstyle:VisibilityModifier")
	private Direction dir;

}
