package com.nutriconsultas.alimentos;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
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
  @Column(precision = 5)
  private Double cantSugerida;
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
    
    return intPart>0?intPart + " ":"" + h1.intValue() + "/" + k1.intValue();
  }
}
