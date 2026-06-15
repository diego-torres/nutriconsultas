package com.nutriconsultas.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WebSecurityConfigRegressionTest {

	@Autowired
	private MockMvc mockMvc;

	@BeforeEach
	void clearSecurityContext() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void adminPath_withoutSession_isNotServedByMobileJwtChain() throws Exception {
		mockMvc.perform(get("/admin/pacientes")).andExpect(status().is3xxRedirection());
	}

	@Test
	void openApiDocs_withoutSession_requiresNutritionistLogin() throws Exception {
		mockMvc.perform(get("/v3/api-docs/mobile")).andExpect(status().is3xxRedirection());
	}

}
