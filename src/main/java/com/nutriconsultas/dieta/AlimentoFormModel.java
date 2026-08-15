package com.nutriconsultas.dieta;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AlimentoFormModel {

	private Long ingestaAlimento;

	private Long alimento;

	private Double porciones;

	private String cantidad;

	private String tipoPorcion;

}
