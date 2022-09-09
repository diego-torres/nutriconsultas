package com.nutriconsultas.paciente;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.NotBlank;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;

@Entity
public class Paciente {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @NotBlank(message = "El nombre es requerido")
  @Column(nullable = false, length = 100, unique = true)
  private String name;

  @DateTimeFormat(pattern = "yyyy-MM-dd")
  @Temporal(TemporalType.DATE)
  private Date dob;

  @DateTimeFormat(iso = ISO.DATE_TIME)
  @Temporal(TemporalType.TIMESTAMP)
  private Date registro = new Date();

  @Column(length = 100)
  private String email;
  @Column(length = 25)
  private String phone;
  @Column(length = 1)
  private String gender;

  @Column(length = 100)
  private String responsibleName;

  private String parentesco;

  @Column(precision = 5, scale = 2)
  private Double peso;
  @Column(precision = 3, scale = 2)
  private Double estatura;
  @Column(precision = 3, scale = 1)
  private Double imc;

  private NivelPeso nivelPeso;

  // ANTECEDENTES
  @Column(columnDefinition = "TEXT")
  private String antecedentesPrenatales;
  @Column(columnDefinition = "TEXT")
  private String antecedentesNatales;
  @Column(columnDefinition = "TEXT")
  private String antecedentesPatologicosPersonales;
  @Column(columnDefinition = "TEXT")
  private String antecedentesPatologicosFamiliares;
  @Column(columnDefinition = "TEXT")
  private String complicaciones;
  @Column(length = 4)
  private String tipoSanguineo;

  // NUTRICION Y DESARROLLO
  @Column(columnDefinition = "TEXT")
  private String historialAlimenticio;
  @Column(columnDefinition = "TEXT")
  private String desarrolloPsicomotor;
  @Column(columnDefinition = "TEXT")
  private String alergias;

  // BANDERAS DE PATOLOGIAS COMUNES
  private Boolean hipertension = false, diabetes = false, hipotiroidismo = false, obesidad = false, anemia = false,
      bulimia = false, anorexia = false, enfermedadesHepaticas = false;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Date getDob() {
    return dob;
  }

