package com.nutriconsultas.alimentos;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Entity
public class Alimento {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @NotNull
  @NotBlank
  private String nombreAlimento;
  @NotNull
  @NotBlank
  private String clasificacion;
  @Column(length = 50)
  private String cantSugerida;
  private String unidad;
  private Integer pesoBrutoRedondeado;
  private Integer pesoNeto;
  private Integer energia;
  @Column(precision = 5, scale = 1)
  private Double proteina;
  @Column(precision = 5, scale = 1)
  private Double lipidos;
  @Column(precision = 5, scale = 1)
  private Double hidratosDeCarbono;
  @Column(precision = 5, scale = 1)
  private Double fibra;
  @Column(precision = 5, scale = 1)
  private Double vitA;
  @Column(precision = 5, scale = 1)
  private Double acidoAscorbico;
  @Column(precision = 5, scale = 1)
  private Double hierroNoHem;
  @Column(precision = 5, scale = 1)
  private Double potasio;
  @Column(precision = 5, scale = 1)
  private Double indiceGlicemico;
  @Column(precision = 5, scale = 1)
  private Double cargaGlicemica;
  @Column(precision = 5, scale = 1)
  private Double acidoFolico;
  @Column(precision = 5, scale = 1)
  private Double calcio;
  @Column(precision = 5, scale = 1)
  private Double hierro;
  @Column(precision = 5, scale = 1)
  private Double sodio;
  @Column(precision = 5, scale = 1)
  private Double azucarPorEquivalente;
  @Column(precision = 5, scale = 1)
  private Double selenio;
  @Column(precision = 5, scale = 1)
  private Double fosforo;
  @Column(precision = 5, scale = 1)
  private Double colesterol;
  @Column(precision = 5, scale = 1)
  private Double agSaturados;
  @Column(precision = 5, scale = 1)
  private Double agMonoinsaturados;
  @Column(precision = 5, scale = 1)
  private Double agPoliinsaturados;
  @Column(precision = 5, scale = 1)
  private Double etanol;

  private Boolean aptoParaRenales = true;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getNombreAlimento() {
    return nombreAlimento;
  }

  public void setNombreAlimento(String nombreAlimento) {
    this.nombreAlimento = nombreAlimento;
  }

  public String getClasificacion() {
    return clasificacion;
  }

  public void setClasificacion(String clasificacion) {
    this.clasificacion = clasificacion;
  }

  public String getCantSugerida() {
    return cantSugerida;
  }

  public void setCantSugerida(String cantSugerida) {
    this.cantSugerida = cantSugerida;
  }

  public String getUnidad() {
    return unidad;
  }

  public void setUnidad(String unidad) {
    this.unidad = unidad;
  }

  public Integer getPesoBrutoRedondeado() {
    return pesoBrutoRedondeado;
  }

  public void setPesoBrutoRedondeado(Integer pesoBrutoRedondeado) {
    this.pesoBrutoRedondeado = pesoBrutoRedondeado;
  }

  public Integer getPesoNeto() {
    return pesoNeto;
  }

  public void setPesoNeto(Integer pesoNeto) {
    this.pesoNeto = pesoNeto;
  }

  public Integer getEnergia() {
    return energia;
  }

  public void setEnergia(Integer energia) {
    this.energia = energia;
  }

  public Double getProteina() {
    return proteina;
  }

  public void setProteina(Double proteina) {
    this.proteina = proteina;
  }

  public Double getLipidos() {
    return lipidos;
  }

  public void setLipidos(Double lipidos) {
    this.lipidos = lipidos;
  }

  public Double getHidratosDeCarbono() {
    return hidratosDeCarbono;
  }

  public void setHidratosDeCarbono(Double hidratosDeCarbono) {
    this.hidratosDeCarbono = hidratosDeCarbono;
  }

  public Double getFibra() {
    return fibra;
  }

  public void setFibra(Double fibra) {
    this.fibra = fibra;
  }

  public Double getVitA() {
    return vitA;
  }

  public void setVitA(Double vitA) {
    this.vitA = vitA;
  }

  public Double getAcidoAscorbico() {
    return acidoAscorbico;
  }

  public void setAcidoAscorbico(Double acidoAscorbico) {
    this.acidoAscorbico = acidoAscorbico;
  }

  public Double getHierroNoHem() {
    return hierroNoHem;
  }

  public void setHierroNoHem(Double hierroNoHem) {
    this.hierroNoHem = hierroNoHem;
  }

  public Double getPotasio() {
    return potasio;
  }

  public void setPotasio(Double potasio) {
    this.potasio = potasio;
  }

  public Double getIndiceGlicemico() {
    return indiceGlicemico;
  }

  public void setIndiceGlicemico(Double indiceGlicemico) {
    this.indiceGlicemico = indiceGlicemico;
  }

  public Double getCargaGlicemica() {
    return cargaGlicemica;
  }

  public void setCargaGlicemica(Double cargaGlicemica) {
    this.cargaGlicemica = cargaGlicemica;
  }

  public Double getAcidoFolico() {
    return acidoFolico;
  }

  public void setAcidoFolico(Double acidoFolico) {
    this.acidoFolico = acidoFolico;
  }

  public Double getCalcio() {
    return calcio;
  }

  public void setCalcio(Double calcio) {
    this.calcio = calcio;
  }

  public Double getHierro() {
    return hierro;
  }

  public void setHierro(Double hierro) {
    this.hierro = hierro;
  }

  public Double getSodio() {
    return sodio;
  }

  public void setSodio(Double sodio) {
    this.sodio = sodio;
  }

  public Double getAzucarPorEquivalente() {
    return azucarPorEquivalente;
  }

  public void setAzucarPorEquivalente(Double azucarPorEquivalente) {
    this.azucarPorEquivalente = azucarPorEquivalente;
  }

  public Double getSelenio() {
    return selenio;
  }

  public void setSelenio(Double selenio) {
    this.selenio = selenio;
  }

  public Double getFosforo() {
    return fosforo;
  }

  public void setFosforo(Double fosforo) {
    this.fosforo = fosforo;
  }

  public Double getColesterol() {
    return colesterol;
  }

  public void setColesterol(Double colesterol) {
    this.colesterol = colesterol;
  }

  public Double getAgSaturados() {
    return agSaturados;
  }

  public void setAgSaturados(Double agSaturados) {
    this.agSaturados = agSaturados;
  }

  public Double getAgMonoinsaturados() {
    return agMonoinsaturados;
  }

  public void setAgMonoinsaturados(Double agMonoinsaturados) {
    this.agMonoinsaturados = agMonoinsaturados;
  }

  public Double getAgPoliinsaturados() {
    return agPoliinsaturados;
  }

  public void setAgPoliinsaturados(Double agPoliinsaturados) {
    this.agPoliinsaturados = agPoliinsaturados;
  }

  public Double getEtanol() {
    return etanol;
  }

  public void setEtanol(Double etanol) {
    this.etanol = etanol;
  }

  public Boolean getAptoParaRenales() {
    return aptoParaRenales;
  }

  public void setAptoParaRenales(Boolean aptoParaRenales) {
    this.aptoParaRenales = aptoParaRenales;
  }

}
