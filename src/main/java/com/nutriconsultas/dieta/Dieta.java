package com.nutriconsultas.dieta;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Dieta {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  private String nombre;
  private Integer kcal;

  @Column(precision = 5)
  private Double proteina;
  @Column(precision = 5)
  private Double lipidos;
  @Column(precision = 5)
  private Double hidratosDeCarbono;

  @OneToMany(mappedBy = "dieta", 
    cascade = jakarta.persistence.CascadeType.ALL, 
    orphanRemoval = true, 
    targetEntity = Ingesta.class, 
    fetch = jakarta.persistence.FetchType.LAZY)
  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  @JsonManagedReference
  private List<Ingesta> ingestas = new ArrayList<>();
}
