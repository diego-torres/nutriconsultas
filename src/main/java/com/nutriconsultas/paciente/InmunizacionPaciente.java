package com.nutriconsultas.paciente;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

@Entity
public class InmunizacionPaciente {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false)
  @JoinColumn(name = "paciente_id")
  private Paciente paciente;
  
  private String inmunizacion;

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

  public String getInmunizacion() {
    return inmunizacion;
  }

  public void setInmunizacion(String inmunizacion) {
    this.inmunizacion = inmunizacion;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((inmunizacion == null) ? 0 : inmunizacion.hashCode());
    result = prime * result + ((paciente == null) ? 0 : paciente.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    InmunizacionPaciente other = (InmunizacionPaciente) obj;
    if (inmunizacion == null) {
      if (other.inmunizacion != null)
        return false;
    } else if (!inmunizacion.equals(other.inmunizacion))
      return false;
    if (paciente == null) {
      if (other.paciente != null)
        return false;
    } else if (!paciente.equals(other.paciente))
      return false;
    return true;
  }

  @Override
  public String toString() {
    return "InmunizacionPaciente [id=" + id + ", inmunizacion=" + inmunizacion + ", paciente=" + paciente + "]";
  }

  
}
