package com.nutriconsultas;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Test class to validate Thymeleaf templates during the test lifecycle.
 *
 * <p>
 * This test ensures that all Thymeleaf templates are syntactically correct and can be
 * processed without errors. Template validation runs during the test phase, not during
 * compilation, allowing the application to start even if templates need to be fixed.
 *
 * <p>
 * The validation uses a modular approach where each template or template group has its
 * own validator that defines the required mock model variables. This allows for
 * template-specific validation requirements.
 */
public class ThymeleafTemplateValidationTest {

	private static final String TEMPLATES_DIR = "src/main/resources/templates";

	@Test
	public void validateAllTemplates() {
		ThymeleafValidator validator = new ThymeleafValidator();
		boolean isValid = validator.validateTemplates(TEMPLATES_DIR);
		assertTrue(isValid, "All Thymeleaf templates must be valid. Check the logs for specific errors.");
	}

}
