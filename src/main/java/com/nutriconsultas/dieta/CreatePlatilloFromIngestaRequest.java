package com.nutriconsultas.dieta;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class CreatePlatilloFromIngestaRequest {

	private String nombre;

	private List<Long> alimentoIngestaIds = new ArrayList<>();

	private List<Long> platilloIngestaIds = new ArrayList<>();

}