  public void setDob(Date dob) {
    this.dob = dob;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getPhone() {
    return phone;
  }

  public void setPhone(String phone) {
    this.phone = phone;
  }

  public String getGender() {
    return gender;
  }

  public void setGender(String gender) {
    this.gender = gender;
  }

  public String getResponsibleName() {
    return responsibleName;
  }

  public void setResponsibleName(String responsibleName) {
    this.responsibleName = responsibleName;
  }

  public Date getRegistro() {
    return registro;
  }

  public void setRegistro(Date registro) {
    this.registro = registro;
  }

  public String getParentesco() {
    return parentesco;
  }

  public void setParentesco(String parentesco) {
    this.parentesco = parentesco;
  }

  public String getAntecedentesPrenatales() {
    return antecedentesPrenatales;
  }

  public void setAntecedentesPrenatales(String antecedentesPrenatales) {
    this.antecedentesPrenatales = antecedentesPrenatales;
  }

  public String getAntecedentesNatales() {
    return antecedentesNatales;
  }

  public void setAntecedentesNatales(String antecedentesNatales) {
    this.antecedentesNatales = antecedentesNatales;
  }

  public String getAntecedentesPatologicosPersonales() {
    return antecedentesPatologicosPersonales;
  }

  public void setAntecedentesPatologicosPersonales(String antecedentesPatologicosPersonales) {
    this.antecedentesPatologicosPersonales = antecedentesPatologicosPersonales;
  }

  public String getAntecedentesPatologicosFamiliares() {
    return antecedentesPatologicosFamiliares;
  }

  public void setAntecedentesPatologicosFamiliares(String antecedentesPatologicosFamiliares) {
    this.antecedentesPatologicosFamiliares = antecedentesPatologicosFamiliares;
  }

  public String getComplicaciones() {
    return complicaciones;
  }

  public void setComplicaciones(String complicaciones) {
    this.complicaciones = complicaciones;
  }

  public String getTipoSanguineo() {
    return tipoSanguineo;
  }

  public void setTipoSanguineo(String tipoSanguineo) {
    this.tipoSanguineo = tipoSanguineo;
  }

  public String getHistorialAlimenticio() {
    return historialAlimenticio;
  }

  public void setHistorialAlimenticio(String historialAlimenticio) {
    this.historialAlimenticio = historialAlimenticio;
  }

  public String getDesarrolloPsicomotor() {
    return desarrolloPsicomotor;
  }

  public void setDesarrolloPsicomotor(String desarrolloPsicomotor) {
    this.desarrolloPsicomotor = desarrolloPsicomotor;
  }

  public String getAlergias() {
    return alergias;
  }

  public void setAlergias(String alergias) {
    this.alergias = alergias;
  }

  public Double getPeso() {
    if (peso == null)
      return 0.0d;
    return peso;
  }

  public void setPeso(Double peso) {
    this.peso = peso;
  }

  public Double getEstatura() {
    if (estatura == null)
      return 0.0d;
    return estatura;
  }

  public void setEstatura(Double estatura) {
    this.estatura = estatura;
  }

  public Double getImc() {
    if (imc == null)
      return 0.0d;
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

  public Boolean getHipertension() {
    return hipertension;
  }

  public void setHipertension(Boolean hipertension) {
    this.hipertension = hipertension;
  }

  public Boolean getDiabetes() {
    return diabetes;
  }

  public void setDiabetes(Boolean diabetes) {
    this.diabetes = diabetes;
  }

  public Boolean getHipotiroidismo() {
    return hipotiroidismo;
  }

  public void setHipotiroidismo(Boolean hipotiroidismo) {
    this.hipotiroidismo = hipotiroidismo;
  }

  public Boolean getObesidad() {
    return obesidad;
  }

  public void setObesidad(Boolean obesidad) {
    this.obesidad = obesidad;
  }

  public Boolean getAnemia() {
    return anemia;
  }

  public void setAnemia(Boolean anemia) {
    this.anemia = anemia;
  }

  public Boolean getBulimia() {
    return bulimia;
  }

  public void setBulimia(Boolean bulimia) {
    this.bulimia = bulimia;
  }

  public Boolean getAnorexia() {
    return anorexia;
  }

  public void setAnorexia(Boolean anorexia) {
    this.anorexia = anorexia;
  }

  public Boolean getEnfermedadesHepaticas() {
    return enfermedadesHepaticas;
  }

  public void setEnfermedadesHepaticas(Boolean enfermedadesHepaticas) {
    this.enfermedadesHepaticas = enfermedadesHepaticas;
  }

  @Override
  public String toString() {
    return "Paciente [alergias=" + alergias + ", anemia=" + anemia + ", anorexia=" + anorexia + ", antecedentesNatales="
        + antecedentesNatales + ", antecedentesPatologicosFamiliares=" + antecedentesPatologicosFamiliares
        + ", antecedentesPatologicosPersonales=" + antecedentesPatologicosPersonales + ", antecedentesPrenatales="
        + antecedentesPrenatales + ", bulimia=" + bulimia + ", complicaciones=" + complicaciones
        + ", desarrolloPsicomotor=" + desarrolloPsicomotor + ", diabetes=" + diabetes + ", dob=" + dob + ", email="
        + email + ", enfermedadesHepaticas=" + enfermedadesHepaticas + ", estatura=" + estatura + ", gender=" + gender
        + ", hipertension=" + hipertension + ", hipotiroidismo=" + hipotiroidismo + ", historialAlimenticio="
        + historialAlimenticio + ", id=" + id + ", imc=" + imc + ", name=" + name + ", nivelPeso=" + nivelPeso
        + ", obesidad=" + obesidad + ", parentesco=" + parentesco + ", peso=" + peso + ", phone=" + phone
        + ", registro=" + registro + ", responsibleName=" + responsibleName + ", tipoSanguineo=" + tipoSanguineo + "]";
  }

}
