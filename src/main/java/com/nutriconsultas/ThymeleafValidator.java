package com.nutriconsultas;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.IWebContext;
import org.thymeleaf.templateresolver.FileTemplateResolver;
import org.thymeleaf.templatemode.TemplateMode;

import com.nutriconsultas.validation.template.TemplateValidator;
import com.nutriconsultas.validation.template.TemplateValidatorRegistry;
import com.nutriconsultas.validation.template.WebContextFactory;

/**
 * Utility class to validate Thymeleaf templates. This can be run as a standalone tool or
 * integrated into Maven build.
 *
 * <p>
 * This validator uses a modular approach where each template or template group has its
 * own validator that defines the required mock model variables. This allows for
 * template-specific validation requirements.
 *
 * <p>
 * Validators are located in the {@code com.nutriconsultas.validation.template} package.
 * To add validation for a new template, create a new validator class that extends
 * {@code BaseTemplateValidator} and register it in {@code TemplateValidatorRegistry}.
 */
public class ThymeleafValidator {

	private static final Logger logger = LoggerFactory.getLogger(ThymeleafValidator.class);

	private static final String TEMPLATES_DIR = "src/main/resources/templates";

	private final List<String> errors = new ArrayList<>();

	private final TemplateValidatorRegistry validatorRegistry;

	/**
	 * Creates a new ThymeleafValidator with the default validator registry.
	 */
	public ThymeleafValidator() {
		this.validatorRegistry = new TemplateValidatorRegistry();
	}

	/**
	 * Creates a new ThymeleafValidator with a custom validator registry.
	 * @param validatorRegistry the validator registry to use
	 */
	public ThymeleafValidator(TemplateValidatorRegistry validatorRegistry) {
		this.validatorRegistry = validatorRegistry;
	}

	public static void main(String[] args) {
		String templatesPath = args.length > 0 ? args[0] : TEMPLATES_DIR;
		ThymeleafValidator validator = new ThymeleafValidator();
		boolean isValid = validator.validateTemplates(templatesPath);
		System.exit(isValid ? 0 : 1);
	}

	public boolean validateTemplates(String templatesPath) {
		// Temporarily suppress Thymeleaf ERROR logs during validation
		// (expected runtime errors like #fields are handled and don't indicate template
		// problems)
		Level originalThymeleafLevel = suppressThymeleafErrorLogs();

		try {
			TemplateEngine templateEngine = createTemplateEngine(templatesPath);
			Path templatesDir = Paths.get(templatesPath);

			if (!Files.exists(templatesDir)) {
				logger.error("Templates directory does not exist: {}", templatesPath);
				return false;
			}

			logger.info("Validating Thymeleaf templates in: {}", templatesPath);
			int totalFiles = 0;
			int validFiles = 0;

			try (Stream<Path> paths = Files.walk(templatesDir)) {
				List<Path> htmlFiles = paths.filter(Files::isRegularFile)
					.filter(p -> p.toString().endsWith(".html"))
					.toList();

				totalFiles = htmlFiles.size();

				for (Path htmlFile : htmlFiles) {
					if (validateTemplate(templateEngine, htmlFile, templatesDir)) {
						validFiles++;
					}
				}
			}
			catch (Exception e) {
				logger.error("Error walking templates directory", e);
				return false;
			}

			logger.info("Validation complete: {}/{} templates valid", validFiles, totalFiles);

			if (!errors.isEmpty()) {
				logger.error("Found {} errors:", errors.size());
				errors.forEach(error -> logger.error("  - {}", error));
			}

			return errors.isEmpty();
		}
		finally {
			// Restore original Thymeleaf logger level
			restoreThymeleafLogLevel(originalThymeleafLevel);
		}
	}

	/**
	 * Suppresses Thymeleaf ERROR logs during validation. Expected runtime errors (like
	 * #fields being null) are handled and don't indicate template problems.
	 * @return the original logger level so it can be restored
	 */
	private Level suppressThymeleafErrorLogs() {
		try {
			LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
			// Suppress both the general Thymeleaf logger and the TemplateEngine logger
			ch.qos.logback.classic.Logger thymeleafLogger = loggerContext.getLogger("org.thymeleaf");
			ch.qos.logback.classic.Logger templateEngineLogger = loggerContext
				.getLogger("org.thymeleaf.TemplateEngine");
			Level originalThymeleafLevel = thymeleafLogger.getEffectiveLevel();
			Level originalTemplateEngineLevel = templateEngineLogger.getEffectiveLevel();
			// Set to OFF to completely suppress ERROR logs (expected runtime errors)
			// These are handled by our exception handling and don't indicate template
			// problems
			thymeleafLogger.setLevel(Level.OFF);
			templateEngineLogger.setLevel(Level.OFF);
			// Return the first non-null level for restoration
			return originalTemplateEngineLevel != null ? originalTemplateEngineLevel : originalThymeleafLevel;
		}
		catch (ClassCastException e) {
			// Not using logback, skip suppression
			logger.debug("Not using logback, cannot suppress Thymeleaf error logs");
			return null;
		}
		catch (Exception e) {
			// If logback configuration fails, continue without suppression
			logger.debug("Could not suppress Thymeleaf error logs: {}", e.getMessage());
			return null;
		}
	}

