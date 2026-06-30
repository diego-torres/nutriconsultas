package com.nutriconsultas.mobile.dto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.nutriconsultas.dieta.AlimentoIngesta;
import com.nutriconsultas.dieta.PlatilloIngesta;

class DietPlatilloDtoTest {

	@Test
	void fromEntity_mapsIdAndMacroFieldsFromPlatilloIngesta() {
		final PlatilloIngesta platillo = new PlatilloIngesta();
		platillo.setId(42L);
		platillo.setName("Avena con fruta");
		platillo.setPortions(2);
		platillo.setEnergia(320);
		platillo.setProteina(12.5);
		platillo.setHidratosDeCarbono(48.0);
		platillo.setLipidos(8.0);
		platillo.setRecommendations("Servir tibia");
		platillo.setImageUrl("/uploads/platillo.jpg");

		final DietPlatilloDto dto = DietPlatilloDto.fromEntity(platillo);

		assertThat(dto.id()).isEqualTo(42L);
		assertThat(dto.nombre()).isEqualTo("Avena con fruta");
		assertThat(dto.porciones()).isEqualTo(2);
		assertThat(dto.kcal()).isEqualTo(320);
		assertThat(dto.proteina()).isEqualTo(12.5);
		assertThat(dto.carbohidratos()).isEqualTo(48.0);
		assertThat(dto.grasas()).isEqualTo(8.0);
		assertThat(dto.recommendations()).isEqualTo("Servir tibia");
		assertThat(dto.imageUrl()).isEqualTo("/uploads/platillo.jpg");
	}

	@Test
	void fromEntity_returnsNullForNullPlatillo() {
		assertThat(DietPlatilloDto.fromEntity(null)).isNull();
	}

	@Test
	void fromEntity_alimentoMapsOptionalMacrosWhenPresent() {
		final AlimentoIngesta alimento = new AlimentoIngesta();
		alimento.setName("Manzana");
		alimento.setPortions(1);
		alimento.setEnergia(52);
		alimento.setUnidad("pieza");
		alimento.setProteina(0.3);
		alimento.setHidratosDeCarbono(14.0);
		alimento.setLipidos(0.2);

		final DietAlimentoDto dto = DietAlimentoDto.fromEntity(alimento);

		assertThat(dto.nombre()).isEqualTo("Manzana");
		assertThat(dto.unidad()).isEqualTo("pieza");
		assertThat(dto.proteina()).isEqualTo(0.3);
		assertThat(dto.carbohidratos()).isEqualTo(14.0);
		assertThat(dto.grasas()).isEqualTo(0.2);
	}

}
