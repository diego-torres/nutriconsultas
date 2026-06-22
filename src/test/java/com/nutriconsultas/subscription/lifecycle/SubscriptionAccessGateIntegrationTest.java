package com.nutriconsultas.subscription.lifecycle;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SubscriptionAccessGateIntegrationTest {

	private static final String UNINVITED_USER = "auth0|self-registered-user";

	@DynamicPropertySource
	static void enableAccessGate(final DynamicPropertyRegistry registry) {
		registry.add("nutriconsultas.subscription.enforce-nutritionist-access", () -> "true");
	}

	@Autowired
	private MockMvc mockMvc;

	@Test
	void uninvitedAuthenticatedUserCannotOpenAdminDashboard() throws Exception {
		mockMvc.perform(get("/admin").with(oidcLogin().idToken(token -> token.subject(UNINVITED_USER))))
			.andExpect(status().is3xxRedirection())
			.andExpect(redirectedUrl("/admin/subscription/access-denied"));
	}

	@Test
	void accessDeniedPageIsReachableForUninvitedUser() throws Exception {
		mockMvc
			.perform(get("/admin/subscription/access-denied")
				.with(oidcLogin().idToken(token -> token.subject(UNINVITED_USER))))
			.andExpect(status().isOk())
			.andExpect(view().name("sbadmin/subscription/access-denied"));
	}

}
