package com.nutriconsultas.validation.template;

import java.util.Map;

import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.IWebContext;

/**
 * Interface for template-specific validators. Each template or template group can have
 * its own validator that defines the required mock model variables.
 */
public interface TemplateValidator {

	/**
	 * Returns the template path pattern this validator handles. This can be a specific
	 * template path or a pattern (e.g., "sbadmin/pacientes/*").
	 * @return the template path pattern
	 */
	String getTemplatePathPattern();

	/**
	 * Creates mock model variables specific to this template. These variables will be
	 * available in the Thymeleaf context during validation.
	 * @return a map of variable names to mock objects
	 */
	Map<String, Object> createMockModelVariables();

	/**
	 * Validates a specific template. The default implementation processes the template
	 * with the provided context, but validators can override this for custom validation
	 * logic.
	 * @param templateEngine the Thymeleaf template engine
	 * @param templateName the name of the template to validate
	 * @param webContext the web context with mock variables
	 * @return true if the template is valid, false otherwise
	 */
	default boolean validateTemplate(final TemplateEngine templateEngine, final String templateName,
			final IWebContext webContext) {
		boolean result = false;
		try {
			templateEngine.process(templateName, webContext);
			result = true;
		}
		catch (final Exception e) {
			// Return false on exception
		}
		return result;
	}

	/**
	 * Checks if this validator handles the given template path.
	 * @param templatePath the template path to check
	 * @return true if this validator handles the template
	 */
	default boolean handlesTemplate(final String templatePath) {
		final String pattern = getTemplatePathPattern();
		boolean result;
		if (pattern.endsWith("*")) {
			final String prefix = pattern.substring(0, pattern.length() - 1);
			result = templatePath.startsWith(prefix);
		}
		else {
			result = templatePath.equals(pattern);
		}
		return result;
	}

}
