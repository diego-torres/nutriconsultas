package com.nutriconsultas.ai;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AiDraftCreatedEntityLinksTest {

	@Test
	void pathForPlatilloAndDieta() {
		assertThat(AiDraftCreatedEntityLinks.path(AiDraftCreatedEntityType.PLATILLO, 9L))
			.isEqualTo("/admin/platillos/9");
		assertThat(AiDraftCreatedEntityLinks.path(AiDraftCreatedEntityType.DIETA, 12L)).isEqualTo("/admin/dietas/12");
	}

}
