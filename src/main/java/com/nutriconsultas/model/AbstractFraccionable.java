package com.nutriconsultas.model;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@MappedSuperclass
@EqualsAndHashCode(callSuper=false)
public abstract class AbstractFraccionable extends AbstractNutrible {
  @Column(precision = 5)
  protected Double cantSugerida;

  public String getFractionalCantSugerida() {
    if (cantSugerida == null) {
      return "";
    }

    Integer intPart = cantSugerida.intValue();
    // convert the fractional part to a fraction
    Double fractionalPart = cantSugerida - intPart;
    Double tolerance = 1.0E-6;
    Double h1 = 1d;
    Double h2 = 0d;
    Double k1 = 0d;
    Double k2 = 1d;
    Double b = fractionalPart;
    do {
      Double a = Math.floor(b);
      Double aux = h1;
      h1 = a * h1 + h2;
      h2 = aux;
      aux = k1;
      k1 = a * k1 + k2;
      k2 = aux;
      b = 1 / (b - a);
    } while (Math.abs(fractionalPart - h1 / k1) > fractionalPart * tolerance);

    return intPart > 0 ? intPart + " " : "" + h1.intValue() + "/" + k1.intValue();
  }
}
