package com.nutriconsultas.model;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@MappedSuperclass
@EqualsAndHashCode(callSuper = false)
public class AbstractNutrible extends AbstractMacroNutrible {
  private Integer pesoBrutoRedondeado;
  private Integer pesoNeto;
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

  public Double getFibra() {
    if (fibra == null)
      return 0.0;
    return fibra;
  }

  public Double getVitA() {
    if (vitA == null)
      return 0.0;
    return vitA;
  }

  public Double getAcidoAscorbico() {
    if (acidoAscorbico == null)
      return 0.0;
    return acidoAscorbico;
  }

  public Double getHierroNoHem() {
    if (hierroNoHem == null)
      return 0.0;
    return hierroNoHem;
  }

  public Double getPotasio() {
    if (potasio == null)
      return 0.0;
    return potasio;
  }

  public Double getIndiceGlicemico() {
    if (indiceGlicemico == null)
      return 0.0;
    return indiceGlicemico;
  }

  public Double getCargaGlicemica() {
    if (cargaGlicemica == null)
      return 0.0;
    return cargaGlicemica;
  }

  public Double getAcidoFolico() {
    if (acidoFolico == null)
      return 0.0;
    return acidoFolico;
  }

  public Double getCalcio() {
    if (calcio == null)
      return 0.0;
    return calcio;
  }

  public Double getHierro() {
    if (hierro == null)
      return 0.0;
    return hierro;
  }

  public Double getSodio() {
    if (sodio == null)
      return 0.0;
    return sodio;
  }

  public Double getAzucarPorEquivalente() {
    if (azucarPorEquivalente == null)
      return 0.0;
    return azucarPorEquivalente;
  }

}
