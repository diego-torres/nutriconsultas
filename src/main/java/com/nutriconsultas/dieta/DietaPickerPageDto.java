package com.nutriconsultas.dieta;

import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DietaPickerPageDto {

	private List<DietaPickerItemDto> items = new ArrayList<>();

	private int page;

	private int size;

	private long totalElements;

	private boolean hasNext;

}
