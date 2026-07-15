package com.nutriconsultas.paciente;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

/**
 * Verifies the dd/mm/yyyy DOB input mask used on patient create/edit forms.
 */
class DateOfBirthMaskScriptTest {

	static boolean isNodeAvailable() {
		try {
			final Process process = new ProcessBuilder("node", "--version").start();
			return process.waitFor(5, TimeUnit.SECONDS) && process.exitValue() == 0;
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			return false;
		}
		catch (Exception ex) {
			return false;
		}
	}

	@Test
	void maskScriptAndForms_includeAutoSlashSupport() throws Exception {
		final Path maskScript = Path.of("src/main/resources/static/sbadmin/js/date-of-birth-mask.js");
		final Path nuevo = Path.of("src/main/resources/templates/sbadmin/pacientes/nuevo.html");
		final Path afiliacion = Path.of("src/main/resources/templates/sbadmin/pacientes/afiliacion.html");

		assertThat(maskScript).exists();
		assertThat(nuevo).exists();
		assertThat(afiliacion).exists();

		final String script = Files.readString(maskScript, StandardCharsets.UTF_8);
		assertThat(script).contains("function format(");
		assertThat(script).contains("data-dob-mask");
		assertThat(script).contains("replace(/\\D/g");

		final String nuevoHtml = Files.readString(nuevo, StandardCharsets.UTF_8);
		assertThat(nuevoHtml).contains("data-dob-mask");
		assertThat(nuevoHtml).contains("date-of-birth-mask.js");
		assertThat(nuevoHtml).contains("maxlength=\"10\"");

		final String afiliacionHtml = Files.readString(afiliacion, StandardCharsets.UTF_8);
		assertThat(afiliacionHtml).contains("data-dob-mask");
		assertThat(afiliacionHtml).contains("date-of-birth-mask.js");
		assertThat(afiliacionHtml).contains("maxlength=\"10\"");
	}

	@Test
	@EnabledIf("isNodeAvailable")
	void format_insertsSlashesWhileTypingDigits() throws Exception {
		final Path script = Path.of("scripts/test-date-of-birth-mask.cjs").toAbsolutePath();
		final Process process = new ProcessBuilder("node", script.toString())
			.directory(Path.of("").toAbsolutePath().toFile())
			.redirectErrorStream(true)
			.start();
		final boolean finished = process.waitFor(30, TimeUnit.SECONDS);
		final String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();

		assertThat(finished).as("node mask script timed out").isTrue();
		assertThat(process.exitValue()).as("node mask script failed: %s", output).isZero();
		assertThat(output).isEqualTo("ok");
	}

}
