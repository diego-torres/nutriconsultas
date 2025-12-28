package com.nutriconsultas;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.templateresolver.FileTemplateResolver;
import org.thymeleaf.templatemode.TemplateMode;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Utility class to validate Thymeleaf templates. This can be run as a standalone tool or
 * integrated into Maven build.
 */
public class ThymeleafValidator {

	private static final Logger logger = LoggerFactory.getLogger(ThymeleafValidator.class);

	private static final String TEMPLATES_DIR = "src/main/resources/templates";

	private static final List<String> errors = new ArrayList<>();

	public static void main(String[] args) {
		String templatesPath = args.length > 0 ? args[0] : TEMPLATES_DIR;
		ThymeleafValidator validator = new ThymeleafValidator();
		boolean isValid = validator.validateTemplates(templatesPath);
		System.exit(isValid ? 0 : 1);
	}

	public boolean validateTemplates(String templatesPath) {
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

			// Try to process the template (this will catch syntax errors)
			templateEngine.process(templateName, new org.thymeleaf.context.Context());

			logger.debug("✓ Valid: {}", htmlFile);
			return true;
		}
		catch (Exception e) {
			String errorMsg = String.format("%s: %s", htmlFile, e.getMessage());
			errors.add(errorMsg);
			logger.error("✗ Invalid: {} - {}", htmlFile, e.getMessage());
			return false;
		}
	}

}
