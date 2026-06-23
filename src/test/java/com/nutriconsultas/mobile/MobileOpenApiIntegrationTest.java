package com.nutriconsultas.mobile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser
class MobileOpenApiIntegrationTest {

	private static final List<String> REQUIRED_PATHS = List.of("/rest/mobile/patient/visits",
			"/rest/mobile/patient/visits/{visitId}", "/rest/mobile/patient/diet-plans",
			"/rest/mobile/patient/diet-plans/{assignmentId}", "/rest/mobile/patient/diet-plans/{assignmentId}/pdf",
			"/rest/mobile/patient/messages", "/rest/mobile/patient/progress",
			"/rest/mobile/patient/progress/measurements", "/rest/mobile/invitations",
			"/rest/mobile/invitations/{token}/preview", "/rest/mobile/invitations/{token}/redeem");

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Test
	void mobileOpenApiGroupDocumentsPatientEndpoints() throws Exception {
		final MvcResult result = mockMvc.perform(get("/v3/api-docs/mobile"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.openapi").value("3.1.0"))
			.andExpect(jsonPath("$.info.title").value("Minutriporcion Patient Mobile API"))
			.andExpect(jsonPath("$.components.securitySchemes['bearer-jwt']").exists())
			.andReturn();

		final JsonNode paths = objectMapper.readTree(result.getResponse().getContentAsString()).path("paths");
		for (final String path : REQUIRED_PATHS) {
			assertThat(paths.has(path)).as("OpenAPI path %s", path).isTrue();
		}
		assertThat(paths.path("/rest/mobile/patient/messages").has("get")).isTrue();
		assertThat(paths.path("/rest/mobile/patient/messages").has("post")).isTrue();
	}

	@Test
	void mobileOpenApiYamlIsAvailableForExport() throws Exception {
		mockMvc.perform(get("/v3/api-docs.yaml/mobile"))
			.andExpect(status().isOk())
			.andExpect(result -> assertThat(result.getResponse().getContentType()).contains("openapi"));

		if (Boolean.getBoolean("openapi.export")) {
			exportYamlSpec();
		}
	}

	private void exportYamlSpec() throws Exception {
		final MvcResult yamlResult = mockMvc.perform(get("/v3/api-docs.yaml/mobile")).andReturn();
		final Path output = Path.of("docs", "api", "openapi-mobile.yaml");
		Files.createDirectories(output.getParent());
		Files.writeString(output, yamlResult.getResponse().getContentAsString(StandardCharsets.UTF_8),
				StandardCharsets.UTF_8);
	}

}
