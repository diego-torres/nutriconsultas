package com.nutriconsultas.platillos;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class Platillo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String description;
    private String imageUrl;
    private String videoUrl;
    private String pdfUrl;
    private Integer energia = 0;
    private String ingestasSugeridas;

    @OneToMany(mappedBy = "platillo", 
    cascade = CascadeType.ALL, 
    orphanRemoval = true, 
    targetEntity = Ingrediente.class, 
    fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @JsonManagedReference
    private List<Ingrediente> ingredientes = new ArrayList<>();

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

    public String getImageUrl() {
        if (this.imageUrl == null || this.imageUrl.isBlank())
            return "/sbadmin/img/plato-vacio.jpg";
        return imageUrl;
    }

    public Integer getEnergia() {
        if (energia == null)
            return 0;
        return energia;
    }

    public Double getProteina() {
        if (proteina == null)
            return 0.0;
        return proteina;
    }

    public Double getLipidos() {
        if (lipidos == null)
            return 0.0;
        return lipidos;
    }

    public Double getHidratosDeCarbono() {
        if (hidratosDeCarbono == null)
            return 0.0;
        return hidratosDeCarbono;
    }

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
