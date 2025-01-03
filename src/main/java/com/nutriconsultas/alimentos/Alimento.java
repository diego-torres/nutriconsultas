package com.nutriconsultas.alimentos;

import com.nutriconsultas.model.AbstractFraccionable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper=false)
public class Alimento extends AbstractFraccionable {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @NotNull
  @NotBlank
  private String nombreAlimento;
  @NotNull
  @NotBlank
  private String clasificacion;
  private String unidad;
  private Integer pesoBrutoRedondeado;
  private Integer pesoNeto;
  private Integer energia;
  @Column(precision = 5)
  private Double proteina;
  @Column(precision = 5)
  private Double lipidos;
  @Column(precision = 5)
  private Double hidratosDeCarbono;
  @Column(precision = 5)
  private Double fibra;
  @Column(precision = 5)
  private Double vitA;
  @Column(precision = 5)
  private Double acidoAscorbico;
  @Column(precision = 5)
  private Double hierroNoHem;
  @Column(precision = 5)
  private Double potasio;
  @Column(precision = 5)
  private Double indiceGlicemico;
  @Column(precision = 5)
  private Double cargaGlicemica;
  @Column(precision = 5)
  private Double acidoFolico;
  @Column(precision = 5)
  private Double calcio;
  @Column(precision = 5)
  private Double hierro;
  @Column(precision = 5)
  private Double sodio;
  @Column(precision = 5)
  private Double azucarPorEquivalente;
  @Column(precision = 5)
  private Double selenio;
  @Column(precision = 5)
  private Double fosforo;
  @Column(precision = 5)
  private Double colesterol;
  @Column(precision = 5)
  private Double agSaturados;
  @Column(precision = 5)
  private Double agMonoinsaturados;
  @Column(precision = 5)
  private Double agPoliinsaturados;
  @Column(precision = 5)
  private Double etanol;

  
}
