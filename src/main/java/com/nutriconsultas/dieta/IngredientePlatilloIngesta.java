package com.nutriconsultas.dieta;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.nutriconsultas.alimentos.Alimento;
import com.nutriconsultas.model.AbstractFraccionable;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@EqualsAndHashCode(callSuper = false)
public class IngredientePlatilloIngesta extends AbstractFraccionable {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  private String description;

  @ManyToOne(optional = false)
  @JsonBackReference
  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  private PlatilloIngesta platillo;

  @ManyToOne
  private Alimento alimento;

  private String unidad;
}
