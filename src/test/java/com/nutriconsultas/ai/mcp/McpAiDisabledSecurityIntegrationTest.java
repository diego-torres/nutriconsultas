package com.nutriconsultas.ai.mcp;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * MCP access when {@code nutriconsultas.ai.enabled=false} (#395).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class McpAiDisabledSecurityIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void aiDisabledReturnsServiceUnavailableJsonRpc() throws Exception {
		mockMvc.perform(post("/mcp/nutriconsultas").contentType(MediaType.APPLICATION_JSON).content("""
				{"jsonrpc":"2.0","id":1,"method":"tools/list","params":{}}
				""").with(oidcLogin().idToken(token -> token.subject("auth0|any-user"))))
			.andExpect(status().isServiceUnavailable())
			.andExpect(jsonPath("$.error.code").value(McpJsonRpcResponses.ERROR_UNAVAILABLE));
	}

}
