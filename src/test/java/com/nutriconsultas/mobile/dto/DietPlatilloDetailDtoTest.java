package com.nutriconsultas.mobile.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.nutriconsultas.alimentos.Alimento;
import com.nutriconsultas.dieta.IngredientePlatilloIngesta;
import com.nutriconsultas.dieta.PlatilloIngesta;

class DietPlatilloDetailDtoTest {

	@Test
	void fromEntity_mapsIngredientsDescriptionAndNutritionFacts() {
		final PlatilloIngesta platillo = new PlatilloIngesta();
		platillo.setId(42L);
		platillo.setName("Avena con fruta");
		platillo.setPortions(2);
		platillo.setRecommendations("Mezclar y servir tibia");
		platillo.setVideoUrl("https://video.example/avena");
		platillo.setPdfUrl("/uploads/recetas/avena.pdf");
		platillo.setEnergia(320);
		platillo.setProteina(12.5);
		platillo.setHidratosDeCarbono(48.0);
		platillo.setLipidos(8.0);
		platillo.setFibra(6.0);
		platillo.setSodio(120.0);

		final Alimento alimento = new Alimento();
		alimento.setNombreAlimento("Avena");
		final IngredientePlatilloIngesta ingrediente = new IngredientePlatilloIngesta();
		ingrediente.setId(1L);
		ingrediente.setAlimento(alimento);
		ingrediente.setCantSugerida(0.5);
		ingrediente.setUnidad("taza");
		ingrediente.setPlatillo(platillo);
		platillo.setIngredientes(List.of(ingrediente));

		final DietPlatilloDetailDto dto = DietPlatilloDetailDto.fromEntity(platillo);

		assertThat(dto.id()).isEqualTo(42L);
		assertThat(dto.nombre()).isEqualTo("Avena con fruta");
		assertThat(dto.porciones()).isEqualTo(2);
		assertThat(dto.description()).isEqualTo("Mezclar y servir tibia");
		assertThat(dto.videoUrl()).isEqualTo("https://video.example/avena");
		assertThat(dto.pdfUrl()).isEqualTo("/uploads/recetas/avena.pdf");
		assertThat(dto.imageUrl()).isEqualTo("/sbadmin/img/plato-vacio.jpg");
		assertThat(dto.ingredientes()).hasSize(1);
		assertThat(dto.ingredientes().get(0).nombre()).isEqualTo("Avena");
		assertThat(dto.ingredientes().get(0).cantidad()).isEqualTo("1/2");
		assertThat(dto.ingredientes().get(0).unidad()).isEqualTo("taza");
		assertThat(dto.nutritionFacts().kcal()).isEqualTo(320);
		assertThat(dto.nutritionFacts().proteina()).isEqualTo(12.5);
		assertThat(dto.nutritionFacts().carbohidratos()).isEqualTo(48.0);
		assertThat(dto.nutritionFacts().grasas()).isEqualTo(8.0);
		assertThat(dto.nutritionFacts().fibra()).isEqualTo(6.0);
		assertThat(dto.nutritionFacts().sodio()).isEqualTo(120.0);
	}

}
