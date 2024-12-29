package com.nutriconsultas.platillos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IngredienteFormModel {
    private Long alimentoId;
    private String cantidad;
    private Integer peso;
}
