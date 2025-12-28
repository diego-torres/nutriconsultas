package com.nutriconsultas.dieta;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlatilloFormModel {
  private Long ingestaPlatillo;
  private Long platillo;
  private Integer porciones; 
}
