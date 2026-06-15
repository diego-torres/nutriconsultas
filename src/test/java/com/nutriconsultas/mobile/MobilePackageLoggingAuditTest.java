package com.nutriconsultas.mobile;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/**
 * Static audit for PHI-safe logging in the mobile API package (#115).
 */
class MobilePackageLoggingAuditTest {

	private static final Path MOBILE_SRC = Path.of("src", "main", "java", "com", "nutriconsultas", "mobile");

	private static final Pattern LOG_STATEMENT = Pattern.compile("log\\.(info|debug|warn|error|trace)\\(");

	private static final Pattern BODY_IN_LOG = Pattern.compile("\\b(body|getBody\\(\\)|request\\.body\\(\\))\\b");

	private static final Pattern PACIENTE_PLACEHOLDER = Pattern
		.compile("log\\.(info|debug|warn|error|trace)\\([^)]*\\{[^}]*\\b(paciente|Paciente)\\b");

	private static final Pattern ENTITY_PLACEHOLDER = Pattern.compile(
			"log\\.(info|debug|warn|error|trace)\\([^)]*\\{[^}]*\\b(PatientMessage|CalendarEvent|ClinicalExam|AnthropometricMeasurement|PacienteDieta)\\b");

	private static final Pattern INFO_PHI_ACCESSOR = Pattern.compile(
			"log\\.(info|warn|error)\\([^)]*\\b(getEmail|getNombre|getTelefono|getDireccion|getFechaNacimiento)\\b");

	private static final Pattern INFO_RAW_SUB = Pattern
		.compile("log\\.(info|warn|error)\\([^)]*\\bpatientAuthSub\\b(?!.*LogRedaction\\.redactUserId)");

	@Test
	void mobilePackageHasNoPhiLoggingViolations() throws IOException {
		assertThat(Files.isDirectory(MOBILE_SRC)).as("mobile source directory").isTrue();

		final List<String> violations = new ArrayList<>();
		try (Stream<Path> paths = Files.walk(MOBILE_SRC)) {
			paths.filter(path -> path.toString().endsWith(".java"))
				.filter(path -> !path.getFileName().toString().equals("PhiLogTurboFilter.java"))
				.forEach(path -> scanFile(path, violations));
		}

		assertThat(violations).as("mobile PHI logging violations:\n%s", String.join("\n", violations)).isEmpty();
	}

	private static void scanFile(final Path file, final List<String> violations) {
		try {
			final List<String> lines = Files.readAllLines(file);
			for (int index = 0; index < lines.size(); index++) {
				final String line = lines.get(index).trim();
				if (line.startsWith("//") || line.startsWith("*")) {
					continue;
				}
				if (!LOG_STATEMENT.matcher(line).find()) {
					continue;
				}
				final String location = file + ":" + (index + 1);
				if (BODY_IN_LOG.matcher(line).find() && !line.contains("LogRedaction")) {
					violations.add(location + " — message body must not be logged");
				}
				if (PACIENTE_PLACEHOLDER.matcher(line).find() && !line.contains("LogRedaction")) {
					violations.add(location + " — Paciente must use LogRedaction");
				}
				if (ENTITY_PLACEHOLDER.matcher(line).find() && !line.contains("LogRedaction")) {
					violations.add(location + " — patient entity must use LogRedaction");
				}
				if (INFO_PHI_ACCESSOR.matcher(line).find() && !line.contains("LogRedaction")) {
					violations.add(location + " — PHI accessor must not be logged at INFO+");
				}
				if (INFO_RAW_SUB.matcher(line).find()) {
					violations.add(location + " — patientAuthSub must use LogRedaction.redactUserId at INFO+");
				}
			}
		}
		catch (IOException ex) {
			violations.add(file + " — could not read file: " + ex.getMessage());
		}
	}

}
