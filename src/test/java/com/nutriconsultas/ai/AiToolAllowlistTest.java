package com.nutriconsultas.ai;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AiToolAllowlistTest {

	private AiToolAllowlist allowlist;

	@BeforeEach
	void setUp() {
		allowlist = new AiToolAllowlist(new AiOpenAiToolCatalog());
	}

	@Test
	void allowsRegisteredTools() {
		assertThat(allowlist.isAllowed(SearchFoodCatalogToolService.TOOL_NAME)).isTrue();
		assertThat(allowlist.isAllowed(CreateDietPlanDraftToolService.TOOL_NAME)).isTrue();
		assertThat(allowlist.allowedToolNames()).hasSize(9);
	}

	@Test
	void rejectsUnknownTool() {
		assertThat(allowlist.isAllowed("delete_all_patients")).isFalse();
		assertThat(allowlist.isAllowed("")).isFalse();
		assertThat(allowlist.isAllowed(null)).isFalse();
	}

}