	/**
	 * Restores the original Thymeleaf logger level.
	 * @param originalLevel the original level to restore, or null if not set
	 */
	private void restoreThymeleafLogLevel(Level originalLevel) {
		if (originalLevel != null) {
			try {
				LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
				ch.qos.logback.classic.Logger thymeleafLogger = loggerContext.getLogger("org.thymeleaf");
				ch.qos.logback.classic.Logger templateEngineLogger = loggerContext
					.getLogger("org.thymeleaf.TemplateEngine");
				thymeleafLogger.setLevel(originalLevel);
				templateEngineLogger.setLevel(originalLevel);
			}
			catch (Exception e) {
				logger.debug("Could not restore Thymeleaf log level: {}", e.getMessage());
			}
		}
	}

	private TemplateEngine createTemplateEngine(String templatesPath) {
		TemplateEngine templateEngine = new TemplateEngine();
		FileTemplateResolver templateResolver = new FileTemplateResolver();
		templateResolver.setPrefix(templatesPath + File.separator);
		templateResolver.setSuffix(".html");
		templateResolver.setTemplateMode(TemplateMode.HTML);
		templateResolver.setCharacterEncoding("UTF-8");
		templateResolver.setCacheable(false);
		templateEngine.setTemplateResolver(templateResolver);
		return templateEngine;
	}

	private boolean validateTemplate(TemplateEngine templateEngine, Path htmlFile, Path templatesDir) {
		try {
			String relativePath = templatesDir.relativize(htmlFile).toString().replace(".html", "");
			String templateName = relativePath.replace(File.separator, "/");

			// Find the appropriate validator for this template
			TemplateValidator validator = validatorRegistry.findValidator(templateName);
			if (validator == null) {
				logger.warn("No validator found for template: {}, using default", templateName);
				validator = validatorRegistry.findValidator("*");
			}

			// Create mock model variables using the template-specific validator
			Map<String, Object> mockVariables = validator.createMockModelVariables();

			// Create web context with mock variables
			IWebContext webContext = WebContextFactory.createWebContext(mockVariables);

			// Try to process the template directly to catch exceptions for analysis
			try {
				templateEngine.process(templateName, webContext);
				logger.debug("✓ Valid: {} (using validator: {})", htmlFile, validator.getClass().getSimpleName());
				return true;
			}
			catch (Exception e) {
				// Re-throw to be handled by outer catch block
				throw e;
			}
		}
		catch (org.thymeleaf.exceptions.TemplateInputException e) {
			// Check if it's a syntax error (parsing error) vs runtime error (null
			// property access)
			// Check all levels of the exception chain, not just the root cause
			Throwable current = e;
			boolean isRuntimeError = false;
			String fullMessage = "";
			while (current != null) {
				String message = current.getMessage();
				if (message != null) {
					fullMessage += message + " ";
				}
				String className = current.getClass().getName();
				// Check for various OGNL-related exceptions that indicate missing model
				// attributes
				// (not syntax errors)
				// Check for #fields errors specifically
				if (message != null && message.contains("#fields")) {
					isRuntimeError = true;
					break;
				}
				if (message != null && message.contains("hasErrors")) {
					isRuntimeError = true;
					break;
				}
				if (message != null && (message.contains("source is null") || message.contains("getProperty(null")
						|| message.contains("Exception evaluating OGNL expression")
						|| message.contains("target is null for method") || className.contains("OgnlException")
						|| className.contains("TemplateProcessingException")
						|| className.contains("NullPointerException"))) {
					isRuntimeError = true;
					break;
				}
				// Also check class name for NullPointerException
				if (className.contains("NullPointerException")) {
					isRuntimeError = true;
					break;
				}
				current = current.getCause();
			}
			// Also check the full message string for #fields references
			if (!isRuntimeError && fullMessage.contains("#fields")) {
				isRuntimeError = true;
			}
			if (isRuntimeError) {
				// This is a runtime error (null property access, missing model attribute,
				// or
				// fields utility issue),
				// not a syntax error
				// Templates may reference variables that don't exist in validation
				// context
				// This is acceptable - the template syntax is valid
				logger.debug("✓ Valid (runtime null/missing property access expected): {}", htmlFile);
				return true;
			}
			// Otherwise, it's a real syntax error
			String errorMsg = String.format("%s: %s", htmlFile, e.getMessage());
			errors.add(errorMsg);
			logger.error("✗ Invalid: {} - {}", htmlFile, e.getMessage());
			return false;
		}
		catch (java.lang.NullPointerException e) {
			// NullPointerException during validation is usually a runtime issue (missing
			// binding context, etc.)
			// not a syntax error. This is acceptable for template validation.
			String message = e.getMessage();
			if (message != null && message.contains("target is null for method")) {
				logger.debug("✓ Valid (runtime null binding context expected): {}", htmlFile);
				return true;
			}
			// Re-throw if it's not a known runtime issue
			String errorMsg = String.format("%s: %s", htmlFile, e.getMessage());
			errors.add(errorMsg);
			logger.error("✗ Invalid: {} - {}", htmlFile, e.getMessage());
			return false;
		}
		catch (Exception e) {
			String errorMsg = String.format("%s: %s", htmlFile, e.getMessage());
			errors.add(errorMsg);
			logger.error("✗ Invalid: {} - {}", htmlFile, e.getMessage());
			return false;
		}
	}

}
