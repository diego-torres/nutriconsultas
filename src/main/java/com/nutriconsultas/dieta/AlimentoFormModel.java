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

	private Integer porciones;

	private String tipoPorcion;

}
