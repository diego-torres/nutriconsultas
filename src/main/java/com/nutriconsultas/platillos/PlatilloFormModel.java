package com.nutriconsultas.platillos;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlatilloFormModel extends Platillo {
   private String ingredientesJsonString;
   public Platillo toPlatillo() {
       Platillo platillo = new Platillo();
       platillo.setId(this.getId());
       platillo.setName(this.getName());
       platillo.setDescription(this.getDescription());
       platillo.setImageUrl(this.getImageUrl());
       platillo.setVideoUrl(this.getVideoUrl());
       platillo.setPdfUrl(this.getPdfUrl());
       platillo.setEnergia(this.getEnergia());
       platillo.setIngestasSugeridas(this.getIngestasSugeridas());
       platillo.setProteina(this.getProteina());
       platillo.setLipidos(this.getLipidos());
       platillo.setHidratosDeCarbono(this.getHidratosDeCarbono());
       platillo.setFibra(this.getFibra());
       platillo.setVitA(this.getVitA());
       platillo.setAcidoAscorbico(this.getAcidoAscorbico());
       platillo.setHierroNoHem(this.getHierroNoHem());
       platillo.setPotasio(this.getPotasio());
       platillo.setIndiceGlicemico(this.getIndiceGlicemico());
       platillo.setCargaGlicemica(this.getCargaGlicemica());
       platillo.setAcidoFolico(this.getAcidoFolico());
       platillo.setCalcio(this.getCalcio());
       return platillo;
   }
}
