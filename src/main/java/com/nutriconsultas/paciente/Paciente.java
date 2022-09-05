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

  @Column(length = 100)
  private String email;
  @Column(length = 25)
  private String phone;
  @Column(length = 1)
  private String gender;

  @Column(length = 100)
  private String responsibleName;

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

}
