package com.nutriconsultas.paciente.invitation;

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
 * Static audit for invitation token logging safety (#141).
 */
class InvitationPackageLoggingAuditTest {

	private static final Path INVITATION_SRC = Path.of("src", "main", "java", "com", "nutriconsultas",
			"paciente", "invitation");

	private static final Pattern LOG_STATEMENT = Pattern.compile("log\\.(info|debug|warn|error|trace)\\(");

	private static final Pattern FORBIDDEN_TOKEN_PLACEHOLDER = Pattern.compile(
			"log\\.(info|warn|error)\\([^)]*\\b(urlToken|rawUrlToken|humanCode|inviteUrl|tokenHash)\\b");

	@Test
	void invitationPackageHasNoTokenLoggingViolations() throws IOException {
		assertThat(Files.isDirectory(INVITATION_SRC)).as("invitation source directory").isTrue();

		final List<String> violations = new ArrayList<>();
		try (Stream<Path> paths = Files.walk(INVITATION_SRC)) {
			paths.filter(path -> path.toString().endsWith(".java")).forEach(path -> scanFile(path, violations));
		}

		assertThat(violations).as("invitation token logging violations:\n%s", String.join("\n", violations)).isEmpty();
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
				if (FORBIDDEN_TOKEN_PLACEHOLDER.matcher(line).find()) {
					violations.add(location + " — raw invitation token material must never be logged");
				}
			}
		}
		catch (IOException ex) {
			violations.add(file + " — could not read file: " + ex.getMessage());
		}
	}

}
