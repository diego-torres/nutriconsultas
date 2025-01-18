package com.nutriconsultas.dieta;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.nutriconsultas.model.AbstractNutrible;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
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
public class PlatilloIngesta extends AbstractNutrible {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  private String name;
  private Integer portions = 1;
  private String recommendations;
  private String imageUrl;
  private String videoUrl;
  private String pdfUrl;

  @OneToMany(mappedBy = "platillo", //
      cascade = CascadeType.ALL, //
      orphanRemoval = true, //
      targetEntity = IngredientePlatilloIngesta.class, //
      fetch = FetchType.LAZY)
  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  @JsonManagedReference
  private List<IngredientePlatilloIngesta> ingredientes = new ArrayList<>();

  @ManyToOne
  @JoinColumn(name = "ingesta_id")
  private Ingesta ingesta;

  public String getImageUrl() {
    if (this.imageUrl == null || this.imageUrl.isBlank())
      return "/sbadmin/img/plato-vacio.jpg";
    return imageUrl;
  }
}
