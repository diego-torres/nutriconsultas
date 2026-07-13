package com.nutriconsultas.dieta;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ReplaceIngestaSelectionRequest {

	private List<Long> alimentoIngestaIds = new ArrayList<>();

	private List<Long> platilloIngestaIds = new ArrayList<>();

}
