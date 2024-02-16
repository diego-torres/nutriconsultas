package com.nutriconsultas.consulta;

import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

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

  @Column(precision = 5)
  @NotNull
  @Min(10)
  @Max(200)
  private Double peso;
  @Column(precision = 3)
  @NotNull
  @Max(3)
  @DecimalMin(value = "0.5")
  private Double estatura;
  @Column(precision = 3)
  private Double imc;

  private NivelPeso nivelPeso;
  private Integer sistolica, diastolica, pulso, indiceGlucemico;

  @Column(precision = 5)
  private Double spo2;

  @Column(precision = 5)
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

  @Override
  public String toString() {
    return "Consulta [diastolica=" + diastolica + ", estatura=" + estatura + ", fechaConsulta=" + fechaConsulta
        + ", id=" + id + ", imc=" + imc + ", indiceGlucemico=" + indiceGlucemico + ", nivelPeso=" + nivelPeso
        + ", notasInterconsulta=" + notasInterconsulta + ", paciente=" + paciente + ", peso=" + peso + ", pulso="
        + pulso + ", sistolica=" + sistolica + ", spo2=" + spo2 + ", temperatura=" + temperatura + "]";
  }

  

}
