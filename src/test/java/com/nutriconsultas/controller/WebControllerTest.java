package com.nutriconsultas.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Objects;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import com.nutriconsultas.contact.ContactInquiryRepository;
import com.nutriconsultas.subscription.PlanTier;

import lombok.extern.slf4j.Slf4j;

@SpringBootTest
@AutoConfigureMockMvc
@Slf4j
@ActiveProfiles("test")
public class WebControllerTest {

	private static final String TEST_SITE_KEY = "6LeIxAcTAAAAAJcZVRqyHh71UMIEGNQ_MXjiZKhI"; // notsecret

	private static final String TEST_SECRET_KEY = "6LeIxAcTAAAAAGG-vFI1TnRWxMZNFuojJ4WifJWe"; // notsecret

	@DynamicPropertySource
	static void recaptchaTestKeys(final DynamicPropertyRegistry registry) {
		registry.add("recaptcha.site-key", () -> TEST_SITE_KEY);
		registry.add("recaptcha.secret-key", () -> TEST_SECRET_KEY);
	}

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ContactInquiryRepository contactInquiryRepository;

	@Test
	public void testIndex() throws Exception {
		log.info("Starting testIndex");
		mockMvc.perform(MockMvcRequestBuilders.get("/"))
			.andExpect(status().isOk())
			.andExpect(MockMvcResultMatchers.view().name("eterna/index"));
		log.info("Finishing testIndex");
	}

	@Test
	public void testIndexWithTrailingSlash() throws Exception {
		log.info("Starting testIndexWithTrailingSlash");
		mockMvc.perform(MockMvcRequestBuilders.get("/"))
			.andExpect(status().isOk())
			.andExpect(MockMvcResultMatchers.view().name("eterna/index"));
		log.info("Finishing testIndexWithTrailingSlash");
	}

	@Test
	public void testContactPage() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.get("/contact"))
			.andExpect(status().isOk())
			.andExpect(MockMvcResultMatchers.view().name("eterna/contact"));
	}

	@Test
	public void testContactPageWithPlanParam() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.get("/contact").param("plan", "nutriologo-profesional"))
			.andExpect(status().isOk())
			.andExpect(MockMvcResultMatchers.view().name("eterna/contact"))
			.andExpect(MockMvcResultMatchers.model().attribute("selectedPlan", PlanTier.PROFESIONAL))
			.andExpect(MockMvcResultMatchers.model().attribute("selectedPlanDisplayName", "Profesional"));
	}

	@Test
	public void testIndexWithPlanParam() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.get("/").param("plan", "nutriologo-basico"))
			.andExpect(status().isOk())
			.andExpect(MockMvcResultMatchers.view().name("eterna/index"))
			.andExpect(MockMvcResultMatchers.model().attribute("selectedPlan", PlanTier.BASICO));
	}

	@Test
	public void testIndexWithInvalidPlanParam() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.get("/").param("plan", "not-a-plan"))
			.andExpect(status().isOk())
			.andExpect(MockMvcResultMatchers.model().attributeDoesNotExist("selectedPlanDisplayName"));
	}

	@Test
	public void testContactFormSuccessWithPlan() throws Exception {
		final long before = contactInquiryRepository.count();
		mockMvc
			.perform(MockMvcRequestBuilders.post("/contact")
				.contentType(Objects.requireNonNull(MediaType.APPLICATION_FORM_URLENCODED))
				.param("name", "Test User")
				.param("email", "test@example.com")
				.param("subject", "Solicitud de acceso — Plan Plus")
				.param("message", "Test message")
				.param("planRoleSlug", "nutriologo-plus")
				.param("recaptcha-response", "test-token"))
			.andExpect(status().isOk())
			.andExpect(content().string("OK"));
		assertThat(contactInquiryRepository.count()).isEqualTo(before + 1);
		final var saved = contactInquiryRepository.findAll()
			.stream()
			.filter(inquiry -> "test@example.com".equals(inquiry.getEmail()))
			.reduce((first, second) -> second)
			.orElseThrow();
		assertThat(saved.getPlanRoleSlug()).isEqualTo("nutriologo-plus");
	}

	@Test
	public void testContactFormSuccess() throws Exception {
		final long before = contactInquiryRepository.count();
		mockMvc
			.perform(MockMvcRequestBuilders.post("/contact")
				.contentType(Objects.requireNonNull(MediaType.APPLICATION_FORM_URLENCODED))
				.param("name", "Test User")
				.param("email", "test@example.com")
				.param("subject", "Test Subject")
				.param("message", "Test message")
				.param("recaptcha-response", "test-token"))
			.andExpect(status().isOk())
			.andExpect(content().string("OK"));
		assertThat(contactInquiryRepository.count()).isEqualTo(before + 1);
	}

	@Test
	public void testContactFormMissingFields() throws Exception {
		log.info("Starting testContactFormMissingFields");
		mockMvc
			.perform(MockMvcRequestBuilders.post("/contact")
				.contentType(Objects.requireNonNull(MediaType.APPLICATION_FORM_URLENCODED))
				.param("name", "")
				.param("email", "")
				.param("subject", "")
				.param("message", ""))
			.andExpect(status().isBadRequest());
		log.info("Finishing testContactFormMissingFields");
	}

	@Test
	public void testContactFormMissingRecaptcha() throws Exception {
		log.info("Starting testContactFormMissingRecaptcha");
		mockMvc
			.perform(MockMvcRequestBuilders.post("/contact")
				.contentType(Objects.requireNonNull(MediaType.APPLICATION_FORM_URLENCODED))
				.param("name", "Test User")
				.param("email", "test@example.com")
				.param("subject", "Test Subject")
				.param("message", "Test message"))
			.andExpect(status().isBadRequest())
			.andExpect(content().string("Por favor, completa la verificación reCAPTCHA."));
		log.info("Finishing testContactFormMissingRecaptcha");
	}

	@Test
	public void testContactFormInvalidEmail() throws Exception {
		log.info("Starting testContactFormInvalidEmail");
		mockMvc
			.perform(MockMvcRequestBuilders.post("/contact")
				.contentType(Objects.requireNonNull(MediaType.APPLICATION_FORM_URLENCODED))
				.param("name", "Test User")
				.param("email", "invalid-email")
				.param("subject", "Test Subject")
				.param("message", "Test message")
				.param("recaptcha-response", "test-token"))
			.andExpect(status().isBadRequest());
		log.info("Finishing testContactFormInvalidEmail");
	}

}
