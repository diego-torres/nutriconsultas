package com.nutriconsultas.ai.mcp;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nutriconsultas.ai.AiChatService;
import com.nutriconsultas.ai.AiChatThread;
import com.nutriconsultas.subscription.Clinic;
import com.nutriconsultas.subscription.ClinicMember;
import com.nutriconsultas.subscription.ClinicMemberRepository;
import com.nutriconsultas.subscription.ClinicMemberRole;
import com.nutriconsultas.subscription.ClinicRepository;
import com.nutriconsultas.subscription.MembershipStatus;
import com.nutriconsultas.subscription.PlanTier;
import com.nutriconsultas.subscription.Subscription;
import com.nutriconsultas.subscription.SubscriptionRepository;
import com.nutriconsultas.subscription.SubscriptionStatus;

/**
 * MCP endpoint auth, entitlement, and tenant isolation (#395).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = { "nutriconsultas.ai.enabled=true", "nutriconsultas.ai.openai.api-key=test-key",
		"nutriconsultas.ai.openai.model=gpt-test", "nutriconsultas.ai.scope-classifier-enabled=false" })
class McpNutriconsultasSecurityIntegrationTest {

	private static final String NUTRITIONIST_A = "auth0|mcp-security-a";

	private static final String NUTRITIONIST_B = "auth0|mcp-security-b";

	private static final String UNENTITLED_USER = "auth0|mcp-security-unentitled";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private AiChatService chatService;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private SubscriptionRepository subscriptionRepository;

	@Autowired
	private ClinicRepository clinicRepository;

	@Autowired
	private ClinicMemberRepository clinicMemberRepository;

	@Autowired
	private ObjectMapper objectMapper;

	@BeforeEach
	void setUpEntitlements() {
		resetPlusEntitlements(NUTRITIONIST_A);
		resetPlusEntitlements(NUTRITIONIST_B);
		jdbcTemplate.update("DELETE FROM clinic_member WHERE user_id = ?", UNENTITLED_USER);
		jdbcTemplate.update("DELETE FROM clinic WHERE director_user_id = ?", UNENTITLED_USER);
	}

	@Test
	void unauthenticatedMcpRequestIsDenied() throws Exception {
		mockMvc.perform(post("/mcp/nutriconsultas").contentType(MediaType.APPLICATION_JSON).content(toolsListBody()))
			.andExpect(status().is3xxRedirection());
	}

	@Test
	void unentitledUserReceivesForbiddenJsonRpc() throws Exception {
		mockMvc
			.perform(post("/mcp/nutriconsultas").contentType(MediaType.APPLICATION_JSON)
				.content(toolsListBody())
				.with(oidcLogin().idToken(token -> token.subject(UNENTITLED_USER))))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.error.code").value(McpJsonRpcResponses.ERROR_FORBIDDEN));
	}

	@Test
	void entitledUserCanListTools() throws Exception {
		mockMvc
			.perform(post("/mcp/nutriconsultas").contentType(MediaType.APPLICATION_JSON)
				.content(toolsListBody())
				.with(oidcLogin().idToken(token -> token.subject(NUTRITIONIST_A))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.result.tools.length()").value(8));
	}

	@Test
	void crossTenantThreadIdReturnsNotFoundJsonRpc() throws Exception {
		final AiChatThread thread = chatService.startThread(NUTRITIONIST_A, "MCP tenant", null, null, null);

		mockMvc
			.perform(post("/mcp/nutriconsultas").contentType(MediaType.APPLICATION_JSON)
				.content(toolsCallBody("catalog.search_foods", thread.getId()))
				.with(oidcLogin().idToken(token -> token.subject(NUTRITIONIST_B))))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.error.code").value(McpJsonRpcResponses.ERROR_NOT_FOUND));
	}

	private void resetPlusEntitlements(final String userId) {
		jdbcTemplate.update("DELETE FROM clinic_member WHERE user_id = ?", userId);
		jdbcTemplate.update("DELETE FROM clinic WHERE director_user_id = ?", userId);
		provisionPlusSubscription(userId);
	}

	private void provisionPlusSubscription(final String userId) {
		Subscription subscription = new Subscription();
		subscription.setPlanTier(PlanTier.PLUS);
		subscription.setStatus(SubscriptionStatus.ACTIVE);
		subscription.setPaymentExempt(true);
		subscription = subscriptionRepository.saveAndFlush(subscription);

		final Clinic clinic = new Clinic();
		clinic.setName("Clinic " + userId);
		clinic.setDirectorUserId(userId);
		clinic.setSubscription(subscription);
		final Clinic savedClinic = clinicRepository.saveAndFlush(clinic);

		final ClinicMember member = new ClinicMember();
		member.setClinic(savedClinic);
		member.setUserId(userId);
		member.setRole(ClinicMemberRole.DIRECTOR);
		member.setMembershipStatus(MembershipStatus.ACTIVE);
		clinicMemberRepository.saveAndFlush(member);
	}

	private String toolsListBody() throws Exception {
		return objectMapper.writeValueAsString(
				java.util.Map.of("jsonrpc", "2.0", "id", 1, "method", "tools/list", "params", java.util.Map.of()));
	}

	private String toolsCallBody(final String toolName, final long threadId) throws Exception {
		return objectMapper.writeValueAsString(java.util.Map.of("jsonrpc", "2.0", "id", 2, "method", "tools/call",
				"params", java.util.Map.of("name", toolName, "arguments", java.util.Map.of("query", "avena"), "_meta",
						java.util.Map.of("threadId", threadId))));
	}

}
