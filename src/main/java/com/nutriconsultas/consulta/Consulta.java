package com.nutriconsultas.consulta;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.springframework.format.annotation.DateTimeFormat;

import com.nutriconsultas.paciente.NivelPeso;
import com.nutriconsultas.paciente.Paciente;

@Entity
public class Consulta {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false)
  @JoinColumn(name = "paciente_id")
  private Paciente paciente;

  @DateTimeFormat(pattern = "yyyy-MM-dd")
  @Temporal(TemporalType.DATE)
  private Date fechaConsulta;

  @Column(precision = 5, scale = 2)
  private Double peso;
  @Column(precision = 3, scale = 2)
  private Double estatura;
  @Column(precision = 3, scale = 1)
  private Double imc;

  private NivelPeso nivelPeso;
  private Integer sistolica, diastolica, pulso, indiceGlucemico;

  @Column(precision = 5, scale = 2)
  private Double spo2;

  @Column(precision = 5, scale = 2)
  private Double temperatura;

  @Column(columnDefinition = "TEXT")
  private String notasInterconsulta;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Paciente getPaciente() {
    return paciente;
  }

  public void setPaciente(Paciente paciente) {
    this.paciente = paciente;
  }

  public Date getFechaConsulta() {
    return fechaConsulta;
  }

  public void setFechaConsulta(Date fechaConsulta) {
    this.fechaConsulta = fechaConsulta;
  }

  public Double getPeso() {
    return peso;
  }

  public void setPeso(Double peso) {
    this.peso = peso;
  }

  public Double getEstatura() {
    return estatura;
  }

  public void setEstatura(Double estatura) {
    this.estatura = estatura;
  }

  public Double getImc() {
    return imc;
  }

  public void setImc(Double imc) {
    this.imc = imc;
  }

  public NivelPeso getNivelPeso() {
    return nivelPeso;
  }

  public void setNivelPeso(NivelPeso nivelPeso) {
    this.nivelPeso = nivelPeso;
  }

  public Integer getSistolica() {
    return sistolica;
  }

  public void setSistolica(Integer sistolica) {
    this.sistolica = sistolica;
  }

  public Integer getDiastolica() {
    return diastolica;
  }

  public void setDiastolica(Integer diastolica) {
    this.diastolica = diastolica;
  }

  public Integer getPulso() {
    return pulso;
  }

  public void setPulso(Integer pulso) {
    this.pulso = pulso;
  }

  public Double getSpo2() {
    return spo2;
  }

  public void setSpo2(Double spo2) {
    this.spo2 = spo2;
  }

  public Double getTemperatura() {
    return temperatura;
  }

  public void setTemperatura(Double temperatura) {
    this.temperatura = temperatura;
  }

  public String getNotasInterconsulta() {
    return notasInterconsulta;
  }

  public void setNotasInterconsulta(String notasInterconsulta) {
    this.notasInterconsulta = notasInterconsulta;
  }

  public Integer getIndiceGlucemico() {
    return indiceGlucemico;
  }

  public void setIndiceGlucemico(Integer indiceGlucemico) {
    this.indiceGlucemico = indiceGlucemico;
  }

  
}
