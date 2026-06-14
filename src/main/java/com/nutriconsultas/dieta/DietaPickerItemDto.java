package com.nutriconsultas.dieta;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DietaPickerItemDto {

	private Long id;

	private String nombre;

	private Integer energiaKcal;

}
