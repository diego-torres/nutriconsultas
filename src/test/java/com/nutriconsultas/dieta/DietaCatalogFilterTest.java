package com.nutriconsultas.dieta;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DietaCatalogFilterTest {

	@Test
	void fromRequestValueNullOrBlankReturnsTodas() {
		assertThat(DietaCatalogFilter.fromRequestValue(null)).isEqualTo(DietaCatalogFilter.TODAS);
		assertThat(DietaCatalogFilter.fromRequestValue("")).isEqualTo(DietaCatalogFilter.TODAS);
		assertThat(DietaCatalogFilter.fromRequestValue("   ")).isEqualTo(DietaCatalogFilter.TODAS);
	}

	@Test
	void fromRequestValueParsesSistemaAndPropias() {
		assertThat(DietaCatalogFilter.fromRequestValue("sistema")).isEqualTo(DietaCatalogFilter.SISTEMA);
		assertThat(DietaCatalogFilter.fromRequestValue("SISTEMA")).isEqualTo(DietaCatalogFilter.SISTEMA);
		assertThat(DietaCatalogFilter.fromRequestValue("propias")).isEqualTo(DietaCatalogFilter.PROPIAS);
		assertThat(DietaCatalogFilter.fromRequestValue("mis")).isEqualTo(DietaCatalogFilter.PROPIAS);
	}

	@Test
	void fromRequestValueUnknownReturnsTodas() {
		assertThat(DietaCatalogFilter.fromRequestValue("otro")).isEqualTo(DietaCatalogFilter.TODAS);
	}

}
